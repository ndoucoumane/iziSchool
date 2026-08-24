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
public class ArrearsReportResponse {

    private long totalOverdue;
    private BigDecimal overdueAmount;
    private BigDecimal bucket0To7Days;
    private BigDecimal bucket8To30Days;
    private BigDecimal bucket30PlusDays;
    private String currency;
}
