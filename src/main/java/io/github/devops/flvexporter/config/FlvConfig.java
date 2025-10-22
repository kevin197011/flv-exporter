package io.github.devops.flvexporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Component
@ConfigurationProperties(prefix = "flv")
public class FlvConfig {
    
    private Map<String, List<String>> urls;
    
    public Map<String, List<String>> getUrls() {
        return urls;
    }
    
    public void setUrls(Map<String, List<String>> urls) {
        this.urls = urls;
    }
    
    // 获取扁平化的URL列表，包含项目信息
    public List<FlvUrl> getFlatUrls() {
        List<FlvUrl> flatUrls = new ArrayList<>();
        if (urls != null) {
            for (Map.Entry<String, List<String>> entry : urls.entrySet()) {
                String project = entry.getKey();
                List<String> urlList = entry.getValue();
                if (urlList != null) {
                    for (String url : urlList) {
                        String[] pathAndId = extractPathAndId(url);
                        String pathPart = pathAndId[0];
                        String streamId = pathAndId[1];
                        String streamName = project + "_" + pathPart + "_" + streamId;
                        flatUrls.add(new FlvUrl(streamName, url, project, "Stream " + streamId + " of project " + project));
                    }
                }
            }
        }
        return flatUrls;
    }
    
    // 从URL中提取路径部分和流ID
    private String[] extractPathAndId(String url) {
        try {
            // 解析URL，如 "https://xxx.xxx.com/video/test.flv"
            // 提取路径部分 "/video/test.flv"
            String path = url.substring(url.indexOf("://") + 3);
            int pathStart = path.indexOf('/');
            if (pathStart != -1) {
                String fullPath = path.substring(pathStart + 1); // "video/test.flv"
                
                // 分割路径和文件名
                String[] pathParts = fullPath.split("/");
                if (pathParts.length >= 2) {
                    String pathPart = pathParts[pathParts.length - 2]; // "video"
                    String fileName = pathParts[pathParts.length - 1]; // "test.flv"
                    
                    // 移除扩展名，得到流ID
                    String streamId = fileName;
                    if (fileName.contains(".")) {
                        streamId = fileName.substring(0, fileName.lastIndexOf('.'));
                    }
                    
                    return new String[]{pathPart, streamId};
                }
            }
            
            // 如果解析失败，使用默认值
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            String streamId = fileName.contains(".") ? 
                fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            return new String[]{"STREAM", streamId};
            
        } catch (Exception e) {
            // 如果提取失败，使用URL的hash值作为ID
            return new String[]{"STREAM", String.valueOf(Math.abs(url.hashCode()))};
        }
    }
    
    public static class FlvUrl {
        private String name;
        private String url;
        private String project;
        private String description;
        
        public FlvUrl() {}
        
        public FlvUrl(String name, String url, String project, String description) {
            this.name = name;
            this.url = url;
            this.project = project;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getProject() {
            return project;
        }
        
        public void setProject(String project) {
            this.project = project;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}