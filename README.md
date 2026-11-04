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
# 构建（产出可执行 fat JAR：target/semgravio-admin.jar，稳定产物名由 pom <finalName> 唯一定义）
mvn package -DskipTests

# 发布前冒烟（隔离 PG/Redis 验证镜像可启动、健康检查 UP）
./scripts/smoke-test.sh

# Docker 部署（deploy.sh 会校验 fat JAR 并自动补构建，亦可直接用 compose）
./deploy.sh
# 或：docker-compose -f docker-compose.prod.yml --env-file .env.production up -d
```
