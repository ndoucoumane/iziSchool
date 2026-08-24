package com.izischool.auth.dto;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.domain.UserStatus;
import com.izischool.school.domain.School;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
public class CreateUserRequest {

    @Schema(description = "Identifiant unique Keycloak (ex: sub du JWT ou identifiant généré)", example = "kc-user-123456")
    private String keycloakUserId;

    @Schema(description = "ID de l'école de rattachement (optionnel pour SUPER_ADMIN, obligatoire pour les autres rôles)")
    private UUID schoolId;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    @Schema(example = "Ousmane")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    @Schema(example = "Diop")
    private String lastName;

    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est obligatoire")
    @Size(max = 255)
    @Schema(example = "directeur@ecolepilote.sn")
    private String email;

    @Size(max = 50)
    @Schema(example = "+221771234567")
    private String phone;

    @NotBlank(message = "Le mot de passe initial est obligatoire")
    @Size(min = 6, max = 100, message = "Le mot de passe doit comporter au moins 6 caractères")
    @Schema(example = "Passer1234!", description = "Mot de passe initial pour la connexion de l'utilisateur")
    private String password;

    @NotNull(message = "Le rôle utilisateur est obligatoire (SUPER_ADMIN, DIRECTOR, ADMIN, ACCOUNTANT, PARENT)")
    @Schema(example = "DIRECTOR")
    private UserRole role;

    @Schema(example = "ACTIVE")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    public UserProfile toEntity(School school) {
        String finalKeycloakId = (keycloakUserId != null && !keycloakUserId.isBlank()) 
                ? keycloakUserId 
                : "kc-" + UUID.randomUUID();

        return UserProfile.builder()
                .keycloakUserId(finalKeycloakId)
                .school(school)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .role(role)
                .status(status != null ? status : UserStatus.ACTIVE)
                .build();
    }
}
