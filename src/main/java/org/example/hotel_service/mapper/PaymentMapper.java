package org.example.hotel_service.mapper;

import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.entities.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = AppMapperConfig.class)
public interface PaymentMapper {

    @Mapping(target = "reservationId", source = "reservation.reservationId")
    @Mapping(target = "guestId", source = "guest.userId")
    @Mapping(target = "method", expression = "java(payment.getMethod() != null ? payment.getMethod().name() : null)")
    @Mapping(target = "status", expression = "java(payment.getStatus() != null ? payment.getStatus().name() : null)")
    PaymentResponse toResponse(Payment payment);
}
