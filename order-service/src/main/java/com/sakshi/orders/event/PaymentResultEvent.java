package com.sakshi.orders.event;

public record PaymentResultEvent(String orderId, PaymentStatus status, String reason) {
}
