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
public class OverdueStudentPaymentResponse {

    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private UUID classId;
    private String className;
    private BigDecimal overdueAmount;
    private long overdueSchedulesCount;
    private LocalDate oldestDueDate;
    private long daysOverdue;
    private String arrearsBucket; // "0-7j", "8-30j", "30j+"
    private String parentName;
    private String parentPhone;
    private String currency;
}
