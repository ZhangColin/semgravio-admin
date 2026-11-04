#!/usr/bin/env bash
# 部署黑盒排练（#118）：从打包/发布命令的外部行为入手，在隔离环境验证完整部署契约。
# 只断言退出码、发布包内容、容器状态与 HTTP 边界——不 mock shell 内部函数，
# 驱动的是真实 fat JAR、真实 Compose、真实容器与真实健康端点。
#
# 覆盖（对应 #118 验收，逐条编号）：
#   ① 静态契约：deploy/*.sh 语法、Compose 渲染、变量模板一致、无宿主端口、
#      external webnet、健康路径与启动宽限期
#   ② 打包黑盒：脏工作区拒绝、fat JAR 可执行（Start-Class＋依赖）、REVISION 记录、
#      敏感文件排除
#   ③ 发布前置失败：缺连接参数/私钥、缺 Docker、缺 webnet/共享 PG/Redis/应用环境配置
#      ——均非零退出且不触碰现有应用容器
#   ④ 隔离首装：测试专属 PG/Redis/网络上 release-remote.sh 全绿（首装跳过备份），
#      且 webnet 内 http://semgravio-admin:8081/actuator/health 按 DNS 名可达
#      （＝admin-web 反代目标契约）
#   ⑤ 前置失败不动现有应用：.env.production 缺失时容器 ID/运行状态原样
#   ⑥ 重复发布：升级前 pg_dump 备份（含 Flyway 表＋种子数据）、远端 .env.production
#      内容保留、幂等重建后健康
#   ⑦ 健康超时：错误凭据 → 非零退出＋诊断提示＋容器现场保留
#
# 隔离承诺（#118 AC）：只用 rehearsal-admin-* 前缀的容器/网络/镜像与 mktemp 临时目录，
# 清场只针对这些测试资源；绝不触碰共享 PG 卷、共享网络（本地 webnet 在场也不加入，
# 部署面副本把网络重定向到测试专属网络）、生产配置与兄弟应用容器。
#
# 用法（仓库根，需 Docker Desktop 运行中＋干净工作区＋cartisan SNAPSHOT 在开发机 ~/.m2）：
#   deploy/rehearsal.sh            # 全量（含 mvn package，约 10 分钟）
#   KEEP=1 deploy/rehearsal.sh     # 结束后保留现场排查
# 失败时现场保留并提示清场命令；成功自动全量清场。重跑会先清掉上一轮 rehearsal-admin-* 残留。
set -euo pipefail
cd "$(dirname "$0")/.."

# ---- 测试专属资源名（清场只认这些名字） ---------------------------------------
NET="rehearsal-admin-webnet"
PG="rehearsal-admin-pg"
RDS="rehearsal-admin-redis"
APP_CT="rehearsal-semgravio-admin"
IMG="rehearsal-semgravio-admin:ecs"
PG_SU_PASS="rehearsal-pg-su"
DB_PASS="rehearsal-db-pass"
RDS_PASS="rehearsal-redis-pass"
API_SECRET="rehearsal-api-secret"
COMPOSE_PRISTINE="deploy/server.compose.yml"
ENV_TEMPLATE="deploy/env.server.example"

ok()   { printf '  ✓ %s\n' "$*"; }
die()  { printf '✗ %s\n' "$*" >&2; exit 1; }
phase(){ printf '\n== %s ==\n' "$*"; }

# 期望失败的命令：非零退出＋输出命中关键文案，才算这条失败路径真的被走到
expect_fail() { # $1=关键文案(grep -E) $2=描述; $3...=命令
  local pat=$1 desc=$2 out rc=0
  shift 2
  out=$("$@" 2>&1) || rc=$?
  [ "$rc" -ne 0 ] || { printf '✗ %s：退出码竟是 0\n' "$desc" >&2; exit 1; }
  printf '%s' "$out" | grep -qE "$pat" \
    || { printf '✗ %s：输出未含「%s」，实际输出：\n%s\n' "$desc" "$pat" "$out" >&2; exit 1; }
  ok "$desc（非零退出＋文案命中）"
}

T=$(mktemp -d "${TMPDIR:-/tmp}/semgravio-rehearsal.XXXXXX")

