package org.example.hotel_service.services.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.QrPaymentResponse;
import org.example.hotel_service.entities.Payment;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.enums.PaymentMethod;
import org.example.hotel_service.enums.PaymentStatus;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.PaymentRepository;
import org.example.hotel_service.repositories.ReservationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService implements PaymentServiceImp {

    PaymentRepository paymentRepository;
    ReservationRepository reservationRepository;

    // =====================  Thông tin mô phỏng ngân hàng  =====================
    private static final String BANK_NAME = "VIETCOMBANK";
    private static final String BANK_ACCOUNT = "1234567890";
    private static final String BANK_ACCOUNT_NAME = "HOTEL GRAND LUXURY";

    // =========================================================================

    @Override
    @Transactional
    public QrPaymentResponse generateQrPayment(Long reservationId, Long amount, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithDetailsByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        // Kiểm tra quyền truy cập
        ensureReservationReadable(reservation, jwt);

        // Không cho thanh toán đơn đã hủy
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(ErrorCode.RESERVATION_STATUS_TRANSITION_INVALID);
        }

        // Kiểm tra số tiền hợp lệ
        long remaining = Math.max(0L, reservation.getTotalAmount() - reservation.getPaidAmount());
        if (amount == null || amount <= 0 || amount > remaining) {
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_INVALID);
        }

        // Tạo mã giao dịch mô phỏng
        String transId = "QR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Tạo bản ghi Payment trạng thái PENDING
        Payment payment = Payment.builder()
                .reservation(reservation)
                .guest(reservation.getGuest())
                .amount(amount)
                .method(PaymentMethod.QR_CODE)
                .provider("QR_SIMULATOR")
                .providerTransId(transId)
                .status(PaymentStatus.PENDING)
                .note("Thanh toán QR mô phỏng")
                .build();

        Payment saved = paymentRepository.save(payment);

        // Tạo nội dung QR mô phỏng (giống VietQR format đơn giản)
        String qrContent = buildQrContent(reservation.getReservationCode(), amount, transId);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        return QrPaymentResponse.builder()
                .paymentId(saved.getPaymentId())
                .reservationId(reservationId)
                .amount(amount)
                .status(saved.getStatus().name())
                .qrContent(qrContent)
                .expiresAt(expiresAt)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse confirmQrPayment(Long paymentId, Jwt jwt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new ApiException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // Cập nhật trạng thái thanh toán
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setNote("Thanh toán QR thành công (mô phỏng)");
        Payment saved = paymentRepository.save(payment);

        // Cập nhật paidAmount của Reservation
        Reservation reservation = payment.getReservation();
        reservation.setPaidAmount(reservation.getPaidAmount() + payment.getAmount());
        reservationRepository.save(reservation);

        log.info("QR Payment confirmed: paymentId={}, amount={}, reservationId={}",
                paymentId, payment.getAmount(), reservation.getReservationId());

        return toPaymentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, Jwt jwt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        // Kiểm tra quyền: admin/staff luôn xem được, guest chỉ xem của mình
        ensureReservationReadable(payment.getReservation(), jwt);

        return toPaymentResponse(payment);
    }

    // ========================  Helper methods  ========================

    /**
     * Tạo URL VietQR – QR chuyển khoản ngân hàng chuẩn Napas  
     * Format: https://img.vietqr.io/image/{BANK_BIN}-{ACCOUNT_NO}-{TEMPLATE}.png?amount={AMOUNT}&addInfo={DESC}
     */
    private String buildQrContent(String reservationCode, Long amount, String transId) {
        // VietQR format with Vietcombank (BIN: 970436)
        String bankBin = "970436";  // VIETCOMBANK
        String accountNo = BANK_ACCOUNT;
        String template = "compact2";
        String description = "Thanh toan " + reservationCode;

        try {
            String encodedDesc = java.net.URLEncoder.encode(description, "UTF-8");
            return String.format(
                    "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                    bankBin, accountNo, template, amount, encodedDesc,
                    java.net.URLEncoder.encode(BANK_ACCOUNT_NAME, "UTF-8")
            );
        } catch (java.io.UnsupportedEncodingException e) {
            // Fallback – plain URL without encoding
            return String.format(
                    "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s",
                    bankBin, accountNo, template, amount, description.replace(" ", "+")
            );
        }
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
                    roles.add(item.toString().toUpperCase());
                }
            }
            if (!roles.isEmpty()) {
                return roles;
            }
        }
        Object roleObj = jwt.getClaims().get("role");
        if (roleObj != null) {
            return Set.of(roleObj.toString().toUpperCase());
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
}
