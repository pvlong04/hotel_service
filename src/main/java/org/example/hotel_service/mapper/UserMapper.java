package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.request.CreateUserRequest;
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.request.UpdateUserRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.dtos.response.UserResponse;
import org.example.hotel_service.entities.Profile;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = AppMapperConfig.class)
public interface UserMapper {

    User toUser(RegisterRequest request);

    User toUser(CreateUserRequest request);

    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    @Mapping(target = "fullName", source = "profile.fullName")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "address", source = "profile.address")
    @Mapping(target = "dob", source = "profile.dob")
    @Mapping(target = "gender", expression = "java(user.getProfile() != null && user.getProfile().getGender() != null ? user.getProfile().getGender().name() : null)")
    @Mapping(target = "nationalId", source = "profile.nationalId")
    @Mapping(target = "nationality", source = "profile.nationality")
    @Mapping(target = "roles", expression = "java(extractRoleNames(user.getUserRoles()))")
    UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "fullName", source = "profile.fullName")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "roles", expression = "java(extractRoleNames(user.getUserRoles()))")
    AuthResponse.UserInfo toAuthUserInfo(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfile(UpdateUserRequest request, @MappingTarget Profile profile);

    default Set<String> extractRoleNames(Set<UserRole> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return Set.of();
        }
        return userRoles.stream()
                .filter(ur -> ur.getRole() != null && ur.getRole().getName() != null)
                .map(ur -> ur.getRole().getName().name())
                .collect(Collectors.toSet());
    }
}
