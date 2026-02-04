package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu thêm hình ảnh loại phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeImageRequest {

    @NotNull(message = "Room Type ID không được để trống")
    private Long roomTypeId;

    @NotBlank(message = "URL hình ảnh không được để trống")
    @Size(max = 500, message = "URL không quá 500 ký tự")
    private String url;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private Boolean isPrimary = false;

    @Size(max = 255, message = "Alt text không quá 255 ký tự")
    private String altText;
}
