#!/bin/bash

echo "🚀 在主机上直接运行FLV Exporter"
echo "================================"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 未找到Java，请安装Java 17+"
    exit 1
fi

# 构建应用
echo "📦 构建应用..."
./gradlew bootJar

# 检查JAR文件
JAR_FILE=$(find build/libs -name "*.jar" | head -1)
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ 未找到JAR文件"
    exit 1
fi

echo "✅ 找到JAR文件: $JAR_FILE"

# 设置配置文件路径
CONFIG_FILE="config/application.yml"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "⚠️  配置文件不存在，复制示例配置..."
    cp config/application-example.yml "$CONFIG_FILE"
fi

# 启动应用
echo "🎯 启动FLV Exporter..."
echo "配置文件: $CONFIG_FILE"
echo "访问地址: http://localhost:8080"
echo "指标地址: http://localhost:8080/actuator/prometheus"
echo ""

java -Xms256m -Xmx512m \
     -XX:+UseG1GC \
     -Dspring.config.location="file:$CONFIG_FILE" \
     -jar "$JAR_FILE"