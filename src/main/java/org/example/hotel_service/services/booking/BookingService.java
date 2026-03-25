package org.example.hotel_service.services.booking;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.request.BookingRequest;
import org.example.hotel_service.dtos.response.BookingResponse;
import org.example.hotel_service.repositories.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class BookingService implements BookingServiceImp {
    BookingRepository repo;

    @Override
    public BookingResponse previewBooking(BookingRequest request) {
        return BookingResponse.builder()
                .bookingId(null)
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .roomIds(request.getRoomIds() != null ? request.getRoomIds() : List.of())
                .build();
    }
}
