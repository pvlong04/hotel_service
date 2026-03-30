package org.example.hotel_service.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi thông tin phòng vật lý.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {

    Long roomId;
    String roomNumber;
    Long roomTypeId;
    String roomTypeCode;
    String roomTypeName;
    Integer floorId;
    String floorCode;
    String floorName;
    String status;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    List<ImageItem> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ImageItem {
        Long imageId;
        String url;
        String caption;
        Boolean isPrimary;
        Integer sortOrder;
    }
}
