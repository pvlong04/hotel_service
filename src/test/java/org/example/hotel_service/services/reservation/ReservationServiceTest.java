package org.example.hotel_service.services.reservation;

import org.example.hotel_service.dtos.request.CheckAvailabilityRequest;
import org.example.hotel_service.dtos.request.CreatePaymentRequest;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.PaymentMethod;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.PaymentRepository;
import org.example.hotel_service.repositories.ReservationChargeRepository;
import org.example.hotel_service.repositories.ReservationRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.example.hotel_service.repositories.RoomTypeRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RoomRepository roomRepository;
    @Mock
    RoomTypeRepository roomTypeRepository;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    ReservationChargeRepository reservationChargeRepository;

    @InjectMocks
    ReservationService reservationService;

    Jwt guestJwt;

    @BeforeEach
    void setUp() {
        guestJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(claims -> {
                    claims.put("userId", 1L);
                    claims.put("roles", List.of("GUEST"));
                })
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
    }

    @Test
    void checkAvailability_shouldThrowWhenDateRangeInvalid() {
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(1))
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> reservationService.checkAvailability(request));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    void createPayment_shouldThrowWhenAmountExceedsRemaining() {
        User guest = User.builder().userId(1L).username("guest").build();
        Reservation reservation = Reservation.builder()
                .reservationId(10L)
                .status(ReservationStatus.CONFIRMED)
                .guest(guest)
                .totalAmount(1_000_000L)
                .paidAmount(900_000L)
                .build();

        when(reservationRepository.findWithDetailsByReservationId(10L)).thenReturn(Optional.of(reservation));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .reservationId(10L)
                .amount(200_000L)
                .method(PaymentMethod.CASH)
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> reservationService.createPayment(request, guestJwt));
        assertEquals(ErrorCode.PAYMENT_AMOUNT_INVALID, ex.getErrorCode());
    }
}


