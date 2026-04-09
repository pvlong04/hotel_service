package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.entities.Room;
import org.example.hotel_service.entities.RoomImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(config = AppMapperConfig.class, uses = ImageUrlMapper.class)
public interface RoomMapper {

    @Mapping(target = "roomTypeId", source = "roomType.roomTypeId")
    @Mapping(target = "roomTypeCode", source = "roomType.code")
    @Mapping(target = "roomTypeName", source = "roomType.name")
    @Mapping(target = "floorId", source = "floor.floorId")
    @Mapping(target = "floorCode", source = "floor.code")
    @Mapping(target = "floorName", source = "floor.name")
    @Mapping(target = "status", expression = "java(room.getStatus() != null ? room.getStatus().name() : null)")
    @Mapping(target = "images", expression = "java(mapImages(room.getImages()))")
    RoomResponse toResponse(Room room);

    @Mapping(target = "url", source = "url", qualifiedByName = "normalizeImageUrl")
    RoomResponse.ImageItem toImageItem(RoomImage image);

    default List<RoomResponse.ImageItem> mapImages(List<RoomImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .sorted(Comparator.comparing(RoomImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toImageItem)
                .toList();
    }
}
