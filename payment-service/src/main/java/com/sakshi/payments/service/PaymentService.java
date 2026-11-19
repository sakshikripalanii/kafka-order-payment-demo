package com.sakshi.payments.service;

import com.sakshi.payments.event.OrderCreatedEvent;
import com.sakshi.payments.event.PaymentResultEvent;
import com.sakshi.payments.event.PaymentStatus;
import com.sakshi.payments.model.Payment;
import com.sakshi.payments.repo.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // Fake rule so you can see both outcomes: anything above this fails.
    static final BigDecimal LIMIT = new BigDecimal("10000");

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Returns the result to publish, or empty if this order was already processed
     * (Kafka can deliver the same message more than once).
     */
    @Transactional
    public Optional<PaymentResultEvent> process(OrderCreatedEvent event) {
        if (paymentRepository.existsById(event.orderId())) {
            log.info("Order {} already processed, skipping", event.orderId());
            return Optional.empty();
        }

        boolean approved = event.amount().compareTo(LIMIT) <= 0;
        PaymentStatus status = approved ? PaymentStatus.PAID : PaymentStatus.FAILED;
        String reason = approved ? null : "Amount exceeds limit of " + LIMIT;

        paymentRepository.save(new Payment(event.orderId(), status, reason));
        log.info("Order {} payment {}", event.orderId(), status);
        return Optional.of(new PaymentResultEvent(event.orderId(), status, reason));
    }
}
