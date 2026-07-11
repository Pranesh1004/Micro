package com.genc.arfoms.checkin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Enriched check-in row for the operations registry. Combines the persisted
 * check-in record with passenger + flight context fetched live from the
 * booking and flight services, so the UI never hardcodes those values.
 */
public record CheckInDetailsView(
        Long checkInId,
        Long bookingId,
        String pnr,
        String passengerName,
        String seatNumber,
        Long flightId,
        String flightNumber,
        String origin,
        String destination,
        String checkInStatus,
        Integer baggageCount,
        BigDecimal baggageWeight,
        LocalDateTime checkInTime) {
}

