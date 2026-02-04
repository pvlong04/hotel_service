package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu phản hồi đánh giá (từ khách sạn)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyReviewRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String reply;
}
