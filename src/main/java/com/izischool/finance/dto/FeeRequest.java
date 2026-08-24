package com.izischool.finance.dto;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import com.izischool.school.domain.School;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class FeeRequest {

    @NotBlank(message = "Fee name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 50)
    private String code;

    @NotNull(message = "Fee type is required (REGISTRATION, TUITION, EXAM, CANTEEN, TRANSPORT, UNIFORM, OTHER)")
    private FeeType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be strictly positive")
    private BigDecimal amount;

    @Builder.Default
    @Size(max = 10)
    private String currency = "XOF";

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    private UUID classId;

    private UUID gradeLevelId;

    @Size(max = 500)
    private String description;

    @Builder.Default
    private boolean mandatory = true;

    public Fee toEntity(School school, AcademicYear academicYear) {
        String feeCode = (code != null && !code.isBlank())
                ? code
                : type.name() + "-" + name.toUpperCase().replaceAll("\\s+", "");

        return Fee.builder()
                .school(school)
                .academicYear(academicYear)
                .name(name)
                .code(feeCode)
                .feeType(type)
                .amount(amount)
                .currency(currency != null && !currency.isBlank() ? currency : school.getCurrency())
                .description(description)
                .mandatory(mandatory)
                .active(true)
                .build();
    }
}
