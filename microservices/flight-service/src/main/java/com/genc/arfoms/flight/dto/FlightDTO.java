package com.genc.arfoms.flight.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object for {@link com.genc.arfoms.model.Flight}.
 * Carries flight details (including the resolved airline name) to clients.
 */
@Data
public class FlightDTO {

    private Integer flightId;
    private Integer airlineId;
    private String flightNumber;
    private String origin;
    private String destination;
    private String flightStatus;
    private LocalDate departureDate;
    private LocalTime departureTimeOnly;
    private LocalDate arrivalDate;
    private LocalTime arrivalTimeOnly;
    private String airlineName;
}

