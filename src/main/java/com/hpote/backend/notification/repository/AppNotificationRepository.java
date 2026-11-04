package com.hpote.backend.notification.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hpote.backend.notification.model.AppNotification;

public interface AppNotificationRepository extends JpaRepository<AppNotification, String> {

    List<AppNotification> findTop50ByRecipientIdOrderByCreatedAtDesc(String recipientId);

    List<AppNotification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(String recipientId);

    long countByRecipientIdAndReadAtIsNull(String recipientId);
}
