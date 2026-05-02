#!/bin/bash
# deploy.sh - 手动部署脚本
# 用法: ./deploy.sh

set -e

SERVER_HOST="${DEPLOY_HOST:-23.94.213.201}"
DEPLOY_USER="${DEPLOY_USER:-djy}"
REPO="Zc86-bir/smart-learning-platform"

echo "=== Smart Learn Platform 部署脚本 ==="
echo "服务器: $DEPLOY_USER@$SERVER_HOST"
echo "仓库: $REPO"
echo ""

# 检查 SSH 连接
echo "检查 SSH 连接..."
ssh -o ConnectTimeout=10 $DEPLOY_USER@$SERVER_HOST "echo 'SSH 连接成功'" || {
    echo "错误: 无法连接到服务器"
    exit 1
}

# 执行部署
echo ""
echo "开始部署..."
ssh $DEPLOY_USER@$SERVER_HOST << 'REMOTE_SCRIPT'
set -e

echo "1. 进入部署目录..."
cd ~/smart-learn || { mkdir -p ~/smart-learn && cd ~/smart-learn; }

echo "2. 拉取最新代码..."
if [ -d ".git" ]; then
    git pull origin master
else
    git clone git@github.com:Zc86-bir/smart-learning-platform.git .
    git checkout master
fi

echo "3. 停止旧服务..."
docker compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true

echo "4. 启动新服务..."
docker compose -f docker-compose.prod.yml up -d --build

echo "5. 等待服务健康检查..."
for i in $(seq 1 30); do
    if docker compose -f docker-compose.prod.yml ps | grep -q "healthy"; then
        echo "服务健康检查通过!"
        break
    fi
    echo "等待中... ($i/30)"
    sleep 2
done

echo ""
echo "=== 部署完成 ==="
docker compose -f docker-compose.prod.yml ps
REMOTE_SCRIPT

echo ""
echo "部署完成!"
echo "访问地址: http://$SERVER_HOST:8080"
echo "API 文档: http://$SERVER_HOST:8080/swagger-ui.html"
