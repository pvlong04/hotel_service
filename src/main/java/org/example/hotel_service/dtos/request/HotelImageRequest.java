package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.HotelImageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu thêm hình ảnh khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelImageRequest {

    @NotNull(message = "Hotel ID không được để trống")
    Integer hotelId;

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Size(max = 500, message = "URL không quá 500 ký tự")
    String url;

    HotelImageType imageType;

    @Builder.Default
    Integer displayOrder = 0;

    @Builder.Default
    Boolean isPrimary = false;

    @Size(max = 255, message = "Alt text không quá 255 ký tự")
    String altText;
}
