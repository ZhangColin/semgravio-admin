#!/usr/bin/env bash
# 一键换版：开发机打包 → scp 上传 → 服务器 infra 前置检查/备份/解包/重建/健康等待/验收。
# 仓库根执行（Git Bash）：scripts/release/release.sh
#
# 连接参数一律环境变量传入，本脚本不带任何现网默认值（不复制 aiplatform 的 IP/密钥路径/目录）：
#   SERVER_HOST=<服务器地址>  SERVER_USER=<登录用户>  SSH_KEY=<私钥路径>
#   REMOTE_DIR=<远端应用目录>  INFRA_DIR=<共享 infra 目录>
#   （REMOTE_DIR/INFRA_DIR 相对服务器 $HOME，绝对路径以 / 开头也可）
# 常用开关：SKIP_BUILD=1 复用 dist/ 里最新的发布包只发布；SKIP_BACKUP=1 跳过部署前 pg_dump。
#
# 前置检查不过 → 非零退出且不重建现有应用；健康超时/发布失败 → 非零退出、保留容器现场、
# 不自动恢复数据库（备份≠回滚，人工恢复边界见 docs/deploy/release.md）。
# 服务器侧日常运维（status/logs/restart/dump/smoke/sh）用 scripts/release/ops.sh。
set -euo pipefail
cd "$(dirname "$0")/../.."

# ---- 连接参数（必填，无默认值） ----------------------------------------------
: "${SERVER_HOST:?SERVER_HOST 必填——服务器地址（如 1.2.3.4），本脚本不内置任何现网默认值}"
: "${SERVER_USER:?SERVER_USER 必填——SSH 登录用户}"
: "${SSH_KEY:?SSH_KEY 必填——SSH 私钥路径（如 \$HOME/ssh/<主机>.pem）}"
: "${REMOTE_DIR:?REMOTE_DIR 必填——远端应用目录（相对 \$HOME 或绝对路径）}"
: "${INFRA_DIR:?INFRA_DIR 必填——共享 infra 目录（相对 \$HOME 或绝对路径，仅做前置健康检查，不做任何生命周期操作）}"
REMOTE="${SERVER_USER}@${SERVER_HOST}"
SSH_OPTS=(-i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=15)

echo "== 0/4 前置检查（SSH 连通性与密钥） =="
[ -f "$SSH_KEY" ] || { echo "错误：找不到私钥 $SSH_KEY"; exit 1; }
ssh "${SSH_OPTS[@]}" "$REMOTE" true || { echo "错误：SSH 连不上 $REMOTE（BatchMode 免密应为已配置）"; exit 1; }

echo "== 1/4 打包（开发机完成 Maven 构建） =="
if [ "${SKIP_BUILD:-0}" = "1" ]; then
  BUNDLE=$(ls -t dist/semgravio-admin-deploy-*.tar.gz 2>/dev/null | head -1 || true)
  [ -n "$BUNDLE" ] || { echo "错误：dist/ 没有现成发布包；去掉 SKIP_BUILD 全新打包，或先跑 scripts/release/build-bundle.sh"; exit 1; }
  echo "复用现有包：$BUNDLE"
else
  BUNDLE=$(scripts/release/build-bundle.sh | tee /dev/stderr | tail -n 1)
  [ -n "$BUNDLE" ] || exit 1
fi
BASENAME=$(basename "$BUNDLE")

echo "== 2/4 上传（$(du -h "$BUNDLE" | cut -f1) → $REMOTE） =="
scp -i "$SSH_KEY" "$BUNDLE" "$REMOTE:"

echo "== 3/4 服务器：infra 前置检查 → 备份 → 解包 → 重建 → 健康等待 → 验收 =="
ssh "${SSH_OPTS[@]}" "$REMOTE" \
  "BUNDLE_FN=$BASENAME REMOTE_DIR=$REMOTE_DIR INFRA_DIR=$INFRA_DIR SKIP_BACKUP=${SKIP_BACKUP:-0} bash -s" <<'REMOTE'
set -euo pipefail
if [ "${REMOTE_DIR:0:1}" = "/" ]; then APP_DIR="$REMOTE_DIR"; else APP_DIR="$HOME/$REMOTE_DIR"; fi
if [ "${INFRA_DIR:0:1}" = "/" ]; then INFRA_PATH="$INFRA_DIR"; else INFRA_PATH="$HOME/$INFRA_DIR"; fi
DB_NAME=semgravio_admin
DB_USER=semgravio_admin
APP_CONTAINER=semgravio-admin

echo "-- infra 前置检查（缺失/不健康即中止——先在服务器上跑 infra 起栈：cd $INFRA_PATH && ./deploy.sh）"
# 本发布对共享 infra 只读检查：绝不启动/停止/重建 semgravio-pg、semgravio-redis、nginx
infra_bad=0
command -v docker >/dev/null 2>&1 || { echo "   ✗ docker 不可用"; infra_bad=1; }
docker compose version >/dev/null 2>&1 || { echo "   ✗ docker compose v2 插件不可用"; infra_bad=1; }
for c in semgravio-pg semgravio-redis; do
  st=$(docker inspect -f '{{.State.Health.Status}}' "$c" 2>/dev/null || echo missing)
  if [ "$st" != "healthy" ]; then
    echo "   ✗ $c：$st"
    infra_bad=1
  else
    echo "   ✓ $c healthy"
  fi
done
if ! docker network inspect webnet >/dev/null 2>&1; then
  echo "   ✗ 网络 webnet 缺失"
  infra_bad=1
else
  echo "   ✓ webnet 在"
fi
# 远端应用环境配置：重复发布靠它保持配置不丢，缺失说明首装未完成
if [ ! -f "$APP_DIR/.env.production" ]; then
  echo "   ✗ $APP_DIR/.env.production 缺失——先按 docs/deploy/first-install.md 完成首装"
  infra_bad=1
else
  echo "   ✓ 远端 .env.production 在（解包不覆盖它）"
fi
[ "$infra_bad" = 0 ] || { echo "infra 前置检查未过：未动现有应用，修好后重跑发布"; exit 1; }

echo "-- 部署前备份（库已存在才备份；首装不伪造旧版本备份）"
if [ "${SKIP_BACKUP}" != "1" ]; then
  if docker exec semgravio-pg psql -U "$DB_USER" -d "$DB_NAME" -tAc 'SELECT 1' >/dev/null 2>&1; then
    BACKUP="$HOME/backup-admin-$(date +%F-%H%M).sql.gz"
    docker exec semgravio-pg pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP"
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
docker compose -f "$APP_DIR/docker-compose.prod.yml" --env-file "$APP_DIR/.env.production" \
  up -d --build

echo "-- 健康等待（/actuator/health，最长 180s；期间 Flyway 跑迁移属正常）"
i=0
until docker exec "$APP_CONTAINER" wget -qO- http://localhost:8081/actuator/health 2>/dev/null | grep -q '"UP"'; do
  i=$((i+1))
  if [ "$i" -ge 90 ]; then
    echo "   ✗ 应用 180s 未就绪——发布失败，容器现场保留（未回滚、未自动恢复数据库）"
    echo "   排查：scripts/release/ops.sh status / logs；人工恢复边界见 docs/deploy/release.md"
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
REMOTE

echo "== 4/4 完成：$BASENAME 已上线（健康 UP，OpenAPI 可达） =="
