package com.genc.arfoms.booking.controller;

import com.genc.arfoms.booking.dto.BookingResult;
import com.genc.arfoms.booking.model.Flight;
import com.genc.arfoms.booking.model.Passenger;
import com.genc.arfoms.booking.model.Payment;
import com.genc.arfoms.booking.model.SeatInventory;
import com.genc.arfoms.booking.model.SeatStatus;
import com.genc.arfoms.booking.service.FlightService;
import com.genc.arfoms.booking.service.PassengerService;
import com.genc.arfoms.booking.service.SeatPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SeatPaymentController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter STORED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";

    @Autowired
    private SeatPaymentService seatPaymentService;

    @Autowired
    private FlightService flightService;

    @Autowired
    private PassengerService passengerService;


    @GetMapping("/airline/api/seats")
    public Map<String, Object> getSeatData(@RequestParam(defaultValue = "1") Long flightId) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeatPaymentController.class);
        
        log.info("getSeatData: Fetching seats for flightId={}", flightId);

        List<SeatInventory> allSeats = seatPaymentService.getSeatsByFlight(flightId);
        log.info("getSeatData: Found {} total seats", allSeats.size());

        List<String> bookedSeats = allSeats.stream()
                .filter(s -> s.getSeatStatus() == SeatStatus.BOOKED)
                .map(SeatInventory::getSeatNumber)
                .toList();
        log.info("getSeatData: Found {} booked seats: {}", bookedSeats.size(), bookedSeats);

        Flight flight = flightService.getFlight(flightId);
        Passenger passenger = passengerService.getPassenger();

        Map<String, Object> response = new HashMap<>();
        response.put("flightId", flightId);
        response.put("allSeats", allSeats);
        response.put("bookedSeatNumbers", bookedSeats);
        response.put("flight", flight);
        response.put("passenger", passenger);

        if (flight != null) {
            response.put("departureTime", formatStored(flight.getDepartureTime(), TIME_FMT));
            response.put("arrivalTime", formatStored(flight.getArrivalTime(), TIME_FMT));
            response.put("flightDate", formatStored(flight.getDepartureTime(), DATE_FMT));
            
            // Include seat layout configuration from flight
            response.put("seatRows", flight.getSeatRows());
            response.put("seatColumns", flight.getSeatColumns());
            response.put("seatAisleAfter", flight.getSeatAisleAfter());
            response.put("seatCount", flight.getSeatCount());
        }

        return response;
    }


    private static String formatStored(String stored, DateTimeFormatter target) {
        if (stored == null || stored.isBlank()) {
            return "-";
        }
        try {
            return LocalDateTime.parse(stored, STORED_FMT).format(target);
        } catch (DateTimeParseException ex) {
            return stored;
        }
    }



    @GetMapping("/api/payment")
    public Payment getBooking(
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) String seatNumber) {
        return seatPaymentService.buildBooking(flightId, seatNumber);
    }

    @PostMapping("/booking/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) String seatNumber) {

        BookingResult result = seatPaymentService.confirmBooking(flightId, seatNumber);

        Map<String, Object> response = new HashMap<>();
        response.put(KEY_SUCCESS, result.isSuccess());
        response.put(KEY_MESSAGE, result.getMessage());

        if (result.isSuccess()) {
            response.put("bookingId", result.getBookingId());
            response.put("seatNumber", result.getSeatNumber());
            response.put("totalAmount", result.getTotalAmount());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}

