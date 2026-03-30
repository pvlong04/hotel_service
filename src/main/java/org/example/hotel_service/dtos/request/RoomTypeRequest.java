package org.example.hotel_service.dtos.request;

import java.math.BigDecimal;
import java.util.List;

import org.example.hotel_service.enums.BedType;
import org.example.hotel_service.enums.RoomTypeStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
 * Tạo / cập nhật loại phòng.
 * ADMIN sử dụng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeRequest {


    @NotBlank(message = "Mã loại phòng không được để trống")
    @Size(max = 50, message = "Mã loại phòng không quá 50 ký tự")
    String code;

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 150, message = "Tên loại phòng không quá 150 ký tự")
    String name;

    @Size(max = 5000, message = "Mô tả không quá 5000 ký tự")
    String description;

    @Min(value = 1, message = "Sức chứa tối thiểu là 1")
    @NotNull(message = "Sức chứa không được để trống")
    Integer capacity;

    @Min(value = 1, message = "Số người lớn tối đa tối thiểu là 1")
    @NotNull(message = "Số người lớn tối đa không được để trống")
    Integer maxAdults;

    @Min(value = 0, message = "Số trẻ em tối đa không được âm")
    @Builder.Default
    Integer maxChildren = 0;

    @Min(value = 0, message = "Giá mỗi đêm không được âm")
    @NotNull(message = "Giá mỗi đêm không được để trống")
    Long pricePerNight;

    /** Giá cuối tuần – null nghĩa là dùng giá cơ bản */
    @Min(value = 0, message = "Giá cuối tuần không được âm")
    Long weekendPrice;

    /** Phí giường phụ – null nghĩa là không cho thuê giường phụ */
    @Min(value = 0, message = "Phí giường phụ không được âm")
    Long extraBedPrice;

    BedType bedType;

    @Min(value = 1, message = "Số giường tối thiểu là 1")
    @Builder.Default
    Integer bedCount = 1;

    @DecimalMin(value = "0.0", message = "Diện tích không được âm")
    BigDecimal roomSize;

    RoomTypeStatus status;

    /** Danh sách ID tiện nghi */
    List<Integer> amenityIds;
}
