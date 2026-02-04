package org.example.hotel_service.dtos.request;

import java.math.BigDecimal;
import java.util.List;

import org.example.hotel_service.enums.BedType;
import org.example.hotel_service.enums.RoomTypeStatus;

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
 * DTO yêu cầu tạo/cập nhật loại phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeRequest {

    @NotNull(message = "Hotel ID không được để trống")
    Integer hotelId;

    @NotBlank(message = "Mã loại phòng không được để trống")
    @Size(max = 50, message = "Mã loại phòng không quá 50 ký tự")
    String code;

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 150, message = "Tên loại phòng không quá 150 ký tự")
    String name;

    String description;

    @Min(value = 1, message = "Sức chứa tối thiểu là 1")
    Integer capacity;

    @Min(value = 1, message = "Số người lớn tối đa tối thiểu là 1")
    Integer maxAdults;

    @Min(value = 0, message = "Số trẻ em tối đa không được âm")
    Integer maxChildren;

    @Min(value = 0, message = "Giá cơ bản không được âm")
    Integer basePrice;

    @Min(value = 0, message = "Giá cuối tuần không được âm")
    Integer weekendPrice;

    @Min(value = 0, message = "Phí giường phụ không được âm")
    Integer extraBedPrice;

    BedType bedType;

    BigDecimal roomSize;

    RoomTypeStatus status;

    // Danh sách ID tiện nghi
    List<Integer> amenityIds;
}
