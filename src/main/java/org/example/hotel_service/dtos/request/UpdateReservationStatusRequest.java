package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.ReservationStatus;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu cập nhật trạng thái đặt phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateReservationStatusRequest {

    ReservationStatus status;

    @Size(max = 500, message = "Lý do hủy không quá 500 ký tự")
    String cancelReason;

    @Size(max = 2000, message = "Ghi chú nội bộ không quá 2000 ký tự")
    String internalNotes;
}
