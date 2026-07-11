package com.genc.arfoms.checkin.dto;

import java.math.BigDecimal;

/**
 * Read-only projection of a Booking owned by the booking-service.
 * Populated by {@code BookingClient} via a server-to-server call so the
 * check-in module never has to hardcode passenger data.
 */
public record BookingView(
        Long bookingId,
        String pnr,
        Long flightId,
        String passengerName,
        String seatNumber,
        BigDecimal fareAmount,
        String bookingStatus) {
}

