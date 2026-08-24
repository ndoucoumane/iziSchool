package com.izischool.dashboard.dto;

import com.izischool.finance.domain.FeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingDueDateResponse {

    private LocalDate dueDate;
    private String feeName;
    private FeeType feeType;
    private BigDecimal totalAmountExpected;
    private BigDecimal totalAmountCollected;
    private BigDecimal remainingAmount;
    private long totalStudentsCount;
    private long paidStudentsCount;
    private long pendingStudentsCount;
    private boolean isToday;
    private long daysRemaining;
    private String currency;
}
