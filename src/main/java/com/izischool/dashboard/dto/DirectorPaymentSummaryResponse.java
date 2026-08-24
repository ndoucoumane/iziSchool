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
public class DirectorPaymentSummaryResponse {

    private BigDecimal totalExpected;
    private BigDecimal totalCollected;
    private BigDecimal totalRemaining;
    private BigDecimal totalOverdue;
    private BigDecimal collectionRate;
    private BigDecimal todayCollected;
    private BigDecimal thisMonthCollected;
    private long upToDateStudentsCount;
    private long partialPaymentStudentsCount;
    private long unpaidStudentsCount;
    private long overdueStudentsCount;
    private long totalStudentsCount;
    private String currency;
}
