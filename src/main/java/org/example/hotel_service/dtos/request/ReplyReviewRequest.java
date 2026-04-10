package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplyReviewRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 5000, message = "Nội dung phản hồi tối đa 5000 ký tự")
    String reply;
}

