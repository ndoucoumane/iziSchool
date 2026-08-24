package com.izischool.academic.dto;

import com.izischool.academic.domain.SchoolClass;
import com.izischool.academic.domain.SchoolClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClassResponse {

    private UUID id;
    private String name;
    private String code;
    private int capacity;
    private SchoolClassStatus status;
    private UUID academicYearId;
    private String academicYearName;
    private UUID gradeLevelId;
    private String gradeLevelName;
    private UUID schoolId;
    private Instant createdAt;
    private Instant updatedAt;

    public static SchoolClassResponse fromEntity(SchoolClass schoolClass) {
        if (schoolClass == null) {
            return null;
        }

        UUID academicYearId = null;
        String academicYearName = null;
        if (schoolClass.getAcademicYear() != null) {
            try {
                academicYearId = schoolClass.getAcademicYear().getId();
                academicYearName = schoolClass.getAcademicYear().getName();
            } catch (Exception ignored) {
            }
        }

        UUID gradeLevelId = null;
        String gradeLevelName = null;
        if (schoolClass.getGradeLevel() != null) {
            try {
                gradeLevelId = schoolClass.getGradeLevel().getId();
                gradeLevelName = schoolClass.getGradeLevel().getName();
            } catch (Exception ignored) {
            }
        }

        UUID schoolId = null;
        if (schoolClass.getSchool() != null) {
            try {
                schoolId = schoolClass.getSchool().getId();
            } catch (Exception ignored) {
            }
        }

        return SchoolClassResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .code(schoolClass.getCode())
                .capacity(schoolClass.getCapacity() != null ? schoolClass.getCapacity() : 0)
                .status(schoolClass.getStatus())
                .academicYearId(academicYearId)
                .academicYearName(academicYearName)
                .gradeLevelId(gradeLevelId)
                .gradeLevelName(gradeLevelName)
                .schoolId(schoolId)
                .createdAt(schoolClass.getCreatedAt())
                .updatedAt(schoolClass.getUpdatedAt())
                .build();
    }
}
