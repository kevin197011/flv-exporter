#!/bin/bash

echo "🔍 网络连接对比测试"
echo "==================="

# 测试URL列表
urls=(
    "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-flv-file.flv"
    "https://cn-kkw.gdgazx.com/nlive/N7101.flv"
    "https://tc-eu2.nbhyqx.com/PT/108834.flv"
)

for url in "${urls[@]}"; do
    echo ""
    echo "测试URL: $url"
    echo "----------------------------------------"
    
    # 宿主机测试
    echo "🖥️  宿主机测试:"
    timeout 10 curl -I --connect-timeout 5 --max-time 10 "$url" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "    ✅ 宿主机访问成功"
    else
        echo "    ❌ 宿主机访问失败"
    fi
    
    # 容器内测试
    echo "🐳 容器内测试:"
    docker exec flv-exporter timeout 10 curl -I --connect-timeout 5 --max-time 10 "$url" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "    ✅ 容器内访问成功"
    else
        echo "    ❌ 容器内访问失败"
    fi
done

echo ""
echo "🔧 容器网络信息:"
docker exec flv-exporter sh -c "
echo 'DNS配置:'
cat /etc/resolv.conf
echo ''
echo '网络接口:'
ip addr show | grep -E 'inet|UP'
echo ''
echo 'Java网络属性:'
java -XshowSettings:properties -version 2>&1 | grep -E 'network|ssl|tls'
"