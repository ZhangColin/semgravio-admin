#!/usr/bin/env bash
# 发布的服务器侧动作：infra 前置检查 → 备份 → 解包 → 重建单实例 → 健康等待 → 验收。
# 由 deploy/release.sh 经 SSH 灌入服务器执行；deploy/rehearsal.sh 在隔离环境直接驱动
# 同一正本做黑盒排练——两处行为永不劈叉。
#
# 必填（经环境变量传入）：
#   BUNDLE_FN    发布包文件名（scp 落在 $HOME）
#   REMOTE_DIR   应用目录（相对 $HOME 或绝对路径）
#   INFRA_DIR    共享 infra 目录（仅用于失败提示，不做任何生命周期操作）
#   SKIP_BACKUP  =1 跳过部署前 pg_dump
# 部署目标名默认＝生产契约，可经环境变量覆盖（deploy/rehearsal.sh 借此把同一份动作
# 跑在测试专属容器/网络/超时上；生产发布不传即全默认）：
#   PG_CONTAINER(semgravio-pg) REDIS_CONTAINER(semgravio-redis) WEBNET(webnet)
#   APP_CONTAINER(semgravio-admin) DB_NAME/DB_USER(semgravio_admin) HEALTH_TIMEOUT(180s)
#
# 前置检查不过 → 非零退出且不重建现有应用；健康超时/验收失败 → 非零退出、保留容器
# 现场、不自动恢复数据库（备份≠回滚，人工恢复边界见 deploy/部署文档.md）。
set -euo pipefail

: "${BUNDLE_FN:?BUNDLE_FN 必填——发布包文件名}"
: "${REMOTE_DIR:?REMOTE_DIR 必填——远端应用目录}"
: "${INFRA_DIR:?INFRA_DIR 必填——共享 infra 目录}"
SKIP_BACKUP="${SKIP_BACKUP:-0}"
PG_CONTAINER="${PG_CONTAINER:-semgravio-pg}"
REDIS_CONTAINER="${REDIS_CONTAINER:-semgravio-redis}"
WEBNET="${WEBNET:-webnet}"
APP_CONTAINER="${APP_CONTAINER:-semgravio-admin}"
DB_NAME="${DB_NAME:-semgravio_admin}"
DB_USER="${DB_USER:-semgravio_admin}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"

if [ "${REMOTE_DIR:0:1}" = "/" ]; then APP_DIR="$REMOTE_DIR"; else APP_DIR="$HOME/$REMOTE_DIR"; fi
if [ "${INFRA_DIR:0:1}" = "/" ]; then INFRA_PATH="$INFRA_DIR"; else INFRA_PATH="$HOME/$INFRA_DIR"; fi

echo "-- infra 前置检查（缺失/不健康即中止——先在服务器上跑 infra 起栈：cd $INFRA_PATH && ./deploy.sh）"
# 本发布对共享 infra 只读检查：绝不启动/停止/重建 PG、Redis、nginx（按上述容器名检查）
infra_bad=0
command -v docker >/dev/null 2>&1 || { echo "   ✗ docker 不可用"; infra_bad=1; }
docker compose version >/dev/null 2>&1 || { echo "   ✗ docker compose v2 插件不可用"; infra_bad=1; }
for c in "$PG_CONTAINER" "$REDIS_CONTAINER"; do
  st=$(docker inspect -f '{{.State.Health.Status}}' "$c" 2>/dev/null || echo missing)
  if [ "$st" != "healthy" ]; then
    echo "   ✗ $c：$st"
    infra_bad=1
  else
    echo "   ✓ $c healthy"
  fi
done
if ! docker network inspect "$WEBNET" >/dev/null 2>&1; then
  echo "   ✗ 网络 $WEBNET 缺失"
  infra_bad=1
else
  echo "   ✓ $WEBNET 在"
fi
# 远端应用环境配置：重复发布靠它保持配置不丢，缺失说明首装未完成
if [ ! -f "$APP_DIR/.env.production" ]; then
  echo "   ✗ $APP_DIR/.env.production 缺失——先按 deploy/部署文档.md 完成首装"
  infra_bad=1
else
  echo "   ✓ 远端 .env.production 在（解包不覆盖它）"
fi
[ "$infra_bad" = 0 ] || { echo "infra 前置检查未过：未动现有应用，修好后重跑发布"; exit 1; }

echo "-- 部署前备份（库已存在才备份；首装不伪造旧版本备份）"
if [ "${SKIP_BACKUP}" != "1" ]; then
  if docker exec "$PG_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc 'SELECT 1' >/dev/null 2>&1; then
    BACKUP="$HOME/backup-admin-$(date +%F-%H%M%S).sql.gz"
    docker exec "$PG_CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP"
    echo "   $BACKUP ($(du -h "$BACKUP" | cut -f1))"
  else
    echo "   $DB_NAME 库不存在——按首装处理，跳过备份（不伪造旧版本备份）"
  fi
else
  echo "   SKIP_BACKUP=1，已跳过"
fi

echo "-- 解包 $BUNDLE_FN（发布包不含 .env*，现网 env 不被覆盖）"
tar -xzf "$HOME/$BUNDLE_FN" --strip-components=1 -C "$APP_DIR"
rm "$HOME/$BUNDLE_FN"
echo "   REVISION: $(cat "$APP_DIR/REVISION")"

echo "-- 重建单实例容器（服务器本地构建镜像，build 有层缓存）"
docker compose -f "$APP_DIR/deploy/server.compose.yml" --env-file "$APP_DIR/.env.production" \
  up -d --build

echo "-- 健康等待（/actuator/health，最长 ${HEALTH_TIMEOUT}s；期间 Flyway 跑迁移属正常）"
i=0
tries=$((HEALTH_TIMEOUT / 2))
until docker exec "$APP_CONTAINER" wget -qO- http://localhost:8081/actuator/health 2>/dev/null | grep -q '"UP"'; do
  i=$((i+1))
  if [ "$i" -ge "$tries" ]; then
    echo "   ✗ 应用 ${HEALTH_TIMEOUT}s 未就绪——发布失败，容器现场保留（未回滚、未自动恢复数据库）"
    echo "   排查：deploy/server.sh status / logs；人工恢复边界见 deploy/部署文档.md"
    echo "   现场："
    docker ps -a --filter "name=$APP_CONTAINER" --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'
    exit 1
  fi
  sleep 2
done
echo "   ✓ $(docker exec "$APP_CONTAINER" wget -qO- http://localhost:8081/actuator/health)"

echo "-- 验收：OpenAPI 文档可达（admin-web 对接面）"
OPENAPI_HTTP=$(docker exec "$APP_CONTAINER" wget --server-response -qO /dev/null http://localhost:8081/v3/api-docs 2>&1 | awk '/^  HTTP/{print $2}' | tail -1)
echo "   /v3/api-docs HTTP $OPENAPI_HTTP（期望 200）"
[ "$OPENAPI_HTTP" = "200" ] || { echo "   ✗ OpenAPI 文档不可达——发布失败（现场保留，排查同上）"; exit 1; }

docker ps --filter "name=$APP_CONTAINER" --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'
