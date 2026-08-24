package com.izischool.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFinancialStatusResponse {

    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private UUID classId;
    private String className;
    private BigDecimal totalExpected;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private BigDecimal overdueAmount;
    private String status; // UP_TO_DATE, PARTIALLY_PAID, UNPAID, OVERDUE
    private LocalDate lastPaymentDate;
    private BigDecimal lastPaymentAmount;
    private String currency;
}
