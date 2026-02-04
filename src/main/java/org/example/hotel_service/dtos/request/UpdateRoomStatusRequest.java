package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.RoomStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu cập nhật trạng thái phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private RoomStatus status;

    @Size(max = 255, message = "Lý do không quá 255 ký tự")
    private String reason;
}
