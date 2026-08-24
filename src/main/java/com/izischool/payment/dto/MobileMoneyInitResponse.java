package com.izischool.payment.dto;

import com.izischool.payment.domain.PaymentProvider;
import com.izischool.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyInitResponse {

    private UUID paymentId;
    private PaymentStatus status;
    private PaymentProvider provider;
    private String paymentUrl;
    private String reference;
}
