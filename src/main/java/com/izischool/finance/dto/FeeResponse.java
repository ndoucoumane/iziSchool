package com.izischool.finance.dto;

import com.izischool.finance.domain.Fee;
import com.izischool.finance.domain.FeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeResponse {

    private UUID id;
    private String name;
    private String code;
    private FeeType type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private boolean mandatory;
    private boolean active;
    private UUID academicYearId;
    private String academicYearName;
    private UUID schoolId;
    private Instant createdAt;
    private Instant updatedAt;

    public static FeeResponse fromEntity(Fee fee) {
        if (fee == null) {
            return null;
        }

        UUID academicYearId = null;
        String academicYearName = null;
        if (fee.getAcademicYear() != null) {
            try {
                academicYearId = fee.getAcademicYear().getId();
                academicYearName = fee.getAcademicYear().getName();
            } catch (Exception ignored) {
            }
        }

        UUID schoolId = null;
        if (fee.getSchool() != null) {
            try {
                schoolId = fee.getSchool().getId();
            } catch (Exception ignored) {
            }
        }

        return FeeResponse.builder()
                .id(fee.getId())
                .name(fee.getName())
                .code(fee.getCode())
                .type(fee.getFeeType())
                .amount(fee.getAmount())
                .currency(fee.getCurrency())
                .description(fee.getDescription())
                .mandatory(fee.isMandatory())
                .active(fee.isActive())
                .academicYearId(academicYearId)
                .academicYearName(academicYearName)
                .schoolId(schoolId)
                .createdAt(fee.getCreatedAt())
                .updatedAt(fee.getUpdatedAt())
                .build();
    }
}
