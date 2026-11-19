package com.sakshi.orders.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakshi.orders.config.KafkaTopics;
import com.sakshi.orders.event.OrderCreatedEvent;
import com.sakshi.orders.event.PaymentResultEvent;
import com.sakshi.orders.event.PaymentStatus;
import com.sakshi.orders.model.Order;
import com.sakshi.orders.model.OrderStatus;
import com.sakshi.orders.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, String> kafkaTemplate,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order create(String customerId, BigDecimal amount) {
        Order order = new Order(UUID.randomUUID().toString(), customerId, amount);
        orderRepository.save(order);

        String payload = toJson(new OrderCreatedEvent(order.getId(), customerId, amount));
        publishAfterCommit(KafkaTopics.ORDERS_CREATED, order.getId(), payload);
        return order;
    }

    /**
     * Called when the payment-service reports a result. Safe to receive twice:
     * once an order has left CREATED, later messages are ignored.
     */
    @Transactional
    public void applyPaymentResult(PaymentResultEvent event) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.orderId()));

        if (order.getStatus() != OrderStatus.CREATED) {
            log.info("Order {} already {}, ignoring duplicate payment result", order.getId(), order.getStatus());
            return;
        }

        if (event.status() == PaymentStatus.PAID) {
            order.markPaid();
        } else {
            order.markFailed();
        }
        orderRepository.save(order);
        log.info("Order {} is now {}", order.getId(), order.getStatus());
    }

    @Transactional(readOnly = true)
    public Optional<Order> find(String id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    // Send to Kafka only after the DB commit, so we never announce an order that was rolled back.
    private void publishAfterCommit(String topic, String key, String payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send(topic, key, payload);
                }
            });
        } else {
            kafkaTemplate.send(topic, key, payload);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise event", e);
        }
    }
}
