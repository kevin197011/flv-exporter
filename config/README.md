# 配置文件说明

## 配置文件列表

### application.yml
- **用途**: 默认配置文件
- **环境**: 开发环境
- **特点**: 基础配置，适合本地开发和测试

### application-docker.yml  
- **用途**: Docker环境配置
- **环境**: Docker容器
- **特点**: 
  - 优化了超时和并发参数
  - 配置了容器内日志路径
  - 增强了健康检查配置

### application-prod.yml
- **用途**: 生产环境配置
- **环境**: 生产部署
- **特点**:
  - 更长的检测间隔(1分钟)
  - 更高的并发数(50)
  - 更多的重试次数(5次)
  - 完整的日志配置

### logback-spring.xml
- **用途**: 日志配置
- **功能**:
  - 控制台和文件双重输出
  - 按日期和大小滚动
  - 错误日志单独记录
  - 30天日志保留

## 使用方法

### 本地开发
```bash
# 使用默认配置
./gradlew bootRun

# 指定配置文件
./gradlew bootRun --args='--spring.config.location=config/application.yml'
```

### Docker部署
```bash
# 使用docker profile
docker run -e SPRING_PROFILES_ACTIVE=docker flv-exporter

# 挂载外部配置
docker run -v ./config:/app/config flv-exporter
```

### 生产部署
```bash
# 使用生产配置
java -jar app.jar --spring.profiles.active=prod

# 指定配置文件路径
java -jar app.jar --spring.config.location=config/application-prod.yml
```

## 配置参数说明

### FLV检测参数
- `flv.check.interval`: 检测间隔(毫秒)
- `flv.check.timeout`: 连接超时(毫秒)  
- `flv.check.threads`: 并发线程数
- `flv.check.retries`: 失败重试次数

### 环境建议值
| 环境 | interval | timeout | threads | retries |
|------|----------|---------|---------|---------|
| 开发 | 30000 | 10000 | 10 | 3 |
| Docker | 30000 | 15000 | 20 | 3 |
| 生产 | 60000 | 20000 | 50 | 5 |

## 自定义配置

### 添加新的FLV流
编辑对应环境的配置文件：
```yaml
flv:
  urls:
    your_project:
      - https://your-domain.com/stream1.flv
      - https://your-domain.com/stream2.flv
```

### 修改监控参数
根据实际网络环境调整：
```yaml
flv:
  check:
    interval: 45000  # 45秒检测一次
    timeout: 12000   # 12秒超时
    threads: 15      # 15个并发线程
    retries: 4       # 重试4次
```

### 日志配置
修改 `logback-spring.xml` 中的参数：
- 日志级别: `<logger name="..." level="DEBUG">`
- 文件大小: `<maxFileSize>200MB</maxFileSize>`
- 保留天数: `<maxHistory>60</maxHistory>`

## 注意事项

1. **生产环境**: 请替换 `application-prod.yml` 中的示例URL为实际地址
2. **性能调优**: 根据服务器性能调整线程数和超时时间
3. **日志管理**: 定期清理日志文件，避免磁盘空间不足
4. **安全配置**: 生产环境建议限制actuator端点访问