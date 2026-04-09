package org.example.hotel_service.services.dashboard;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.dtos.response.DashboardOverviewResponse;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.ReservationRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.example.hotel_service.repositories.RoomTypeRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService implements DashboardServiceImp {

    UserRepository userRepository;
    RoomTypeRepository roomTypeRepository;
    RoomRepository roomRepository;
    ReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

        return DashboardOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .totalRoomTypes(roomTypeRepository.count())
                .totalRooms(roomRepository.count())
                .totalReservations(reservationRepository.count())
                .build();
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

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof Iterable<?> iterable) {
            Set<String> roles = new HashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString().toUpperCase(Locale.ROOT));
                }
            }
            if (!roles.isEmpty()) {
                return roles;
            }
        }

        Object roleObj = jwt.getClaims().get("role");
        if (roleObj != null) {
            return Set.of(roleObj.toString().toUpperCase(Locale.ROOT));
        }
        return Set.of();
    }
}

