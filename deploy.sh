#!/bin/bash
# 管理后台网关 - 生产环境部署脚本
# 使用方法: ./deploy.sh

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

COMPOSE="docker-compose -f docker-compose.prod.yml --env-file .env.production"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   管理后台网关 - 生产环境部署${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查环境配置
if [ ! -f .env.production ]; then
  echo -e "${RED}错误: 未找到 .env.production${NC}"
  echo -e "${YELLOW}  首装请先按 docs/deploy/first-install.md 准备（infra helper 建库建号后抄录分片）${NC}"
  exit 1
fi
echo -e "${BLUE}✓ 读取 .env.production${NC}"

# 渲染预检：必填变量缺失（:? 语法）在启动前即响亮失败，不半路留下半启动态
echo -e "${BLUE}✓ Compose 渲染预检...${NC}"
$COMPOSE config >/dev/null || {
  echo -e "${RED}错误: Compose 渲染失败——.env.production 缺少必填变量（见上方报错），${NC}"
  echo -e "${RED}       既有配置不会被自动补全或切换，请对照 .env.production.example 人工核对${NC}"
  exit 1
}

# 检查并确保 JAR 文件（稳定产物名由 pom.xml <finalName> 唯一定义，见 #115）
JAR_FILE="target/semgravio-admin.jar"
if [ ! -f "$JAR_FILE" ]; then
  echo -e "${YELLOW}未找到 JAR，执行 mvn package -DskipTests 构建...${NC}"
  mvn -q -DskipTests package || { echo -e "${RED}错误: Maven package 失败${NC}"; exit 1; }
fi
# 真实校验：瘦 JAR 无法 java -jar 启动，禁止带入镜像（模式匹配避免 SIGPIPE+pipefail 误判）
JAR_MANIFEST="$(unzip -p "$JAR_FILE" META-INF/MANIFEST.MF 2>/dev/null || true)"
if [[ "$JAR_MANIFEST" != *"Start-Class: com.semgravio.AdminApplication"* ]]; then
  echo -e "${RED}错误: ${JAR_FILE} 是瘦 JAR（无 Spring Boot Start-Class 清单），请执行 mvn clean package -DskipTests 重新构建${NC}"
  exit 1
fi
JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "${GREEN}✓ JAR: $JAR_FILE ($JAR_SIZE)${NC}"
echo ""

# 停止旧容器
echo -e "${YELLOW}停止旧容器...${NC}"
$COMPOSE down 2>/dev/null || true

# 构建镜像
echo -e "${YELLOW}构建镜像...${NC}"
$COMPOSE build

# 启动新容器
echo -e "${YELLOW}启动服务...${NC}"
$COMPOSE up -d

# 等待容器启动
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 15

# 通过 docker 检查容器健康状态
if docker inspect --format='{{.State.Health.Status}}' semgravio-admin 2>/dev/null | grep -q healthy; then
  echo ""
  echo -e "${GREEN}========================================${NC}"
  echo -e "${GREEN}   ✓ 服务已启动 (healthy)${NC}"
  echo -e "${GREEN}========================================${NC}"
  echo ""
  echo "常用命令："
  echo "  查看日志: docker logs -f semgravio-admin"
  echo "  停止服务: $COMPOSE down"
  echo "  重启服务: $COMPOSE restart"
  echo ""
else
  # 容器可能还在启动中，再等一会
  echo -e "${YELLOW}⚠ 等待健康检查...${NC}"
  sleep 20
  if docker inspect --format='{{.State.Health.Status}}' semgravio-admin 2>/dev/null | grep -q healthy; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   ✓ 服务已启动 (healthy)${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "常用命令："
    echo "  查看日志: docker logs -f semgravio-admin"
    echo "  停止服务: $COMPOSE down"
    echo "  重启服务: $COMPOSE restart"
    echo ""
  else
    echo -e "${RED}✗ 服务未就绪${NC}"
    echo -e "${YELLOW}  状态: $(docker inspect --format='{{.State.Health.Status}}' semgravio-admin 2>/dev/null || echo '未知')${NC}"
    echo -e "${YELLOW}  查看日志: docker logs semgravio-admin${NC}"
    exit 1
  fi
fi
