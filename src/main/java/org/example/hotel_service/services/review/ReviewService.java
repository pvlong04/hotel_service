package org.example.hotel_service.services.review;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.dtos.request.CreateReviewRequest;
import org.example.hotel_service.dtos.request.ReplyReviewRequest;
import org.example.hotel_service.dtos.response.ReviewResponse;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.entities.Review;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.ReservationRepository;
import org.example.hotel_service.repositories.ReviewRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService implements ReviewServiceImp {

    ReviewRepository reviewRepository;
    ReservationRepository reservationRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, Jwt jwt) {
        UUID userId = extractUserId(jwt);

        Reservation reservation = reservationRepository.findWithDetailsByReservationId(request.getReservationId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (reservation.getGuest() == null || !userId.equals(reservation.getGuest().getUserId())) {
            throw new ApiException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }

        if (reviewRepository.existsByReservation_ReservationId(request.getReservationId())) {
            throw new ApiException(ErrorCode.ILLEGAL_ARGUMENT);
        }

        Review review = Review.builder()
                .reservation(reservation)
                .guest(reservation.getGuest())
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .cleanlinessRating(request.getCleanlinessRating())
                .serviceRating(request.getServiceRating())
                .locationRating(request.getLocationRating())
                .facilitiesRating(request.getFacilitiesRating())
                .valueRating(request.getValueRating())
                .isVerified(true)
                .isVisible(true)
                .build();

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponse replyReview(Long reviewId, ReplyReviewRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

        UUID userId = extractUserId(jwt);
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Review review = reviewRepository.findWithDetailsByReviewId(reviewId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        review.setReply(request.getReply().trim());
        review.setRepliedAt(LocalDateTime.now());
        review.setRepliedBy(actor);

        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getPublicReviews() {
        return reviewRepository.findByIsVisibleTrueOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getPublicReviewsByRoomId(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new ApiException(ErrorCode.ILLEGAL_ARGUMENT);
        }

        return reviewRepository.findPublicReviewsByRoomId(roomId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return reviewRepository.findByGuest_UserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .reservationId(review.getReservation() != null ? review.getReservation().getReservationId() : null)
                .reservationCode(review.getReservation() != null ? review.getReservation().getReservationCode() : null)
                .guestId(review.getGuest() != null ? review.getGuest().getUserId() : null)
                .guestUsername(review.getGuest() != null ? review.getGuest().getUsername() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .cleanlinessRating(review.getCleanlinessRating())
                .serviceRating(review.getServiceRating())
                .locationRating(review.getLocationRating())
                .facilitiesRating(review.getFacilitiesRating())
                .valueRating(review.getValueRating())
                .isVerified(review.getIsVerified())
                .isVisible(review.getIsVisible())
                .reply(review.getReply())
                .repliedAt(review.getRepliedAt())
                .repliedByUserId(review.getRepliedBy() != null ? review.getRepliedBy().getUserId() : null)
                .repliedByUsername(review.getRepliedBy() != null ? review.getRepliedBy().getUsername() : null)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim instanceof UUID id) {
            return id;
        }
        if (userIdClaim instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
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

    private void requireAnyRole(Jwt jwt, Roles... roles) {
        Set<String> currentRoles = extractRoles(jwt);
        for (Roles role : roles) {
            if (currentRoles.contains(role.name())) {
                return;
            }
        }
        throw new ApiException(ErrorCode.ACCESS_DENIED);
    }
}

