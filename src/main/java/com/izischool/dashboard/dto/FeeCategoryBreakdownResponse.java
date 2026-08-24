package com.izischool.dashboard.dto;

import com.izischool.finance.domain.FeeType;
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
public class FeeCategoryBreakdownResponse {

    private UUID feeId;
    private String feeName;
    private String feeCode;
    private FeeType feeType;
    private BigDecimal expectedAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal overdueAmount;
    private BigDecimal percentageOfTotalCollected;
    private BigDecimal collectionRate;
    private long totalSchedulesCount;
    private long paidSchedulesCount;
    private String currency;
}
