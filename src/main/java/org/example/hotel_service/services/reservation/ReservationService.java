package org.example.hotel_service.services.reservation;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.request.CheckAvailabilityRequest;
import org.example.hotel_service.dtos.request.CreateChargeRequest;
import org.example.hotel_service.dtos.request.CreatePaymentRequest;
import org.example.hotel_service.dtos.request.CreateReservationRequest;
//import org.example.hotel_service.dtos.request.CreateZaloPayPayloadRequest;
import org.example.hotel_service.dtos.request.UpdateReservationStatusRequest;
import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.ReservationCreatedResponse;
import org.example.hotel_service.dtos.response.ReservationChargeResponse;
import org.example.hotel_service.dtos.response.ReservationResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.entities.Payment;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.entities.ReservationCharge;
import org.example.hotel_service.entities.ReservationItem;
import org.example.hotel_service.entities.Room;
import org.example.hotel_service.entities.RoomImage;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.PaymentStatus;
import org.example.hotel_service.enums.ReservationItemStatus;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.RoomStatus;
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
import org.example.hotel_service.enums.NotificationType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReservationService implements ReservationServiceImp {

    ReservationRepository reservationRepository;
    UserRepository userRepository;
    RoomRepository roomRepository;
    RoomTypeRepository roomTypeRepository;
    PaymentRepository paymentRepository;
    ReservationChargeRepository reservationChargeRepository;
    EmailService emailService;
    NotificationServiceImp notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> checkAvailability(CheckAvailabilityRequest request) {
        validateDateRange(request.getCheckInDate(), request.getCheckOutDate());

        List<Room> availableRooms = roomRepository.findAvailableRooms(
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        return availableRooms.stream()
                .filter(room -> request.getRoomTypeId() == null
                        || (room.getRoomType() != null && request.getRoomTypeId().equals(room.getRoomType().getRoomTypeId())))
                .filter(room -> {
                    int totalGuest = safeInt(request.getAdults()) + safeInt(request.getChildren());
                    return totalGuest <= 0
                            || (room.getRoomType() != null && room.getRoomType().getCapacity() != null
                            && room.getRoomType().getCapacity() >= totalGuest);
                })
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationCreatedResponse createReservation(CreateReservationRequest request, Jwt jwt) {
        validateDateRange(request.getCheckInDate(), request.getCheckOutDate());

        User guest = resolveGuestForReservation(request, jwt);

        int nights = (int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        List<Room> availableRooms = roomRepository.findAvailableRooms(
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        Map<Long, Room> availableById = availableRooms.stream()
                .collect(Collectors.toMap(Room::getRoomId, r -> r));

        Reservation reservation = Reservation.builder()
                .reservationCode(generateReservationCode())
                .guest(guest)
                .status(ReservationStatus.PENDING)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .nightsCount(nights)
                .adultCount(safeInt(request.getAdultCount()))
                .childCount(safeInt(request.getChildCount()))
                .specialRequests(request.getSpecialRequests())
                .totalAmount(0L)
                .paidAmount(0L)
                .build();

        Set<Long> selectedRoomIds = new HashSet<>();
        long total = 0L;

        for (CreateReservationRequest.ReservationRoomItem reqItem : request.getRooms()) {
            RoomType roomType = roomTypeRepository.findById(reqItem.getRoomTypeId())
                    .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));

            Room selectedRoom;
            if (reqItem.getRoomId() != null) {
                selectedRoom = availableById.get(reqItem.getRoomId());
                if (selectedRoom == null || !Objects.equals(selectedRoom.getRoomType().getRoomTypeId(), roomType.getRoomTypeId())) {
                    throw new ApiException(ErrorCode.ROOM_NOT_FOUND);
                }
                if (!selectedRoomIds.add(selectedRoom.getRoomId())) {
                    throw new ApiException(ErrorCode.ROOM_NOT_FOUND);
                }
            } else {
                selectedRoom = availableRooms.stream()
                        .filter(room -> !selectedRoomIds.contains(room.getRoomId()))
                        .filter(room -> room.getRoomType() != null && roomType.getRoomTypeId().equals(room.getRoomType().getRoomTypeId()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
                selectedRoomIds.add(selectedRoom.getRoomId());
            }

            long rate = roomType.getPricePerNight() != null ? roomType.getPricePerNight() : 0L;
            long amount = rate * nights;
            total += amount;

            ReservationItem item = ReservationItem.builder()
                    .reservation(reservation)
                    .room(selectedRoom)
                    .roomType(roomType)
                    .ratePerNight(rate)
                    .nights(nights)
                    .amount(amount)
                    .status(ReservationItemStatus.BOOKED)
                    .build();
            reservation.getItems().add(item);
        }

        reservation.setTotalAmount(total);
        Reservation saved = reservationRepository.save(reservation);

        try {
            emailService.sendBookingConfirmationEmail(saved);
        } catch (Exception ex) {
            log.warn("Booking confirmation email failed for reservation {}: {}", saved.getReservationId(), ex.getMessage());
        }

        // Push notification
        try {
            notificationService.createAndPush(
                    guest,
                    NotificationType.RESERVATION_CREATED,
                    "Đặt phòng thành công",
                    String.format("Đơn %s đã được tạo. Check-in: %s, Check-out: %s",
                            saved.getReservationCode(), saved.getCheckInDate(), saved.getCheckOutDate()),
                    saved.getReservationId()
            );
        } catch (Exception ex) {
            log.warn("Notification push failed for reservation {}: {}", saved.getReservationId(), ex.getMessage());
        }

        try {
            Long actorId = extractUserId(jwt);
            User actor = userRepository.findById(actorId).orElse(null);
            if (actor != null) {
                notificationService.notifyHierarchy(
                        actor,
                        resolveActorRole(jwt),
                        "tao",
                        "don dat phong",
                        saved.getReservationId(),
                        saved.getReservationCode()
                );
            }
        } catch (Exception ex) {
            log.warn("Hierarchy notification failed for reservation {}: {}", saved.getReservationId(), ex.getMessage());
        }

        return ReservationCreatedResponse.builder()
                .reservationId(saved.getReservationId())
                .reservationCode(saved.getReservationCode())
                .status(saved.getStatus().name())
                .checkInDate(saved.getCheckInDate())
                .checkOutDate(saved.getCheckOutDate())
                .nightsCount(saved.getNightsCount())
                .totalAmount(saved.getTotalAmount())
                .paidAmount(saved.getPaidAmount())
                .message("Tạo đơn đặt phòng thành công")
                .build();
    }

    private User resolveGuestForReservation(CreateReservationRequest request, Jwt jwt) {
        Long actorId = extractUserId(jwt);
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        boolean isStaffOrAdmin = hasAnyRole(jwt, Roles.STAFF, Roles.ADMIN);
        Long targetGuestId = request.getGuestId();

        if (!isStaffOrAdmin) {
            if (targetGuestId != null && !Objects.equals(targetGuestId, actorId)) {
                throw new ApiException(ErrorCode.ACCESS_DENIED);
            }
            ensureActiveGuest(actor);
            return actor;
        }

        if (targetGuestId == null) {
            ensureActiveGuest(actor);
            return actor;
        }

        User guest = userRepository.findById(targetGuestId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        ensureActiveGuest(guest);
        return guest;
    }

    private void ensureActiveGuest(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_INACTIVE_BANE);
        }
        boolean isGuest = user.getUserRoles() != null && user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole() != null && userRole.getRole().getName() == Roles.GUEST);
        if (!isGuest) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithDetailsByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        ensureReservationReadable(reservation, jwt);
        return toReservationResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Jwt jwt) {
        Long userId = extractUserId(jwt);
        return reservationRepository.findByGuest_UserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations(ReservationStatus status, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

        List<Reservation> reservations;
        if (status != null) {
            reservations = reservationRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            reservations = reservationRepository.findAll().stream()
                    .sorted(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .collect(Collectors.toList());
        }

        return reservations.stream().map(this::toReservationResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationResponse updateReservationStatus(Long reservationId, UpdateReservationStatusRequest request, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithDetailsByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        if (request.getStatus() == null) {
            throw new ApiException(ErrorCode.KEY_VALID);
        }

        ReservationStatus from = reservation.getStatus();
        ReservationStatus to = request.getStatus();

        boolean isGuest = hasAnyRole(jwt, Roles.GUEST) && !hasAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        if (isGuest) {
            Long userId = extractUserId(jwt);
            if (!Objects.equals(reservation.getGuest().getUserId(), userId)) {
                throw new ApiException(ErrorCode.RESERVATION_ACCESS_DENIED);
            }
            if (to != ReservationStatus.CANCELLED ||
                    !(from == ReservationStatus.PENDING || from == ReservationStatus.CONFIRMED)) {
                throw new ApiException(ErrorCode.RESERVATION_STATUS_TRANSITION_INVALID);
            }
        } else {
            requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
            if (!isValidTransition(from, to)) {
                throw new ApiException(ErrorCode.RESERVATION_STATUS_TRANSITION_INVALID);
            }
        }

        applyStatusTransition(reservation, to, request.getCancelReason(), jwt);
        Reservation saved = reservationRepository.save(reservation);

        // Push notification on status change
        try {
            NotificationType notifType = switch (to) {
                case CONFIRMED -> NotificationType.RESERVATION_CONFIRMED;
                case CHECKED_IN -> NotificationType.RESERVATION_CHECKIN;
                case CHECKED_OUT -> NotificationType.RESERVATION_CHECKOUT;
                case CANCELLED -> NotificationType.RESERVATION_CANCELLED;
                default -> null;
            };
            if (notifType != null && saved.getGuest() != null) {
                String title = switch (to) {
                    case CONFIRMED -> "Đơn đặt phòng đã xác nhận";
                    case CHECKED_IN -> "Check-in thành công";
                    case CHECKED_OUT -> "Check-out thành công";
                    case CANCELLED -> "Đơn đặt phòng đã hủy";
                    default -> "Cập nhật đơn đặt phòng";
                };
                notificationService.createAndPush(
                        saved.getGuest(), notifType, title,
                        String.format("Đơn %s: %s", saved.getReservationCode(), title),
                        saved.getReservationId()
                );
            }
        } catch (Exception ex) {
            log.warn("Notification push failed for reservation {}: {}", saved.getReservationId(), ex.getMessage());
        }

        try {
            Long actorId = extractUserId(jwt);
            User actor = userRepository.findById(actorId).orElse(null);
            if (actor != null) {
                String action = to == ReservationStatus.CANCELLED ? "huy" : "cap nhat";
                notificationService.notifyHierarchy(
                        actor,
                        resolveActorRole(jwt),
                        action,
                        "don dat phong",
                        saved.getReservationId(),
                        saved.getReservationCode() + " -> " + to.name()
                );
            }
        } catch (Exception ex) {
            log.warn("Hierarchy notification failed for reservation status {}: {}", saved.getReservationId(), ex.getMessage());
        }

        return toReservationResponse(saved);
    }

    @Override
    @Transactional
    public ReservationResponse createPayment(CreatePaymentRequest request, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithDetailsByReservationId(request.getReservationId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        ensureReservationReadable(reservation, jwt);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(ErrorCode.RESERVATION_STATUS_TRANSITION_INVALID);
        }

        long remaining = Math.max(0L, reservation.getTotalAmount() - reservation.getPaidAmount());
        if (request.getAmount() == null || request.getAmount() <= 0 || request.getAmount() > remaining) {
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_INVALID);
        }

        Payment payment = Payment.builder()
                .reservation(reservation)
                .guest(reservation.getGuest())
                .amount(request.getAmount())
                .method(request.getMethod())
                .provider(request.getProvider())
                .providerTransId(request.getProviderTransId())
                .status(PaymentStatus.COMPLETED)
                .note(request.getNote())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        reservation.setPaidAmount(reservation.getPaidAmount() + request.getAmount());
        Reservation savedRes = reservationRepository.save(reservation);

        // Push payment notification
        try {
            if (savedRes.getGuest() != null) {
                notificationService.createAndPush(
                        savedRes.getGuest(),
                        NotificationType.PAYMENT_SUCCESS,
                        "Thanh toán thành công",
                        String.format("Đã thanh toán %,d VND cho đơn %s",
                                request.getAmount(), savedRes.getReservationCode()),
                        savedRes.getReservationId()
                );
            }
        } catch (Exception ex) {
            log.warn("Payment notification push failed: {}", ex.getMessage());
        }

        return toReservationResponse(savedRes);
    }

    @Override
    @Transactional
    public ReservationResponse createCharge(CreateChargeRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

        Reservation reservation = reservationRepository.findWithDetailsByReservationId(request.getReservationId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new ApiException(ErrorCode.RESERVATION_STATUS_TRANSITION_INVALID);
        }

        Long actorId = extractUserId(jwt);
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        ReservationCharge charge = ReservationCharge.builder()
                .reservation(reservation)
                .chargeType(request.getChargeType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .createdBy(actor)
                .build();
        reservationChargeRepository.save(charge);

        reservation.setTotalAmount(reservation.getTotalAmount() + request.getAmount());
        Reservation saved = reservationRepository.save(reservation);

        try {
            if (saved.getGuest() != null) {
                notificationService.createAndPush(
                        saved.getGuest(),
                        NotificationType.SYSTEM,
                        "Phát sinh chi phí",
                        String.format("Đơn %s phát sinh thêm %,d VND", saved.getReservationCode(), request.getAmount()),
                        saved.getReservationId()
                );
            }
        } catch (Exception ex) {
            log.warn("Charge notification push failed for reservation {}: {}", saved.getReservationId(), ex.getMessage());
        }

        return toReservationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getReservationPayments(Long reservationId, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithDetailsByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        ensureReservationReadable(reservation, jwt);

        return paymentRepository.findByReservation_ReservationIdOrderByPaidAtDescCreatedAtDesc(reservationId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationChargeResponse> getReservationCharges(Long reservationId, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithDetailsByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        ensureReservationReadable(reservation, jwt);

        return reservationChargeRepository.findByReservation_ReservationIdOrderByCreatedAtDesc(reservationId)
                .stream()
                .map(this::toChargeResponse)
                .collect(Collectors.toList());
    }


    private void applyStatusTransition(Reservation reservation, ReservationStatus targetStatus, String cancelReason, Jwt jwt) {
        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(targetStatus);

        if (targetStatus == ReservationStatus.CONFIRMED) {
            reservation.setConfirmedAt(now);
            Long actorId = extractUserId(jwt);
            User actor = userRepository.findById(actorId).orElse(null);
            reservation.setConfirmedBy(actor);
            return;
        }

        if (targetStatus == ReservationStatus.CHECKED_IN) {
            reservation.setCheckedInAt(now);
            List<Room> touchedRooms = new ArrayList<>();
            for (ReservationItem item : reservation.getItems()) {
                item.setStatus(ReservationItemStatus.CHECKED_IN);
                item.setCheckedInAt(now);
                if (item.getRoom() != null) {
                    item.getRoom().setStatus(RoomStatus.OCCUPIED);
                    touchedRooms.add(item.getRoom());
                }
            }
            roomRepository.saveAll(touchedRooms);
            return;
        }

        if (targetStatus == ReservationStatus.CHECKED_OUT) {
            reservation.setCheckedOutAt(now);
            List<Room> touchedRooms = new ArrayList<>();
            for (ReservationItem item : reservation.getItems()) {
                item.setStatus(ReservationItemStatus.CHECKED_OUT);
                item.setCheckedOutAt(now);
                if (item.getRoom() != null) {
                    item.getRoom().setStatus(RoomStatus.AVAILABLE);
                    touchedRooms.add(item.getRoom());
                }
            }
            roomRepository.saveAll(touchedRooms);
            return;
        }

        if (targetStatus == ReservationStatus.CANCELLED) {
            reservation.setCancelledAt(now);
            reservation.setCancelReason(cancelReason);
            Long actorId = extractUserId(jwt);
            User actor = userRepository.findById(actorId).orElse(null);
            reservation.setCancelledBy(actor);
            List<Room> touchedRooms = new ArrayList<>();
            for (ReservationItem item : reservation.getItems()) {
                item.setStatus(ReservationItemStatus.CANCELLED);
                if (item.getRoom() != null && item.getRoom().getStatus() == RoomStatus.OCCUPIED) {
                    item.getRoom().setStatus(RoomStatus.AVAILABLE);
                    touchedRooms.add(item.getRoom());
                }
            }
            if (!touchedRooms.isEmpty()) {
                roomRepository.saveAll(touchedRooms);
            }
        }
    }

    private boolean isValidTransition(ReservationStatus from, ReservationStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case PENDING -> to == ReservationStatus.CONFIRMED || to == ReservationStatus.CANCELLED;
            case CONFIRMED -> to == ReservationStatus.CHECKED_IN || to == ReservationStatus.CANCELLED;
            case CHECKED_IN -> to == ReservationStatus.CHECKED_OUT;
            case CHECKED_OUT, CANCELLED -> false;
        };
    }

    private void ensureReservationReadable(Reservation reservation, Jwt jwt) {
        if (hasAnyRole(jwt, Roles.ADMIN, Roles.STAFF)) {
            return;
        }
        Long userId = extractUserId(jwt);
        if (!Objects.equals(reservation.getGuest().getUserId(), userId)) {
            throw new ApiException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }
    }


    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private ReservationResponse toReservationResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .reservationId(reservation.getReservationId())
                .reservationCode(reservation.getReservationCode())
                .status(reservation.getStatus() != null ? reservation.getStatus().name() : null)
                .guestId(reservation.getGuest() != null ? reservation.getGuest().getUserId() : null)
                .guestUsername(reservation.getGuest() != null ? reservation.getGuest().getUsername() : null)
                .checkInDate(reservation.getCheckInDate())
                .checkOutDate(reservation.getCheckOutDate())
                .nightsCount(reservation.getNightsCount())
                .adultCount(reservation.getAdultCount())
                .childCount(reservation.getChildCount())
                .specialRequests(reservation.getSpecialRequests())
                .totalAmount(reservation.getTotalAmount())
                .paidAmount(reservation.getPaidAmount())
                .cancelReason(reservation.getCancelReason())
                .confirmedAt(reservation.getConfirmedAt())
                .checkedInAt(reservation.getCheckedInAt())
                .checkedOutAt(reservation.getCheckedOutAt())
                .cancelledAt(reservation.getCancelledAt())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .items(mapReservationItems(reservation.getItems()))
                .build();
    }

    private List<ReservationResponse.Item> mapReservationItems(List<ReservationItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> ReservationResponse.Item.builder()
                        .reservationItemId(item.getReservationItemId())
                        .roomId(item.getRoom() != null ? item.getRoom().getRoomId() : null)
                        .roomNumber(item.getRoom() != null ? item.getRoom().getRoomNumber() : null)
                        .roomTypeId(item.getRoomType() != null ? item.getRoomType().getRoomTypeId() : null)
                        .roomTypeCode(item.getRoomType() != null ? item.getRoomType().getCode() : null)
                        .roomTypeName(item.getRoomType() != null ? item.getRoomType().getName() : null)
                        .ratePerNight(item.getRatePerNight())
                        .nights(item.getNights())
                        .amount(item.getAmount())
                        .status(item.getStatus() != null ? item.getStatus().name() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private RoomResponse toRoomResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getRoomNumber())
                .roomTypeId(room.getRoomType() != null ? room.getRoomType().getRoomTypeId() : null)
                .roomTypeCode(room.getRoomType() != null ? room.getRoomType().getCode() : null)
                .roomTypeName(room.getRoomType() != null ? room.getRoomType().getName() : null)
                .floorId(room.getFloor() != null ? room.getFloor().getFloorId() : null)
                .floorCode(room.getFloor() != null ? room.getFloor().getCode() : null)
                .floorName(room.getFloor() != null ? room.getFloor().getName() : null)
                .status(room.getStatus() != null ? room.getStatus().name() : null)
                .note(room.getNote())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .images(mapImages(room.getImages()))
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .reservationId(payment.getReservation() != null ? payment.getReservation().getReservationId() : null)
                .guestId(payment.getGuest() != null ? payment.getGuest().getUserId() : null)
                .amount(payment.getAmount())
                .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                .provider(payment.getProvider())
                .providerTransId(payment.getProviderTransId())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .note(payment.getNote())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private ReservationChargeResponse toChargeResponse(ReservationCharge charge) {
        return ReservationChargeResponse.builder()
                .chargeId(charge.getChargeId())
                .reservationId(charge.getReservation() != null ? charge.getReservation().getReservationId() : null)
                .chargeType(charge.getChargeType() != null ? charge.getChargeType().name() : null)
                .description(charge.getDescription())
                .amount(charge.getAmount())
                .createdByUserId(charge.getCreatedBy() != null ? charge.getCreatedBy().getUserId() : null)
                .createdByUsername(charge.getCreatedBy() != null ? charge.getCreatedBy().getUsername() : null)
                .createdAt(charge.getCreatedAt())
                .build();
    }

    private List<RoomResponse.ImageItem> mapImages(List<RoomImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .sorted(Comparator.comparing(RoomImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(img -> RoomResponse.ImageItem.builder()
                        .imageId(img.getImageId())
                        .url(img.getUrl())
                        .caption(img.getCaption())
                        .isPrimary(img.getIsPrimary())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof Iterable<?> iterable) {
            Set<String> roles = new HashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString().toUpperCase(Locale.ROOT));
                }
            }
            if (!roles.isEmpty()) {
                return roles;
            }
        }

        Object roleObj = jwt.getClaims().get("role");
        if (roleObj != null) {
            return Set.of(roleObj.toString().toUpperCase(Locale.ROOT));
        }
        return Set.of();
    }

    private boolean hasAnyRole(Jwt jwt, Roles... roles) {
        Set<String> currentRoles = extractRoles(jwt);
        for (Roles role : roles) {
            if (currentRoles.contains(role.name())) {
                return true;
            }
        }
        return false;
    }

    private Roles resolveActorRole(Jwt jwt) {
        if (hasAnyRole(jwt, Roles.ADMIN)) {
            return Roles.ADMIN;
        }
        if (hasAnyRole(jwt, Roles.STAFF)) {
            return Roles.STAFF;
        }
        return Roles.GUEST;
    }

    private void requireAnyRole(Jwt jwt, Roles... roles) {
        if (!hasAnyRole(jwt, roles)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String generateReservationCode() {
        LocalDateTime now = LocalDateTime.now();
        int suffix = now.getNano() % 10000;
        return String.format(
                "RSV%04d%02d%02d%02d%02d%02d%04d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                now.getMinute(),
                now.getSecond(),
                suffix
        );
    }
}



