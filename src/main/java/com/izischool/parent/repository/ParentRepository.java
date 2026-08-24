package com.izischool.parent.repository;

import com.izischool.parent.domain.Parent;
import com.izischool.parent.domain.ParentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    Optional<Parent> findByIdAndSchool_IdAndDeletedFalse(UUID id, UUID schoolId);

    Page<Parent> findBySchool_IdAndDeletedFalse(UUID schoolId, Pageable pageable);

    Page<Parent> findBySchool_IdAndStatusAndDeletedFalse(UUID schoolId, ParentStatus status, Pageable pageable);

    Optional<Parent> findBySchool_IdAndPhoneAndDeletedFalse(UUID schoolId, String phone);

    boolean existsBySchool_IdAndPhoneAndDeletedFalse(UUID schoolId, String phone);

    Optional<Parent> findBySchool_IdAndEmailAndDeletedFalse(UUID schoolId, String email);

    boolean existsBySchool_IdAndEmailAndDeletedFalse(UUID schoolId, String email);
}
