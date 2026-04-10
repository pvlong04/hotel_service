package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.CreateReviewRequest;
import org.example.hotel_service.dtos.request.ReplyReviewRequest;
import org.example.hotel_service.dtos.response.ReviewResponse;
import org.example.hotel_service.services.review.ReviewServiceImp;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewServiceImp reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReviewResponse result = reviewService.createReview(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đánh giá thành công", result));
    }

    @PatchMapping("/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReplyReviewRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReviewResponse result = reviewService.replyReview(reviewId, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Phản hồi đánh giá thành công", result));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getPublicReviews() {
        List<ReviewResponse> result = reviewService.getPublicReviews();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá công khai thành công", result));
    }

    @GetMapping("/public/rooms/{roomId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getPublicReviewsByRoomId(
            @PathVariable Long roomId
    ) {
        List<ReviewResponse> result = reviewService.getPublicReviewsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá công khai theo phòng thành công", result));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<ReviewResponse> result = reviewService.getMyReviews(jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá của bạn thành công", result));
    }
}

