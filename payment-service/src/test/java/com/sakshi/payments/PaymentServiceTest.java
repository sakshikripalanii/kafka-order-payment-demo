package com.sakshi.payments;

import com.sakshi.payments.event.OrderCreatedEvent;
import com.sakshi.payments.event.PaymentResultEvent;
import com.sakshi.payments.event.PaymentStatus;
import com.sakshi.payments.model.Payment;
import com.sakshi.payments.repo.PaymentRepository;
import com.sakshi.payments.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository);
    }

    @Test
    void smallAmountIsPaid() {
        when(paymentRepository.existsById("o-1")).thenReturn(false);

        Optional<PaymentResultEvent> result =
                paymentService.process(new OrderCreatedEvent("o-1", "cust-1", new BigDecimal("500")));

        assertTrue(result.isPresent());
        assertEquals(PaymentStatus.PAID, result.get().status());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void largeAmountFails() {
        when(paymentRepository.existsById("o-2")).thenReturn(false);

        Optional<PaymentResultEvent> result =
                paymentService.process(new OrderCreatedEvent("o-2", "cust-1", new BigDecimal("15000")));

        assertTrue(result.isPresent());
        assertEquals(PaymentStatus.FAILED, result.get().status());
    }

    @Test
    void alreadyProcessedOrderIsSkipped() {
        when(paymentRepository.existsById("o-3")).thenReturn(true);

        Optional<PaymentResultEvent> result =
                paymentService.process(new OrderCreatedEvent("o-3", "cust-1", new BigDecimal("500")));

        assertTrue(result.isEmpty());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
