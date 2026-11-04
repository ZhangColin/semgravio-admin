#!/usr/bin/env bash
# 一键换版：开发机打包 → scp 上传 → 服务器 infra 前置检查/备份/解包/重建/健康等待/验收。
# 仓库根执行（Git Bash）：deploy/release.sh
#
# 连接参数一律环境变量传入，本脚本不带任何现网默认值（不复制 aiplatform 的 IP/密钥路径/目录）：
#   SERVER_HOST=<服务器地址>  SERVER_USER=<登录用户>  SSH_KEY=<私钥路径>
#   REMOTE_DIR=<远端应用目录>  INFRA_DIR=<共享 infra 目录>
#   （REMOTE_DIR/INFRA_DIR 相对服务器 $HOME，绝对路径以 / 开头也可）
# 常用开关：SKIP_BUILD=1 复用 dist/ 里最新的发布包只发布；SKIP_BACKUP=1 跳过部署前 pg_dump。
#
# 前置检查不过 → 非零退出且不重建现有应用；健康超时/发布失败 → 非零退出、保留容器现场、
# 不自动恢复数据库（备份≠回滚，人工恢复边界见 deploy/部署文档.md）。
# 服务器侧动作正本＝deploy/release-remote.sh（经 SSH 灌入执行；deploy/rehearsal.sh
# 在隔离环境直接驱动同一文件做黑盒排练，两处行为不劈叉）。
# 服务器侧日常运维（status/logs/restart/dump/smoke/sh）用 deploy/server.sh。
# 完整 runbook（首装/发布/运维/备份恢复/secret 同步/验收）见 deploy/部署文档.md。
set -euo pipefail
cd "$(dirname "$0")/.."

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
  [ -n "$BUNDLE" ] || { echo "错误：dist/ 没有现成发布包；去掉 SKIP_BUILD 全新打包，或先跑 deploy/build-bundle.sh"; exit 1; }
  echo "复用现有包：$BUNDLE"
else
  # 只在 dist/ 开头的行里取末行——mvn 警告等杂散 stdout 不会被误当成包路径
  BUNDLE=$(deploy/build-bundle.sh | tee /dev/stderr | grep -E '^dist/' | tail -n 1)
  [ -n "$BUNDLE" ] || exit 1
fi
BASENAME=$(basename "$BUNDLE")

echo "== 2/4 上传（$(du -h "$BUNDLE" | cut -f1) → $REMOTE） =="
scp -i "$SSH_KEY" "$BUNDLE" "$REMOTE:"

echo "== 3/4 服务器：infra 前置检查 → 备份 → 解包 → 重建 → 健康等待 → 验收 =="
ssh "${SSH_OPTS[@]}" "$REMOTE" \
  "BUNDLE_FN=$BASENAME REMOTE_DIR=$REMOTE_DIR INFRA_DIR=$INFRA_DIR SKIP_BACKUP=${SKIP_BACKUP:-0} bash -s" \
  < deploy/release-remote.sh

echo "== 4/4 完成：$BASENAME 已上线（健康 UP，OpenAPI 可达） =="
