package org.example.hotel_service.services.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.response.NotificationResponse;
import org.example.hotel_service.entities.Notification;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.NotificationType;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.UserStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.NotificationRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService implements NotificationServiceImp {

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Jwt jwt) {
        Long userId = extractUserId(jwt);
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Jwt jwt) {
        Long userId = extractUserId(jwt);
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Jwt jwt) {
        Long userId = extractUserId(jwt);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Jwt jwt) {
        Long userId = extractUserId(jwt);
        List<Notification> unread = notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void createAndPush(User user, NotificationType type, String title, String content, Long referenceId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);

        // Push via WebSocket to user-specific queue
        try {
            messagingTemplate.convertAndSendToUser(
                    user.getUserId().toString(),
                    "/queue/notifications",
                    response
            );
        } catch (Exception e) {
            log.warn("Failed to push notification via WebSocket to user {}: {}", user.getUserId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void notifyHierarchy(User actor, Roles actorRole, String action, String resource, Long referenceId, String detail) {
        if (actor == null || actorRole == null) {
            return;
        }

        Set<Roles> recipientRoles = switch (actorRole) {
            case GUEST -> EnumSet.of(Roles.ADMIN, Roles.STAFF);
            case STAFF -> EnumSet.of(Roles.ADMIN);
            case ADMIN -> EnumSet.noneOf(Roles.class);
        };

        if (recipientRoles.isEmpty()) {
            return;
        }

        String normalizedAction = action == null ? "cap nhat" : action.trim();
        String normalizedResource = resource == null ? "du lieu" : resource.trim();
        String suffix = (detail == null || detail.isBlank()) ? "" : (" - " + detail.trim());

        List<User> recipients = userRepository.findDistinctByAnyRoleIn(recipientRoles).stream()
                .filter(u -> u.getUserId() != null && !u.getUserId().equals(actor.getUserId()))
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .toList();

        for (User recipient : recipients) {
            createAndPush(
                    recipient,
                    NotificationType.SYSTEM,
                    "Thong bao he thong",
                    String.format("%s (%s) da %s %s%s",
                            actor.getUsername(), actorRole.name(), normalizedAction, normalizedResource, suffix),
                    referenceId
            );
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .title(notification.getTitle())
                .content(notification.getContent())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
}
