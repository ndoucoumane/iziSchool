package com.izischool.payment.provider;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WavePaymentProvider implements PaymentProviderService {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.WAVE;
    }

    @Override
    public Payment initiatePayment(Payment payment, Map<String, Object> extraParams) {
        log.info("Initiating Wave payment for ref: {}, amount: {}", payment.getPaymentReference(), payment.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    @Override
    public Payment verifyPayment(Payment payment) {
        log.info("Verifying Wave transaction: {}", payment.getProviderTransactionId());
        return payment;
    }
}
