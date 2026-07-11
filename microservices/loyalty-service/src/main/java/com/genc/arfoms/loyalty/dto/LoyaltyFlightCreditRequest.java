package com.genc.arfoms.loyalty.dto;

/**
 * Request payload for crediting loyalty miles after a completed flight.
 * Self-contained: the distance is supplied by the caller so the loyalty
 * service stays decoupled from the Booking/Flight modules.
 */
public record LoyaltyFlightCreditRequest(Long memberId, Long bookingId, String passengerName, double distanceMiles) {
}

