package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.response.NotificationResponse;
import org.example.hotel_service.entities.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = AppMapperConfig.class)
public interface NotificationMapper {

    @Mapping(target = "type", expression = "java(notification.getType() != null ? notification.getType().name() : null)")
    NotificationResponse toResponse(Notification notification);
}
