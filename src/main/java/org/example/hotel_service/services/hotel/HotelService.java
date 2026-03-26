package org.example.hotel_service.services.hotel;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.dtos.request.HotelImageRequest;
import org.example.hotel_service.dtos.request.HotelRequest;
import org.example.hotel_service.dtos.request.HotelSearchRequest;
import org.example.hotel_service.dtos.response.HotelSearchResponse;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.entities.Hotel;
import org.example.hotel_service.entities.HotelImage;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.enums.HotelImageType;
import org.example.hotel_service.enums.HotelStatus;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.HotelRepository;
import org.example.hotel_service.repositories.RoomRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
public class HotelService implements HotelServiceImp {
    HotelRepository hotelRepository;
    RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public HotelSearchResponse getCurrentHotel() {
        Hotel hotel = findCurrentHotel();
        return toSearchResponse(hotel, null, null, null);
    }

    @Override
    @Transactional
    public HotelSearchResponse updateCurrentHotel(HotelRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);

        Hotel hotel = findCurrentHotel();
        applyHotelRequest(hotel, request);

        if (request.getImages() != null) {
            hotel.getImages().clear();
            hotel.getImages().addAll(mapRequestImages(request.getImages(), hotel));
        }

        Hotel saved = hotelRepository.save(hotel);
        return toSearchResponse(saved, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        validateSearchRequest(request);

        Hotel hotel = findCurrentHotel();

        LocalDate checkInDate = request.getCheckInDate();
        LocalDate checkOutDate = request.getCheckOutDate();
        Integer availableRoomsOverride = null;
        if (checkInDate != null && checkOutDate != null) {
            availableRoomsOverride = roomRepository
                    .findAvailableRooms(hotel.getHotelId(), checkInDate, checkOutDate)
                    .size();
        }

        HotelSearchResponse response = toSearchResponse(
                hotel,
                request.getLatitude(),
                request.getLongitude(),
                availableRoomsOverride
        );

        boolean matched = matchesLegacySearch(hotel, response, request);
        List<HotelSearchResponse> content = matched ? List.of(response) : List.of();

        return PageResponse.<HotelSearchResponse>builder()
                .content(content)
                .page(0)
                .size(content.size())
                .totalElements(content.size())
                .totalPages(content.isEmpty() ? 0 : 1)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelSearchResponse getHotelById(Integer hotelId) {
        Hotel hotel = findCurrentHotel();
        if (!hotel.getHotelId().equals(hotelId)) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }
        return toSearchResponse(hotel, null, null, null);
    }

    @Override
    @Transactional
    public HotelSearchResponse createHotel(HotelRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);

        if (hotelRepository.findFirstByOrderByHotelIdAsc().isPresent()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        Hotel hotel = Hotel.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .starRating(request.getStarRating())
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone(request.getTimezone())
                .status(request.getStatus() != null ? request.getStatus() : HotelStatus.ACTIVE)
                .build();

        hotel.setImages(mapRequestImages(request.getImages(), hotel));

        Hotel saved = hotelRepository.save(hotel);
        return toSearchResponse(saved, null, null, null);
    }

    @Override
    @Transactional
    public HotelSearchResponse updateHotel(Integer hotelId, HotelRequest request, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);

