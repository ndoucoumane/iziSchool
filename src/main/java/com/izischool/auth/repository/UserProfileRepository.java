package com.izischool.auth.repository;

import com.izischool.auth.domain.UserProfile;
import com.izischool.auth.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @EntityGraph(attributePaths = {"school"})
    Optional<UserProfile> findByKeycloakUserId(String keycloakUserId);

    @EntityGraph(attributePaths = {"school"})
    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByIdAndSchool_Id(UUID id, UUID schoolId);

    Page<UserProfile> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    Page<UserProfile> findBySchool_IdAndRoleAndDeletedFalse(UUID schoolId, UserRole role, Pageable pageable);

    boolean existsByKeycloakUserId(String keycloakUserId);

    boolean existsByEmail(String email);
}
