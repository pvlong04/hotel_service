package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.request.AmenityRequest;
import org.example.hotel_service.dtos.response.AmenityResponse;
import org.example.hotel_service.entities.Amenity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = AppMapperConfig.class)
public interface AmenityMapper {

    @Mapping(target = "category", expression = "java(amenity.getCategory() != null ? amenity.getCategory().name() : null)")
    AmenityResponse toResponse(Amenity amenity);

    Amenity toEntity(AmenityRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AmenityRequest request, @MappingTarget Amenity amenity);
}
