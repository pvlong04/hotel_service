package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.AmenityRequest;
import org.example.hotel_service.dtos.response.AmenityResponse;
import org.example.hotel_service.enums.AmenityCategory;
import org.example.hotel_service.services.amenity.AmenityServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/amenities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityController {

    AmenityServiceImp amenityService;

    /**
     * GET /amenities — Lấy tất cả tiện nghi (public)
     * GET /amenities?category=ROOM — Lọc theo danh mục
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> getAmenities(
            @RequestParam(required = false) AmenityCategory category
    ) {
        List<AmenityResponse> result = category != null
                ? amenityService.getAmenitiesByCategory(category)
                : amenityService.getAllAmenities();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tiện nghi thành công", result));
    }

    /**
     * GET /amenities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AmenityResponse>> getAmenityById(@PathVariable Integer id) {
        AmenityResponse result = amenityService.getAmenityById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy tiện nghi thành công", result));
    }

    /**
     * POST /amenities — Tạo tiện nghi mới (admin)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AmenityResponse>> createAmenity(@Valid @RequestBody AmenityRequest request) {
        AmenityResponse result = amenityService.createAmenity(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tiện nghi thành công", result));
    }

    /**
     * PUT /amenities/{id} — Cập nhật tiện nghi (admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AmenityResponse>> updateAmenity(
            @PathVariable Integer id,
            @Valid @RequestBody AmenityRequest request
    ) {
        AmenityResponse result = amenityService.updateAmenity(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tiện nghi thành công", result));
    }

    /**
     * DELETE /amenities/{id} — Xóa tiện nghi (admin)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAmenity(@PathVariable Integer id) {
        amenityService.deleteAmenity(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tiện nghi thành công", null));
    }
}
