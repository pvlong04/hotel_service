package org.example.hotel_service.services.room;

import org.example.hotel_service.dtos.request.RoomRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.enums.RoomStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;

public interface RoomServiceImp {
    PageResponse<RoomResponse> getAllRoom();

    PageResponse<RoomResponse> getRooms(RoomStatus status, int page, int size);

    RoomResponse getRoomById(Long id);

    RoomResponse createRoom(RoomRequest request, Jwt jwt);

    RoomResponse updateRoom(Long id, RoomRequest request, Jwt jwt);

    void deleteRoom(Long id, Jwt jwt);

    List<RoomResponse> getAvailableRooms(LocalDate checkIn, LocalDate checkOut);

    List<RoomResponse.ImageItem> getRoomImages(Long id);

    RoomResponse.ImageItem addRoomImage(Long roomId, String url, String caption, Jwt jwt);

    void deleteRoomImage(Long roomId, Long imageId, Jwt jwt);

    void setPrimaryImage(Long roomId, Long imageId, Jwt jwt);
}
