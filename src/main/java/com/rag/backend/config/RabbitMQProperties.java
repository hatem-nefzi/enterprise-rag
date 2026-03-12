package com.rag.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "rabbitmq.document")
@Data
@Component
public class RabbitMQProperties {
    private int initialConsumers = 3;
    private int maxConsumers = 10;
    private int prefetchCount = 1;
    private long messageTtlMs = 86400000; // 24 hours
    
}
