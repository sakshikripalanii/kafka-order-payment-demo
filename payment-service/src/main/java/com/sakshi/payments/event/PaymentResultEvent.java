package com.sakshi.payments.event;

public record PaymentResultEvent(String orderId, PaymentStatus status, String reason) {
}
