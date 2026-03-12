package com.rag.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "storage")
@Data
@Component
public class StorageProperties {
    private String uploadDir = "/tmp/rag-uploads";
}

