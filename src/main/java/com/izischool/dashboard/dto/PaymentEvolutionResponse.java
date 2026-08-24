package com.izischool.dashboard.dto;

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
public class PaymentEvolutionResponse {

    private String period; // Date or period string (e.g., "2026-08-23", "2026-W34", "2026-08")
    private BigDecimal collectedAmount;
    private BigDecimal expectedAmount;
    private long transactionCount;
}
