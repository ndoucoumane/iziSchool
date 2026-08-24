package com.izischool.finance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class GeneratePaymentScheduleRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Fee ID is required")
    private UUID feeId;

    @Builder.Default
    @Min(value = 1, message = "Number of installments must be at least 1")
    private int numberOfInstallments = 10;

    private BigDecimal totalAmount;

    @NotNull(message = "First due date is required")
    private LocalDate firstDueDate;
}
