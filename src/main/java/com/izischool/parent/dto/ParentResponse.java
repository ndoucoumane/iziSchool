package com.izischool.parent.dto;

import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentStatus;
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
public class ParentResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String whatsappPhone;
    private String email;
    private String address;
    private ParentStatus status;
    private UUID schoolId;
    private Instant createdAt;
    private Instant updatedAt;

    public static ParentResponse fromEntity(Parent parent) {
        if (parent == null) {
            return null;
        }
        return ParentResponse.builder()
                .id(parent.getId())
                .firstName(parent.getFirstName())
                .lastName(parent.getLastName())
                .fullName(parent.getFullName())
                .phone(parent.getPhone())
                .whatsappPhone(parent.getWhatsappPhone())
                .email(parent.getEmail())
                .address(parent.getAddress())
                .status(parent.getStatus())
                .schoolId(parent.getSchool() != null ? parent.getSchool().getId() : null)
                .createdAt(parent.getCreatedAt())
                .updatedAt(parent.getUpdatedAt())
                .build();
    }
}
