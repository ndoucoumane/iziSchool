package com.izischool.finance.dto;

import com.izischool.finance.domain.PaymentSchedule;
import com.izischool.finance.domain.PaymentScheduleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentScheduleResponse {

    private UUID id;
    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private UUID feeId;
    private String feeName;
    private String feeCode;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal remainingAmount;
    private String currency;
    private PaymentScheduleStatus status;
    private int installmentNumber;
    private String description;
    private Instant createdAt;

    public static PaymentScheduleResponse fromEntity(PaymentSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        UUID studentId = null;
        String studentNumber = null;
        String studentName = null;
        if (schedule.getStudent() != null) {
            try {
                studentId = schedule.getStudent().getId();
                studentNumber = schedule.getStudent().getStudentNumber();
                studentName = schedule.getStudent().getFullName();
            } catch (Exception ignored) {
            }
        }

        UUID feeId = null;
        String feeName = null;
        String feeCode = null;
        if (schedule.getFee() != null) {
            try {
                feeId = schedule.getFee().getId();
                feeName = schedule.getFee().getName();
                feeCode = schedule.getFee().getCode();
            } catch (Exception ignored) {
            }
        }

        return PaymentScheduleResponse.builder()
                .id(schedule.getId())
                .studentId(studentId)
                .studentNumber(studentNumber)
                .studentName(studentName)
                .feeId(feeId)
                .feeName(feeName)
                .feeCode(feeCode)
                .dueDate(schedule.getDueDate())
                .amountDue(schedule.getAmountDue())
                .amountPaid(schedule.getAmountPaid())
                .remainingAmount(schedule.getRemainingAmount())
                .currency(schedule.getCurrency())
                .status(schedule.getStatus())
                .installmentNumber(schedule.getInstallmentNumber())
                .description(schedule.getDescription())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}