        Hotel hotel = findCurrentHotel();
        if (!hotel.getHotelId().equals(hotelId)) {
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);
        }
        applyHotelRequest(hotel, request);

        if (request.getImages() != null) {
            hotel.getImages().clear();
            hotel.getImages().addAll(mapRequestImages(request.getImages(), hotel));
        }

        Hotel saved = hotelRepository.save(hotel);
        return toSearchResponse(saved, null, null, null);
    }

    @Override
    @Transactional
    public void deleteHotel(Integer hotelId, Jwt jwt) {
        requireAnyRole(jwt, Roles.ADMIN);
        throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
    }

    private Hotel findCurrentHotel() {
        return hotelRepository.findFirstWithImagesAndRoomTypesByOrderByHotelIdAsc()
                .orElseThrow(() -> new ApiException(ErrorCode.HOTEL_NOT_FOUND));
    }

    private boolean matchesLegacySearch(Hotel hotel, HotelSearchResponse response, HotelSearchRequest request) {
        String keyword = normalizeKeyword(request.getKeyword());
        if (keyword != null) {
            String lowerKeyword = keyword.toLowerCase();
            boolean keywordMatched = (hotel.getName() != null && hotel.getName().toLowerCase().contains(lowerKeyword))
                    || (hotel.getAddress() != null && hotel.getAddress().toLowerCase().contains(lowerKeyword));
            if (!keywordMatched) {
                return false;
            }
        }

        if (hotel.getStatus() != HotelStatus.ACTIVE) {
            return false;
        }

        Integer star = hotel.getStarRating();
        if (request.getMinStarRating() != null && (star == null || star < request.getMinStarRating())) {
            return false;
        }
        if (request.getMaxStarRating() != null && (star == null || star > request.getMaxStarRating())) {
            return false;
        }

        Integer minPrice = response.getMinPrice();
        Integer maxPrice = response.getMaxPrice();
        if (request.getMinPrice() != null && (maxPrice == null || maxPrice < request.getMinPrice())) {
            return false;
        }
        if (request.getMaxPrice() != null && (minPrice == null || minPrice > request.getMaxPrice())) {
            return false;
        }

        return matchesRequestedRoomCount(response, request.getRooms());
    }

    private void applyHotelRequest(Hotel hotel, HotelRequest request) {
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setPhone(request.getPhone());
        hotel.setEmail(request.getEmail());
        hotel.setStarRating(request.getStarRating());
        hotel.setCheckInTime(request.getCheckInTime());
        hotel.setCheckOutTime(request.getCheckOutTime());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setTimezone(request.getTimezone());
        hotel.setStatus(request.getStatus() != null ? request.getStatus() : hotel.getStatus());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }


    private void validateSearchRequest(HotelSearchRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.KEY_VALID);
        }

        if (request.getCheckInDate() != null && request.getCheckOutDate() != null
                && !request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (request.getMinStarRating() != null && request.getMaxStarRating() != null
                && request.getMinStarRating() > request.getMaxStarRating()) {
            throw new ApiException(ErrorCode.KEY_VALID);
        }

        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice() > request.getMaxPrice()) {
            throw new ApiException(ErrorCode.KEY_VALID);
        }
    }

    private boolean matchesRequestedRoomCount(HotelSearchResponse response, Integer requestedRooms) {
        if (requestedRooms == null || requestedRooms <= 0) {
            return true;
        }
        Integer availableRooms = response.getAvailableRooms();
        return availableRooms != null && availableRooms >= requestedRooms;
    }

    private BigDecimal calculateDistanceKm(BigDecimal hotelLat, BigDecimal hotelLng, Double fromLat, Double fromLng) {
        if (hotelLat == null || hotelLng == null || fromLat == null || fromLng == null) {
            return null;
        }

        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(hotelLat.doubleValue() - fromLat);
        double dLng = Math.toRadians(hotelLng.doubleValue() - fromLng);
        double originLat = Math.toRadians(fromLat);
        double targetLat = Math.toRadians(hotelLat.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(originLat) * Math.cos(targetLat)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusKm * c).setScale(2, RoundingMode.HALF_UP);
    }

    private HotelSearchResponse toSearchResponse(Hotel hotel, Double fromLat, Double fromLng, Integer availableRoomsOverride) {
        List<RoomType> roomTypes = hotel.getRoomTypes() == null ? List.of() : hotel.getRoomTypes();

        Long minPriceLong = roomTypes.stream()
                .map(RoomType::getPricePerNight)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(0L);

        Long maxPriceLong = roomTypes.stream()
                .map(RoomType::getPricePerNight)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L);

        int availableRooms = roomTypes.stream()
                .map(RoomType::getAvailableRooms)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

        if (availableRoomsOverride != null) {
            availableRooms = Math.max(availableRoomsOverride, 0);
        }

        List<HotelSearchResponse.ImageItem> images = mapImages(hotel.getImages());
        String primaryImageUrl = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(HotelSearchResponse.ImageItem::getUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).getUrl());

        return HotelSearchResponse.builder()
                .hotelId(hotel.getHotelId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .starRating(hotel.getStarRating())
                .primaryImageUrl(primaryImageUrl)
                .images(images)
                .averageRating(null)
                .reviewCount(0)
                .minPrice(castLongToInteger(minPriceLong))
                .maxPrice(castLongToInteger(maxPriceLong))
                .availableRooms(availableRooms)
                .latitude(hotel.getLatitude())
                .longitude(hotel.getLongitude())
                .distance(calculateDistanceKm(hotel.getLatitude(), hotel.getLongitude(), fromLat, fromLng))
                .build();
    }

    private Integer castLongToInteger(Long value) {
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return value.intValue();
    }

    private List<HotelImage> mapRequestImages(List<HotelImageRequest> requestImages, Hotel hotel) {
        if (requestImages == null || requestImages.isEmpty()) {
            return new ArrayList<>();
        }

        List<HotelImage> mappedImages = new ArrayList<>();
        for (int i = 0; i < requestImages.size(); i++) {
            HotelImageRequest imageRequest = requestImages.get(i);
            if (imageRequest == null || imageRequest.getUrl() == null || imageRequest.getUrl().isBlank()) {
                continue;
            }

            mappedImages.add(HotelImage.builder()
                    .hotel(hotel)
                    .url(imageRequest.getUrl().trim())
                    .caption(imageRequest.getCaption())
                    .type(imageRequest.getType() != null ? imageRequest.getType() : HotelImageType.OTHER)
                    .isPrimary(Boolean.TRUE.equals(imageRequest.getIsPrimary()))
                    .sortOrder(imageRequest.getSortOrder() != null ? imageRequest.getSortOrder() : i)
                    .build());
        }

        normalizePrimaryImage(mappedImages);
        return mappedImages;
    }

    private void normalizePrimaryImage(List<HotelImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        HotelImage firstPrimary = null;
        for (HotelImage image : images) {
            if (Boolean.TRUE.equals(image.getIsPrimary())) {
                if (firstPrimary == null) {
                    firstPrimary = image;
                } else {
                    image.setIsPrimary(false);
                }
            }
        }

        if (firstPrimary == null) {
            images.get(0).setIsPrimary(true);
        }
    }

    private List<HotelSearchResponse.ImageItem> mapImages(List<HotelImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .sorted(Comparator.comparing(HotelImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(img -> HotelSearchResponse.ImageItem.builder()
                        .imageId(img.getImageId())
                        .url(img.getUrl())
                        .caption(img.getCaption())
                        .type(img.getType() != null ? img.getType().name() : null)
                        .isPrimary(img.getIsPrimary())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
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


}
