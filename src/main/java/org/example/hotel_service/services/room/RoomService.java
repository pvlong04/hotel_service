package org.example.hotel_service.services.room;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.request.RoomRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.entities.Floor;
import org.example.hotel_service.entities.Hotel;
import org.example.hotel_service.entities.Room;
import org.example.hotel_service.entities.RoomImage;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.RoomStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.FloorRepository;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService implements RoomServiceImp {
    RoomRepository roomRepository;
    HotelRepository hotelRepository;
    RoomTypeRepository roomTypeRepository;
    FloorRepository floorRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getRooms(Integer hotelId, RoomStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Room> roomPage;

        if (hotelId != null && status != null) {
            roomPage = roomRepository.findByHotel_HotelIdAndStatus(hotelId, status, pageable);
        } else if (hotelId != null) {
            roomPage = roomRepository.findByHotel_HotelId(hotelId, pageable);
        } else {
            roomPage = roomRepository.findAll(pageable);
        }

        return PageResponse.<RoomResponse>builder()
                .content(roomPage.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(roomPage.getNumber())
                .size(roomPage.getSize())
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .first(roomPage.isFirst())
                .last(roomPage.isLast())
                .hasNext(roomPage.hasNext())
                .hasPrevious(roomPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findWithDetailsByRoomId(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        return toResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND));

        if (roomRepository.existsByHotel_HotelIdAndRoomNumber(hotel.getHotelId(), request.getRoomNumber())) {
            throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));

        validateRoomTypeBelongsToHotel(roomType, hotel.getHotelId());

        Floor floor = resolveFloor(request.getFloorId(), hotel.getHotelId());

        Room room = Room.builder()
                .hotel(hotel)
                .roomNumber(request.getRoomNumber())
                .roomType(roomType)
                .floor(floor)
                .status(request.getStatus() != null ? request.getStatus() : RoomStatus.AVAILABLE)
                .note(request.getNote())
                .build();

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

        Room room = roomRepository.findWithDetailsByRoomId(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND));

        if (roomRepository.existsByHotel_HotelIdAndRoomNumberAndRoomIdNot(hotel.getHotelId(), request.getRoomNumber(), id)) {
            throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        validateRoomTypeBelongsToHotel(roomType, hotel.getHotelId());

        Floor floor = resolveFloor(request.getFloorId(), hotel.getHotelId());

        room.setHotel(hotel);
        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(roomType);
        room.setFloor(floor);
        room.setStatus(request.getStatus() != null ? request.getStatus() : room.getStatus());
        room.setNote(request.getNote());

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(Integer hotelId, LocalDate checkIn, LocalDate checkOut) {
        if (hotelId == null || checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }

        List<Room> rooms = roomRepository.findAvailableRooms(hotelId, checkIn, checkOut);
        return rooms.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse.ImageItem> getRoomImages(Long id) {
        Room room = roomRepository.findWithDetailsByRoomId(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        return mapImages(room.getImages());
    }

    private Floor resolveFloor(Integer floorId, Integer hotelId) {
        if (floorId == null) {
            return null;
        }
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ApiException(ErrorCode.FLOOR_NOT_FOUND));
        if (floor.getHotel() == null || !hotelId.equals(floor.getHotel().getHotelId())) {
            throw new ApiException(ErrorCode.FLOOR_NOT_FOUND);
        }
        return floor;
    }

    private void validateRoomTypeBelongsToHotel(RoomType roomType, Integer hotelId) {
        if (roomType.getHotel() == null || !hotelId.equals(roomType.getHotel().getHotelId())) {
            throw new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND);
        }
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof Iterable<?> iterable) {
            Set<String> roles = new HashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            if (!roles.isEmpty()) {
                return roles;
            }
        }

        Object roleObj = jwt.getClaims().get("role");
        if (roleObj != null) {
            return Set.of(roleObj.toString());
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

    private RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .hotelId(room.getHotel() != null ? room.getHotel().getHotelId() : null)
                .hotelName(room.getHotel() != null ? room.getHotel().getName() : null)
                .roomNumber(room.getRoomNumber())
                .roomTypeId(room.getRoomType() != null ? room.getRoomType().getRoomTypeId() : null)
                .roomTypeCode(room.getRoomType() != null ? room.getRoomType().getCode() : null)
                .roomTypeName(room.getRoomType() != null ? room.getRoomType().getName() : null)
                .floorId(room.getFloor() != null ? room.getFloor().getFloorId() : null)
                .floorCode(room.getFloor() != null ? room.getFloor().getCode() : null)
                .floorName(room.getFloor() != null ? room.getFloor().getName() : null)
                .status(room.getStatus() != null ? room.getStatus().name() : null)
                .note(room.getNote())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .images(mapImages(room.getImages()))
                .build();
    }

    private List<RoomResponse.ImageItem> mapImages(List<RoomImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .sorted(Comparator.comparing(RoomImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(img -> RoomResponse.ImageItem.builder()
                        .imageId(img.getImageId())
                        .url(img.getUrl())
                        .caption(img.getCaption())
                        .isPrimary(img.getIsPrimary())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }
}
