package com.izischool.school.dto;

import com.izischool.school.domain.SchoolStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchoolStatusRequest {

    @NotNull(message = "School status is required")
    private SchoolStatus status;
}
