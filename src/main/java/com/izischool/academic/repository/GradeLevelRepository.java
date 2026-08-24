package com.izischool.academic.repository;

import com.izischool.academic.domain.GradeLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, UUID> {

    Optional<GradeLevel> findByIdAndSchool_Id(UUID id, UUID schoolId);

    List<GradeLevel> findBySchool_IdAndActiveTrueOrderByDisplayOrderAsc(UUID schoolId);

    Page<GradeLevel> findBySchool_Id(UUID schoolId, Pageable pageable);

    boolean existsBySchool_IdAndCode(UUID schoolId, String code);
}
