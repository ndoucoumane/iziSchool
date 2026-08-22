package com.izischool.notification.repository;

import com.izischool.notification.domain.Notification;
import com.izischool.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndSchool_Id(UUID id, UUID schoolId);

    Page<Notification> findBySchool_IdOrderByCreatedAtDesc(UUID schoolId, Pageable pageable);

    Page<Notification> findBySchool_IdAndStudent_IdOrderByCreatedAtDesc(UUID schoolId, UUID studentId, Pageable pageable);

    List<Notification> findBySchool_IdAndStatus(UUID schoolId, NotificationStatus status);
}
