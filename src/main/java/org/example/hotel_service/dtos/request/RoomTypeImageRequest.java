package org.example.hotel_service.dtos.request;

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
 * DTO yêu cầu thêm hình ảnh loại phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeImageRequest {

    @NotNull(message = "Room Type ID không được để trống")
    Long roomTypeId;

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Size(max = 500, message = "URL không quá 500 ký tự")
    String url;

    @Builder.Default
    Integer displayOrder = 0;

    @Builder.Default
    Boolean isPrimary = false;

    @Size(max = 255, message = "Alt text không quá 255 ký tự")
    String altText;
}
