package com.izischool.student.dto;

import com.izischool.student.domain.StudentStatus;
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
public class UpdateStudentStatusRequest {

    @NotNull(message = "Status is required (ACTIVE, INACTIVE, TRANSFERRED, GRADUATED)")
    private StudentStatus status;
}
