package org.example.hotel_service.services.booking;

import org.example.hotel_service.dtos.request.BookingRequest;
import org.example.hotel_service.dtos.response.BookingResponse;

public interface BookingServiceImp{

    BookingResponse previewBooking(BookingRequest request);
}
