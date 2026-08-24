package com.izischool.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto implements Serializable {

    private long totalStudents;
    private BigDecimal totalExpectedAmount;
    private BigDecimal totalCollectedAmount;
    private BigDecimal totalOutstandingAmount;
    private BigDecimal totalOverdueAmount;
    private long paymentsTodayCount;
    private BigDecimal paymentsTodayAmount;
    private BigDecimal paymentsThisMonthAmount;
    private long overdueSchedulesCount;
    private BigDecimal collectionRate;
    private String currency;
}
