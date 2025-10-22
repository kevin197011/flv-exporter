#!/bin/bash

# FLV Exporter Docker 构建脚本

set -e

echo "🚀 开始构建 FLV Exporter Docker 镜像..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker"
    exit 1
fi

# 选择构建方式
echo "请选择构建方式:"
echo "1) Alpine 版本 (推荐，镜像更小)"
echo "2) Ubuntu 版本 (兼容性更好)"
echo "3) 使用 docker-compose 构建完整栈"

read -p "请输入选择 (1-3): " choice

case $choice in
    1)
        echo "📦 构建 Alpine 版本..."
        docker build -f Dockerfile -t flv-exporter:alpine .
        echo "✅ Alpine 版本构建完成"
        echo "🏃 运行命令: docker run -p 8080:8080 flv-exporter:alpine"
        ;;
    2)
        echo "📦 构建 Ubuntu 版本..."
        docker build -f Dockerfile.ubuntu -t flv-exporter:ubuntu .
        echo "✅ Ubuntu 版本构建完成"
        echo "🏃 运行命令: docker run -p 8080:8080 flv-exporter:ubuntu"
        ;;
    3)
        echo "📦 构建完整监控栈..."
        docker-compose build
        echo "✅ 完整栈构建完成"
        echo "🏃 启动命令: docker-compose up -d"
        ;;
    *)
        echo "❌ 无效选择"
        exit 1
        ;;
esac

echo ""
echo "🎉 构建完成！"
echo ""
echo "📋 可用的镜像:"
docker images | grep flv-exporter || echo "   (使用 docker images 查看)"
echo ""
echo "📖 更多信息请查看 DOCKER_DEPLOYMENT.md"