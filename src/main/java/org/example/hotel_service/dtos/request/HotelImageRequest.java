package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.HotelImageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO hình ảnh trong yêu cầu tạo/cập nhật khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelImageRequest {

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Size(max = 500, message = "URL không quá 500 ký tự")
    String url;

    @Size(max = 255, message = "Caption không quá 255 ký tự")
    String caption;

    HotelImageType type;

    @Builder.Default
    Integer sortOrder = 0;

    @Builder.Default
    Boolean isPrimary = false;
}
