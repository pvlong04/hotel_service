package org.example.hotel_service.services.roomtype;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.dtos.request.RoomTypeRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomTypeResponse;
import org.example.hotel_service.entities.Amenity;
import org.example.hotel_service.entities.Hotel;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.entities.RoomTypeImage;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.RoomTypeStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.AmenityRepository;
import org.example.hotel_service.repositories.HotelRepository;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeService implements RoomTypeServiceImp {
	RoomTypeRepository roomTypeRepository;
	HotelRepository hotelRepository;
	AmenityRepository amenityRepository;
	RoomRepository roomRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<RoomTypeResponse> getRoomTypes(RoomTypeStatus status, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<RoomType> roomTypePage = status == null
				? roomTypeRepository.findAllBy(pageable)
				: roomTypeRepository.findByStatus(status, pageable);

		return PageResponse.<RoomTypeResponse>builder()
				.content(roomTypePage.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
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
		return toResponse(roomType);
	}

	@Override
	@Transactional
	public RoomTypeResponse createRoomType(RoomTypeRequest request, Jwt jwt) {
		requireAnyRole(jwt, Roles.ADMIN);
		Hotel hotel = getCurrentHotel();

		String code = request.getCode().trim();
		if (roomTypeRepository.existsByCode(code)) {
			throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
		}

		RoomType roomType = RoomType.builder()
				.hotel(hotel)
				.code(code)
				.name(request.getName())
				.description(request.getDescription())
				.capacity(request.getCapacity())
				.maxAdults(request.getMaxAdults())
				.maxChildren(request.getMaxChildren())
				.pricePerNight(request.getPricePerNight())
				.weekendPrice(request.getWeekendPrice())
				.extraBedPrice(request.getExtraBedPrice())
				.bedType(request.getBedType())
				.bedCount(request.getBedCount())
				.roomSize(request.getRoomSize())
				.status(request.getStatus() != null ? request.getStatus() : RoomTypeStatus.ACTIVE)
				.amenities(resolveAmenities(request.getAmenityIds()))
				.images(new ArrayList<>())
				.build();

		RoomType saved = roomTypeRepository.save(roomType);
		return toResponse(saved);
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

		roomType.setCode(code);
		roomType.setName(request.getName());
		roomType.setDescription(request.getDescription());
		roomType.setCapacity(request.getCapacity());
		roomType.setMaxAdults(request.getMaxAdults());
		roomType.setMaxChildren(request.getMaxChildren());
		roomType.setPricePerNight(request.getPricePerNight());
		roomType.setWeekendPrice(request.getWeekendPrice());
		roomType.setExtraBedPrice(request.getExtraBedPrice());
		roomType.setBedType(request.getBedType());
		roomType.setBedCount(request.getBedCount());
		roomType.setRoomSize(request.getRoomSize());
		roomType.setStatus(request.getStatus() != null ? request.getStatus() : roomType.getStatus());
		roomType.setAmenities(resolveAmenities(request.getAmenityIds()));

		RoomType saved = roomTypeRepository.save(roomType);
		return toResponse(saved);
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

	private Hotel getCurrentHotel() {
		return hotelRepository.findFirstByOrderByHotelIdAsc()
				.orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND));
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

	private RoomTypeResponse toResponse(RoomType roomType) {
		return RoomTypeResponse.builder()
				.roomTypeId(roomType.getRoomTypeId())
				.code(roomType.getCode())
				.name(roomType.getName())
				.description(roomType.getDescription())
				.capacity(roomType.getCapacity())
				.maxAdults(roomType.getMaxAdults())
				.maxChildren(roomType.getMaxChildren())
				.pricePerNight(roomType.getPricePerNight())
				.weekendPrice(roomType.getWeekendPrice())
				.extraBedPrice(roomType.getExtraBedPrice())
				.bedType(roomType.getBedType() != null ? roomType.getBedType().name() : null)
				.bedCount(roomType.getBedCount())
				.roomSize(roomType.getRoomSize())
				.status(roomType.getStatus() != null ? roomType.getStatus().name() : null)
				.totalRooms(roomType.getTotalRooms())
				.availableRooms(roomType.getAvailableRooms())
				.amenities(mapAmenities(roomType.getAmenities()))
				.images(mapImages(roomType.getImages()))
				.createdAt(roomType.getCreatedAt())
				.updatedAt(roomType.getUpdatedAt())
				.build();
	}

	private List<RoomTypeResponse.AmenityItem> mapAmenities(List<Amenity> amenities) {
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
				.collect(Collectors.toList());
	}

	private List<RoomTypeResponse.ImageItem> mapImages(List<RoomTypeImage> images) {
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
				.collect(Collectors.toList());
	}
}
