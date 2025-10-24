#!/bin/bash

echo "🔍 测试Docker容器网络连接"
echo "=========================="

# 测试容器内网络连接
echo "1. 测试容器内网络连接..."
docker exec flv-exporter sh -c "
echo '测试DNS解析:'
nslookup tc-eu2.nbhyqx.com || echo 'DNS解析失败'

echo '测试HTTP连接:'
curl -I --connect-timeout 10 --max-time 30 https://tc-eu2.nbhyqx.com/PT/108834.flv || echo 'HTTP连接失败'

echo '测试示例FLV:'
curl -I --connect-timeout 10 --max-time 30 https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-flv-file.flv || echo 'HTTP连接失败'
"

echo ""
echo "2. 检查容器网络配置..."
docker exec flv-exporter sh -c "
echo '网络接口:'
ip addr show

echo 'DNS配置:'
cat /etc/resolv.conf

echo 'Java版本:'
java -version
"

echo ""
echo "3. 查看应用日志..."
docker logs --tail 20 flv-exporter