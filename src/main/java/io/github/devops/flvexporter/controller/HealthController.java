package io.github.devops.flvexporter.controller;

import io.github.devops.flvexporter.config.FlvConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HealthController {
    
    private final FlvConfig flvConfig;
    
    public HealthController(FlvConfig flvConfig) {
        this.flvConfig = flvConfig;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("service", "FLV Exporter");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("description", "Prometheus exporter for FLV stream monitoring");
        
        // 统计信息
        List<FlvConfig.FlvUrl> flatUrls = flvConfig.getFlatUrls();
        int totalStreams = flatUrls.size();
        int projectCount = flvConfig.getUrls() != null ? flvConfig.getUrls().size() : 0;
        
        model.addAttribute("totalStreams", totalStreams);
        model.addAttribute("projectCount", projectCount);
        model.addAttribute("projects", flvConfig.getUrls());
        
        return "index";
    }
    
    @GetMapping("/api")
    @ResponseBody
    public Map<String, Object> apiInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "FLV Exporter");
        response.put("version", "1.0.0");
        response.put("description", "Prometheus exporter for FLV stream monitoring");
        response.put("endpoints", Map.of(
            "metrics", "/actuator/prometheus",
            "health", "/actuator/health",
            "config", "/config"
        ));
        return response;
    }
    
    @GetMapping("/config")
    @ResponseBody
    public Map<String, Object> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("configured_projects", flvConfig.getUrls() != null ? flvConfig.getUrls().size() : 0);
        response.put("configured_streams", flvConfig.getFlatUrls().size());
        response.put("projects", flvConfig.getUrls());
        response.put("streams", flvConfig.getFlatUrls());
        return response;
    }
}