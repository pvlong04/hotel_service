package org.example.hotel_service.services.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentTimeoutScheduler {

    PaymentService paymentService;

    @Scheduled(fixedDelayString = "${app.payment.vnpay.reconcile-delay-ms:60000}")
    public void reconcileExpiredPendingVnPayPayments() {
        int cancelled = paymentService.cancelExpiredPendingVnPayPayments();
        if (cancelled > 0) {
            log.info("VNPay reconcile job cancelled {} reservation(s)", cancelled);
        }
    }
}

