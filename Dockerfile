# 多阶段构建 - 构建阶段 (使用Ubuntu 18.04基础的Java 11)
FROM gradle:7.6-jdk11-focal AS builder

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

# 运行阶段 (使用Ubuntu 18.04，对旧SSL算法宽松)
FROM ubuntu:18.04

# 安装Java 11和必要工具 (Ubuntu 18.04对SHA1withRSA等算法宽松)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        openjdk-11-jre-headless \
        curl \
        ca-certificates \
        tzdata && \
    # 设置时区
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    # 更新CA证书
    update-ca-certificates && \
    # 清理缓存
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

# JVM参数优化 - Ubuntu 18.04 + Java 11兼容性设置
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.useSystemProxies=true \
    -Dcom.sun.net.ssl.checkRevocation=false \
    -Dtrust_all_cert=true"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=file:/app/config/application.yml -jar app.jar"]