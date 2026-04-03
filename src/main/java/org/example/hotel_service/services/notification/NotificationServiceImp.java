package org.example.hotel_service.services.notification;

import org.example.hotel_service.dtos.response.NotificationResponse;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.NotificationType;
import org.example.hotel_service.enums.Roles;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface NotificationServiceImp {

    List<NotificationResponse> getMyNotifications(Jwt jwt);

    long getUnreadCount(Jwt jwt);

    void markAsRead(Long notificationId, Jwt jwt);

    void markAllAsRead(Jwt jwt);

    /**
     * Tạo notification và push qua WebSocket cho user.
     */
    void createAndPush(User user, NotificationType type, String title, String content, Long referenceId);

    /**
     * Notify higher roles about lower-role actions.
     */
    void notifyHierarchy(User actor, Roles actorRole, String action, String resource, Long referenceId, String detail);
}
