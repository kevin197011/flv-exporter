#!/bin/bash

echo "📋 检查应用配置"
echo "==============="

echo "1. 访问配置端点:"
curl -s http://localhost:8080/config | jq . 2>/dev/null || curl -s http://localhost:8080/config

echo ""
echo "2. 检查应用健康状态:"
curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health

echo ""
echo "3. 检查Prometheus指标 (FLV相关):"
curl -s http://localhost:8080/actuator/prometheus | grep flv

echo ""
echo "4. 检查应用日志 (最近的检测):"
docker-compose logs --tail 20 flv-exporter | grep -E "(检测|异常|失败|成功|FlvCheckService)"