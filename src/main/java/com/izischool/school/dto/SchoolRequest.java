package com.izischool.school.dto;

import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
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
public class SchoolRequest {

    @NotBlank(message = "School name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "School code is required")
    @Size(min = 2, max = 50)
    private String code;

    @NotBlank(message = "Email is required")
    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 10)
    private String currency;

    @Size(max = 500)
    private String logoUrl;

    // Administrateur / Directeur initial (Optionnel)
    private String adminFirstName;
    private String adminLastName;
    private String adminEmail;
    private String adminPassword;
    private String adminPhone;

    public School toEntity() {
        return School.builder()
                .name(name)
                .code(code)
                .email(email)
                .phone(phone)
                .address(address)
                .city(city != null && !city.isBlank() ? city : "Dakar")
                .country(country != null && !country.isBlank() ? country : "Sénégal")
                .currency(currency != null && !currency.isBlank() ? currency : "XOF")
                .logoUrl(logoUrl)
                .status(SchoolStatus.ACTIVE)
                .build();
    }
}
