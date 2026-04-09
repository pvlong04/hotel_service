package org.example.hotel_service.services.roomtype;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.dtos.request.RoomTypeRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomTypeResponse;
import org.example.hotel_service.entities.Amenity;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.RoomTypeStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.mapper.RoomTypeMapper;
import org.example.hotel_service.repositories.AmenityRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.example.hotel_service.repositories.RoomTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeService implements RoomTypeServiceImp {
	RoomTypeRepository roomTypeRepository;
	AmenityRepository amenityRepository;
	RoomRepository roomRepository;
	RoomTypeMapper roomTypeMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<RoomTypeResponse> getRoomTypes(RoomTypeStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<RoomType> roomTypePage = status == null
				? roomTypeRepository.findAllBy(pageable)
				: roomTypeRepository.findByStatus(status, pageable);

		return PageResponse.<RoomTypeResponse>builder()
				.content(roomTypePage.getContent().stream().map(roomTypeMapper::toResponse).collect(Collectors.toList()))
				.page(roomTypePage.getNumber())
				.size(roomTypePage.getSize())
				.totalElements(roomTypePage.getTotalElements())
				.totalPages(roomTypePage.getTotalPages())
				.first(roomTypePage.isFirst())
				.last(roomTypePage.isLast())
				.hasNext(roomTypePage.hasNext())
				.hasPrevious(roomTypePage.hasPrevious())
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public RoomTypeResponse getRoomTypeById(Long id) {
		RoomType roomType = roomTypeRepository.findWithDetailsByRoomTypeId(id)
				.orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));
		return roomTypeMapper.toResponse(roomType);
	}

	@Override
	@Transactional
	public RoomTypeResponse createRoomType(RoomTypeRequest request, Jwt jwt) {
		requireAnyRole(jwt, Roles.ADMIN);

		String code = request.getCode().trim();
		if (roomTypeRepository.existsByCode(code)) {
			throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
		}

		RoomType roomType = roomTypeMapper.toEntity(request);
		roomType.setCode(code);
		roomType.setStatus(request.getStatus() != null ? request.getStatus() : RoomTypeStatus.ACTIVE);
		roomType.setAmenities(resolveAmenities(request.getAmenityIds()));
		roomType.setImages(new ArrayList<>());

		RoomType saved = roomTypeRepository.save(roomType);
		return roomTypeMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request, Jwt jwt) {
		requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

		RoomType roomType = roomTypeRepository.findWithDetailsByRoomTypeId(id)
				.orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));

		String code = request.getCode().trim();
		if (roomTypeRepository.existsByCodeAndRoomTypeIdNot(code, id)) {
			throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
		}

		roomTypeMapper.updateEntity(request, roomType);
		roomType.setCode(code);
		roomType.setStatus(request.getStatus() != null ? request.getStatus() : roomType.getStatus());
		roomType.setAmenities(resolveAmenities(request.getAmenityIds()));

		RoomType saved = roomTypeRepository.save(roomType);
		return roomTypeMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public void deleteRoomType(Long id, Jwt jwt) {
		requireAnyRole(jwt, Roles.ADMIN);
		RoomType roomType = roomTypeRepository.findWithDetailsByRoomTypeId(id)
				.orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));

		if (roomRepository.existsByRoomType_RoomTypeId(id)) {
			throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
		}
		roomTypeRepository.delete(roomType);
	}


	private List<Amenity> resolveAmenities(List<Integer> amenityIds) {
		if (amenityIds == null || amenityIds.isEmpty()) {
			return new ArrayList<>();
		}
		List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
		if (amenities.size() != amenityIds.size()) {
			throw new ApiException(ErrorCode.ILLEGAL_ARGUMENT);
		}
		return amenities;
	}

	private Set<String> extractRoles(Jwt jwt) {
		Object rolesObj = jwt.getClaims().get("roles");
		if (rolesObj instanceof Iterable<?> iterable) {
			Set<String> roles = new HashSet<>();
			for (Object item : iterable) {
				if (item != null) {
					roles.add(item.toString().toUpperCase());
				}
			}
			if (!roles.isEmpty()) {
				return roles;
			}
		}

		Object roleObj = jwt.getClaims().get("role");
		if (roleObj != null) {
			return Set.of(roleObj.toString().toUpperCase());
		}
		return Set.of();
	}

	private void requireAnyRole(Jwt jwt, Roles... allowedRoles) {
		Set<String> currentRoles = extractRoles(jwt);
		for (Roles allowedRole : allowedRoles) {
			if (currentRoles.contains(allowedRole.name())) {
				return;
			}
		}
		throw new ApiException(ErrorCode.ACCESS_DENIED);
	}
}
