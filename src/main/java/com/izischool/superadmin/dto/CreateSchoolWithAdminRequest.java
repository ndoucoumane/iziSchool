package com.izischool.superadmin.dto;

import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreateSchoolWithAdminRequest {

    @NotBlank(message = "Le nom de l'école est obligatoire")
    @Size(max = 200)
    @Schema(example = "Groupe Scolaire SABEL")
    private String name;

    @NotBlank(message = "Le code unique de l'école est obligatoire")
    @Size(min = 2, max = 50)
    @Schema(example = "SABEL-DAK")
    private String code;

    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email de l'école est obligatoire")
    @Schema(example = "contact@sabel.sn")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Schema(example = "+221338001122")
    private String phone;

    @Schema(example = "Point E, Dakar")
    private String address;

    @Schema(example = "Dakar")
    private String city;

    @Schema(example = "Sénégal")
    private String country;

    @Schema(example = "XOF")
    private String currency;

    private String logoUrl;

    // Compte Administrateur / Directeur initial (Optionnel)
    @Schema(example = "Cheikh")
    private String adminFirstName;

    @Schema(example = "Tidiane")
    private String adminLastName;

    @Email
    @Schema(example = "directeur@sabel.sn")
    private String adminEmail;

    @Schema(example = "MonMotDePasse123!")
    private String adminPassword;

    @Schema(example = "+221770001122")
    private String adminPhone;

    public School toSchoolEntity() {
        return School.builder()
                .name(name)
                .code(code.toUpperCase().trim())
                .email(email)
                .phone(phone)
                .address(address)
                .city(city != null ? city : "Dakar")
                .country(country != null ? country : "Sénégal")
                .currency(currency != null && !currency.isBlank() ? currency : "XOF")
                .logoUrl(logoUrl)
                .status(SchoolStatus.ACTIVE)
                .build();
    }
}
