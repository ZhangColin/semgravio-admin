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

唯一部署正本：[deploy/部署文档.md](deploy/部署文档.md)（首装、日常发布、运维、备份恢复、secret 同步、验收）。

```bash
# 一键换版：打包 → 上传 → 服务器前置检查/备份/重建/健康验收（连接参数见部署文档）
deploy/release.sh

# 服务端运维：status/logs/restart/dump/smoke/sh
deploy/server.sh status

# 本地黑盒排练：静态契约＋打包＋发布行为＋隔离首装全链路（改部署面后回归）
deploy/rehearsal.sh
```

admin-web 反代目标：`http://semgravio-admin:8081`，健康检查 `/actuator/health`（仅共享 webnet 内解析，宿主不发布端口）。
