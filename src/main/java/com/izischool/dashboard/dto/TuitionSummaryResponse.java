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
public class TuitionSummaryResponse {

    private BigDecimal expectedAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private double collectionRate;
    private long numberOfStudentsExpected;
    private long numberOfStudentsPaid;
    private long numberOfStudentsPartiallyPaid;
    private long numberOfStudentsUnpaid;
}
