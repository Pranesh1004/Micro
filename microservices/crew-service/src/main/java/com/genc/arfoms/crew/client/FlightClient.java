package com.genc.arfoms.crew.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@FeignClient(name = "flight-service")
public interface FlightClient {

    @GetMapping("/api/flights/{id}")
    void verifyFlightExists(@PathVariable("id") Long id);

    @GetMapping("/api/flights/{id}")
    FlightView getFlight(@PathVariable("id") Long id);

    public record FlightView(Long flightId, LocalDateTime departureTime, LocalDateTime arrivalTime) {
    }
}
