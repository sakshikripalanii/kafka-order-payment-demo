package com.sakshi.payments.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(String orderId, String customerId, BigDecimal amount) {
}
