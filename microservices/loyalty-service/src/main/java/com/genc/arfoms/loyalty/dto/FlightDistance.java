package com.genc.arfoms.loyalty.dto;

/**
 * Mirror of the flight-service distance payload, used to deserialize the
 * response when the loyalty service asks flight-service for a flight's distance.
 */
public record FlightDistance(
        Long flightId,
        String flightNumber,
        String origin,
        String destination,
        double distanceMiles) {
}

