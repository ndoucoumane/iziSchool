package com.izischool.auth.dto;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import com.izischool.auth.domain.UserStatus;
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
public class UserProfileResponse {

    private UUID id;
    private String keycloakUserId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private UUID schoolId;
    private String schoolCode;
    private String schoolName;

    public static UserProfileResponse fromEntity(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(profile.getId())
                .keycloakUserId(profile.getKeycloakUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .role(profile.getRole())
                .status(profile.getStatus())
                .schoolId(profile.getSchool() != null ? profile.getSchool().getId() : null)
                .schoolCode(profile.getSchool() != null ? profile.getSchool().getCode() : null)
                .schoolName(profile.getSchool() != null ? profile.getSchool().getName() : null)
                .build();
    }
}
