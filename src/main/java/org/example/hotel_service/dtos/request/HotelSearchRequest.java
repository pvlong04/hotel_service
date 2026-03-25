package org.example.hotel_service.dtos.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu tìm kiếm khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelSearchRequest {

    @Size(max = 255, message = "Từ khóa không quá 255 ký tự")
    String keyword; // Tìm theo tên, địa chỉ

    LocalDate checkInDate;
    LocalDate checkOutDate;

    @Min(value = 1, message = "Số người lớn tối thiểu là 1")
    Integer adults;
    @Min(value = 0, message = "Số trẻ em không được âm")
    Integer children;
    @Min(value = 1, message = "Số phòng tối thiểu là 1")
    Integer rooms; // Số phòng cần

    // Filter
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    Integer minStarRating;
    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    Integer maxStarRating;
    @Min(value = 0, message = "Giá tối thiểu không được âm")
    Integer minPrice;
    @Min(value = 0, message = "Giá tối đa không được âm")
    Integer maxPrice;

    // Location
    Double latitude;
    Double longitude;
    @Positive(message = "Bán kính phải lớn hơn 0")
    Double radiusKm; // Bán kính tìm kiếm

    // Sort
    @Pattern(regexp = "^(?i)(name|rating|starRating|createdAt|updatedAt)?$", message = "sortBy không hợp lệ")
    String sortBy; // name, rating, createdAt, updatedAt
    @Pattern(regexp = "^(?i)(asc|desc)?$", message = "sortDirection chỉ nhận asc/desc")
    String sortDirection; // asc, desc

    // Pagination
    @Builder.Default
    @Min(value = 0, message = "Page không được âm")
    Integer page = 0;
    @Builder.Default
    @Min(value = 1, message = "Size tối thiểu là 1")
    @Max(value = 100, message = "Size tối đa là 100")
    Integer size = 10;
}
