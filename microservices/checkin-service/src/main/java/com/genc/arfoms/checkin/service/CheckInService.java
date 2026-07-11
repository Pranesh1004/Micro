package com.genc.arfoms.checkin.service;

import com.genc.arfoms.checkin.client.BookingClient;
import com.genc.arfoms.checkin.client.FlightClient;
import com.genc.arfoms.checkin.dto.BoardingPassView;
import com.genc.arfoms.checkin.dto.BookingLookupResult;
import com.genc.arfoms.checkin.dto.BookingView;
import com.genc.arfoms.checkin.dto.CheckInDetailsView;
import com.genc.arfoms.checkin.dto.FlightView;
import com.genc.arfoms.checkin.model.CheckIn;
import com.genc.arfoms.checkin.model.CheckInStatus;
import com.genc.arfoms.checkin.repository.CheckInRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.genc.arfoms.checkin.exception.NoDataFoundException;
import com.genc.arfoms.checkin.exception.UserAlreadyExistsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CheckInService {

    private final Logger logger = LoggerFactory.getLogger(CheckInService.class);
    private final CheckInRepository checkInRepository;
    private final BookingClient bookingClient;
    private final FlightClient flightClient;

    public CheckInService(CheckInRepository checkInRepository,
                          BookingClient bookingClient,
                          FlightClient flightClient) {
        this.checkInRepository = checkInRepository;
        this.bookingClient = bookingClient;
        this.flightClient = flightClient;
    }

    /**
     * Looks up a booking (and its flight) live from the owning services so the
     * agent can confirm the real passenger before checking them in. No data is
     * hardcoded here - everything comes from the booking/flight databases.
     */
    public BookingLookupResult lookupBooking(Long bookingId) {
        logger.info("Looking up booking details for Booking ID: {}", bookingId);
        BookingView booking = getBookingSafe(bookingId);
        if (booking == null) {
            logger.warn("Booking lookup failed: Booking ID {} not found", bookingId);
            throw new NoDataFoundException("Booking not found");
        }
        FlightView flight = getFlightSafe(booking.flightId());
        Optional<CheckIn> existing = checkInRepository.findFirstByBookingId(bookingId);
        logger.info("Booking ID {} lookup completed. Passenger: '{}', checked-in: {}", bookingId, booking.passengerName(), existing.isPresent());
        return new BookingLookupResult(
                booking,
                flight,
                existing.isPresent(),
                existing.map(CheckIn::getCheckInId).orElse(null));
    }

    public CheckIn checkInPassenger(CheckIn checkIn) {
        logger.info("Checking in passenger for Booking ID: {}", checkIn.getBookingId());
        BookingView booking = getBookingSafe(checkIn.getBookingId());
        if (booking == null) {
            logger.warn("Check-in failed: Booking ID {} does not exist", checkIn.getBookingId());
            throw new IllegalArgumentException("Booking does not exist");
        }
        if ("CANCELLED".equalsIgnoreCase(booking.bookingStatus())) {
            logger.warn("Check-in failed: Booking ID {} is CANCELLED", checkIn.getBookingId());
            throw new IllegalArgumentException("Cannot check in a cancelled booking");
        }
        if (checkInRepository.existsByBookingId(booking.bookingId())) {
            logger.warn("Check-in failed: Booking ID {} already checked in", checkIn.getBookingId());
            throw new UserAlreadyExistsException("Passenger already checked in for this booking");
        }
        checkIn.setBookingId(booking.bookingId());
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setCheckInStatus(CheckInStatus.CHECKED_IN);
        CheckIn saved = checkInRepository.save(checkIn);
        logger.info("Passenger checked in successfully. Check-in ID: {}", saved.getCheckInId());
        return saved;
    }

    public CheckIn boardPassenger(Long checkInId) {
        logger.info("Boarding passenger for Check-in ID: {}", checkInId);
        CheckIn checkIn = getCheckIn(checkInId);
        if (checkIn.getCheckInStatus() != CheckInStatus.CHECKED_IN) {
            logger.warn("Boarding failed: passenger Check-in ID {} status is: {}", checkInId, checkIn.getCheckInStatus());
            throw new IllegalArgumentException("Only checked-in passengers can be boarded");
        }
        checkIn.setCheckInStatus(CheckInStatus.BOARDED);
        CheckIn saved = checkInRepository.save(checkIn);
        logger.info("Passenger boarded successfully. Check-in ID: {}", checkInId);
        return saved;
    }

    /**
     * Issues a boarding pass for an already checked-in passenger. The pass is
     * built from the persisted check-in plus the live booking + flight details,
     * so every field reflects real data.
     */
    public BoardingPassView issueBoardingPass(Long checkInId) {
        logger.info("Issuing boarding pass for Check-in ID: {}", checkInId);
        CheckIn checkIn = getCheckIn(checkInId);
        if (checkIn.getCheckInStatus() == CheckInStatus.OFFLOADED) {
            logger.warn("Boarding pass issue failed: passenger Check-in ID {} is OFFLOADED", checkInId);
            throw new IllegalArgumentException("Cannot issue a boarding pass for an offloaded passenger");
        }
        BookingView booking = getBookingSafe(checkIn.getBookingId());
        if (booking == null) {
            logger.warn("Boarding pass issue failed: Booking ID {} no longer exists", checkIn.getBookingId());
            throw new IllegalArgumentException("Booking no longer exists");
        }
        FlightView flight = getFlightSafe(booking.flightId());
        logger.info("Successfully generated boarding pass for Check-in ID: {}", checkInId);
        return new BoardingPassView(
                checkIn.getCheckInId(),
                booking.bookingId(),
                booking.pnr(),
                booking.passengerName(),
                booking.seatNumber(),
                flight != null ? flight.flightNumber() : null,
                flight != null ? flight.origin() : null,
                flight != null ? flight.destination() : null,
                flight != null ? flight.departureTime() : null,
                checkIn.getCheckInStatus() != null ? checkIn.getCheckInStatus().name() : null,
                checkIn.getCheckInId(),
                LocalDateTime.now());
    }

    public CheckIn tagBaggage(Long checkInId, int bagCount) {
        logger.info("Tagging {} bags for Check-in ID: {}", bagCount, checkInId);
        CheckIn checkIn = getCheckIn(checkInId);
        checkIn.setBaggageCount(bagCount);
        CheckIn saved = checkInRepository.save(checkIn);
        logger.info("Baggage tagged successfully for Check-in ID: {}", checkInId);
        return saved;
    }

    public CheckIn getCheckIn(Long checkInId) {
        return checkInRepository.findById(checkInId)
                .orElseThrow(() -> {
                    logger.warn("Check-in lookup failed: Check-in ID {} not found", checkInId);
                    return new NoDataFoundException("Check-in not found");
                });
    }

    public List<CheckIn> getAll() {
        logger.info("Fetching all check-ins.");
        return checkInRepository.findAll();
    }

    /**
     * Returns every check-in enriched with the live passenger + flight context
     * so the operations registry shows real names/routes from the database.
     */
    public List<CheckInDetailsView> getAllDetails() {
        logger.info("Fetching all details (enriched registry).");
        return checkInRepository.findAll().stream()
                .map(this::toDetails)
                .toList();
    }

    private CheckInDetailsView toDetails(CheckIn checkIn) {
        BookingView booking = getBookingSafe(checkIn.getBookingId());
        FlightView flight = booking != null ? getFlightSafe(booking.flightId()) : null;
        return new CheckInDetailsView(
                checkIn.getCheckInId(),
                checkIn.getBookingId(),
                booking != null ? booking.pnr() : null,
                booking != null ? booking.passengerName() : null,
                booking != null ? booking.seatNumber() : null,
                flight != null ? flight.flightId() : (booking != null ? booking.flightId() : null),
                flight != null ? flight.flightNumber() : null,
                flight != null ? flight.origin() : null,
                flight != null ? flight.destination() : null,
                checkIn.getCheckInStatus() != null ? checkIn.getCheckInStatus().name() : null,
                checkIn.getBaggageCount(),
                checkIn.getBaggageWeight(),
                checkIn.getCheckInTime());
    }

    private BookingView getBookingSafe(Long bookingId) {
        if (bookingId == null) return null;
        try {
            logger.debug("Calling booking-service Feign client for Booking ID: {}", bookingId);
            return bookingClient.getBooking(bookingId);
        } catch (feign.FeignException.NotFound e) {
            logger.warn("Booking ID {} not found in booking-service", bookingId);
            return null;
        } catch (Exception e) {
            logger.error("Error communicating with booking-service for Booking ID {}: {}", bookingId, e.getMessage());
            return null;
        }
    }

    private FlightView getFlightSafe(Long flightId) {
        if (flightId == null) return null;
        try {
            logger.debug("Calling flight-service Feign client for Flight ID: {}", flightId);
            return flightClient.getFlight(flightId);
        } catch (feign.FeignException.NotFound e) {
            logger.warn("Flight ID {} not found in flight-service", flightId);
            return null;
        } catch (Exception e) {
            logger.error("Error communicating with flight-service for Flight ID {}: {}", flightId, e.getMessage());
            return null;
        }
    }
}

