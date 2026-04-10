package org.example.hotel_service.services.roomavailability;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.dtos.response.RoomAvailabilityLogResponse;
import org.example.hotel_service.entities.RoomAvailabilityLog;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.RoomAvailabilityLogRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAvailabilityLogService implements RoomAvailabilityLogServiceImp {

    RoomAvailabilityLogRepository roomAvailabilityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomAvailabilityLogResponse> getLogsByRoomId(Long roomId, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        return roomAvailabilityLogRepository.findByRoom_RoomIdOrderByChangedAtDesc(roomId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAvailabilityLogResponse> getLogsByReservationId(Long reservationId, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        return roomAvailabilityLogRepository.findByReservation_ReservationIdOrderByChangedAtDesc(reservationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private RoomAvailabilityLogResponse toResponse(RoomAvailabilityLog log) {
        return RoomAvailabilityLogResponse.builder()
                .logId(log.getLogId())
                .roomId(log.getRoom() != null ? log.getRoom().getRoomId() : null)
                .roomNumber(log.getRoom() != null ? log.getRoom().getRoomNumber() : null)
                .oldStatus(log.getOldStatus() != null ? log.getOldStatus().name() : null)
                .newStatus(log.getNewStatus() != null ? log.getNewStatus().name() : null)
                .reason(log.getReason())
                .reservationId(log.getReservation() != null ? log.getReservation().getReservationId() : null)
                .reservationCode(log.getReservation() != null ? log.getReservation().getReservationCode() : null)
                .changedByUserId(log.getChangedBy() != null ? log.getChangedBy().getUserId() : null)
                .changedByUsername(log.getChangedBy() != null ? log.getChangedBy().getUsername() : null)
                .changedAt(log.getChangedAt())
                .build();
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof Iterable<?> iterable) {
            Set<String> roles = new HashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString().toUpperCase());
                }
            }
            if (!roles.isEmpty()) {
                return roles;
            }
        }

        Object roleObj = jwt.getClaims().get("role");
        if (roleObj != null) {
            return Set.of(roleObj.toString().toUpperCase());
        }
        return Set.of();
    }

    private void requireAnyRole(Jwt jwt, Roles... roles) {
        Set<String> currentRoles = extractRoles(jwt);
        for (Roles role : roles) {
            if (currentRoles.contains(role.name())) {
                return;
            }
        }
        throw new ApiException(ErrorCode.ACCESS_DENIED);
    }
}

