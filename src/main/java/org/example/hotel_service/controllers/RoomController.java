package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.RoomRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.enums.RoomStatus;
import org.example.hotel_service.services.room.RoomServiceImp;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomServiceImp roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> getRooms(
            @RequestParam(required = false) Integer hotelId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<RoomResponse> result = roomService.getRooms(hotelId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse result = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết phòng thành công", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        RoomResponse result = roomService.createRoom(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phòng thành công", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        RoomResponse result = roomService.updateRoom(id, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng thành công", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        roomService.deleteRoom(id, jwt);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng thành công", null));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms(
            @RequestParam Integer hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        List<RoomResponse> result = roomService.getAvailableRooms(hotelId, checkIn, checkOut);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng trống thành công", result));
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<RoomResponse.ImageItem>>> getRoomImages(@PathVariable Long id) {
        List<RoomResponse.ImageItem> result = roomService.getRoomImages(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ảnh phòng thành công", result));
    }
}
