package com.izischool.academic.dto;

import com.izischool.academic.domain.AcademicYear;
import com.izischool.academic.domain.AcademicYearStatus;
import com.izischool.school.domain.School;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearRequest {

    @NotBlank(message = "Academic year name is required (e.g. 2026-2027)")
    @Size(max = 50)
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    public AcademicYear toEntity(School school) {
        return AcademicYear.builder()
                .school(school)
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .status(AcademicYearStatus.PLANNED)
                .build();
    }
}
