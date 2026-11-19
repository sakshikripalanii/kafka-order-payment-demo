package com.sakshi.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakshi.orders.event.PaymentResultEvent;
import com.sakshi.orders.event.PaymentStatus;
import com.sakshi.orders.model.Order;
import com.sakshi.orders.model.OrderStatus;
import com.sakshi.orders.repo.OrderRepository;
import com.sakshi.orders.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, kafkaTemplate, new ObjectMapper());
    }

    @Test
    void createSavesOrderAndPublishesEvent() {
        Order order = orderService.create("cust-1", new BigDecimal("250.00"));

        verify(orderRepository).save(any(Order.class));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("orders.created"), eq(order.getId()), payload.capture());
        assertTrue(payload.getValue().contains(order.getId()));
        assertTrue(payload.getValue().contains("cust-1"));
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void paymentResultMarksOrderPaid() {
        Order order = new Order("o-1", "cust-1", new BigDecimal("100.00"));
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent("o-1", PaymentStatus.PAID, null));

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void duplicatePaymentResultIsIgnored() {
        Order order = new Order("o-2", "cust-1", new BigDecimal("100.00"));
        order.markPaid();
        when(orderRepository.findById("o-2")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent("o-2", PaymentStatus.FAILED, "late message"));

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
