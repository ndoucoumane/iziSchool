package com.izischool.school.dto;

import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
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
public class SchoolResponse {

    private UUID id;
    private String name;
    private String code;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String currency;
    private String logoUrl;
    private SchoolStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static SchoolResponse fromEntity(School school) {
        if (school == null) {
            return null;
        }
        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .code(school.getCode())
                .email(school.getEmail())
                .phone(school.getPhone())
                .address(school.getAddress())
                .city(school.getCity())
                .country(school.getCountry())
                .currency(school.getCurrency())
                .logoUrl(school.getLogoUrl())
                .status(school.getStatus())
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .build();
    }
}
