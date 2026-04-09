package org.example.hotel_service.services.reservation;

import org.example.hotel_service.dtos.request.CheckAvailabilityRequest;
import org.example.hotel_service.dtos.request.CreatePaymentRequest;
import org.example.hotel_service.dtos.request.CreateReservationRequest;
import org.example.hotel_service.dtos.response.ReservationCreatedResponse;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.entities.Room;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.PaymentMethod;
import org.example.hotel_service.enums.RoomStatus;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.enums.UserStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.PaymentRepository;
import org.example.hotel_service.repositories.ReservationChargeRepository;
import org.example.hotel_service.repositories.ReservationRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.example.hotel_service.repositories.RoomTypeRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.example.hotel_service.services.email.EmailService;
import org.example.hotel_service.services.notification.NotificationServiceImp;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
    @Mock
    EmailService emailService;
    @Mock
    NotificationServiceImp notificationService;

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

    @Test
    void createReservation_shouldApplyWeekendPriceForFridaySaturdaySunday() {
        User guest = buildActiveGuest(1L);
        RoomType roomType = RoomType.builder()
                .roomTypeId(100L)
                .pricePerNight(800_000L)
                .weekendPrice(900_000L)
                .build();
        Room room = Room.builder()
                .roomId(10L)
                .roomNumber("101")
                .roomType(roomType)
                .status(RoomStatus.AVAILABLE)
                .build();

        CreateReservationRequest request = CreateReservationRequest.builder()
                .checkInDate(LocalDate.of(2026, 4, 9))  // Thursday
                .checkOutDate(LocalDate.of(2026, 4, 12)) // Sunday (nights: Thu, Fri, Sat)
                .adultCount(2)
                .childCount(0)
                .rooms(List.of(CreateReservationRequest.ReservationRoomItem.builder()
                        .roomTypeId(100L)
                        .roomId(10L)
                        .build()))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(roomTypeRepository.findById(100L)).thenReturn(Optional.of(roomType));
        when(roomRepository.findAvailableRooms(request.getCheckInDate(), request.getCheckOutDate())).thenReturn(List.of(room));
        doAnswer(invocation -> invocation.getArgument(0)).when(reservationRepository).save(any(Reservation.class));

        ReservationCreatedResponse result = reservationService.createReservation(request, guestJwt);

        // Thu: 800k, Fri: 900k, Sat: 900k
        assertEquals(2_600_000L, result.getTotalAmount());
    }

    @Test
    void createReservation_shouldFallbackToWeekdayPriceWhenWeekendPriceIsNull() {
        User guest = buildActiveGuest(1L);
        RoomType roomType = RoomType.builder()
                .roomTypeId(101L)
                .pricePerNight(700_000L)
                .weekendPrice(null)
                .build();
        Room room = Room.builder()
                .roomId(11L)
                .roomNumber("102")
                .roomType(roomType)
                .status(RoomStatus.AVAILABLE)
                .build();

        CreateReservationRequest request = CreateReservationRequest.builder()
                .checkInDate(LocalDate.of(2026, 4, 10))  // Friday
                .checkOutDate(LocalDate.of(2026, 4, 12)) // Sunday (nights: Fri, Sat)
                .adultCount(2)
                .childCount(0)
                .rooms(List.of(CreateReservationRequest.ReservationRoomItem.builder()
                        .roomTypeId(101L)
                        .roomId(11L)
                        .build()))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(guest));
        when(roomTypeRepository.findById(101L)).thenReturn(Optional.of(roomType));
        when(roomRepository.findAvailableRooms(request.getCheckInDate(), request.getCheckOutDate())).thenReturn(List.of(room));
        doAnswer(invocation -> invocation.getArgument(0)).when(reservationRepository).save(any(Reservation.class));

        ReservationCreatedResponse result = reservationService.createReservation(request, guestJwt);

        assertEquals(1_400_000L, result.getTotalAmount());
    }

    private User buildActiveGuest(Long userId) {
        User guest = User.builder()
                .userId(userId)
                .username("guest")
                .status(UserStatus.ACTIVE)
                .build();

        Role guestRole = Role.builder().name(Roles.GUEST).build();
        UserRole userRole = UserRole.builder().user(guest).role(guestRole).build();
        guest.setUserRoles(Set.of(userRole));
        return guest;
    }
}


