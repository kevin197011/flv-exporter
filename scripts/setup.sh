#!/bin/bash

# FLV Exporter 快速设置脚本

echo "🚀 FLV Exporter 快速设置"
echo "========================"

# 检查配置文件
if [ ! -f "config/application.yml" ]; then
    echo "📝 创建配置文件..."
    cp config/application-example.yml config/application.yml
    echo "✅ 配置文件已创建: config/application.yml"
    echo "⚠️  请编辑配置文件，添加你的FLV流URL"
    echo ""
else
    echo "✅ 配置文件已存在: config/application.yml"
fi

# 创建必要目录
mkdir -p logs
echo "✅ 日志目录已创建: logs/"

echo ""
echo "🔧 下一步操作:"
echo "1. 编辑配置文件: vim config/application.yml"
echo "2. 启动服务: docker-compose up -d"
echo "3. 访问应用: http://localhost:8080"
echo "4. 查看指标: http://localhost:8080/actuator/prometheus"
echo "5. 访问Grafana: http://localhost:3000 (admin/admin123)"
echo ""