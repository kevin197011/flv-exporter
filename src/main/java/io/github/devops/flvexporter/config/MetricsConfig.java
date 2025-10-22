package io.github.devops.flvexporter.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // 只保留FLV相关的指标，过滤掉其他所有指标
            registry.config()
                    .meterFilter(MeterFilter.denyNameStartsWith("jvm"))
                    .meterFilter(MeterFilter.denyNameStartsWith("system"))
                    .meterFilter(MeterFilter.denyNameStartsWith("process"))
                    .meterFilter(MeterFilter.denyNameStartsWith("tomcat"))
                    .meterFilter(MeterFilter.denyNameStartsWith("http"))
                    .meterFilter(MeterFilter.denyNameStartsWith("logback"))
                    .meterFilter(MeterFilter.denyNameStartsWith("hikaricp"))
                    .meterFilter(MeterFilter.denyNameStartsWith("jdbc"))
                    .meterFilter(MeterFilter.denyNameStartsWith("spring"))
                    .meterFilter(MeterFilter.denyNameStartsWith("application"))
                    .meterFilter(MeterFilter.denyNameStartsWith("disk"))
                    .meterFilter(MeterFilter.denyNameStartsWith("executor"))
                    // 只允许FLV相关的指标通过
                    .meterFilter(MeterFilter.acceptNameStartsWith("flv"));
        };
    }
}