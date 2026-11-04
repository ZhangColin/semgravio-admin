# 日常发布与运维：开发机发布包、SSH 一键换版、服务端运维

> 适用工单：#117（父规格 #114）。**首次部署**（共享 infra 建库建号、环境配置、
> 首次启动）见 [first-install.md](first-install.md)；冒烟与完整 runbook 见 #118。
> 本文档覆盖升级发布与服务端日常运维。

## 交付结论（先读这个）

- 发布体验与 aiplatform 同款：开发机 Git Bash 一条命令完成
  **打包 → 上传 → 服务器备份/解包/重建/健康等待/验收**。
- 发布对共享 infra **只读**：只检查 `semgravio-pg`/`semgravio-redis`/`webnet`
  健康与远端环境配置存在，绝不启动/停止/重建共享 PG、Redis、nginx。
- 重复发布**不覆盖**服务器已有 `.env.production`，既有数据源不会被自动切换。
- admin-web 反代目标固定 `http://semgravio-admin:8081`，健康检查
  `GET /actuator/health`（仅 webnet 内解析，宿主不发布端口）。

## 连接参数（五个必填，脚本无任何现网默认值）

| 环境变量 | 含义 | 示例 |
|---|---|---|
| `SERVER_HOST` | 服务器地址 | `1.2.3.4` |
| `SERVER_USER` | SSH 登录用户 | `ubuntu` |
| `SSH_KEY` | 私钥路径（开发机侧） | `$HOME/ssh/<主机>.pem` |
| `REMOTE_DIR` | 远端应用目录 | `semgravio-admin`（相对 `$HOME`，绝对路径以 `/` 开头也可） |
| `INFRA_DIR` | 共享 infra 目录 | `semgravio-infra`（仅做前置健康检查） |

## 一键换版

```bash
SERVER_HOST=... SERVER_USER=... SSH_KEY=... REMOTE_DIR=... INFRA_DIR=... \
  ./scripts/release/release.sh
```

流程（任一步失败即非零退出，**后续步骤不执行**）：

1. **前置检查（开发机）**：私钥存在、SSH 免密连通（BatchMode）。
2. **打包（开发机）**：见下一节；`SKIP_BUILD=1` 复用 `dist/` 最新发布包跳过构建。
3. **上传**：scp 到服务器 `$HOME`。
4. **服务器侧**：
   - infra 前置检查——docker/compose v2、`webnet`、`semgravio-pg`/`semgravio-redis`
     healthy、`$REMOTE_DIR/.env.production` 存在；**任何缺失即中止，不重建现有应用**。
   - **备份**（见下节）→ 解包（不含 `.env*`，现网 env 不被覆盖）→
     打印 REVISION → `docker compose up -d --build` 重建单实例容器。
   - **健康等待**：容器内轮询 `/actuator/health` 最长 180s（期间 Flyway 迁移属正常）。
   - **验收**：Actuator 返回 `{"status":"UP"}` 且 `/v3/api-docs` HTTP 200。
5. 完成输出容器状态表。

## 发布包（单独打包）

```bash
./scripts/release/build-bundle.sh        # 产出 dist/semgravio-admin-deploy-<时间戳>.tar.gz
ALLOW_DIRTY=1 ./scripts/release/build-bundle.sh   # 显式允许带脏打包（不推荐）
```

- **默认拒绝脏工作区**：源码树按 `git archive HEAD` 导出（恒 LF，Windows 上不带
  `\r`），与 jar 的构建基线严格一致。
- **内容**：已提交源码正本＋部署文件（`Dockerfile`/`docker-compose.prod.yml`/
  `deploy.sh`/`scripts/`/`docs/`）＋`target/semgravio-admin.jar`＋`REVISION`。
- **排除**：`.env.production`、私钥/证书、备份、临时产物——均未被 git 跟踪，
  `git archive` 天然排除；已提交文件命中敏感模式（`.env*`（非 example）/
  `.pem/.key/.p12/.jks`/`backup-*`）则**拒绝打包**。

## 备份与升级检测

- 仅当 `semgravio_admin` 库已存在（`psql` 可连接）才在替换容器前生成
  **`pg_dump` 压缩备份**：`~/backup-admin-<YYYY-MM-DD-HHMM>.sql.gz`。
- 首装（库不存在）**不生成备份、不伪造旧版本备份**。
- `SKIP_BACKUP=1` 可跳过备份（应急开关，慎用）。

## 服务端日常运维

```bash
# 连接参数同发布（SERVER_HOST/SERVER_USER/SSH_KEY）
./scripts/release/ops.sh status    # admin 容器概览（状态/镜像）
./scripts/release/ops.sh logs      # 跟日志（--tail 200）
./scripts/release/ops.sh restart   # 重启应用容器（不重建镜像、不动 infra）
./scripts/release/ops.sh dump      # pg_dump → ~/backup-admin-<时间>.sql.gz
./scripts/release/ops.sh smoke     # 冒烟：Actuator UP ＋ OpenAPI 可达
./scripts/release/ops.sh sh        # 登服务器交互 shell
```

- 运维入口**只面向 admin 应用容器**：不提供共享 infra（PG/Redis/nginx）或
  网关操作；infra 运维归 aiplatform 部署单元的 `deploy/infra/`。
- 宿主上没有 8081 端口，冒烟一律在容器内探测。

## 失败排查与人工恢复边界

| 现象 | 排查 |
|---|---|
| 前置检查不过（infra 不健康/webnet 缺失/远端 env 缺失） | 先在服务器起 infra：`cd $INFRA_DIR && ./deploy.sh`；首装未完成则按 first-install.md 补齐，修好后重跑发布（现有应用未被触碰） |
| 健康等待 180s 超时 | 现场保留，未回滚：`./scripts/release/ops.sh status`、`./scripts/release/ops.sh logs`；常见原因是 Flyway 迁移慢或 `.env.production` 凭据错误 |
| OpenAPI 验收非 200 | 同上看应用日志；`/v3/api-docs` 在安全白名单内，非鉴权问题 |

**人工恢复边界（务必知悉）**：

- 发布失败**不会自动恢复数据库、不会自动回滚容器**——现场（容器、日志、备份）
  全部保留，恢复是**人工决策**。
- 备份是升级前的数据快照，`gunzip -c <备份>.sql.gz | psql …` 手工导入属于
  人工恢复动作；**备份≠完整回滚**（不覆盖镜像回退、配置回滚、下游域状态）。
- 发布不自动建库/换库/迁移旧数据；数据库身份由 infra helper 管理。

## 本文档明确不包含

nginx 配置、域名、证书、80/443 端口、共享 PG/Redis/nginx 的生命周期操作、
数据库超管凭据——分别归 admin-web / infra / 各自的专项 SOP。
