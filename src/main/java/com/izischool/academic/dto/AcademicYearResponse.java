package com.izischool.academic.dto;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearResponse {

    private UUID id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private AcademicYearStatus status;
    private UUID schoolId;
    private Instant createdAt;
    private Instant updatedAt;

    public static AcademicYearResponse fromEntity(AcademicYear year) {
        if (year == null) {
            return null;
        }
        return AcademicYearResponse.builder()
                .id(year.getId())
                .name(year.getName())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .status(year.getStatus())
                .schoolId(year.getSchool() != null ? year.getSchool().getId() : null)
                .createdAt(year.getCreatedAt())
                .updatedAt(year.getUpdatedAt())
                .build();
    }
}
