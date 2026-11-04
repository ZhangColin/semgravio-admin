# 首装接入 runbook：共享 infra 配置与单实例健康服务

> 适用工单：#116（父规格 #114）。日常升级发布见 [release.md](release.md)（#117），完整运维
> runbook 与隔离黑盒排练见 #118。本文档只覆盖**首次部署**。

## 交付结论（先读这个）

- admin-web 的反向代理目标固定为 **`http://semgravio-admin:8081`**，健康检查路径
  **`GET /actuator/health`**。该地址只在共享 `webnet` 内解析，宿主不发布 8081。
- 本应用**不提供 nginx**、不管理共享 PG/Redis/nginx 生命周期、不做域名/证书。
- 容器名 = Docker DNS 名 = 镜像名 = Compose 项目名 = `semgravio-admin`。

## 前置条件

- 服务器已完成 infra 一键起栈（aiplatform 部署单元的 `deploy/infra/`）：
  `semgravio-pg` / `semgravio-redis` healthy，`webnet` 已存在。
- 服务器已有 Docker 与 Compose v2 插件。
- 本文所有命令在**服务器**上执行；建库建号命令在 infra 目录（`deploy/infra/`）执行。

## 1. 准备应用目录与环境配置

```bash
# 应用目录（路径自定，与 aiplatform 部署单元平级即可）
mkdir -p /opt/semgravio-admin && cd /opt/semgravio-admin

# 从发布包/仓库取得 deploy/ 部署目录，在应用根目录准备真实环境文件
cp deploy/env.server.example .env.production
chmod 600 .env.production
```

## 2. 建库建号（infra 官方 helper，唯一合法通道）

应用只拿**分片凭据**：PG 超管密码不出 infra，本仓不复制建库 SQL，
发布脚本不自动建库、不迁移旧数据。

```bash
cd <infra 目录>/deploy/infra/
DB_PASSWORD=$(openssl rand -hex 16) ./ensure-db-user.sh semgravio_admin
```

输出即凭据分片（幂等：重复执行须带同一个 `DB_PASSWORD`，换新值即对齐改密）：

```
DB_HOST=semgravio-pg
DB_PORT=5432
DB_NAME=semgravio_admin
DB_USER=semgravio_admin
DB_PASSWORD=...
```

把分片抄入 `/opt/semgravio-admin/.env.production` 的 PG 三项。

> ⚠ 不读取、不下发、不打印 PG 超管密码；不要手工 `CREATE DATABASE`——权限边界
> （低权限角色、CONNECT 收回）由 helper 统一重放，手工建库会漏。

## 3. 填写其余环境配置

`.env.production` 全量口径见 `deploy/env.server.example` 注释：

| 变量 | 来源 |
|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | 第 2 步 helper 分片（库=角色=`semgravio_admin`，host=`semgravio-pg`） |
| `REDIS_HOST` / `REDIS_PORT` | `semgravio-redis` / `6379`（共享 infra 契约） |
| `REDIS_PASSWORD` | infra 运维按 `deploy/infra/.env` 正本下发的分片（与正本 `REDIS_PASSWORD` 同值） |
| `ADMIN_API_SECRET` | app-registry 启动日志 `admin-console seed created, apiSecret: ...`（一次性抄录） |
| 四个下游地址 | 模板默认值即 webnet 容器名直连，特殊拓扑才覆盖 |

## 4. 启动与验证

```bash
cd /opt/semgravio-admin
./scripts/check-compose.sh          # 渲染与变量契约检查（不改动运行态）
docker compose -f deploy/server.compose.yml --env-file .env.production up -d --build
docker inspect --format='{{.State.Health.Status}}' semgravio-admin   # 等待变为 healthy
```

- 健康：`docker exec semgravio-admin wget -qO- http://localhost:8081/actuator/health`
  返回 `{"status":"UP"}`。
- webnet 内连通（admin-web 反代目标形态）：
  任一 webnet 容器内 `wget -qO- http://semgravio-admin:8081/actuator/health`。

## 5. 首登与破窗账号

1. Flyway 负责完整迁移链（含内置 Operator 种子），JPA 不接管 schema。
2. 用内置破窗账号登录后**立即修改初始密码**（ADR-0003：保留 ID=1，只可改密、
   不可禁用/删除）。

## 重复启动与升级安全

- 重复 `up -d` / 后续发布解包**不会覆盖**服务器已有 `.env.production`，
  既有数据源不会被自动切换；模板新增变量只在文档提示核对，不做静默合并。
- 本应用 Compose 只加入 external `webnet`，`down` 不删共享网络与卷；
  不启动/停止/重建共享 PG、Redis、nginx。
- 升级发布（SSH 一键换版、数据库备份、服务端运维命令）见 [release.md](release.md)；
  冒烟与完整 runbook 见 #118。

## 本文档明确不包含

nginx 配置、域名、证书、80/443 端口、共享 PG/Redis/nginx 的生命周期操作、
旧 `semgravio` 数据库迁移——分别归 admin-web / infra / 各自的专项 SOP。
