package org.example.hotel_service.dtos.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.GeneratedValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.FutureOrPresent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.NoArgsConstructor;

/**
 * Yêu cầu đặt phòng từ khách hàng (GUEST).
 * Khách chọn khách sạn, ngày, số người và danh sách phòng muốn đặt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateReservationRequest {

    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    Integer hotelId;

    @NotNull(message = "Ngày check-in không được để trống")
    @FutureOrPresent(message = "Ngày check-in phải từ hôm nay trở đi")
    LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    LocalDate checkOutDate;

    @Min(value = 1, message = "Số người lớn tối thiểu là 1")
    @NotNull(message = "Số người lớn không được để trống")
    @Builder.Default
    Integer adultCount = 1;

    @Min(value = 0, message = "Số trẻ em không được âm")
    @Builder.Default
    Integer childCount = 0;

    @Size(max = 2000, message = "Yêu cầu đặc biệt không quá 2000 ký tự")
    String specialRequests;

    @NotNull(message = "Danh sách phòng không được để trống")
    @Size(min = 1, message = "Phải chọn ít nhất 1 phòng")
    @Valid
    List<ReservationRoomItem> rooms;

    /**
     * Chi tiết từng phòng trong đơn đặt.
     * Khách có thể chỉ định phòng cụ thể (roomId) hoặc chỉ chọn loại phòng
     * để hệ thống tự phân phòng trống.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReservationRoomItem {

        @NotNull(message = "Room Type ID không được để trống")
        Long roomTypeId;

        /** Tùy chọn: khách chỉ định phòng cụ thể */
        Long roomId;
    }
}
