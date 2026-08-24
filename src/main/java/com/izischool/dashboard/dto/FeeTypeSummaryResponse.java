package com.izischool.dashboard.dto;

import com.izischool.finance.domain.FeeType;
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
public class FeeTypeSummaryResponse {

    private FeeType feeType;
    private String feeTypeName;
    private BigDecimal totalExpected;
    private BigDecimal totalCollected;
    private BigDecimal totalRemaining;
    private BigDecimal totalOverdue;
    private BigDecimal collectionRate;
    private long fullyPaidStudentsCount;
    private long partialStudentsCount;
    private long unpaidStudentsCount;
    private long overdueStudentsCount;
    private String currency;
}
