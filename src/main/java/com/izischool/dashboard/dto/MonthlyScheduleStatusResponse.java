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
public class MonthlyScheduleStatusResponse {

    private int month;
    private int year;
    private String periodLabel;
    private BigDecimal totalExpected;
    private BigDecimal totalCollected;
    private BigDecimal totalRemaining;
    private BigDecimal totalOverdue;
    private BigDecimal collectionRate;
    private long totalSchedulesCount;
    private long paidSchedulesCount;
    private long partialSchedulesCount;
    private long overdueSchedulesCount;
    private long pendingSchedulesCount;
    private long upToDateStudentsCount;
    private long overdueStudentsCount;
    private String currency;
}
