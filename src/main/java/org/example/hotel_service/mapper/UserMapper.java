package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(RegisterRequest request);

    AuthResponse toUserResponse(User user);
}
