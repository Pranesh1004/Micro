package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.model.Flight;
import com.genc.arfoms.booking.repository.FlightRepository;
import org.springframework.stereotype.Service;

@Service
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    public FlightServiceImpl(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public Flight getFlight(Long flightId) {
        return flightRepository.findById(flightId).orElse(null);
    }
}
