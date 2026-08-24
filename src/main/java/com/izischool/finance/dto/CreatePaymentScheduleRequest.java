package com.izischool.finance.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class CreatePaymentScheduleRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Fee ID is required")
    private UUID feeId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be strictly positive")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String description;
}
