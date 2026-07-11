package com.genc.arfoms.flight.dto;

/**
 * Response payload describing the great-circle distance (in miles) between the
 * origin and destination airports of a flight.
 */
public record FlightDistanceResponse(
        Long flightId,
        String flightNumber,
        String origin,
        String destination,
        double distanceMiles) {
}

