package org.example.hotel_service.services.review;

import org.example.hotel_service.dtos.request.CreateReviewRequest;
import org.example.hotel_service.dtos.request.ReplyReviewRequest;
import org.example.hotel_service.dtos.response.ReviewResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface ReviewServiceImp {

    ReviewResponse createReview(CreateReviewRequest request, Jwt jwt);

    ReviewResponse replyReview(Long reviewId, ReplyReviewRequest request, Jwt jwt);

    List<ReviewResponse> getPublicReviews();

    List<ReviewResponse> getPublicReviewsByRoomId(Long roomId);

    List<ReviewResponse> getMyReviews(Jwt jwt);
}

