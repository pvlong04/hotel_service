package org.example.hotel_service.services.amenity;

import org.example.hotel_service.dtos.request.AmenityRequest;
import org.example.hotel_service.dtos.response.AmenityResponse;
import org.example.hotel_service.enums.AmenityCategory;

import java.util.List;

public interface AmenityServiceImp {

    List<AmenityResponse> getAllAmenities();

    List<AmenityResponse> getAmenitiesByCategory(AmenityCategory category);

    AmenityResponse getAmenityById(Integer id);

    AmenityResponse createAmenity(AmenityRequest request);

    AmenityResponse updateAmenity(Integer id, AmenityRequest request);

    void deleteAmenity(Integer id);
}
