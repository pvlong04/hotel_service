package org.example.hotel_service.services.roomavailability;

import org.example.hotel_service.dtos.response.RoomAvailabilityLogResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface RoomAvailabilityLogServiceImp {

    List<RoomAvailabilityLogResponse> getLogsByRoomId(Long roomId, Jwt jwt);

    List<RoomAvailabilityLogResponse> getLogsByReservationId(Long reservationId, Jwt jwt);
}

