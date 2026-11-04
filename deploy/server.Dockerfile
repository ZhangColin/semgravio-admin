# 管理后台网关 - 生产环境 Dockerfile
#
# 前提：本地已执行 mvn package（deploy/build-bundle.sh 会自动构建）。
# 直接复制 Maven 产物构建镜像——构建环境内无需 Maven、Docker CLI 或基础设施组件。
# 产物名由 pom.xml <finalName> 唯一定义（发布基线 #115），此处仅消费同一约定。
#
# 发布黑盒排练：deploy/rehearsal.sh（隔离 PG/Redis 验证健康边界，勿以镜像构建成功冒充应用可运行）

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 时区设置
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制 Maven 构建好的 fat JAR（与 pom.xml finalName 同约定）
ARG JAR_FILE=target/semgravio-admin.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8081

ENTRYPOINT ["java", "--enable-preview", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
