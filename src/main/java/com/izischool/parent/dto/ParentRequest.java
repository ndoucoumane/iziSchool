package com.izischool.parent.dto;

import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentStatus;
import com.izischool.school.domain.School;
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
public class ParentRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Size(max = 50)
    private String phone;

    @Size(max = 50)
    private String whatsappPhone;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String address;

    public Parent toEntity(School school) {
        return Parent.builder()
                .school(school)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .whatsappPhone(whatsappPhone != null ? whatsappPhone : phone)
                .email(email)
                .address(address)
                .status(ParentStatus.ACTIVE)
                .build();
    }
}
