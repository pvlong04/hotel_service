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
    RoomTypeRepository roomTypeRepository;
    FloorRepository floorRepository;


    @Override
    @Transactional
    public PageResponse<RoomResponse> getAllRoom() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").descending());
        Page<Room> roomPage = roomRepository.findAllBy(pageable);

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
    public PageResponse<RoomResponse> getRooms(RoomStatus status, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new ApiException(ErrorCode.ILLEGAL_ARGUMENT);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Room> roomPage = status != null
                ? roomRepository.findByStatus(status, pageable)
                : roomRepository.findAllBy(pageable);

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
        String normalizedRoomNumber = normalizeRoomNumber(request.getRoomNumber());

        if (roomRepository.existsByRoomNumber(normalizedRoomNumber)) {
            throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));

        Floor floor = resolveFloor(request.getFloorId());
        Hotel hotel = roomType.getHotel();
        if (hotel == null) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }

        Room room = Room.builder()
                .hotel(hotel)
                .roomNumber(normalizedRoomNumber)
                .roomType(roomType)
                .floor(floor)
                .status(request.getStatus() != null ? request.getStatus() : RoomStatus.AVAILABLE)
                .note(normalizeNote(request.getNote()))
                .build();

        Room saved = roomRepository.save(room);

        // Save image if provided
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            RoomImage image = RoomImage.builder()
                    .room(saved)
                    .url(request.getImageUrl().trim())
                    .caption("Ảnh phòng " + normalizedRoomNumber)
                    .isPrimary(true)
                    .sortOrder(0)
                    .build();
            saved.getImages().add(image);
            saved = roomRepository.save(saved);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);

        Room room = roomRepository.findWithDetailsByRoomId(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));

        String normalizedRoomNumber = normalizeRoomNumber(request.getRoomNumber());

        if (roomRepository.existsByRoomNumberAndRoomIdNot(normalizedRoomNumber, id)) {
            throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));
        Floor floor = resolveFloor(request.getFloorId());
        Hotel hotel = roomType.getHotel();
        if (hotel == null) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }

        room.setHotel(hotel);
        room.setRoomNumber(normalizedRoomNumber);
        room.setRoomType(roomType);
        room.setFloor(floor);
        room.setStatus(request.getStatus() != null ? request.getStatus() : room.getStatus());
        room.setNote(normalizeNote(request.getNote()));

        // Update image if provided
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            // Remove old primary images and add new one
            room.getImages().removeIf(RoomImage::getIsPrimary);
            RoomImage image = RoomImage.builder()
                    .room(room)
                    .url(request.getImageUrl().trim())
                    .caption("Ảnh phòng " + normalizedRoomNumber)
                    .isPrimary(true)
                    .sortOrder(0)
                    .build();
            room.getImages().add(image);
        }

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
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
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

    private String normalizeRoomNumber(String roomNumber) {
        return roomNumber == null ? null : roomNumber.trim();
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Floor resolveFloor(Integer floorId) {
        if (floorId == null) {
            return null;
        }
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new ApiException(ErrorCode.FLOOR_NOT_FOUND));
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

    private RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getRoomId())
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
