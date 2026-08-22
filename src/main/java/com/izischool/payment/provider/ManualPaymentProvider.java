package com.izischool.payment.provider;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ManualPaymentProvider implements PaymentProviderService {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MANUAL;
    }

    @Override
    public Payment initiatePayment(Payment payment, Map<String, Object> extraParams) {
        log.info("Manual payment recorded: {}", payment.getPaymentReference());
        payment.setStatus(PaymentStatus.SUCCESS);
        return payment;
    }

    @Override
    public Payment verifyPayment(Payment payment) {
        return payment;
    }
}
