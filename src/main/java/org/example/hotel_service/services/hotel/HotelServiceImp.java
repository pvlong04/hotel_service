package org.example.hotel_service.services.hotel;

import org.example.hotel_service.dtos.request.HotelRequest;
import org.example.hotel_service.dtos.request.HotelSearchRequest;
import org.example.hotel_service.dtos.response.HotelSearchResponse;
import org.example.hotel_service.dtos.response.PageResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface HotelServiceImp {
    HotelSearchResponse getCurrentHotel();

    HotelSearchResponse updateCurrentHotel(HotelRequest request, Jwt jwt);

    PageResponse<HotelSearchResponse> searchHotels(HotelSearchRequest request);

    HotelSearchResponse getHotelById(Integer hotelId);

    HotelSearchResponse createHotel(HotelRequest request, Jwt jwt);

    HotelSearchResponse updateHotel(Integer hotelId, HotelRequest request, Jwt jwt);

    void deleteHotel(Integer hotelId, Jwt jwt);
}
