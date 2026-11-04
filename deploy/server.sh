#!/usr/bin/env bash
# 服务器日常运维小工具（连接参数与 deploy/release.sh 同套环境变量，无默认值）。
# 本脚本只操作 admin 应用容器，不提供共享 infra（semgravio-pg/semgravio-redis/semgravio-nginx）
# 或网关的任何操作——infra 归 aiplatform 部署单元的 deploy/infra/ 独立管理。
#
# 仓库根执行（Git Bash）：
#   deploy/server.sh status    admin 容器概览（状态/镜像）
#   deploy/server.sh logs      跟日志（semgravio-admin，--tail 200）
#   deploy/server.sh restart   重启应用容器（不重建镜像、不动 infra，秒级）
#   deploy/server.sh dump      pg_dump semgravio_admin → ~/backup-admin-<时间>.sql.gz
#   deploy/server.sh smoke     冒烟：Actuator UP ＋ OpenAPI 文档可达（容器内探测，宿主无 8081）
#   deploy/server.sh sh        应用级 shell：docker exec -it 进 semgravio-admin 容器
#   deploy/server.sh ssh       登服务器交互 shell（应急排查用）
# 换版上线（打包＋发布＋重建）不是这里，用 deploy/release.sh。
set -euo pipefail
cd "$(dirname "$0")/.."

: "${SERVER_HOST:?SERVER_HOST 必填——服务器地址，与 deploy/release.sh 同值}"
: "${SERVER_USER:?SERVER_USER 必填——SSH 登录用户}"
: "${SSH_KEY:?SSH_KEY 必填——SSH 私钥路径}"
REMOTE="${SERVER_USER}@${SERVER_HOST}"
SSH=(ssh -i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=15 "$REMOTE")

case "${1:-help}" in
  status)
    "${SSH[@]}" "docker ps -a --filter name=semgravio-admin --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'"
    ;;
  logs)
    exec "${SSH[@]}" "docker logs -f --tail 200 semgravio-admin"
    ;;
  restart)
    "${SSH[@]}" 'docker restart semgravio-admin >/dev/null && echo 已重启 semgravio-admin（未重建镜像、未动共享 infra）'
    ;;
  dump)
    # 库不存在（首装未完成）即提示，不伪造空备份；备份≠回滚，恢复是人工决策（见 docs/deploy/release.md）
    "${SSH[@]}" 'if docker exec semgravio-pg psql -U semgravio_admin -d semgravio_admin -tAc "SELECT 1" >/dev/null 2>&1; then
      docker exec semgravio-pg pg_dump -U semgravio_admin semgravio_admin | gzip > ~/backup-admin-$(date +%F-%H%M%S).sql.gz \
        && ls -t ~/backup-admin-*.sql.gz | head -1
    else
      echo "semgravio_admin 库不存在——首装未完成，无库可备份" >&2; exit 1
    fi'
    ;;
  smoke)
    "${SSH[@]}" 'echo "actuator/health : $(docker exec semgravio-admin wget -qO- http://localhost:8081/actuator/health)（期望 {\"status\":\"UP\"}）"
echo "/v3/api-docs    : HTTP $(docker exec semgravio-admin wget --server-response -qO /dev/null http://localhost:8081/v3/api-docs 2>&1 | awk "/^  HTTP/{print \$2}" | tail -1)（期望 200）"'
    ;;
  sh)
    # 应用级 shell：进应用容器（AC「应用级」口径——其余一切经 docker/ssh 在容器面上）
    exec "${SSH[@]}" "docker exec -it semgravio-admin sh"
    ;;
  ssh)
    exec "${SSH[@]}"
    ;;
  *)
    grep '^# ' "$0" | tail -n +2
    ;;
esac
