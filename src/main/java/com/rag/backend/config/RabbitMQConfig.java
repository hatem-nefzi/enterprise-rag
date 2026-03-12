package com.rag.backend.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConfig {
    private final RabbitMQProperties properties;
    // Constants — queue/exchange/routing key names in one place
    public static final String DOCUMENT_PROCESSING_QUEUE = "document.processing.queue";
    public static final String DOCUMENT_PROCESSING_DLQ = "document.processing.dlq";
    
    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "docuement.dlx";

    public static final String ROUTING_KEY_PROCESS = "document.process";
    public static final String ROUTING_KEY_FAILED = "document.failed";

    // Exchanges:
    // Two exchanges: one for normal flow, one dedicated dead-letter exchange.
    // Keeping them separate prevents a failed DLQ message from looping back
    // into the main exchange and causing infinite retry cycles.

    @Bean
    public TopicExchange documentExchange() {
        return ExchangeBuilder
                .topicExchange(DOCUMENT_EXCHANGE)
                .durable(true)// SURVICES RABBITMQ RESTARTS 
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        // DirectExchange for DLX — no complex routing needed for dead letters,
        // just route by exact key "document.failed"
        return ExchangeBuilder
            .directExchange(DEAD_LETTER_EXCHANGE)
            .durable(true)
            .build();
    }

    // Queues
    // Main queue tells RabbitMQ: "if this message fails, send it to the DLX
    // with routing key document.failed". The DLX then routes it to the DLQ.
    // -------------------------------------------------------------------------

    @Bean
    public Queue documentProcessingQueue() {
        return QueueBuilder.durable(DOCUMENT_PROCESSING_QUEUE)
            // where to send failed messages
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            // routing key to use when sending to DLX
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY_FAILED)
            // message expires after 24h if nobody processes it (silent failure prevention)
            .withArgument("x-message-ttl", properties.getMessageTtlMs())
            .build();
    }
    
    @Bean
    public Queue documentProcessingDLQ() {
        // Simple durable queue — no DLX here, dead letters stop here
        // You can manually inspect/replay these messages later
        return QueueBuilder.durable(DOCUMENT_PROCESSING_DLQ)
            .durable()
            .build();
    }

    // -------------------------------------------------------------------------
    // Bindings
    // Wires exchanges to queues.
    // -------------------------------------------------------------------------

    @Bean
    public Binding documentProcessingBinding() {
        // main queue receives messages sent to document.exchange with key "document.process"
        return BindingBuilder
            .bind(documentProcessingQueue())
            .to(documentExchange())
            .with(ROUTING_KEY_PROCESS);
    }

    @Bean
    public Binding deadLetterBinding() {
        // DLQ receives messages sent to document.dlx with key "document.failed"
        return BindingBuilder
            .bind(documentProcessingDLQ())
            .to(deadLetterExchange())
            .with(ROUTING_KEY_FAILED);
    }

    // -------------------------------------------------------------------------
    // Message Converter
    // Serializes/deserializes Java POJOs to JSON automatically.
    // Without this, Spring sends raw bytes — painful to debug.
    // -------------------------------------------------------------------------

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // adds type info to message headers so deserialization works correctly
        converter.setCreateMessageIds(true);
        return converter;
    }

    // -------------------------------------------------------------------------
    // RabbitTemplate
    // The main class you use to SEND messages from your code.
    // Configured with JSON converter and mandatory flag.
    // -------------------------------------------------------------------------

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());

        // mandatory = true means RabbitMQ will return the message to you
        // if no queue matches the routing key, instead of silently dropping it
        template.setMandatory(true);

        // log returned (unroutable) messages instead of silently losing them
        template.setReturnsCallback(returned ->
            log.error("Message returned unroutable: exchange={}, routingKey={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyText())
        );

        return template;
    }

    // -------------------------------------------------------------------------
    // Listener Container Factory
    // Controls how your @RabbitListener consumers behave.
    // -------------------------------------------------------------------------

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());

        // MANUAL ack: message is only removed from queue after your code
        // explicitly acknowledges it. If your service crashes mid-processing,
        // the message goes back to the queue automatically — nothing is lost.
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // prefetch = 1: RabbitMQ sends only 1 message per consumer at a time.
        // Consumer must ack before receiving the next one.
        // Prevents one slow consumer from hoarding all messages.
        factory.setPrefetchCount(properties.getPrefetchCount());

        // start with N consumers, scale up to max under load
        factory.setConcurrentConsumers(properties.getInitialConsumers());
        factory.setMaxConcurrentConsumers(properties.getMaxConsumers());

        // retry failed messages before sending to DLQ
        factory.setDefaultRequeueRejected(false); // don't requeue on rejection, send to DLQ

        return factory;
    }


}
