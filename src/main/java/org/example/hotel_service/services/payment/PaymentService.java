package org.example.hotel_service.services.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.config.VnPayProperties;
import org.example.hotel_service.dtos.request.VnPayCreateRequest;
import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.VnPayCreateResponse;
import org.example.hotel_service.entities.Payment;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.enums.PaymentMethod;
import org.example.hotel_service.enums.PaymentStatus;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.mapper.PaymentMapper;
import org.example.hotel_service.repositories.PaymentRepository;
import org.example.hotel_service.repositories.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService implements PaymentServiceImp {

    PaymentRepository paymentRepository;
    ReservationRepository reservationRepository;
    VnPayProperties vnPayProperties;
    PaymentMapper paymentMapper;

    @Value("${app.payment.vnpay.pending-timeout-minutes:16}")
    @NonFinal
    int pendingTimeoutMinutes;


    @Transactional
    public VnPayCreateResponse createVnPayPayment(VnPayCreateRequest request, String clientIp, Jwt jwt) {
        Long reservationId = request.getReservationId();
        Long amount = request.getAmount();

        Reservation reservation = reservationRepository.findWithDetailsByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        ensureReservationReadable(reservation, jwt);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(ErrorCode.RESERVATION_STATUS_TRANSITION_INVALID);
        }

        long remaining = Math.max(0L, reservation.getTotalAmount() - reservation.getPaidAmount());
        if (amount == null || amount <= 0 || amount > remaining) {
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_INVALID);
        }

        validateVnPayConfig();

        String txnRef = reservation.getReservationCode() + "_" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(15);

        Payment payment = Payment.builder()
                .reservation(reservation)
                .guest(reservation.getGuest())
                .amount(amount)
                .method(PaymentMethod.ONLINE)
                .provider("VNPAY")
                .providerTransId(txnRef)
                .status(PaymentStatus.PENDING)
                .note("Khoi tao thanh toan VNPay")
                .build();

        Payment saved = paymentRepository.save(payment);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", vnPayProperties.getVersion());
        params.put("vnp_Command", vnPayProperties.getCommand());
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CreateDate", formatDate(now));
        params.put("vnp_CurrCode", vnPayProperties.getCurrCode());
        params.put("vnp_IpAddr", (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp);
        params.put("vnp_Locale", vnPayProperties.getLocale());
        params.put("vnp_OrderInfo", "Thanh toan dat phong " + reservation.getReservationCode());
        params.put("vnp_OrderType", vnPayProperties.getOrderType());
        params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_ExpireDate", formatDate(expiresAt));

        String hashData = buildVnPayHashData(params);
        String queryData = buildVnPayQuery(params);
        String secureHash = hmacSha512(vnPayProperties.getHashSecret(), hashData);
        String paymentUrl = vnPayProperties.getPayUrl() + "?" + queryData + "&vnp_SecureHash=" + secureHash;
        String qrImage = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + encode(paymentUrl);

        return VnPayCreateResponse.builder()
                .paymentId(saved.getPaymentId())
                .reservationId(reservationId)
                .amount(amount)
                .status(saved.getStatus().name())
                .txnRef(txnRef)
                .paymentUrl(paymentUrl)
                .qrContent(paymentUrl)
                .qrImage(qrImage)
                .expiresAt(expiresAt)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public PaymentResponse confirmVnPayPayment(Map<String, String> callbackParams, Jwt jwt) {
        validateVnPayConfig();
        if (callbackParams == null || callbackParams.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String secureHash = callbackParams.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        Map<String, String> signData = callbackParams.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().startsWith("vnp_"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
        signData.remove("vnp_SecureHash");
        signData.remove("vnp_SecureHashType");
        String expectedHash = hmacSha512(vnPayProperties.getHashSecret(), buildVnPayHashData(signData));
        if (!expectedHash.equalsIgnoreCase(secureHash)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String txnRef = callbackParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        Payment payment = paymentRepository.findByProviderTransId(txnRef)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (jwt != null) {
            ensureReservationReadable(payment.getReservation(), jwt);
        }

        Long paidAmountFromGateway = parseVnpAmount(callbackParams.get("vnp_Amount"));
        if (paidAmountFromGateway == null || !Objects.equals(paidAmountFromGateway, payment.getAmount())) {
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_INVALID);
        }

        String responseCode = callbackParams.get("vnp_ResponseCode");
        String transactionStatus = callbackParams.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            return toPaymentResponse(payment);
        }

        if (!success) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setNote("VNPay thanh toan that bai. Code=" + (responseCode != null ? responseCode : "N/A"));
            cancelReservationForPaymentFailure(payment.getReservation(), "VNPay thanh toan that bai hoac bi huy");
            Payment saved = paymentRepository.save(payment);
            reservationRepository.save(payment.getReservation());
            return toPaymentResponse(saved);
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setNote("VNPay thanh toan thanh cong");
        Payment saved = paymentRepository.save(payment);

        Reservation reservation = payment.getReservation();
        reservation.setPaidAmount(Math.min(
                reservation.getTotalAmount(),
                reservation.getPaidAmount() + payment.getAmount()
        ));
        reservationRepository.save(reservation);

        log.info("VNPay payment confirmed: paymentId={}, amount={}, reservationId={}",
                payment.getPaymentId(), payment.getAmount(), reservation.getReservationId());

        return toPaymentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, Jwt jwt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        ensureReservationReadable(payment.getReservation(), jwt);

        return toPaymentResponse(payment);
    }

    @Transactional
    public int cancelExpiredPendingVnPayPayments() {
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(Math.max(1, pendingTimeoutMinutes));
        List<Payment> stalePayments = paymentRepository.findByProviderAndStatusAndCreatedAtBefore(
                "VNPAY", PaymentStatus.PENDING, expiredAt
        );

        if (stalePayments.isEmpty()) {
            return 0;
        }

        int cancelledCount = 0;
        for (Payment payment : stalePayments) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setNote("VNPay qua han thanh toan - tu dong huy don");
            cancelReservationForPaymentFailure(payment.getReservation(), "Qua han thanh toan VNPay");
            paymentRepository.save(payment);
            reservationRepository.save(payment.getReservation());
            cancelledCount++;
        }

        if (cancelledCount > 0) {
            log.info("Auto cancelled {} expired VNPay payment(s)", cancelledCount);
        }
        return cancelledCount;
    }

    private void cancelReservationForPaymentFailure(Reservation reservation, String reason) {
        if (reservation == null || reservation.getStatus() == null) {
            return;
        }

        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setCancelReason(reason);
            reservation.setCancelledAt(LocalDateTime.now());
        }
    }

    private void validateVnPayConfig() {
        if (isBlank(vnPayProperties.getTmnCode()) || isBlank(vnPayProperties.getHashSecret()) || isBlank(vnPayProperties.getPayUrl())) {
            throw new ApiException(ErrorCode.KEY_VALID);
        }
    }

    private String formatDate(LocalDateTime time) {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(time);
    }

    private String buildVnPayHashData(Map<String, String> params) {
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(params.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        return sortedEntries.stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> e.getKey() + "=" + asciiEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String buildVnPayQuery(Map<String, String> params) {
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(params.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        return sortedEntries.stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> asciiEncode(e.getKey()) + "=" + asciiEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String asciiEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UNCATEGORIZED_EXIT);
        }
    }

    private Long parseVnpAmount(String rawValue) {
        if (isBlank(rawValue)) {
            return null;
        }
        try {
            long amount = Long.parseLong(rawValue);
            if (amount <= 0 || amount % 100 != 0) {
                return null;
            }
            return amount / 100;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void ensureReservationReadable(Reservation reservation, Jwt jwt) {
        if (hasAnyRole(jwt, Roles.ADMIN, Roles.STAFF)) {
            return;
        }
        UUID userId = extractUserId(jwt);
        if (!Objects.equals(reservation.getGuest().getUserId(), userId)) {
            throw new ApiException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim instanceof UUID id) {
            return id;
        }
        if (userIdClaim instanceof String) {
            String text = (String) userIdClaim;
            if (!text.trim().isEmpty()) {
                return UUID.fromString(text);
            }
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) rolesObj;
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
        return paymentMapper.toResponse(payment);
    }
}
