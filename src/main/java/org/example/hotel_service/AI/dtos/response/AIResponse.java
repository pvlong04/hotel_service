package org.example.hotel_service.AI.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    private String answer;
    private String sessionId;
    private String status;
    private String modelUsed;
    private String warning;

    /** Room suggestions attached when the question is room-related. */
    private List<RoomSuggestion> suggestedRooms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSuggestion {
        private Long roomId;
        private String roomNumber;
        private String roomTypeName;
        private String floorName;
        private String status;
        private Long pricePerNight;
        private Long weekendPrice;
        private String bedType;
        private Integer bedCount;
        private Integer maxAdults;
        private Integer maxChildren;
        private String roomSize;
        private String primaryImageUrl;
        private String note;
    }
}
