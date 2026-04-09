package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.request.RoomTypeRequest;
import org.example.hotel_service.dtos.response.RoomTypeResponse;
import org.example.hotel_service.entities.Amenity;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.entities.RoomTypeImage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Mapper(config = AppMapperConfig.class)
public interface RoomTypeMapper {

    @Mapping(target = "bedType", expression = "java(roomType.getBedType() != null ? roomType.getBedType().name() : null)")
    @Mapping(target = "status", expression = "java(roomType.getStatus() != null ? roomType.getStatus().name() : null)")
    @Mapping(target = "amenities", expression = "java(mapAmenities(roomType.getAmenities()))")
    @Mapping(target = "images", expression = "java(mapImages(roomType.getImages()))")
    RoomTypeResponse toResponse(RoomType roomType);

    RoomType toEntity(RoomTypeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "amenities", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntity(RoomTypeRequest request, @MappingTarget RoomType roomType);

    default List<RoomTypeResponse.AmenityItem> mapAmenities(List<Amenity> amenities) {
        if (amenities == null) {
            return List.of();
        }
        return amenities.stream()
                .filter(Objects::nonNull)
                .map(amenity -> RoomTypeResponse.AmenityItem.builder()
                        .amenityId(amenity.getAmenityId())
                        .name(amenity.getName())
                        .category(amenity.getCategory() != null ? amenity.getCategory().name() : null)
                        .icon(amenity.getIcon())
                        .build())
                .toList();
    }

    default List<RoomTypeResponse.ImageItem> mapImages(List<RoomTypeImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RoomTypeImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(image -> RoomTypeResponse.ImageItem.builder()
                        .imageId(image.getImageId())
                        .url(image.getUrl())
                        .caption(image.getCaption())
                        .isPrimary(image.getIsPrimary())
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();
    }
}
