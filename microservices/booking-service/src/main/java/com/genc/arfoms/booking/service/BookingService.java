package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.model.Booking;
import com.genc.arfoms.booking.model.Flight;
import com.genc.arfoms.booking.model.Passenger;

import java.util.List;

public interface BookingService {

    List<Flight> searchAvailableFlights(Booking searchCriteria);

    Booking prepareBookingDraft(Booking booking);


    Booking createBooking(Booking booking);

    /**
     * Verifies that none of the booking's passengers already hold an active seat
     * on the same flight. Throws IllegalStateException (with a user-facing message)
     * when a duplicate is found. Used to validate early on the passenger page.
     */
    void validateNoDuplicatePassengerOnFlight(Booking booking);

    Booking selectSeat(Long bookingId, String seat);

    Booking modifyBooking(Long bookingId, Booking updatedBooking);

    Booking cancelBooking(Long bookingId);

    Booking getBookingDetails();

    Booking getBookingById(Long bookingId);

    List<Booking> getAllBookings();

    Passenger getPassengerById(Long passengerId);
}
