#!/bin/bash

echo "🔍 测试Mac vs Rocky Linux SSL差异"
echo "================================="

# 测试URL
TEST_URL="https://tc-eu2.nbhyqx.com/PT/108834.flv"

echo "1. 系统信息:"
echo "操作系统: $(uname -a)"
echo "Java版本: $(java -version 2>&1 | head -1)"
echo ""

echo "2. 测试系统SSL策略:"
echo "OpenSSL版本: $(openssl version 2>/dev/null || echo '未安装')"
echo ""

echo "3. 测试Java SSL配置:"
java -XshowSettings:properties -version 2>&1 | grep -E "(ssl|tls|security)" || echo "无SSL相关配置"
echo ""

echo "4. 测试证书信息:"
echo | openssl s_client -connect tc-eu2.nbhyqx.com:443 -servername tc-eu2.nbhyqx.com 2>/dev/null | \
openssl x509 -noout -text | grep -E "(Signature Algorithm|Subject:|Issuer:)" || echo "无法获取证书信息"
echo ""

echo "5. 测试curl访问:"
curl -I --connect-timeout 10 --max-time 30 "$TEST_URL" 2>&1 | head -5
echo ""

echo "6. 测试Java访问 (如果可用):"
if command -v java &> /dev/null; then
    java -Djava.security.debug=ssl -cp . -jar app.jar --test-url="$TEST_URL" 2>&1 | head -10 || echo "Java测试失败"
fi