on_exit() {
  local rc=$?
  if [ "$rc" -ne 0 ] || [ "${KEEP:-0}" = 1 ]; then
    printf '\n%s：排练现场保留（退出码 %s）\n' \
      "$([ "$rc" -ne 0 ] && echo '✗ 排练失败' || echo 'KEEP=1')" "$rc"
    printf '  应用日志：docker logs %s\n  工作目录：%s\n' "$APP_CT" "$T"
    printf '  清场（只删 rehearsal-admin-* 测试资源）：\n'
    printf '    docker rm -fv %s %s && docker rm -f %s && docker network rm %s && docker image rm %s && rm -rf %s\n' \
      "$PG" "$RDS" "$APP_CT" "$NET" "$IMG" "$T"
  else
    docker rm -fv "$PG" "$RDS" >/dev/null 2>&1 || true   # -v 一并移除匿名数据卷
    docker rm -f "$APP_CT" >/dev/null 2>&1 || true
    docker network rm "$NET" >/dev/null 2>&1 || true
    docker image rm "$IMG" >/dev/null 2>&1 || true
    rm -rf "$T"
    ok "清场完成（仅 rehearsal-admin-* 测试资源；共享 PG 卷/webnet/生产配置未触碰）"
  fi
}
trap on_exit EXIT

# 服务器侧发布正本（release.sh 生产经 SSH 灌入同一文件；见 deploy/release-remote.sh 头注释）
run_release() { # $@ = 传给 env 的额外覆盖（DB_NAME=/HEALTH_TIMEOUT=…）
  env HOME="$T" BUNDLE_FN="$BN" REMOTE_DIR=app INFRA_DIR=infra \
      PG_CONTAINER="$PG" REDIS_CONTAINER="$RDS" WEBNET="$NET" APP_CONTAINER="$APP_CT" "$@" \
    bash deploy/release-remote.sh
}

# ---------------------------------------------------------------- 0. 前置
phase "0/8 前置检查（docker/mvn/干净工作区/上一轮残留）"
docker info >/dev/null 2>&1 || die "docker 不可用（Docker Desktop 未启动？）"
command -v mvn >/dev/null 2>&1 || die "mvn 不可用"
command -v unzip >/dev/null 2>&1 || die "unzip 不可用（校验 fat JAR 清单用）"
command -v sha256sum >/dev/null 2>&1 || die "sha256sum 不可用"
[ -z "$(git status --porcelain)" ] \
  || die "工作区不干净——打包正本＝git archive HEAD（dist/ 等 gitignore 产物不算脏），先提交再排练"
for c in "$APP_CT" "$PG" "$RDS"; do
  docker inspect "$c" >/dev/null 2>&1 && { docker rm -fv "$c" >/dev/null; echo "  清掉上一轮残留容器 $c"; }
done
docker network inspect "$NET" >/dev/null 2>&1 \
  && { docker network rm "$NET" >/dev/null; echo "  清掉上一轮残留网络 $NET"; }
docker image inspect "$IMG" >/dev/null 2>&1 \
  && { docker image rm "$IMG" >/dev/null; echo "  清掉上一轮残留镜像 $IMG"; }
ok "前置通过（本地 webnet 在场与否不影响——排练不加入共享网络）"

