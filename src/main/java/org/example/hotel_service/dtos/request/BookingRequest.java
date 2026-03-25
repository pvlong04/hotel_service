package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO yêu cầu tạo/cập nhật booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRequest {

    @NotNull(message = "Trạng thái booking không được để trống")
    BookingStatus status;

    @NotEmpty(message = "Danh sách phòng không được để trống")
    List<Long> roomIds;

    @NotNull(message = "Thời gian lấy phòng không được để trống")
    LocalDate checkIn;

    @NotNull(message = "Thời gian trả phòng không được để trống")
    LocalDate checkOut;
}

