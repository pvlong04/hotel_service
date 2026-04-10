package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(UUID userId);

    long countByUser_UserIdAndIsReadFalse(UUID userId);

    List<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
}
