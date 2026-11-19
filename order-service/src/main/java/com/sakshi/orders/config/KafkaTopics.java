package com.sakshi.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {

    public static final String ORDERS_CREATED = "orders.created";
    public static final String PAYMENTS_PROCESSED = "payments.processed";

    @Bean
    NewTopic ordersCreatedTopic() {
        return TopicBuilder.name(ORDERS_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentsProcessedTopic() {
        return TopicBuilder.name(PAYMENTS_PROCESSED).partitions(1).replicas(1).build();
    }
}
