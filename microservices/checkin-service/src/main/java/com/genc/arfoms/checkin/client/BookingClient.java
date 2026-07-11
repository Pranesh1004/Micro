package com.genc.arfoms.checkin.client;

import com.genc.arfoms.checkin.dto.BookingView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service")
public interface BookingClient {

    @GetMapping("/api/bookings/{id}")
    BookingView getBooking(@PathVariable("id") Long id);
}
