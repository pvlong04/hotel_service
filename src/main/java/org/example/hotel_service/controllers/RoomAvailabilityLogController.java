package org.example.hotel_service.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.response.RoomAvailabilityLogResponse;
import org.example.hotel_service.services.roomavailability.RoomAvailabilityLogServiceImp;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/room-availability-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAvailabilityLogController {

    RoomAvailabilityLogServiceImp roomAvailabilityLogService;

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityLogResponse>>> getLogsByRoomId(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<RoomAvailabilityLogResponse> result = roomAvailabilityLogService.getLogsByRoomId(roomId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy log thay đổi trạng thái phòng thành công", result));
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityLogResponse>>> getLogsByReservationId(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<RoomAvailabilityLogResponse> result = roomAvailabilityLogService.getLogsByReservationId(reservationId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy log trạng thái theo đơn đặt phòng thành công", result));
    }
}

