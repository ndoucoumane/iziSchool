package com.izischool.student.repository;

import com.izischool.student.domain.StudentImport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentImportRepository extends JpaRepository<StudentImport, UUID> {

    Optional<StudentImport> findByIdAndSchool_Id(UUID id, UUID schoolId);

    Page<StudentImport> findBySchool_IdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);
}
