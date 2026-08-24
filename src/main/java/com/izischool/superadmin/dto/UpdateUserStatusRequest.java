package com.izischool.superadmin.dto;

import com.izischool.auth.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class UpdateUserStatusRequest {

    @NotNull(message = "Le statut est obligatoire (ACTIVE, SUSPENDED, DEACTIVATED)")
    @Schema(example = "ACTIVE")
    private UserStatus status;
}
