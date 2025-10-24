#!/bin/bash

echo "🔍 测试SSL连接性能"
echo "=================="

# 测试几个FLV流的SSL握手时间
urls=(
    "https://cn-kkw.gdgazx.com/nlive/N7101.flv"
    "https://tc-eu2.nbhyqx.com/PT/108834.flv"
    "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-flv-file.flv"
)

for url in "${urls[@]}"; do
    echo "测试: $url"
    
    # 宿主机测试
    echo "  宿主机:"
    time curl -I --connect-timeout 30 --max-time 60 "$url" 2>/dev/null && echo "    ✅ 成功" || echo "    ❌ 失败"
    
    # 容器内测试
    echo "  容器内:"
    docker exec flv-exporter sh -c "time curl -I --connect-timeout 30 --max-time 60 '$url'" 2>/dev/null && echo "    ✅ 成功" || echo "    ❌ 失败"
    
    echo ""
done

echo "🔧 建议:"
echo "1. 如果宿主机成功但容器失败，说明是Docker网络问题"
echo "2. 如果都失败，说明是网络环境问题，需要增加超时时间"
echo "3. 如果SSL握手很慢，考虑使用HTTP而不是HTTPS"