# GitHub Actions Secrets 配置指南

## 需要配置的 Secrets

在 GitHub 仓库设置中添加以下 Secrets：

### 1. DEPLOY_HOST
```
23.94.213.201
```

### 2. DEPLOY_USERNAME
```
djy
```

### 3. DEPLOY_SSH_KEY
服务器上的 SSH 私钥（用于 GitHub Actions 连接服务器）

**获取方法：**
1. SSH 到服务器：`ssh djy@23.94.213.201`
2. 查看私钥：`cat ~/.ssh/id_ed25519`
3. 将内容复制到 GitHub Secrets

### 4. 服务器配置（首次部署前执行）

SSH 到服务器后执行以下命令：

```bash
# 1. 确保 Docker 可用
sudo chmod 666 /var/run/docker.sock
sudo usermod -aG docker djy

# 2. 创建部署目录
mkdir -p ~/smart-learn

# 3. 创建 .env.docker 文件
cat > ~/smart-learn/.env.docker << 'EOF'
DB_PASSWORD=T9$mK2pL8nQ4wX7vR3
JWT_SECRET=Y2hhbmdlLXRoaXMtdG8tYS1sb25nLXNlY3JldC1rZXktaW4tcHJvZHVjdGlvbi1lbnZpcm9ubWVudA==
MIMO_API_KEY=sk-c5sd1mtz07qnlmi0m5gdzj92garah98ftr1vwm164y80o9zw
EOF
```

## 配置步骤

1. 进入 GitHub 仓库 → Settings → Secrets and variables → Actions
2. 点击 "New repository secret"
3. 依次添加上述 3 个 Secrets
4. 推送代码到 master 分支触发 CI/CD

## 触发方式

- **推送代码**: `git push origin master`
- **手动触发**: Actions → CI/CD Pipeline → Run workflow

## 部署流程

```
Push to master
    ↓
[Build] Maven 构建 + 测试
    ↓
[Docker] 构建镜像并推送到 GHCR
    ↓
[Deploy] SSH 到服务器，拉取代码，重启服务
    ↓
完成！访问 http://23.94.213.201:8080
```
