package com.izischool.payment.dto;

import com.izischool.payment.domain.PaymentProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateMobileMoneyRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private UUID scheduleId;

    private UUID parentId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be strictly positive")
    private BigDecimal amount;

    @NotNull(message = "Provider is required (WAVE, ORANGE_MONEY, FREE_MONEY, MTN_MOMO, MOOV_MONEY)")
    private PaymentProvider provider;

    private String phone;
}
