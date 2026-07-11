package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.dto.BookingResult;
import com.genc.arfoms.booking.model.Flight;
import com.genc.arfoms.booking.model.Passenger;
import com.genc.arfoms.booking.model.Payment;
import com.genc.arfoms.booking.model.SeatInventory;
import com.genc.arfoms.booking.model.SeatStatus;
import com.genc.arfoms.booking.repository.PaymentRepository;
import com.genc.arfoms.booking.repository.SeatInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class SeatPaymentService {

    private static final int BASE_FARE = 18500;
    private static final int TAXES = 1250;
    private static final int SAVINGS = 950;
    private static final int WINDOW_CHARGE = 700;
    private static final int AISLE_CHARGE = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter STORED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SeatInventoryRepository seatInventoryRepository;
    private final PaymentRepository paymentRepository;
    private final FlightService flightService;
    private final PassengerService passengerService;
    private final MockPaymentService mockPaymentService;

    @Autowired
    public SeatPaymentService(SeatInventoryRepository seatInventoryRepository,
                              PaymentRepository paymentRepository,
                              FlightService flightService,
                              PassengerService passengerService,
                              MockPaymentService mockPaymentService) {
        this.seatInventoryRepository = seatInventoryRepository;
        this.paymentRepository = paymentRepository;
        this.flightService = flightService;
        this.passengerService = passengerService;
        this.mockPaymentService = mockPaymentService;
    }

    public List<SeatInventory> getSeatsByFlight(Long flightId) {
        return seatInventoryRepository.findByFlightIdOrderBySeatNumberAsc(toInt(flightId));
    }

    public List<SeatInventory> getAvailableSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.AVAILABLE);
    }

    public List<SeatInventory> getBookedSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.BOOKED);
    }

    public long countAvailableSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.AVAILABLE).size();
    }

    public long countBookedSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.BOOKED).size();
    }

    public boolean isSeatBooked(Long flightId, String seatNumber) {
        return seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber)
                .map(seat -> seat.getSeatStatus() == SeatStatus.BOOKED)
                .orElse(false);
    }

    @Transactional
    public boolean confirmSeatSelection(Long flightId, String seatNumber) {
        Optional<SeatInventory> seatOpt =
                seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber);
        if (seatOpt.isPresent()) {
            SeatInventory seat = seatOpt.get();
            if (seat.getSeatStatus() == SeatStatus.AVAILABLE) {
                seat.setSeatStatus(SeatStatus.BOOKED);
                seatInventoryRepository.save(seat);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void resetSeatSelection(Long flightId, String seatNumber) {
        Optional<SeatInventory> seatOpt =
                seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber);
        if (seatOpt.isPresent()) {
            SeatInventory seat = seatOpt.get();
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatInventoryRepository.save(seat);
        }
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(int id) {
        return paymentRepository.findById(id);
    }

    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findByFlightIdAndSeatNumber(Integer flightId, String seatNumber) {
        return paymentRepository.findByFlightIdAndSeatNumber(flightId, seatNumber);
    }

    public void deletePayment(int id) {
        paymentRepository.deleteById(id);
    }

    @Transactional
    public BookingResult confirmBooking(Long flightId, String seatNumber) {
        if (flightId == null || seatNumber == null || seatNumber.isBlank()) {
            return BookingResult.failure("Missing flight or seat information.");
        }

        // 1. Break the session relationship reference tracker causing the Transient Exception error
        Passenger passenger = passengerService.getPassenger();
        if (passenger != null) {
            passenger.setBooking(null);
        }

        boolean justBooked = confirmSeatSelection(flightId, seatNumber);

        if (!justBooked) {
            Payment existing = findByFlightIdAndSeatNumber(flightId.intValue(), seatNumber).orElse(null);
            if (existing != null) {
                return BookingResult.success(existing.getBookingId(), existing.getSeatNumber(),
                        existing.getTotalAmount());
            }
            return BookingResult.failure("Sorry, this seat is no longer available. Please choose another seat.");
        }

        // 2. Build payment tracking layout and assign the generated Mock PNR
        Payment bookingDetails = buildBooking(flightId, seatNumber);

        // Save to DB
        Payment saved = savePayment(bookingDetails);
        return BookingResult.success(saved.getBookingId(), saved.getSeatNumber(), saved.getTotalAmount());
    }

    public Payment buildBooking(Long flightId, String seatNumber) {
        int seatCharges = calculateSeatCharge(seatNumber);
        int totalAmount = BASE_FARE + seatCharges + TAXES;

        Flight flight = flightService.getFlight(flightId);
        Passenger passenger = passengerService.getPassenger();

        // Generate Mock PNR using the service
        String mockPnr = mockPaymentService.generatePnr();

        Payment booking = new Payment();
        booking.setBaseFare(String.valueOf(BASE_FARE));
        booking.setSeatCharges(String.valueOf(seatCharges));
        booking.setTaxes(String.valueOf(TAXES));
        booking.setTotalAmount(String.valueOf(totalAmount));
        booking.setSavings(String.valueOf(SAVINGS));
        booking.setSeatNumber(seatNumber);

        if (flight != null) {
            booking.setFlight(flight);
        }

        if (flight != null) {
            booking.setFlightNumber(flight.getFlightNumber());
            booking.setSource(flight.getOrigin());
            booking.setDestination(flight.getDestination());
            booking.setDepartureTime(formatStored(flight.getDepartureTime()));
            booking.setArrivalTime(formatStored(flight.getArrivalTime()));
        }

        if (passenger != null) {
            // Append the PNR string directly into the passenger metadata string
            // so the confirmation page front-end display can read it smoothly!
            booking.setPassengerName(passenger.getName() + " (PNR: " + mockPnr + ")");
        }
        return booking;
    }

    private static String formatStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return "-";
        }
        try {
            return LocalDateTime.parse(stored, STORED_FMT).format(TIME_FMT);
        } catch (DateTimeParseException ex) {
            return stored;
        }
    }

    private int calculateSeatCharge(String seatNumber) {
        if (seatNumber == null) {
            return 0;
        }
        try {
            int col = Integer.parseInt(seatNumber.substring(1));
            switch (col) {
                case 1:
                case 4:
                    return WINDOW_CHARGE;
                case 2:
                case 3:
                    return AISLE_CHARGE;
                default:
                    return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Integer toInt(Long flightId) {
        return flightId == null ? null : flightId.intValue();
    }
}