package com.rag.backend.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadProperties {
    private long maxFileSizeMb;
    private List<String> allowedMimeTypes;
    private List<String> allowedExtensions;
    
}
