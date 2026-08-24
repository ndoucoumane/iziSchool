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
public class RegistrationSummaryResponse {

    private BigDecimal expectedAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private double collectionRate;
    private long totalRegistrations;
    private long paidRegistrations;
    private long partialRegistrations;
    private long unpaidRegistrations;
}
