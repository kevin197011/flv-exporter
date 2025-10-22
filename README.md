# FLV Stream Prometheus Exporter

一个用于监控FLV视频流状态的Prometheus Exporter，定期检测配置的FLV流是否可正常访问。

## 功能特性

- 🎯 定期批量检测FLV视频流状态
- ⚡ 并发执行检测任务，提高检测效率
- 🔄 失败自动重试机制，提高检测可靠性
- 📊 导出Prometheus格式的监控指标
- ⚙️ 通过YAML配置文件管理FLV流URL
- 🏷️ 支持按项目分组管理流URL
- 🔍 支持响应时间监控
- 📈 提供详细的成功/失败统计

## 监控指标

### 流状态指标
- `flv_stream_status` - FLV流状态 (1=正常, 0=异常)
  - Labels: `stream_name` (格式: `项目名_路径_流ID`), `stream_url`, `project`, `description`

### 响应时间指标  
- `flv_stream_response_time_ms` - FLV流响应时间(毫秒)
  - Labels: `stream_name` (格式: `项目名_路径_流ID`), `stream_url`, `project`

### 检测统计指标
- `flv_checks_total` - 总检测次数
  - Labels: `project`
- `flv_checks_successful_total` - 成功检测次数
  - Labels: `project`
- `flv_checks_failed_total` - 失败检测次数
  - Labels: `project`
- `flv_check_duration` - 单次检测耗时
  - Labels: `stream_name`, `project`

## 配置说明

### 应用配置 (application.yml)
```yaml
spring:
  application:
    name: flv-exporter

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

flv:
  check:
    interval: 30000  # 检测间隔(毫秒)，默认30秒
    timeout: 10000   # 连接超时(毫秒)，默认10秒
    threads: 10      # 并发检测线程数，默认10个
    retries: 3       # 失败重试次数，默认3次
  urls:
    g01:  # 项目名称，会作为监控指标的project标签
      - https://example.com/stream1.flv
      - https://example.com/stream2.flv
    g02:
      - https://example.com/stream3.flv
      - https://example.com/stream4.flv
```

## 快速开始

### 1. 配置FLV流
编辑 `src/main/resources/application.yml` 文件，添加需要监控的FLV流：

```yaml
flv:
  check:
    interval: 30000  # 检测间隔
    timeout: 10000   # 连接超时
    threads: 10      # 并发线程数
    retries: 3       # 失败重试次数
  urls:
    demo:  # 演示项目
      - https://sample-videos.com/zip/10/flv/SampleVideo_1280x720_1mb.flv
      - https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-flv-file.flv
    test:  # 测试项目
      - https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.flv
      - https://media.w3.org/2010/05/sintel/trailer.flv
    live:  # 直播项目
      - https://vjs.zencdn.net/v/oceans.flv
      - https://techslides.com/demos/sample-videos/small.flv
```

### 2. 运行应用
```bash
./gradlew bootRun
```

### 3. 访问监控端点
- 应用首页: http://localhost:8080/
- 配置信息: http://localhost:8080/config
- Prometheus指标: http://localhost:8080/actuator/prometheus
- 健康检查: http://localhost:8080/actuator/health

## 构建部署

### 构建JAR包
```bash
./gradlew build
```

### Docker部署
```bash
# 快速启动（包含Prometheus + Grafana + AlertManager）
docker-compose up -d

# 仅启动FLV Exporter
docker build -t flv-exporter .
docker run -p 8080:8080 flv-exporter
```

详细部署说明请参考 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)

### Prometheus配置
在Prometheus配置中添加scrape配置：

```yaml
scrape_configs:
  - job_name: 'flv-exporter'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
```

## Grafana Dashboard

项目包含了预配置的Grafana Dashboard，支持：
- 📊 实时监控FLV流状态
- 🔍 按项目筛选查看
- 📈 成功率和响应时间趋势
- 📋 详细的流状态表格

### 导入Dashboard
1. 在Grafana中导入 `grafana-dashboard.json` 文件
2. 配置Prometheus数据源
3. 详细说明请参考 [GRAFANA_DASHBOARD.md](GRAFANA_DASHBOARD.md)

## 监控告警

### Grafana面板查询示例
```promql
# FLV流状态
flv_stream_status

# 异常流数量
sum(flv_stream_status == 0)

# 按项目查看异常流
sum(flv_stream_status == 0) by (project)

# 平均响应时间
avg(flv_stream_response_time_ms)

# 按项目查看平均响应时间
avg(flv_stream_response_time_ms) by (project)

# 成功率（按项目）
rate(flv_checks_successful_total[5m]) / rate(flv_checks_total[5m]) * 100

# 项目总体成功率
sum(rate(flv_checks_successful_total[5m])) / sum(rate(flv_checks_total[5m])) * 100

# 查看特定流状态（如demo项目的flv路径下SampleVideo_1280x720_1mb流）
flv_stream_status{stream_name="demo_flv_SampleVideo_1280x720_1mb"}
```

### 告警规则示例
```yaml
groups:
  - name: flv_stream_alerts
    rules:
      - alert: FLVStreamDown
        expr: flv_stream_status == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "FLV流 {{ $labels.stream_name }} 无法访问"
          description: "FLV流 {{ $labels.stream_name }} ({{ $labels.stream_url }}) 已离线超过1分钟"
          
      - alert: FLVStreamHighLatency  
        expr: flv_stream_response_time_ms > 5000
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "FLV流 {{ $labels.stream_name }} 响应时间过高"
          description: "FLV流 {{ $labels.stream_name }} 响应时间为 {{ $value }}ms，超过5秒阈值"
```

## 技术栈

- Java 17
- Spring Boot 3.5.6
- Micrometer + Prometheus
- Gradle

## 许可证

MIT License