package org.example.hotel_service.services.amenity;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.request.AmenityRequest;
import org.example.hotel_service.dtos.response.AmenityResponse;
import org.example.hotel_service.entities.Amenity;
import org.example.hotel_service.enums.AmenityCategory;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.repositories.AmenityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityService implements AmenityServiceImp {

    AmenityRepository amenityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getAmenitiesByCategory(AmenityCategory category) {
        return amenityRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AmenityResponse getAmenityById(Integer id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.AMENITY_NOT_FOUND));
        return toResponse(amenity);
    }

    @Override
    @Transactional
    public AmenityResponse createAmenity(AmenityRequest request) {
        Amenity amenity = Amenity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .category(request.getCategory() != null ? request.getCategory() : AmenityCategory.ROOM)
                .build();
        Amenity saved = amenityRepository.save(amenity);
        log.info("Created amenity: id={}, name={}", saved.getAmenityId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AmenityResponse updateAmenity(Integer id, AmenityRequest request) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.AMENITY_NOT_FOUND));

        amenity.setName(request.getName());
        amenity.setDescription(request.getDescription());
        amenity.setIcon(request.getIcon());
        if (request.getCategory() != null) {
            amenity.setCategory(request.getCategory());
        }

        Amenity saved = amenityRepository.save(amenity);
        log.info("Updated amenity: id={}, name={}", saved.getAmenityId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAmenity(Integer id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.AMENITY_NOT_FOUND));
        amenityRepository.delete(amenity);
        log.info("Deleted amenity: id={}, name={}", amenity.getAmenityId(), amenity.getName());
    }

    private AmenityResponse toResponse(Amenity amenity) {
        return AmenityResponse.builder()
                .amenityId(amenity.getAmenityId())
                .name(amenity.getName())
                .description(amenity.getDescription())
                .icon(amenity.getIcon())
                .category(amenity.getCategory() != null ? amenity.getCategory().name() : null)
                .createdAt(amenity.getCreatedAt())
                .build();
    }
}
