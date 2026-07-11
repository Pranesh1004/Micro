package com.genc.arfoms.checkin.dto;

import java.time.LocalDateTime;

/**
 * Boarding pass assembled entirely from live data: the persisted check-in
 * record plus the passenger/flight details fetched from the booking and
 * flight services. Nothing here is hardcoded.
 */
public record BoardingPassView(
        Long checkInId,
        Long bookingId,
        String pnr,
        String passengerName,
        String seatNumber,
        String flightNumber,
        String origin,
        String destination,
        String departureTime,
        String boardingStatus,
        Long boardingSequence,
        LocalDateTime issuedAt) {
}

