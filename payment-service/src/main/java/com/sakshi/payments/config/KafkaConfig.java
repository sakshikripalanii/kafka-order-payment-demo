package com.sakshi.payments.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String ORDERS_CREATED = "orders.created";
    public static final String ORDERS_CREATED_DLT = "orders.created.DLT";
    public static final String PAYMENTS_PROCESSED = "payments.processed";

    @Bean
    NewTopic ordersCreatedTopic() {
        return TopicBuilder.name(ORDERS_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic ordersCreatedDeadLetterTopic() {
        return TopicBuilder.name(ORDERS_CREATED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentsProcessedTopic() {
        return TopicBuilder.name(PAYMENTS_PROCESSED).partitions(1).replicas(1).build();
    }

    /**
     * If a message keeps failing (for example, it is not valid JSON), retry 3 times
     * one second apart, then move it to orders.created.DLT so it stops blocking the topic.
     * Spring Boot picks this bean up for every @KafkaListener automatically.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
