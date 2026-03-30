package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.RoomTypeRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomTypeResponse;
import org.example.hotel_service.enums.RoomTypeStatus;
import org.example.hotel_service.services.roomtype.RoomTypeServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeController {
	RoomTypeServiceImp roomTypeService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<RoomTypeResponse>>> getRoomTypes(
			@RequestParam(required = false) RoomTypeStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		PageResponse<RoomTypeResponse> result = roomTypeService.getRoomTypes(status, page, size);
		return ResponseEntity.ok(ApiResponse.success("Lấy danh sách loại phòng thành công", result));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<RoomTypeResponse>> getRoomTypeById(@PathVariable Long id) {
		RoomTypeResponse result = roomTypeService.getRoomTypeById(id);
		return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết loại phòng thành công", result));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<RoomTypeResponse>> createRoomType(
			@Valid @RequestBody RoomTypeRequest request,
			@AuthenticationPrincipal Jwt jwt
	) {
		RoomTypeResponse result = roomTypeService.createRoomType(request, jwt);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Tạo loại phòng thành công", result));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
			@PathVariable Long id,
			@Valid @RequestBody RoomTypeRequest request,
			@AuthenticationPrincipal Jwt jwt
	) {
		RoomTypeResponse result = roomTypeService.updateRoomType(id, request, jwt);
		return ResponseEntity.ok(ApiResponse.success("Cập nhật loại phòng thành công", result));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteRoomType(
			@PathVariable Long id,
			@AuthenticationPrincipal Jwt jwt
	) {
		roomTypeService.deleteRoomType(id, jwt);
		return ResponseEntity.ok(ApiResponse.success("Xóa loại phòng thành công", null));
	}
}
