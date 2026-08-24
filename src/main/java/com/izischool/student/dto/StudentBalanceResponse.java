package com.izischool.student.dto;

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
public class StudentBalanceResponse {

    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private BigDecimal totalDue;
    private BigDecimal totalPaid;
    private BigDecimal totalOutstanding;
    private String currency;
}
