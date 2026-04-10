package org.example.hotel_service.services.room;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.request.RoomRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.RoomResponse;
import org.example.hotel_service.entities.Floor;
import org.example.hotel_service.entities.RoomAvailabilityLog;
import org.example.hotel_service.entities.Room;
import org.example.hotel_service.entities.RoomImage;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.ReservationStatus;
import org.example.hotel_service.enums.RoomStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.FloorRepository;
import org.example.hotel_service.repositories.RoomAvailabilityLogRepository;
import org.example.hotel_service.repositories.ReservationItemRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.example.hotel_service.repositories.RoomTypeRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.example.hotel_service.services.notification.NotificationServiceImp;
import org.example.hotel_service.mapper.RoomMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService implements RoomServiceImp {
    RoomRepository roomRepository;
    RoomTypeRepository roomTypeRepository;
    FloorRepository floorRepository;
    ReservationItemRepository reservationItemRepository;
    RoomAvailabilityLogRepository roomAvailabilityLogRepository;
    UserRepository userRepository;
    NotificationServiceImp notificationService;
    RoomMapper roomMapper;


    @Override
    @Transactional
    public PageResponse<RoomResponse> getAllRoom() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").descending());
        Page<Room> roomPage = roomRepository.findAllBy(pageable);

        return PageResponse.<RoomResponse>builder()
                .content(roomPage.getContent().stream().map(roomMapper::toResponse).collect(Collectors.toList()))
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
                .content(roomPage.getContent().stream().map(roomMapper::toResponse).collect(Collectors.toList()))
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
        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        String normalizedRoomNumber = normalizeRoomNumber(request.getRoomNumber());

        if (roomRepository.existsByRoomNumber(normalizedRoomNumber)) {
            throw new ApiException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_TYPE_NOT_FOUND));

        Floor floor = resolveFloor(request.getFloorId());

        Room room = Room.builder()
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

        notifyHierarchyForRoomAction(jwt, "tao", saved);

        return roomMapper.toResponse(saved);
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

        room.setRoomNumber(normalizedRoomNumber);
        room.setRoomType(roomType);
        room.setFloor(floor);
        RoomStatus oldStatus = room.getStatus();
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
        if (oldStatus != saved.getStatus()) {
            User actor = resolveActor(jwt);
            roomAvailabilityLogRepository.save(RoomAvailabilityLog.builder()
                    .room(saved)
                    .oldStatus(oldStatus)
                    .newStatus(saved.getStatus())
                    .reason("Cap nhat trang thai phong")
                    .changedBy(actor)
                    .build());
        }
        notifyHierarchyForRoomAction(jwt, "cap nhat", saved);
        return roomMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        roomRepository.delete(room);
        notifyHierarchyForRoomAction(jwt, "xoa", room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }
        List<Room> rooms = roomRepository.findAvailableRooms(checkIn, checkOut);
        return rooms.stream().map(roomMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getHotRooms(LocalDate checkIn, LocalDate checkOut, int limit) {
        if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }

        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Room> availableRooms = roomRepository.findAvailableRooms(checkIn, checkOut);
        if (availableRooms.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = availableRooms.stream().map(Room::getRoomId).toList();
        List<Object[]> bookingCounts = reservationItemRepository.countBookingsByRoomIds(
                roomIds,
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT)
        );

        Map<Long, Long> countByRoomId = bookingCounts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Room> roomById = availableRooms.stream()
                .collect(Collectors.toMap(Room::getRoomId, Function.identity()));

        List<Room> rankedRooms = roomIds.stream()
                .map(roomById::get)
                .sorted(Comparator
                        .comparingLong((Room room) -> countByRoomId.getOrDefault(room.getRoomId(), 0L)).reversed()
                        .thenComparing(Room::getRoomId))
                .limit(safeLimit)
                .toList();

        return rankedRooms.stream().map(roomMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse.ImageItem> getRoomImages(Long id) {
        Room room = roomRepository.findWithDetailsByRoomId(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        return roomMapper.mapImages(room.getImages());
    }

    @Override
    @Transactional
    public RoomResponse.ImageItem addRoomImage(Long roomId, String url, String caption, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        Room room = roomRepository.findWithDetailsByRoomId(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));

        int nextSort = room.getImages().stream()
                .mapToInt(img -> img.getSortOrder() != null ? img.getSortOrder() : 0)
                .max()
                .orElse(-1) + 1;

        boolean hasPrimary = room.getImages().stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));

        RoomImage image = RoomImage.builder()
                .room(room)
                .url(url.trim())
                .caption(caption != null ? caption.trim() : "Ảnh phòng " + room.getRoomNumber())
                .isPrimary(!hasPrimary) // First image becomes primary
                .sortOrder(nextSort)
                .build();
        room.getImages().add(image);
        roomRepository.save(room);

        return roomMapper.toImageItem(image);
    }

    @Override
    @Transactional
    public void deleteRoomImage(Long roomId, Long imageId, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        Room room = roomRepository.findWithDetailsByRoomId(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));

        RoomImage toRemove = room.getImages().stream()
                .filter(img -> img.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        boolean wasPrimary = Boolean.TRUE.equals(toRemove.getIsPrimary());
        room.getImages().remove(toRemove);

        // If deleted the primary, assign next image as primary
        if (wasPrimary && !room.getImages().isEmpty()) {
            room.getImages().get(0).setIsPrimary(true);
        }

        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void setPrimaryImage(Long roomId, Long imageId, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN, Roles.STAFF);
        Room room = roomRepository.findWithDetailsByRoomId(roomId)
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));

        boolean found = false;
        for (RoomImage img : room.getImages()) {
            if (img.getImageId().equals(imageId)) {
                img.setIsPrimary(true);
                found = true;
            } else {
                img.setIsPrimary(false);
            }
        }

        if (!found) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        roomRepository.save(room);
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

    private Roles resolveActorRole(Jwt jwt) {
        Set<String> roles = extractRoles(jwt);
        if (roles.contains(Roles.ADMIN.name())) {
            return Roles.ADMIN;
        }
        if (roles.contains(Roles.STAFF.name())) {
            return Roles.STAFF;
        }
        return Roles.GUEST;
    }

    private User resolveActor(Jwt jwt) {
        UUID actorId = extractUserId(jwt);
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).orElse(null);
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim instanceof UUID id) {
            return id;
        }
        if (userIdClaim instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
        }
        return null;
    }

    private void notifyHierarchyForRoomAction(Jwt jwt, String action, Room room) {
        try {
            UUID actorId = extractUserId(jwt);
            if (actorId == null) {
                return;
            }

            userRepository.findById(actorId).ifPresent(actor -> notificationService.notifyHierarchy(
                    actor,
                    resolveActorRole(jwt),
                    action,
                    "phong",
                    room.getRoomId(),
                    room.getRoomNumber()
            ));
        } catch (Exception ex) {
            log.warn("Hierarchy room notification failed: {}", ex.getMessage());
        }
    }
}
