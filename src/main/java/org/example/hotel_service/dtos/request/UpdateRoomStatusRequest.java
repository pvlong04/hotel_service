package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.RoomStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu cập nhật trạng thái phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoomStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    RoomStatus status;

    @Size(max = 255, message = "Lý do không quá 255 ký tự")
    String reason;
}
