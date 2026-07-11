package com.genc.arfoms.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "flight-service")
public interface FlightClient {

    @GetMapping("/api/flights/{id}")
    void verifyFlightExists(@PathVariable("id") Long id);
}
