package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.RoomStatus;

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
 * DTO yêu cầu tạo/cập nhật phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomRequest {

    @NotNull(message = "Hotel ID không được để trống")
    Integer hotelId;

    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 50, message = "Số phòng không quá 50 ký tự")
    String roomNumber;

    @NotNull(message = "Room Type ID không được để trống")
    Long roomTypeId;

    Integer floorId;

    RoomStatus status;

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    String note;
}
