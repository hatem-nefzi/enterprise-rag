package com.rag.backend.service;

import java.util.UUID;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.rag.backend.config.RabbitMQConfig;
import com.rag.backend.exception.MessagePublishingException;
import com.rag.backend.model.message.DocumentProcessingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPublisher {
    
    private final RabbitTemplate rabbitTemplate;

    public void publishForProcessing(DocumentProcessingMessage message) {

        // assing a unique message ID if not already set 
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }

        log.info("Publishing document for processing : messageId={}, documentId={}, filename={}", 
                message.getMessageId(), message.getDocumentId(), message.getFilename());
    
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_PROCESS,
                message

            );
            log.info("Message published successfully: messageId={}", message.getMessageId());
        }
        catch (AmqpException e) {
            log.error("Failed to publish message for documentId={}: {}", message.getDocumentId(), e.getMessage());
            throw new MessagePublishingException("Failed to queue document for processing : " + message.getDocumentId(), e);
        }
    
}
}
