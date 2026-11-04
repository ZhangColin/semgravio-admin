#!/bin/bash
# semgravio-admin 发布冒烟测试
#
# 验证发布基线（issue #115）：
#   1. Maven package 产出可执行 fat JAR（含 Spring Boot 启动清单，非瘦 JAR）
#   2. Dockerfile 直接复制该产物构建镜像（镜像构建环境内无需 Maven）
#   3. 在隔离的 PG/Redis 环境中启动容器，/actuator/health 返回 UP
#
# 用法：./scripts/smoke-test.sh
# 失败时输出真实错误并保留退出码，不用“镜像构建成功”冒充“应用可运行”。

set -euo pipefail

cd "$(dirname "$0")/.."

JAR_FILE="target/semgravio-admin.jar"
IMAGE_TAG="semgravio-admin:smoketest"
NETWORK="semgravio-admin-smoketest"
PG_CONTAINER="smoketest-admin-pg"
REDIS_CONTAINER="smoketest-admin-redis"
APP_CONTAINER="smoketest-admin-app"
HOST_PORT="18081"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()  { echo -e "${BLUE}[smoke]${NC} $*"; }
ok()   { echo -e "${GREEN}[smoke] ✓ $*${NC}"; }
fail() { echo -e "${RED}[smoke] ✗ $*${NC}"; }

# 清理函数：无论成败都拆掉隔离环境
cleanup() {
  log "清理冒烟环境..."
  docker rm -f "$APP_CONTAINER" "$PG_CONTAINER" "$REDIS_CONTAINER" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
  docker image rm "$IMAGE_TAG" >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail_step() {
  fail "$1"
  exit 1
}

# ---------------------------------------------------------------- 1. 打包
log "步骤 1/4：Maven package（跳过测试，由 mvn verify 兜底质量）"
mvn -q -DskipTests package || fail_step "Maven package 失败，以上输出为真实构建错误"

# ---------------------------------------------------------------- 2. 校验 fat JAR
log "步骤 2/4：校验 $JAR_FILE 是可执行 fat JAR"
[ -f "$JAR_FILE" ] || fail_step "未找到 $JAR_FILE —— 稳定产物名约定被破坏（pom finalName 应为 semgravio-admin）"

MANIFEST=""
if command -v unzip >/dev/null 2>&1; then
  MANIFEST="$(unzip -p "$JAR_FILE" META-INF/MANIFEST.MF 2>/dev/null || true)"
elif command -v jar >/dev/null 2>&1; then
  TMPDIR_MANIFEST="$(mktemp -d)"
  (cd "$TMPDIR_MANIFEST" && jar xf "$(pwd)/$JAR_FILE" META-INF/MANIFEST.MF 2>/dev/null) || true
  MANIFEST="$(cat "$TMPDIR_MANIFEST/META-INF/MANIFEST.MF" 2>/dev/null || true)"
  rm -rf "$TMPDIR_MANIFEST"
fi

[[ "$MANIFEST" == *"Start-Class: com.semgravio.AdminApplication"* ]] \
  || fail_step "$JAR_FILE 是瘦 JAR（无 Spring Boot Start-Class 清单），java -jar 无法启动；请检查 spring-boot-maven-plugin repackage 是否生效"
# 先落盘再模式匹配：避免 grep -q 提前命中致 unzip 吃 SIGPIPE、pipefail 误判
JAR_LISTING="$(unzip -l "$JAR_FILE" 2>/dev/null || true)"
[[ "$JAR_LISTING" == *"BOOT-INF/lib/"* ]] \
  || fail_step "$JAR_FILE 不含 BOOT-INF/lib/ 运行依赖，不是 fat JAR"
JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
ok "fat JAR: $JAR_FILE ($JAR_SIZE)"

# ---------------------------------------------------------------- 3. 构建镜像
log "步骤 3/4：docker build（仅复制 Maven 产物，构建环境无需 Maven）"
docker build -q -t "$IMAGE_TAG" . >/dev/null \
  || fail_step "镜像构建失败，以上输出为真实构建错误"
ok "镜像 $IMAGE_TAG 构建完成"

# ---------------------------------------------------------------- 4. 隔离环境启动 + 健康检查
log "步骤 4/4：隔离 PG/Redis 启动容器并检查 /actuator/health"
docker network create "$NETWORK" >/dev/null

docker run -d --name "$PG_CONTAINER" --network "$NETWORK" \
  -e POSTGRES_USER=semgravio -e POSTGRES_PASSWORD=smoketest -e POSTGRES_DB=semgravio \
  postgres:16-alpine >/dev/null
docker run -d --name "$REDIS_CONTAINER" --network "$NETWORK" redis:7-alpine \
  --requirepass smoketest >/dev/null

for i in $(seq 1 30); do
  if docker exec "$PG_CONTAINER" pg_isready -U semgravio -d semgravio >/dev/null 2>&1; then
    break
  fi
  [ "$i" -eq 30 ] && fail_step "隔离 PG 未就绪（pg_isready 超时）"
  sleep 1
done
ok "隔离 PG 就绪"

docker run -d --name "$APP_CONTAINER" --network "$NETWORK" -p "${HOST_PORT}:8081" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_CONTAINER}:5432/semgravio" \
  -e SPRING_DATASOURCE_USERNAME=semgravio \
  -e SPRING_DATASOURCE_PASSWORD=smoketest \
  -e REDIS_HOST="$REDIS_CONTAINER" \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=smoketest \
  -e ADMIN_API_SECRET=smoketest-secret \
  "$IMAGE_TAG" >/dev/null

HEALTH=""
for i in $(seq 1 60); do
  HEALTH="$(curl -fsS "http://localhost:${HOST_PORT}/actuator/health" 2>/dev/null || true)"
  if [[ "$HEALTH" == *'"status":"UP"'* ]]; then
    break
  fi
  RUNNING="$(docker ps --format '{{.Names}}' 2>/dev/null || true)"
  if [[ "$RUNNING" != *"$APP_CONTAINER"* ]]; then
    fail_step "应用容器已退出，真实启动错误如下：\n$(docker logs "$APP_CONTAINER" 2>&1 | tail -30)"
  fi
  [ "$i" -eq 60 ] && fail_step "健康检查超时（120s），/actuator/health 最后返回: '${HEALTH:-<无响应>}'；容器日志尾部：\n$(docker logs "$APP_CONTAINER" 2>&1 | tail -30)"
  sleep 2
done

ok "健康检查 UP: $HEALTH"
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  发布冒烟全部通过：fat JAR → 镜像 → 启动 → UP${NC}"
echo -e "${GREEN}========================================${NC}"
