package com.genc.arfoms.checkin.dto;

/**
 * Combined result returned to the check-in screen when an agent looks up a
 * booking. Carries the live booking + flight details fetched from their owning
 * services plus whether this booking has already been checked in.
 */
public record BookingLookupResult(
        BookingView booking,
        FlightView flight,
        boolean alreadyCheckedIn,
        Long existingCheckInId) {
}

