package com.sakshi.orders.web;

import com.sakshi.orders.model.Order;
import com.sakshi.orders.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(String id, String customerId, BigDecimal amount, OrderStatus status, Instant createdAt) {

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getAmount(), o.getStatus(), o.getCreatedAt());
    }
}
