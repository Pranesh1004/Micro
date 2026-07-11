package com.genc.arfoms.checkin.client;

import com.genc.arfoms.checkin.dto.FlightView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "flight-service")
public interface FlightClient {

    @GetMapping("/api/flights/{id}")
    FlightView getFlight(@PathVariable("id") Long id);
}
