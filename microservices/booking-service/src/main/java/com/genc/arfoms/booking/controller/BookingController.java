package com.genc.arfoms.booking.controller;

import com.genc.arfoms.booking.model.Booking;
import com.genc.arfoms.booking.model.Flight;
import com.genc.arfoms.booking.model.Passenger;
import com.genc.arfoms.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** List all available flights (browser-friendly GET, e.g. /flights). */
    @GetMapping("/flights")
    public List<Flight> listFlights() {
        log.info("GET /flights - listing all available flights");
        return bookingService.searchAvailableFlights(new Booking());
    }

    /** Search available flights for the given criteria. */
    @PostMapping("/flights/search")
    public List<Flight> searchFlights(@RequestBody Booking searchCriteria) {
        log.info("POST /flights/search - from={}, to={}, date={}, type={}",
                searchCriteria.getFromLocation(), searchCriteria.getToLocation(),
                searchCriteria.getDepartureDate(), searchCriteria.getFlightType());
        return bookingService.searchAvailableFlights(searchCriteria);
    }

    /** Prepare a booking draft (ensures passenger slots) for the passenger-details page. */
    @PostMapping("/flights/passenger")
    public Booking prepareDraft(@RequestBody Booking booking) {
        log.info("POST /flights/passenger - preparing booking draft");
        return bookingService.prepareBookingDraft(booking);
    }

    /** Fetch passenger details by their unique ID. */
    @GetMapping("/flights/passenger/{passengerId}")
    public Passenger getPassenger(@PathVariable Long passengerId) {
        log.info("GET /flights/passenger/{} - fetching passenger details", passengerId);
        return bookingService.getPassengerById(passengerId);
    }

    /** Check passenger uniqueness constraints on a target flight. */
    @PostMapping("/flights/passenger/validate")
    public Map<String, Object> validatePassengers(@RequestBody Booking booking) {
        log.info("POST /flights/passenger/validate - checking passenger uniqueness on flight {}",
                booking.getFlightId());
        bookingService.validateNoDuplicatePassengerOnFlight(booking);
        return Map.of("ok", true);
    }

    /** Persist a new booking and return the saved record (including generated id and PNR). */
    @PostMapping("/flights/passenger/confirm")
    public Booking createBooking(@RequestBody Booking booking) {
        log.info("POST /flights/passenger/confirm - creating new booking");
        return bookingService.createBooking(booking);
    }

    /** Fetch a single booking by id for the confirmation page. */
    @GetMapping("/api/bookings/confirmation/{bookingId}")
    public Booking getConfirmation(@PathVariable Long bookingId) {
        log.info("GET /api/bookings/confirmation/{} - fetching booking confirmation", bookingId);
        return bookingService.getBookingById(bookingId);
    }

    /** Fetch the most recent booking for the manage-booking dashboard. */
    @GetMapping("/api/bookings/manage")
    public Booking manage() {
        log.info("GET /api/bookings/manage - fetching most recent booking");
        return bookingService.getBookingDetails();
    }

    /** Fetch all past bookings (most recent first) for the manage-booking dashboard. */
    @GetMapping("/api/bookings")
    public List<Booking> allBookings() {
        log.info("GET /api/bookings - fetching all bookings");
        return bookingService.getAllBookings();
    }

    /** Modify an existing booking (date / seat / etc.). */
    @PostMapping("/api/bookings/{bookingId}/modify")
    public Booking modify(@PathVariable Long bookingId, @RequestBody Booking updatedBooking) {
        log.info("POST /api/bookings/{}/modify - modifying booking", bookingId);
        return bookingService.modifyBooking(bookingId, updatedBooking);
    }

    /** Cancel an existing booking. */
    @PostMapping("/api/bookings/{bookingId}/cancel")
    public Booking cancel(@PathVariable Long bookingId) {
        log.info("POST /api/bookings/{}/cancel - cancelling booking", bookingId);
        return bookingService.cancelBooking(bookingId);
    }

    /** Exception handler for catching conflicting validation states. */

}