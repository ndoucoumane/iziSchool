package com.izischool.notification.repository;

import com.izischool.notification.domain.NotificationChannel;
import com.izischool.notification.domain.NotificationTemplate;
import com.izischool.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByIdAndSchool_Id(UUID id, UUID schoolId);

    Optional<NotificationTemplate> findBySchool_IdAndTypeAndChannelAndActiveTrue(
            UUID schoolId, NotificationType type, NotificationChannel channel);

    List<NotificationTemplate> findBySchool_IdAndActiveTrue(UUID schoolId);

    Page<NotificationTemplate> findBySchool_Id(UUID schoolId, Pageable pageable);
}
