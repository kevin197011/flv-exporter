# 多阶段构建 - 构建阶段 (使用Java 11，SSL兼容性更好)
FROM gradle:7.6-jdk11 AS builder

# 设置工作目录
WORKDIR /app

# 复制构建文件
COPY build.gradle settings.gradle ./
COPY gradle gradle

# 下载依赖（利用Docker缓存）
RUN gradle dependencies --no-daemon

# 复制源代码
COPY src src

# 构建应用
RUN gradle bootJar --no-daemon -x test

# 运行阶段 (使用Java 11，对旧SSL算法更宽松)
FROM eclipse-temurin:11-jre

# 安装必要工具和设置时区
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r flvexporter && \
    useradd -r -g flvexporter flvexporter

# 设置工作目录
WORKDIR /app

# 复制构建的jar文件
COPY --from=builder /app/build/libs/*.jar app.jar

# 创建配置和日志目录
RUN mkdir -p /app/config /app/logs && \
    chown -R flvexporter:flvexporter /app

# 切换到应用用户
USER flvexporter

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM参数优化 - Java 11兼容性设置
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Djdk.tls.disabledAlgorithms=SSLv3,RC4,DES \
    -Djdk.certpath.disabledAlgorithms=MD2,MD5 \
    -Dcom.sun.net.ssl.checkRevocation=false \
    -Dtrust_all_cert=true"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=file:/app/config/application.yml -jar app.jar"]