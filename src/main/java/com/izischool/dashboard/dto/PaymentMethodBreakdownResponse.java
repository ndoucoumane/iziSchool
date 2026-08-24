package com.izischool.dashboard.dto;

import com.izischool.payment.domain.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodBreakdownResponse {

    private PaymentMethod paymentMethod;
    private String paymentMethodLabel;
    private BigDecimal totalAmount;
    private long transactionCount;
    private BigDecimal percentage;
    private String currency;
}