# ---------------------------------------------------------------- 1. 静态契约
phase "1/8 静态契约（脚本语法/Compose 渲染/变量一致/无宿主端口/external webnet）"
for f in deploy/*.sh; do bash -n "$f" || die "$f 语法错误"; done
ok "deploy/*.sh 语法检查通过"

RENDERED="$(docker compose -f "$COMPOSE_PRISTINE" --env-file "$ENV_TEMPLATE" config)" \
  || die "Compose 渲染失败"
grep -q '^name: semgravio-admin$' <<<"$RENDERED" || die "项目名未显式指定为 semgravio-admin"
grep -q 'container_name: semgravio-admin' <<<"$RENDERED" || die "容器名未显式指定为 semgravio-admin"
grep -qE 'image: semgravio-admin(:[a-z0-9._-]+)?$' <<<"$RENDERED" || die "镜像名非 semgravio-admin 专属值"
! grep -q 'published:' <<<"$RENDERED" || die "存在宿主端口映射（published），8081 不得发布到宿主"
grep -q 'webnet:$' <<<"$RENDERED" || die "未加入 webnet 网络"
grep -A2 'webnet:$' <<<"$RENDERED" | grep -q 'external: true' || die "webnet 未声明为 external"
grep -q '/actuator/health' <<<"$RENDERED" || die "健康检查路径不是 /actuator/health"
START_PERIOD="$(grep -oE 'start_period: [0-9smh]+' <<<"$RENDERED" | head -1 | cut -d' ' -f2)"
SP_SECONDS=0
if [[ "$START_PERIOD" == *m* ]]; then
  MIN="${START_PERIOD%%m*}"; SEC="${START_PERIOD#*m}"; SEC="${SEC%s}"
  SP_SECONDS=$(( MIN * 60 + ${SEC:-0} ))
elif [[ "$START_PERIOD" == *s* ]]; then
  SP_SECONDS="${START_PERIOD%s}"
fi
[ "$SP_SECONDS" -ge 60 ] || die "start_period=${START_PERIOD:-缺失}，未覆盖启动宽限期（应 >= 60s）"
ok "渲染断言通过：单实例形态/无宿主端口/external webnet/健康检查（start_period=${SP_SECONDS}s）"

# 变量契约：compose 引用的每个变量模板都有声明，模板声明的每个变量 compose 都注入
COMPOSE_VARS="$(grep -oE '\$\{[A-Z_][A-Z0-9_]*' "$COMPOSE_PRISTINE" | sed 's/^${//' | sort -u)"
TEMPLATE_VARS="$(grep -vE '^\s*(#|$)' "$ENV_TEMPLATE" | cut -d= -f1 | tr -d ' ' | sort -u)"
for v in $COMPOSE_VARS; do
  grep -qx "$v" <<<"$TEMPLATE_VARS" || die "compose 引用的 $v 未在 $ENV_TEMPLATE 声明（模板与注入变量须一致）"
done
for v in $TEMPLATE_VARS; do
  grep -qx "$v" <<<"$COMPOSE_VARS" || die "模板声明的 $v 未被 compose 注入（模板与注入变量须一致）"
done
ok "变量契约一致（$(wc -l <<<"$COMPOSE_VARS") 项）"

# ---------------------------------------------------------------- 2. 打包黑盒
phase "2/8 打包黑盒（build-bundle.sh 外部行为）"
# 脏工作区拒绝：未跟踪探针文件 → 拒绝打包（在 mvn 之前，快）
probe=.rehearsal-dirty-probe
: > "$probe"
expect_fail '未提交改动' '脏工作区拒绝打包' bash deploy/build-bundle.sh
rm -f "$probe"

# 真打包（干净工作区；mvn package 跳测试——质量由 mvn test / mvn verify 独立把关）
BUNDLE=$(deploy/build-bundle.sh | tee /dev/stderr | grep -E '^dist/' | tail -n 1)
[ -n "$BUNDLE" ] && [ -f "$BUNDLE" ] || die "未取得发布包路径"
BN=$(basename "$BUNDLE")
ok "发布包：$BUNDLE ($(du -h "$BUNDLE" | cut -f1))"

# 解包到隔离「服务器」目录（模拟 scp 落点与 release-remote 的解包动作）
mkdir -p "$T/app"
tar -xzf "$BUNDLE" --strip-components=1 -C "$T/app"

# REVISION ＝ 当前 HEAD（线上容器可追溯到明确提交）
[ "$(cat "$T/app/REVISION")" = "$(git rev-parse HEAD)" ] || die "REVISION 与 git HEAD 不一致"
ok "REVISION 记录＝$(git rev-parse --short HEAD)"

# fat JAR 可执行：Start-Class 启动清单 ＋ BOOT-INF/lib 运行依赖（非瘦 JAR）。
# 先落盘再模式匹配——grep -q 提前命中会让 unzip 吃 SIGPIPE、pipefail 误判（老坑，勿回退成管道）
JAR="$T/app/target/semgravio-admin.jar"
[ -f "$JAR" ] || die "包内缺 target/semgravio-admin.jar"
MANIFEST="$(unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null || true)"
grep -q 'Start-Class: com.semgravio.AdminApplication' <<<"$MANIFEST" \
  || die "瘦 JAR（缺 Start-Class 清单），java -jar 无法启动"
JAR_LISTING="$(unzip -l "$JAR" 2>/dev/null || true)"
grep -q 'BOOT-INF/lib/' <<<"$JAR_LISTING" || die "缺 BOOT-INF/lib 运行依赖，不是 fat JAR"
ok "fat JAR 可执行（Start-Class＋BOOT-INF/lib，$(du -h "$JAR" | cut -f1)）"

# 敏感文件排除：包内不得有真实环境文件/私钥/证书/备份/开发笔记
BAD="$(tar -tzf "$BUNDLE" | grep -E '(^|/)\.env($|\..*)' | grep -v '\.example$' || true)"
BAD="${BAD}$(tar -tzf "$BUNDLE" | grep -E '\.(pem|key|p12|jks|crt|cer|der)$' || true)"
BAD="${BAD}$(tar -tzf "$BUNDLE" | grep -E '(^|/)(backup|backups)[-/]|(^|/)\.scratch/' || true)"
[ -z "$BAD" ] || die "发布包含敏感文件：$BAD"
ok "敏感文件排除（.env* 非 example/密钥/证书/备份/.scratch 均不在包内）"

# 部署文件齐位：服务器部署面完整（含排练自身与唯一 runbook）
for f in build-bundle.sh release.sh release-remote.sh server.sh rehearsal.sh \
         server.compose.yml server.Dockerfile env.server.example 部署文档.md; do
  [ -f "$T/app/deploy/$f" ] || die "包内缺 deploy/$f（服务器部署面不完整）"
done
ok "deploy/ 部署面齐位（入口/正本/排练/文档）"

# ---------------------------------------------------------------- 3. 发布前置失败
phase "3/8 发布前置失败（缺参/缺钥/缺 Docker/缺 webnet、共享 PG/Redis、应用 env）"
# release.sh 连接参数必填（脚本不带任何现网默认值）
expect_fail 'SERVER_HOST 必填' '缺 SERVER_HOST 即拒' \
  env -u SERVER_HOST -u SERVER_USER -u SSH_KEY -u REMOTE_DIR -u INFRA_DIR bash deploy/release.sh
expect_fail '找不到私钥' '缺 SSH 私钥即拒' \
  env SERVER_HOST=127.0.0.1 SERVER_USER=nobody SSH_KEY=/nonexistent.pem REMOTE_DIR=x INFRA_DIR=x \
  bash deploy/release.sh

# release-remote.sh：服务器缺 Docker（PATH 无 docker——外部行为＝command -v 找不到）
mkdir -p "$T/nodocker-bin"
for t in date tar gzip du cat rm grep awk sleep head cut; do
  ln -s "$(command -v "$t")" "$T/nodocker-bin/"
done
cp "$BUNDLE" "$T/"
expect_fail '✗ docker 不可用' '服务器缺 Docker 即拒' \
  env PATH="$T/nodocker-bin" HOME="$T" BUNDLE_FN="$BN" REMOTE_DIR=app INFRA_DIR=infra \
  /usr/bin/bash deploy/release-remote.sh

# release-remote.sh：真 Docker 但 webnet/共享 PG/Redis/应用 env 全缺（生产默认名逐项报缺）
mkdir -p "$T/app-noenv"
cp "$BUNDLE" "$T/"
out=$(env HOME="$T" BUNDLE_FN="$BN" REMOTE_DIR=app-noenv INFRA_DIR=infra \
        WEBNET=rehearsal-admin-webnet-absent bash deploy/release-remote.sh 2>&1) \
  && die "缺 webnet/PG/Redis/env 竟然退出码 0"
for pat in '✗ 网络 rehearsal-admin-webnet-absent 缺失' '✗ semgravio-pg' \
           '✗ semgravio-redis' '✗ .*\.env\.production 缺失' '未动现有应用'; do
  grep -qE "$pat" <<<"$out" || die "前置失败输出缺关键文案「$pat」，实际输出：\n$out"
done
ok "缺 webnet/共享 PG/Redis/应用 env → 非零退出＋逐项报缺＋未动现有应用"

# ---------------------------------------------------------------- 4. 隔离首装
phase "4/8 隔离首装（测试专属 PG/Redis/网络 → release-remote 全绿）"
docker network create "$NET" >/dev/null
# 测试 infra 与生产同形：带 healthcheck——release-remote 的前置检查断言 Health.Status=healthy，
# 无 healthcheck 的容器该字段恒空、会被误判为不健康
docker run -d --name "$PG" --network "$NET" \
  --health-cmd "pg_isready -U postgres" --health-interval 3s --health-timeout 3s \
  --health-retries 20 --health-start-period 3s \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD="$PG_SU_PASS" postgres:16-alpine >/dev/null
docker run -d --name "$RDS" --network "$NET" \
  --health-cmd "redis-cli --no-auth-warning -a '$RDS_PASS' ping | grep -q PONG" \
  --health-interval 3s --health-timeout 3s --health-retries 20 --health-start-period 3s \
  redis:7-alpine --requirepass "$RDS_PASS" >/dev/null
for c in "$PG" "$RDS"; do
  i=0
  until [ "$(docker inspect -f '{{.State.Health.Status}}' "$c" 2>/dev/null)" = healthy ]; do
    i=$((i+1)); [ "$i" -ge 60 ] && die "测试 infra $c 60s 未 healthy"
    sleep 1
  done
done
ok "测试 PG/Redis 已起且 healthy（$NET 内、无宿主端口、无具名卷）"

# 建库建号＝infra ensure-db-user.sh 的排练等价：低权限角色＋同名库，应用不拿超管身份
docker exec "$PG" psql -U postgres -c "CREATE ROLE semgravio_admin LOGIN PASSWORD '$DB_PASS'" >/dev/null
docker exec "$PG" psql -U postgres -c "CREATE DATABASE semgravio_admin OWNER semgravio_admin" >/dev/null
ok "已建 semgravio_admin 库＋同名低权限角色（helper 等价）"

# 远端应用环境配置（一次性写入；6/8 验证重复发布不覆盖它）
cat > "$T/app/.env.production" <<EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://$PG:5432/semgravio_admin
SPRING_DATASOURCE_USERNAME=semgravio_admin
SPRING_DATASOURCE_PASSWORD=$DB_PASS
REDIS_HOST=$RDS
REDIS_PORT=6379
REDIS_PASSWORD=$RDS_PASS
ADMIN_API_SECRET=$API_SECRET
EOF

# 制作排练专用发布包：在解包树上把部署面资源名重定向到 rehearsal-admin-*（仓库正本契约
# 由 1/8 静态检查钉住），再重新打包——release-remote 每次发布都重新解包覆盖 APP_DIR，
# 改动只有烧进发布包本身才活得过重解包（网络 external name 重定向＋容器/镜像/项目名
# 专属化＋DNS 别名保住 semgravio-admin 反代目标契约）
TOPDIR="semgravio-admin-deploy-rehearsal"
mkdir -p "$T/repack/$TOPDIR"
tar -xzf "$BUNDLE" --strip-components=1 -C "$T/repack/$TOPDIR"
sed -i \
  -e 's/^name: semgravio-admin$/name: rehearsal-semgravio-admin/' \
  -e 's/^    container_name: semgravio-admin$/    container_name: rehearsal-semgravio-admin/' \
  -e 's/^    image: semgravio-admin:ecs$/    image: rehearsal-semgravio-admin:ecs/' \
  -e '/^  webnet:$/a\    name: rehearsal-admin-webnet' \
  -e 's/^      - webnet$/      webnet:\n        aliases: [semgravio-admin]/' \
  "$T/repack/$TOPDIR/deploy/server.compose.yml"
docker compose -f "$T/repack/$TOPDIR/deploy/server.compose.yml" --env-file "$ENV_TEMPLATE" config >/dev/null \
  || die "重定向后的部署面渲染失败（sed 改坏了？）"
tar -C "$T/repack" -czf "$T/rehearsal-bundle.master.tar.gz" "$TOPDIR"
rm -rf "$T/repack"
ok "排练专用发布包已制作（部署面重定向到 rehearsal-admin-*，渲染校验通过）"

# 首装发布：DB_NAME 指向不存在的库名 → 演示「首装不伪造备份」；应用数据源仍用真排练库。
# release-remote 解包后会删除 $HOME/$BUNDLE_FN——每次发布前从母本补一份
cp "$T/rehearsal-bundle.master.tar.gz" "$T/$BN"
run_release DB_NAME=rehearsal_admin_absent HEALTH_TIMEOUT=300 2>&1 | tee "$T/first-release.log"
grep -q '跳过备份' "$T/first-release.log" || die "首装未按「库不存在→跳过备份」处理"
grep -q '"status":"UP"' "$T/first-release.log" || die "首装后应用未健康"
ok "首装全绿：前置检查→跳过备份→解包→构建→健康 UP→OpenAPI 200"

# admin-web 反代目标契约：网络内按 DNS 名 semgravio-admin 解析到应用（宿主无 8081）
body="$(docker exec "$PG" wget -qO- http://semgravio-admin:8081/actuator/health 2>/dev/null || true)"
grep -q '"status":"UP"' <<<"$body" \
  || die "网络内 http://semgravio-admin:8081/actuator/health 未 UP（反代目标不可达）：$body"
ok "反代目标 http://semgravio-admin:8081/actuator/health 网络内 UP（admin-web 对接面）"

# ---------------------------------------------------------------- 5. 失败不动现有应用
phase "5/8 前置失败不动现有应用（.env.production 缺失 → 容器原样）"
CID_BEFORE=$(docker inspect -f '{{.Id}}' "$APP_CT")
mkdir -p "$T/app-noenv"
expect_fail '✗ .*\.env\.production 缺失' '应用 env 缺失即拒（infra 在场）' \
  env HOME="$T" BUNDLE_FN="$BN" REMOTE_DIR=app-noenv INFRA_DIR=infra \
      PG_CONTAINER="$PG" REDIS_CONTAINER="$RDS" WEBNET="$NET" APP_CONTAINER="$APP_CT" \
  bash deploy/release-remote.sh
[ "$(docker inspect -f '{{.Id}}' "$APP_CT")" = "$CID_BEFORE" ] || die "前置失败竟重建了现有应用容器"
docker ps --format '{{.Names}}' | grep -qx "$APP_CT" || die "现有应用容器不在运行"
ok "现有应用容器 ID/运行状态原样（失败阶段不替换现有应用）"

# ---------------------------------------------------------------- 6. 重复发布
phase "6/8 重复发布（升级前备份＋远端配置保留＋幂等健康）"
ENV_SHA=$(sha256sum "$T/app/.env.production" | cut -d' ' -f1)
cp "$T/rehearsal-bundle.master.tar.gz" "$T/$BN"
run_release HEALTH_TIMEOUT=180 2>&1 | tee "$T/second-release.log"

# 升级前备份：库已有 Flyway/种子数据 → 备份生成且含 schema 与数据
BK=$(ls -t "$T"/backup-admin-*.sql.gz 2>/dev/null | head -1 || true)
[ -n "$BK" ] || die "重复发布未生成升级前备份"
zgrep -q 'flyway_schema_history' "$BK" || die "备份不含 Flyway 迁移表"
zgrep -q 'sys_admin_users' "$BK" || die "备份不含种子数据表"
ok "升级前备份 $(basename "$BK")（含 Flyway 表＋种子数据）"

# 远端配置保留：解包不覆盖 .env.production
[ "$(sha256sum "$T/app/.env.production" | cut -d' ' -f1)" = "$ENV_SHA" ] \
  || die "重复发布覆盖了 .env.production"
grep -q '"status":"UP"' "$T/second-release.log" || die "重复发布后应用未健康"
ok ".env.production 内容原样（发布包不含 env，解包不触碰）＋容器重建后健康（幂等）"

# ---------------------------------------------------------------- 7. 健康超时
phase "7/8 健康超时（错误凭据 → 非零退出＋诊断现场保留）"
sed -i "s/^SPRING_DATASOURCE_PASSWORD=.*/SPRING_DATASOURCE_PASSWORD=wrong-pass/" "$T/app/.env.production"
cp "$T/rehearsal-bundle.master.tar.gz" "$T/$BN"
expect_fail '未就绪|容器现场保留' '健康超时非零退出＋诊断提示' \
  env HOME="$T" BUNDLE_FN="$BN" REMOTE_DIR=app INFRA_DIR=infra \
      PG_CONTAINER="$PG" REDIS_CONTAINER="$RDS" WEBNET="$NET" APP_CONTAINER="$APP_CT" \
      HEALTH_TIMEOUT=30 \
  bash deploy/release-remote.sh
docker inspect "$APP_CT" >/dev/null 2>&1 || die "超时后容器现场未保留"
ok "容器现场保留（未自动回滚、未自动恢复数据库）——现场与日志归人工排查"

# ---------------------------------------------------------------- 8. 收口
phase "8/8 收口"
printf '排练全部通过 ✅（静态契约＋打包黑盒＋前置失败＋隔离首装＋幂等发布＋超时诊断）\n'
printf '口径：以上为本地黑盒验证（隔离环境）；真实生产验收须按 deploy/部署文档.md 在授权服务器执行。\n'
