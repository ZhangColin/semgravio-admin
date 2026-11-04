#!/usr/bin/env bash
# semgravio-admin Compose 渲染与变量契约检查（#116）
#
# 验证首装接入契约，不改任何运行态：
#   1. 用 deploy/env.server.example 渲染 deploy/server.compose.yml 成功
#      （compose 中 ${VAR:?必填} 语法使缺失变量在渲染期即响亮失败）
#   2. 单实例形态：显式项目名/镜像名/容器名 = semgravio-admin
#   3. 不映射宿主端口（8081 只存在于 webnet 内）
#   4. webnet 为 external：不创建网络、不引入其他应用组件
#   5. 健康检查路径为 /actuator/health 且 start_period 覆盖启动宽限期
#   6. 变量契约：compose 引用的每个变量在模板中都有声明（模板与注入变量一致）
#
# 用法：./scripts/check-compose.sh
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE_FILE="deploy/server.compose.yml"
ENV_EXAMPLE="deploy/env.server.example"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'
log()  { echo -e "${BLUE}[check]${NC} $*"; }
ok()   { echo -e "${GREEN}[check] ✓ $*${NC}"; }
fail() { echo -e "${RED}[check] ✗ $*${NC}"; exit 1; }

command -v docker >/dev/null 2>&1 || fail "docker 不可用"
docker compose version >/dev/null 2>&1 || fail "docker compose v2 插件不可用"
[ -f "$COMPOSE_FILE" ] || fail "缺少 $COMPOSE_FILE"
[ -f "$ENV_EXAMPLE" ] || fail "缺少 $ENV_EXAMPLE"

# ---------------------------------------------------------------- 1. 渲染
log "渲染 $COMPOSE_FILE（--env-file $ENV_EXAMPLE）"
RENDERED="$(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_EXAMPLE" config 2>&1)" \
  || fail "Compose 渲染失败：\n$RENDERED"
ok "渲染成功"

# ---------------------------------------------------------------- 2. 单实例形态断言
echo "$RENDERED" | grep -q '^name: semgravio-admin$' \
  || fail "项目名未显式指定为 semgravio-admin"
echo "$RENDERED" | grep -q 'container_name: semgravio-admin' \
  || fail "容器名未显式指定为 semgravio-admin"
echo "$RENDERED" | grep -qE 'image: semgravio-admin(:[a-z0-9._-]+)?$' \
  || fail "镜像名未显式指定为 semgravio-admin 专属值"
ok "项目名/镜像名/容器名均为 semgravio-admin"

# ---------------------------------------------------------------- 3. 无宿主端口
if echo "$RENDERED" | grep -q 'published:'; then
  fail "存在宿主端口映射（published），8081 不得发布到宿主"
fi
ok "无宿主端口映射"

# ---------------------------------------------------------------- 4. external webnet
echo "$RENDERED" | grep -q 'webnet:$' || fail "未加入 webnet 网络"
echo "$RENDERED" | grep -A2 'webnet:$' | grep -q 'external: true' \
  || fail "webnet 未声明为 external（应用不得创建共享网络）"
ok "webnet 为 external"

# ---------------------------------------------------------------- 5. 健康检查
echo "$RENDERED" | grep -q 'test:' || fail "缺少 healthcheck"
echo "$RENDERED" | grep -q '/actuator/health' \
  || fail "健康检查路径不是 /actuator/health"
# start_period 渲染后可能是 60s 或 1m0s 形态，归一化为秒再比较
START_PERIOD="$(echo "$RENDERED" | grep -oE 'start_period: [0-9smh]+' | head -1 | cut -d' ' -f2)"
SP_SECONDS=0
if [[ "$START_PERIOD" == *m* ]]; then
  MIN="${START_PERIOD%m*}"; SEC="${START_PERIOD#*m}"; SEC="${SEC%s}"
  SP_SECONDS=$(( MIN * 60 + ${SEC:-0} ))
elif [[ "$START_PERIOD" == *s* ]]; then
  SP_SECONDS="${START_PERIOD%s}"
fi
[ "$SP_SECONDS" -ge 60 ] || fail "start_period=${START_PERIOD:-缺失}，未覆盖启动宽限期（应 >= 60s）"
ok "健康检查 /actuator/health，start_period=$START_PERIOD（${SP_SECONDS}s）"

# ---------------------------------------------------------------- 6. 变量契约
# compose 引用的全部变量（剥掉 :- 默认与 :? 必填语法）
COMPOSE_VARS="$(grep -oE '\$\{[A-Z_][A-Z0-9_]*' "$COMPOSE_FILE" | sed 's/^${//' | sort -u)"
# 模板声明的变量（去注释与空行取 key）
TEMPLATE_VARS="$(grep -vE '^\s*(#|$)' "$ENV_EXAMPLE" | cut -d= -f1 | tr -d ' ' | sort -u)"
MISSING=0
for var in $COMPOSE_VARS; do
  echo "$TEMPLATE_VARS" | grep -qx "$var" || { fail "compose 引用的 $var 未在 $ENV_EXAMPLE 模板中声明（模板与注入变量须一致）"; }
done
# SPRING_PROFILES_ACTIVE 由 compose 内联写死，不在模板中——反向检查只覆盖模板其余变量
for var in $TEMPLATE_VARS; do
  echo "$COMPOSE_VARS" | grep -qx "$var" || { fail "模板声明的 $var 未被 compose 注入（模板与注入变量须一致）"; }
done
ok "变量契约一致（$(echo "$COMPOSE_VARS" | wc -l | tr -d ' ') 项）"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Compose 渲染与变量契约检查全部通过${NC}"
echo -e "${GREEN}========================================${NC}"
