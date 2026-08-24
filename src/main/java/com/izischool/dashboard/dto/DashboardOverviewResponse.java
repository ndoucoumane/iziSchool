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
public class DashboardOverviewResponse {

    private long students;
    private BigDecimal totalExpected;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private BigDecimal collectionRate;
    private BigDecimal todayPayments;
    private long overdueSchedules;
    private String currency;
}
