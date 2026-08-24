package com.izischool.payment.provider;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.PaymentProvider;

import java.util.Map;

public interface PaymentProviderService {

    PaymentProvider getProvider();

    Payment initiatePayment(Payment payment, Map<String, Object> extraParams);

    Payment verifyPayment(Payment payment);
}
