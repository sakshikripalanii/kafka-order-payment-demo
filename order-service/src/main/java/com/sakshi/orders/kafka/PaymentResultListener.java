package com.sakshi.orders.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakshi.orders.config.KafkaTopics;
import com.sakshi.orders.event.PaymentResultEvent;
import com.sakshi.orders.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentResultListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENTS_PROCESSED, groupId = "order-service")
    public void onPaymentResult(String message) throws JsonProcessingException {
        log.info("Received payment result: {}", message);
        PaymentResultEvent event = objectMapper.readValue(message, PaymentResultEvent.class);
        orderService.applyPaymentResult(event);
    }
}
