package org.example.hotel_service.services.reservation;

import org.example.hotel_service.dtos.request.CheckAvailabilityRequest;
import org.example.hotel_service.dtos.request.CreateChargeRequest;
import org.example.hotel_service.dtos.request.CreatePaymentRequest;
import org.example.hotel_service.dtos.request.CreateReservationRequest;
import org.example.hotel_service.dtos.request.UpdateReservationStatusRequest;
import org.example.hotel_service.dtos.response.ReservationCreatedResponse;
import org.example.hotel_service.dtos.response.ReservationResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.enums.ReservationStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface ReservationServiceImp {
    List<RoomResponse> checkAvailability(CheckAvailabilityRequest request);

    ReservationCreatedResponse createReservation(CreateReservationRequest request, Jwt jwt);

    ReservationResponse getReservationById(Long reservationId, Jwt jwt);

    List<ReservationResponse> getMyReservations(Jwt jwt);

    List<ReservationResponse> getReservations(Integer hotelId, ReservationStatus status, Jwt jwt);

    ReservationResponse updateReservationStatus(Long reservationId, UpdateReservationStatusRequest request, Jwt jwt);

    ReservationResponse createPayment(CreatePaymentRequest request, Jwt jwt);

    ReservationResponse createCharge(CreateChargeRequest request, Jwt jwt);
}
