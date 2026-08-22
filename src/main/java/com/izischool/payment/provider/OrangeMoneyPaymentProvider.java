package com.izischool.payment.provider;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class OrangeMoneyPaymentProvider implements PaymentProviderService {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.ORANGE_MONEY;
    }

    @Override
    public Payment initiatePayment(Payment payment, Map<String, Object> extraParams) {
        log.info("Initiating Orange Money payment for ref: {}, amount: {}", payment.getPaymentReference(), payment.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    @Override
    public Payment verifyPayment(Payment payment) {
        log.info("Verifying Orange Money transaction: {}", payment.getProviderTransactionId());
        return payment;
    }
}
