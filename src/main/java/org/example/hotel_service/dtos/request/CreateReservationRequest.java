package org.example.hotel_service.dtos.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu đặt phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotNull(message = "Hotel ID không được để trống")
    private Integer hotelId;

    @NotNull(message = "Ngày check-in không được để trống")
    private LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "Số người lớn tối thiểu là 1")
    @Builder.Default
    private Integer adults = 1;

    @Min(value = 0, message = "Số trẻ em không được âm")
    @Builder.Default
    private Integer children = 0;

    @Size(max = 2000, message = "Yêu cầu đặc biệt không quá 2000 ký tự")
    private String specialRequests;

    // Danh sách phòng muốn đặt
    @NotNull(message = "Danh sách phòng không được để trống")
    @Size(min = 1, message = "Phải chọn ít nhất 1 phòng")
    private List<ReservationRoomRequest> rooms;

    /**
     * Chi tiết phòng trong yêu cầu đặt
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationRoomRequest {
        @NotNull(message = "Room Type ID không được để trống")
        private Long roomTypeId;

        // Room ID cụ thể (optional - có thể để hệ thống tự chọn)
        private Long roomId;
    }
}
