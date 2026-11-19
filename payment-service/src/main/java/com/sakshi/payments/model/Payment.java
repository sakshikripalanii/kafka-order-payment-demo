package com.sakshi.payments.model;

import com.sakshi.payments.event.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// The order id is the primary key, so one order can only ever be paid once.
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String orderId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String reason;

    private Instant processedAt;

    protected Payment() {
        // needed by JPA
    }

    public Payment(String orderId, PaymentStatus status, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.processedAt = Instant.now();
    }

    public String getOrderId() { return orderId; }
    public PaymentStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getProcessedAt() { return processedAt; }
}
