#!/usr/bin/env bash
# 组装自包含发布包：dist/semgravio-admin-deploy-<时间戳>.tar.gz（Windows Git Bash / macOS 通用）
# 仓库根执行：scripts/release/build-bundle.sh；末行打印包路径（scripts/release/release.sh 依赖该约定）。
#
# 产物结构（scp 到服务器任意目录解包，docs/deploy/release.md 的命令均以此为根）：
#   <包根>/          已提交源码正本＋部署文件（Dockerfile / docker-compose.prod.yml / deploy.sh /
#                    scripts/ / docs/，经 git archive 导出、恒 LF）
#   <包根>/target/   可执行 fat JAR（开发机 mvn 构建——cartisan-boot SNAPSHOT 只在开发机 ~/.m2，
#                    服务器无需 Java/Maven）
#   <包根>/REVISION  打包时的 git revision（线上容器可追溯提交）
#
# 源码树用 git archive 导出而非拷工作区（与 aiplatform 同口径）：
#   ① Windows Git Bash 无 rsync，git 两平台都在；② Windows checkout 是 CRLF，archive 出仓库
#   正本恒 LF，Linux 容器脚本不带 \r；③ 打包内容＝已提交内容（HEAD），与 REVISION 严格一致。
# 工作区脏则拒绝打包（jar 按工作区构建、源码树按 HEAD，两者会不一致）；确认带脏打包：ALLOW_DIRTY=1。
#
# 发布包不含真实环境文件：.env.production / 私钥 / 证书 / 备份 / 临时产物均未被 git 跟踪
# （.gitignore），git archive 天然排除；若已提交文件命中敏感模式则拒绝打包（防把正本带进历史）。
set -euo pipefail
cd "$(dirname "$0")/../.."

# ---- 工作区检查（默认拒绝脏工作区） -----------------------------------------
if [ -n "$(git status --porcelain)" ]; then
  if [ "${ALLOW_DIRTY:-0}" = "1" ]; then
    echo "警告：工作区有未提交改动，源码树按 HEAD 打包、jar 按工作区构建，两者可能不一致" >&2
  else
    echo "错误：工作区有未提交改动（如下）。先提交再打包；确认带脏打包用：ALLOW_DIRTY=1 scripts/release/build-bundle.sh" >&2
    git status --porcelain >&2
    exit 1
  fi
fi

# ---- 敏感文件检查（已跟踪文件不得命中真实环境/密钥/备份模式） ---------------
# 例外：*.example 模板（.env.production.example 是样例，允许随包下发）
SENSITIVE="$(git ls-files \
  | grep -E '(^|/)\.env($|\..*)' | grep -v '\.example$' || true
  git ls-files | grep -E '\.(pem|key|p12|jks)$' || true
  git ls-files | grep -E '(^|/)(backup|backups)[-/]' || true)"
if [ -n "$SENSITIVE" ]; then
  echo "错误：以下已提交文件命中敏感模式（真实环境文件/私钥/证书/备份不得入库，更不得进发布包）：" >&2
  echo "$SENSITIVE" >&2
  exit 1
fi

STAMP=$(date +%Y%m%d-%H%M)
OUT="dist/semgravio-admin-deploy-${STAMP}"
JAR="target/semgravio-admin.jar"

echo "== 1/3 开发机构建 fat JAR（服务器无需 Java/Maven） =="
mvn clean package -DskipTests -q

echo "== 2/3 组装发布目录（git archive，LF 正本）＋ JAR ＋ REVISION =="
rm -rf "${OUT}"    # 同分钟内重跑兜底
mkdir -p "${OUT}/target"
# git archive 会按本机 core.autocrlf 做 checkout 同款转换（Windows 上＝CRLF，实测踩过），
# 覆盖后输出仓库正本、恒 LF
ga() { git -c core.autocrlf=false -c core.eol=lf archive "$@"; }
ga HEAD | tar -x -C "${OUT}"
cp "${JAR}" "${OUT}/target/"
git rev-parse HEAD > "${OUT}/REVISION"

echo "== 3/3 打包 =="
tar -C dist -czf "${OUT}.tar.gz" "semgravio-admin-deploy-${STAMP}"
rm -rf "${OUT}"
du -sh "${OUT}.tar.gz"
echo "完成，包路径："
echo "${OUT}.tar.gz"
