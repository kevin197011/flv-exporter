package io.github.devops.flvexporter.service;

import io.github.devops.flvexporter.config.FlvConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class FlvCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(FlvCheckService.class);
    
    private final FlvConfig flvConfig;
    private final MeterRegistry meterRegistry;
    private ExecutorService executorService;
    
    @Value("${flv.check.timeout:10000}")
    private int checkTimeout;
    
    @Value("${flv.check.threads:10}")
    private int checkThreads;
    
    @Value("${flv.check.retries:3}")
    private int maxRetries;
    
    // 存储每个流的状态 (1=正常, 0=异常)
    private final Map<String, Double> streamStatus = new ConcurrentHashMap<>();
    
    // 存储每个流的响应时间
    private final Map<String, Double> responseTime = new ConcurrentHashMap<>();
    
    // 记录已注册的指标，避免重复注册
    private final Set<String> registeredGauges = ConcurrentHashMap.newKeySet();
    
    public FlvCheckService(FlvConfig flvConfig, MeterRegistry meterRegistry) {
        this.flvConfig = flvConfig;
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        // 初始化线程池 - 在@Value注入完成后执行
        this.executorService = Executors.newFixedThreadPool(checkThreads, r -> {
            Thread t = new Thread(r, "flv-check-thread");
            t.setDaemon(true);
            return t;
        });
        
        // 注册Gauge指标
        registerGauges();
        
        logger.info("FLV检测服务初始化完成，线程池大小: {}", checkThreads);
    }
    
    private void registerGauges() {
        // 为每个配置的流注册状态和响应时间指标
        if (flvConfig.getUrls() != null) {
            for (FlvConfig.FlvUrl flvUrl : flvConfig.getFlatUrls()) {
                String streamName = flvUrl.getName();
                
                // 初始化状态为0（异常）
                streamStatus.put(streamName, 0.0);
                responseTime.put(streamName, 0.0);
                
                // 检查是否已注册状态指标
                String statusGaugeKey = "flv_stream_status_" + streamName;
                if (!registeredGauges.contains(statusGaugeKey)) {
                    Gauge.builder("flv_stream_status", streamStatus, map -> map.getOrDefault(streamName, 0.0))
                            .description("FLV stream status (1=up, 0=down)")
                            .tag("stream_name", streamName)
                            .tag("stream_url", flvUrl.getUrl())
                            .tag("project", flvUrl.getProject())
                            .tag("description", flvUrl.getDescription())
                            .register(meterRegistry);
                    registeredGauges.add(statusGaugeKey);
                }
                
                // 检查是否已注册响应时间指标
                String responseTimeGaugeKey = "flv_stream_response_time_ms_" + streamName;
                if (!registeredGauges.contains(responseTimeGaugeKey)) {
                    Gauge.builder("flv_stream_response_time_ms", responseTime, map -> map.getOrDefault(streamName, 0.0))
                            .description("FLV stream response time in milliseconds")
                            .tag("stream_name", streamName)
                            .tag("stream_url", flvUrl.getUrl())
                            .tag("project", flvUrl.getProject())
                            .register(meterRegistry);
                    registeredGauges.add(responseTimeGaugeKey);
                }
            }
        }
    }
    
    @Scheduled(fixedDelayString = "${flv.check.interval:30000}")
    public void checkAllStreams() {
        long startTime = System.currentTimeMillis();
        logger.info("开始并发检测所有FLV流状态");
        
        if (flvConfig.getUrls() == null || flvConfig.getUrls().isEmpty()) {
            logger.warn("没有配置FLV流URL");
            return;
        }
        
        List<FlvConfig.FlvUrl> flatUrls = flvConfig.getFlatUrls();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // 并发执行所有检测任务
        for (FlvConfig.FlvUrl flvUrl : flatUrls) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> checkSingleStream(flvUrl), executorService);
            futures.add(future);
        }
        
        // 等待所有检测任务完成
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.get(checkTimeout * 2L, TimeUnit.MILLISECONDS); // 设置总超时时间为单个检测超时的2倍
            
            // 统计检测结果
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            logCheckSummary(flatUrls, totalTime);
            
        } catch (TimeoutException e) {
            logger.warn("部分FLV流检测超时");
            logCheckSummary(flatUrls, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("FLV流检测过程中发生异常: {}", e.getMessage());
            logCheckSummary(flatUrls, System.currentTimeMillis() - startTime);
        }
    }
    
    private void logCheckSummary(List<FlvConfig.FlvUrl> flatUrls, long totalTime) {
        int totalStreams = flatUrls.size();
        int healthyStreams = 0;
        int unhealthyStreams = 0;
        Map<String, Integer> projectStats = new ConcurrentHashMap<>();
        Map<String, Integer> projectHealthy = new ConcurrentHashMap<>();
        
        // 统计各项目的流状态
        for (FlvConfig.FlvUrl flvUrl : flatUrls) {
            String project = flvUrl.getProject();
            String streamName = flvUrl.getName();
            
            projectStats.merge(project, 1, Integer::sum);
            
            Double status = streamStatus.get(streamName);
            if (status != null && status == 1.0) {
                healthyStreams++;
                projectHealthy.merge(project, 1, Integer::sum);
            } else {
                unhealthyStreams++;
            }
        }
        
        // 输出总体统计
        logger.info("=== FLV流检测完成 ===");
        logger.info("检测耗时: {}ms", totalTime);
        logger.info("总流数: {}, 正常: {}, 异常: {}", totalStreams, healthyStreams, unhealthyStreams);
        
        // 输出各项目统计
        for (Map.Entry<String, Integer> entry : projectStats.entrySet()) {
            String project = entry.getKey();
            int total = entry.getValue();
            int healthy = projectHealthy.getOrDefault(project, 0);
            int unhealthy = total - healthy;
            double successRate = total > 0 ? (healthy * 100.0 / total) : 0;
            
            logger.info("项目 [{}]: 总数={}, 正常={}, 异常={}, 成功率={}%", 
                       project, total, healthy, unhealthy, String.format("%.1f", successRate));
        }
        
        // 如果有异常流，列出详细信息
        if (unhealthyStreams > 0) {
            logger.warn("异常流详情:");
            for (FlvConfig.FlvUrl flvUrl : flatUrls) {
                String streamName = flvUrl.getName();
                Double status = streamStatus.get(streamName);
                if (status == null || status == 0.0) {
                    logger.warn("  - {} ({}): {}", streamName, flvUrl.getProject(), flvUrl.getUrl());
                }
            }
        }
        
        logger.info("=== 检测轮次结束 ===");
    }
    
    private void checkSingleStream(FlvConfig.FlvUrl flvUrl) {
        String streamName = flvUrl.getName();
        String streamUrl = flvUrl.getUrl();
        String project = flvUrl.getProject();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        // 带项目标签的计数器
        Counter.builder("flv_checks_total")
                .description("Total number of FLV checks performed")
                .tag("project", project)
                .register(meterRegistry)
                .increment();
        
        try {
            long startTime = System.currentTimeMillis();
            boolean isHealthy = checkFlvStreamWithRetry(streamUrl, streamName);
            long endTime = System.currentTimeMillis();
            
            if (isHealthy) {
                // 成功的流：记录状态为1，记录实际响应时间
                double responseTimeMs = endTime - startTime;
                streamStatus.put(streamName, 1.0);
                responseTime.put(streamName, responseTimeMs);
                Counter.builder("flv_checks_successful_total")
                        .description("Total number of successful FLV checks")
                        .tag("project", project)
                        .register(meterRegistry)
                        .increment();
                logger.debug("FLV流 {} 检测成功，响应时间: {}ms", streamName, responseTimeMs);
            } else {
                // 失败的流：记录状态为0，响应时间强制设为0
                streamStatus.put(streamName, 0.0);
                responseTime.put(streamName, 0.0);
                Counter.builder("flv_checks_failed_total")
                        .description("Total number of failed FLV checks")
                        .tag("project", project)
                        .register(meterRegistry)
                        .increment();
                logger.warn("FLV流 {} 检测失败，已重试{}次，响应时间设为0", streamName, maxRetries);
            }
            
        } catch (Exception e) {
            // 异常的流：记录状态为0，响应时间强制设为0
            streamStatus.put(streamName, 0.0);
            responseTime.put(streamName, 0.0);
            Counter.builder("flv_checks_failed_total")
                    .description("Total number of failed FLV checks")
                    .tag("project", project)
                    .register(meterRegistry)
                    .increment();
            logger.error("检测FLV流 {} 时发生异常: {}，响应时间设为0", streamName, e.getMessage());
        } finally {
            sample.stop(Timer.builder("flv_check_duration")
                    .description("Time taken to check FLV stream")
                    .tag("stream_name", streamName)
                    .tag("project", project)
                    .register(meterRegistry));
        }
    }
    
    private boolean checkFlvStreamWithRetry(String streamUrl, String streamName) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                boolean result = checkFlvStream(streamUrl);
                if (result) {
                    if (attempt > 1) {
                        logger.info("FLV流 {} 在第{}次重试后检测成功", streamName, attempt);
                    }
                    return true;
                }
                
                if (attempt < maxRetries) {
                    logger.warn("FLV流 {} 第{}次检测失败，准备重试", streamName, attempt);
                    // 重试前等待一小段时间，避免立即重试
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                logger.error("FLV流 {} 第{}次检测异常: {} - {}", streamName, attempt, e.getClass().getSimpleName(), e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean checkFlvStream(String streamUrl) {
        try {
            URL url = new URL(streamUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(checkTimeout);
            connection.setReadTimeout(checkTimeout * 2); // 读取超时设为连接超时的2倍
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "close"); // 使用短连接，避免连接池问题
            
            // 禁用SSL验证和优化SSL配置（如果是HTTPS）
            if (connection instanceof javax.net.ssl.HttpsURLConnection) {
                javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) connection;
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
                
                // 创建信任所有证书的SSL上下文
                try {
                    javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                    sslContext.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        }
                    }, new java.security.SecureRandom());
                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                } catch (Exception e) {
                    logger.debug("SSL配置失败: {}", e.getMessage());
                }
            }
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            
            // 检查Content-Type是否为FLV相关
            String contentType = connection.getContentType();
            
            logger.debug("检测FLV流 {} - 响应码: {}, Content-Type: {}", streamUrl, responseCode, contentType);
            
            connection.disconnect();
            
            // 200状态码，对Content-Type要求放宽
            boolean isValid = responseCode == 200;
            
            if (!isValid) {
                logger.warn("FLV流检测失败 {} - 响应码: {}, Content-Type: {}", streamUrl, responseCode, contentType);
            }
            
            return isValid;
                     
        } catch (IOException e) {
            logger.error("检测FLV流网络异常 {} - {}: {}", streamUrl, e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace(); // 打印完整堆栈跟踪
            return false;
        }
    }
    
    // 应用关闭时清理线程池
    @PreDestroy
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("关闭FLV检测线程池");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}