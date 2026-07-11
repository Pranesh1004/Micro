package com.genc.arfoms.loyalty.client;

import com.genc.arfoms.loyalty.dto.FlightDistance;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "flight-service")
public interface FlightDistanceClient {

    @GetMapping("/api/flights/{id}/distance")
    FlightDistance distanceForFlight(@PathVariable("id") Long id);
}
