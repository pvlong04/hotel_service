package org.example.hotel_service.services.roomtype;

import org.example.hotel_service.dtos.request.RoomTypeRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomTypeResponse;
import org.example.hotel_service.enums.RoomTypeStatus;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RoomTypeServiceImp {
	PageResponse<RoomTypeResponse> getRoomTypes(RoomTypeStatus status, int page, int size, Long priceMin, Long priceMax);

	RoomTypeResponse getRoomTypeById(Long id);

	RoomTypeResponse createRoomType(RoomTypeRequest request, Jwt jwt);

	RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request, Jwt jwt);

	void deleteRoomType(Long id, Jwt jwt);
}
