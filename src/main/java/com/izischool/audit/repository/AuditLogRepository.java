package com.izischool.audit.repository;

import com.izischool.audit.domain.AuditAction;
import com.izischool.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findBySchool_IdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<AuditLog> findBySchool_IdAndEntityTypeOrderByCreatedAtDesc(UUID schoolId, String entityType, Pageable pageable);

    Page<AuditLog> findBySchool_IdAndActionOrderByCreatedAtDesc(UUID schoolId, AuditAction action, Pageable pageable);
}
