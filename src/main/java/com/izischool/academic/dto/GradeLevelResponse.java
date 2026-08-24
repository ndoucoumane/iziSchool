package com.izischool.academic.dto;

import com.izischool.academic.domain.GradeLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeLevelResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private int displayOrder;
    private boolean active;

    public static GradeLevelResponse fromEntity(GradeLevel grade) {
        if (grade == null) {
            return null;
        }
        return GradeLevelResponse.builder()
                .id(grade.getId())
                .name(grade.getName())
                .code(grade.getCode())
                .description(grade.getDescription())
                .displayOrder(grade.getDisplayOrder())
                .active(grade.isActive())
                .build();
    }
}
