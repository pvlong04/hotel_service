package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.AmenityCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu tạo/cập nhật tiện nghi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityRequest {

    @NotBlank(message = "Tên tiện nghi không được để trống")
    @Size(max = 120, message = "Tên tiện nghi không quá 120 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả không quá 255 ký tự")
    private String description;

    @Size(max = 100, message = "Icon không quá 100 ký tự")
    private String icon;

    private AmenityCategory category;
}
