# semgravio-admin

管理后台网关后端服务。

## 包含的限界上下文

- **admin** - 管理员、角色、权限、菜单管理

## 技术栈

- Java 21 / Spring Boot 3.4.x / Maven
- cartisan-boot 框架
- PostgreSQL / Redis

## 开发

```bash
# 编译
mvn compile

# 测试
mvn test

# 运行
mvn spring-boot:run
```

## 部署

```bash
# 构建并制作发布包（产出可执行 fat JAR，记录 Git revision）
deploy/build-bundle.sh

# 一键发布（连接参数见 docs/deploy/release.md）
deploy/release.sh

# 服务端运维：status/logs/restart/dump/smoke/sh
deploy/server.sh status

# 发布前冒烟（隔离 PG/Redis 验证镜像可启动、健康检查 UP）
./scripts/smoke-test.sh

# Compose 渲染与变量契约检查（不改动运行态）
./scripts/check-compose.sh

# 首装时在应用根目录直接启动
docker compose -f deploy/server.compose.yml --env-file .env.production up -d --build
```

首装（共享 infra 建库建号、环境配置、健康验证）见 [docs/deploy/first-install.md](docs/deploy/first-install.md)。
日常升级发布与服务器运维（打包/SSH 一键换版/备份/冒烟）见 [docs/deploy/release.md](docs/deploy/release.md)。
admin-web 反代目标：`http://semgravio-admin:8081`，健康检查 `/actuator/health`（仅 webnet 内解析）。
