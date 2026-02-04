package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu tạo/cập nhật tầng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloorRequest {

    @NotNull(message = "Hotel ID không được để trống")
    private Integer hotelId;

    @NotBlank(message = "Mã tầng không được để trống")
    @Size(max = 40, message = "Mã tầng không quá 40 ký tự")
    private String code;

    @Size(max = 120, message = "Tên tầng không quá 120 ký tự")
    private String name;

    private Integer floorOrder;
}
