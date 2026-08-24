package com.izischool.school.repository;

import com.izischool.school.domain.School;
import com.izischool.school.domain.SchoolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

    Optional<School> findByCode(String code);

    boolean existsByCode(String code);

    Page<School> findByStatus(SchoolStatus status, Pageable pageable);
}
