package com.izischool.academic.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class SchoolClassRequest {

    @NotBlank(message = "Class name is required (e.g. 6ème A)")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String code;

    @Size(max = 50)
    private String level;

    private UUID gradeLevelId;

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    @Builder.Default
    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity = 35;
}
