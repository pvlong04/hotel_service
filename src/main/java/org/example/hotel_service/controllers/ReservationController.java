package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.CheckAvailabilityRequest;
import org.example.hotel_service.dtos.request.CreateChargeRequest;
import org.example.hotel_service.dtos.request.CreatePaymentRequest;
import org.example.hotel_service.dtos.request.CreateReservationRequest;
import org.example.hotel_service.dtos.request.UpdateReservationStatusRequest;
import org.example.hotel_service.dtos.response.ReservationCreatedResponse;
import org.example.hotel_service.dtos.response.ReservationResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.services.reservation.ReservationServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReservationController {

    ReservationServiceImp reservationService;

    @PostMapping("/availability")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> checkAvailability(@Valid @RequestBody CheckAvailabilityRequest request) {
        List<RoomResponse> result = reservationService.checkAvailability(request);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra phòng trống thành công", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationCreatedResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReservationCreatedResponse result = reservationService.createReservation(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đặt phòng thành công", result));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReservationResponse result = reservationService.getReservationById(reservationId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đặt phòng thành công", result));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getMyReservations(
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<ReservationResponse> result = reservationService.getMyReservations(jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đặt phòng của bạn thành công", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservations(
            @RequestParam(required = false) Integer hotelId,
            @RequestParam(required = false) ReservationStatus status,
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<ReservationResponse> result = reservationService.getReservations(hotelId, status, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đặt phòng thành công", result));
    }

    @PatchMapping("/{reservationId}/status")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdateReservationStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReservationResponse result = reservationService.updateReservationStatus(reservationId, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đặt phòng thành công", result));
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<ReservationResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReservationResponse result = reservationService.createPayment(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công", result));
    }

    @PostMapping("/charges")
    public ResponseEntity<ApiResponse<ReservationResponse>> createCharge(
            @Valid @RequestBody CreateChargeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReservationResponse result = reservationService.createCharge(request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Thêm phụ phí thành công", result));
    }
}

