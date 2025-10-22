# GitHub Actions 自动构建和推送

## 工作流说明

这个GitHub Actions工作流会在以下情况下自动触发：

### 触发条件
- **推送到main分支** - 自动构建并推送Docker镜像
- **创建版本标签** (如 `v1.0.0`) - 构建并推送带版本标签的镜像
- **Pull Request到main分支** - 仅构建测试，不推送镜像

### 镜像标签策略
- `latest` - main分支的最新版本
- `main-<commit-sha>` - main分支特定提交的镜像
- `v1.0.0` - 版本标签对应的镜像
- `1.0` - 主要版本号镜像

### 使用方法

1. **推送代码到main分支**
   ```bash
   git push origin main
   ```
   
2. **创建版本发布**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **拉取镜像**
   ```bash
   # 拉取最新版本
   docker pull ghcr.io/your-username/flv-exporter:latest
   
   # 拉取特定版本
   docker pull ghcr.io/your-username/flv-exporter:v1.0.0
   ```

### 镜像特性
- 支持多架构：`linux/amd64`, `linux/arm64`
- 使用构建缓存加速构建过程
- 自动推送到GitHub Container Registry (ghcr.io)

### 权限要求
工作流使用 `GITHUB_TOKEN` 自动认证，无需额外配置。确保仓库设置中启用了：
- Actions 权限
- Packages 写入权限