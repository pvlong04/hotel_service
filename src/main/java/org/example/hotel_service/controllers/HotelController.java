package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.HotelRequest;
import org.example.hotel_service.dtos.request.HotelSearchRequest;
import org.example.hotel_service.dtos.response.HotelSearchResponse;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.services.hotel.HotelServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelController {

    HotelServiceImp hotelService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<HotelSearchResponse>> getCurrentHotel() {
        HotelSearchResponse result = hotelService.getCurrentHotel();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin khách sạn hiện tại thành công", result));
    }

    @PutMapping("/current")
    public ResponseEntity<ApiResponse<HotelSearchResponse>> updateCurrentHotel(
            @Valid @RequestBody HotelRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        HotelSearchResponse result = hotelService.updateCurrentHotel(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khách sạn hiện tại thành công", result));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<HotelSearchResponse>>> searchHotels(
            @Valid @RequestBody HotelSearchRequest request
    ) {
        PageResponse<HotelSearchResponse> result = hotelService.searchHotels(request);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm khách sạn thành công", result));
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<HotelSearchResponse>> getHotelById(@PathVariable Integer hotelId) {
        HotelSearchResponse result = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin khách sạn thành công", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HotelSearchResponse>> createHotel(
            @Valid @RequestBody HotelRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        HotelSearchResponse result = hotelService.createHotel(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo khách sạn thành công", result));
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<HotelSearchResponse>> updateHotel(
            @PathVariable Integer hotelId,
            @Valid @RequestBody HotelRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        HotelSearchResponse result = hotelService.updateHotel(hotelId, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khách sạn thành công", result));
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(
            @PathVariable Integer hotelId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        hotelService.deleteHotel(hotelId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Xóa khách sạn thành công", null));
    }
}

