package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.ReservationStatus;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu cập nhật trạng thái đặt phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReservationStatusRequest {

    private ReservationStatus status;

    @Size(max = 500, message = "Lý do hủy không quá 500 ký tự")
    private String cancelReason;

    @Size(max = 2000, message = "Ghi chú nội bộ không quá 2000 ký tự")
    private String internalNotes;
}
