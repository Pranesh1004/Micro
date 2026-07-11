package com.genc.arfoms.checkin.dto;

/**
 * Read-only projection of a Flight owned by the flight-service.
 * Times are kept as ISO strings to avoid coupling to a date type across
 * service boundaries.
 */
public record FlightView(
        Long flightId,
        String flightNumber,
        String origin,
        String destination,
        String departureTime,
        String arrivalTime) {
}

