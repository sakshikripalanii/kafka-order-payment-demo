package com.sakshi.orders.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(String orderId, String customerId, BigDecimal amount) {
}
