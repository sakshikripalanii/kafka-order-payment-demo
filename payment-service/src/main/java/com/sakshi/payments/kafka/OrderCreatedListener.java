package com.sakshi.payments.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakshi.payments.config.KafkaConfig;
import com.sakshi.payments.event.OrderCreatedEvent;
import com.sakshi.payments.event.PaymentResultEvent;
import com.sakshi.payments.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderCreatedListener(PaymentService paymentService,
                                KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.ORDERS_CREATED, groupId = "payment-service")
    public void onOrderCreated(String message) throws JsonProcessingException {
        log.info("Received order event: {}", message);
        // A bad message throws here; the error handler retries it and then sends it to the DLT.
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

        paymentService.process(event).ifPresent(this::publishResult);
    }

    private void publishResult(PaymentResultEvent result) {
        try {
            kafkaTemplate.send(KafkaConfig.PAYMENTS_PROCESSED, result.orderId(),
                    objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise payment result", e);
        }
    }
}
