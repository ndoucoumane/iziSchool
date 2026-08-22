package com.izischool.notification.repository;

import com.izischool.notification.domain.ReminderRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRuleRepository extends JpaRepository<ReminderRule, UUID> {

    Optional<ReminderRule> findByIdAndSchool_Id(UUID id, UUID schoolId);

    List<ReminderRule> findBySchool_IdAndActiveTrue(UUID schoolId);

    Page<ReminderRule> findBySchool_Id(UUID schoolId, Pageable pageable);
}
