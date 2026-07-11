package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.model.*;
import com.genc.arfoms.booking.repository.*;
import com.genc.arfoms.booking.model.SeatStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final String BOOKING_NOT_FOUND = "Booking not found for id: ";
    private static final int MAX_PASSENGERS = 9;
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private static final Set<String> INTERNATIONAL_AIRPORT_CODES = buildInternationalAirportCodes();

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final PassengerRepository passengerRepository;
    private final MockPaymentService mockPaymentService;

    public BookingServiceImpl(BookingRepository bookingRepository, FlightRepository flightRepository,
                              SeatInventoryRepository seatInventoryRepository,
                              PassengerRepository passengerRepository,
                              MockPaymentService mockPaymentService) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.passengerRepository = passengerRepository;
        this.mockPaymentService = mockPaymentService;
    }

    @Override
    public List<Flight> searchAvailableFlights(Booking searchCriteria) {
        String fromCanonical = canonicalLocation(searchCriteria.getFromLocation());
        String toCanonical = canonicalLocation(searchCriteria.getToLocation());
        LocalDate departureDate = parseDate(searchCriteria.getDepartureDate());
        LocalDate todayIst = LocalDate.now(IST_ZONE);

        List<Flight> flights = flightRepository.findAll().stream()
                .filter(f -> matchesLocation(f.getOrigin(), fromCanonical))
                .filter(f -> matchesLocation(f.getDestination(), toCanonical))
                .filter(f -> {
                    LocalDate dep = extractDate(f.getDepartureTime());
                    if (departureDate != null) {
                        return dep != null && dep.equals(departureDate);
                    }
                    return dep == null || !dep.isBefore(todayIst);
                })
                .collect(Collectors.toList());

        flights = new ArrayList<>(flights.stream()
                .collect(Collectors.toMap(
                        Flight::getFlightNumber, f -> f,
                        (a, b) -> a.getFlightId() <= b.getFlightId() ? a : b,
                        LinkedHashMap::new
                )).values()
        );

        String flightType = searchCriteria.getFlightType();
        if (flightType != null && !flightType.isBlank()) {
            boolean wantInternational = "international".equalsIgnoreCase(flightType);
            flights = flights.stream()
                    .filter(f -> isInternationalFlight(f) == wantInternational)
                    .collect(Collectors.toList());
        }

        Map<Integer, String> airlineMap = Collections.emptyMap();
        final Map<Integer, String> resolvedAirlineMap = airlineMap;
        flights.forEach(f -> f.setAirlineName(resolveAirlineName(f, resolvedAirlineMap)));
        flights.forEach(this::attachAvailableSeats);
        return flights;
    }

    private void attachAvailableSeats(Flight flight) {
        try {
            Integer fid = flight.getFlightId() != null ? flight.getFlightId().intValue() : null;
            if (fid != null) {
                int available = seatInventoryRepository.findByFlightIdAndSeatStatus(fid, SeatStatus.AVAILABLE).size();
                if (available > 0) {
                    flight.setAvailableSeats(available);
                    return;
                }
                boolean hasInventory = !seatInventoryRepository.findByFlightId(fid).isEmpty();
                if (hasInventory) {
                    flight.setAvailableSeats(0);
                    return;
                }
            }
            flight.setAvailableSeats(declaredCapacity(flight));
        } catch (Exception ex) {
            log.warn("Available-seat lookup failed for flight {} ({})", flight.getFlightNumber(), ex.getMessage());
            flight.setAvailableSeats(declaredCapacity(flight));
        }
    }

    private static int declaredCapacity(Flight flight) {
        if (flight.getSeatCount() != null && flight.getSeatCount() > 0) {
            return flight.getSeatCount();
        }
        if (flight.getSeatRows() != null && flight.getSeatColumns() != null) {
            return Math.max(flight.getSeatRows() * flight.getSeatColumns(), 0);
        }
        return 0;
    }

    private static String resolveAirlineName(Flight flight, Map<Integer, String> airlineMap) {
        if (flight.getAirlineId() != null) {
            String mapped = airlineMap.get(flight.getAirlineId());
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }
        if (flight.getFlightName() != null && !flight.getFlightName().isBlank()) {
            return flight.getFlightName();
        }
        return "Unknown";
    }

    private static final Map<String, String> LOCATION_TO_CODE = buildLocationToCode();

    private static Set<String> buildInternationalAirportCodes() {
        Set<String> codes = new HashSet<>();
        for (IndianAirports.Airport airport : InternationalAirports.AIRPORTS) {
            codes.add(airport.code().toUpperCase(Locale.ROOT));
            codes.add(airport.city().toUpperCase(Locale.ROOT));
        }
        return codes;
    }

    private static Map<String, String> buildLocationToCode() {
        Map<String, String> map = new HashMap<>();
        // Add domestic airports
        for (IndianAirports.Airport airport : IndianAirports.AIRPORTS) {
            String code = airport.code().toUpperCase(Locale.ROOT);
            map.put(code, code);
            map.put(airport.city().toUpperCase(Locale.ROOT), code);
        }
        // Add international airports
        for (IndianAirports.Airport airport : InternationalAirports.AIRPORTS) {
            String code = airport.code().toUpperCase(Locale.ROOT);
            map.put(code, code);
            map.put(airport.city().toUpperCase(Locale.ROOT), code);
        }
        return map;
    }

    private static String canonicalLocation(String location) {
        if (location == null) return null;
        String key = location.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty()) return null;
        return LOCATION_TO_CODE.getOrDefault(key, key);
    }

    private static boolean matchesLocation(String stored, String queryCanonical) {
        if (queryCanonical == null) return true;
        return queryCanonical.equals(canonicalLocation(stored));
    }

    private static LocalDate extractDate(String value) {
        if (value == null || value.isBlank()) return null;
        String datePart = value.trim().replace('T', ' ').split(" ")[0];
        try {
            return LocalDate.parse(datePart);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    @Override
    public Booking prepareBookingDraft(Booking booking) {
        Booking bookingDraft = booking == null ? new Booking() : booking;
        int passengerCount = clampPassengers(bookingDraft.getPassengers());
        bookingDraft.setPassengers(passengerCount);
        ensurePassengerSlots(bookingDraft, passengerCount);
        return bookingDraft;
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking) {
        Booking bookingToSave = booking == null ? new Booking() : booking;

        Flight resolvedFlight = resolveFlightReference(bookingToSave);
        bookingToSave.setFlight(resolvedFlight);

        int passengerCount = clampPassengers(bookingToSave.getPassengers());
        bookingToSave.setPassengers(passengerCount);
        ensurePassengerSlots(bookingToSave, passengerCount);

        // FIXED: Passengers are mapped without calling immediate early repository updates
        bookingToSave.setPassengerDetails(resolvePassengers(bookingToSave.getPassengerDetails()));

        enforceOnePassengerPerFlight(bookingToSave);

        if (bookingToSave.getFlightType() == null || bookingToSave.getFlightType().isBlank()) {
            bookingToSave.setFlightType("domestic");
        }
        MockPaymentService.MockPaymentResult paymentResult = mockPaymentService.processBookingPayment(bookingToSave);
        bookingToSave.setPnr(paymentResult.pnr());
        bookingToSave.setStatus(paymentResult.bookingStatus());
        if (bookingToSave.getSeat() == null || bookingToSave.getSeat().isBlank()) {
            bookingToSave.setSeat("12B");
        }
        if (bookingToSave.getPassengers() <= 0) {
            bookingToSave.setPassengers(1);
        }
        if (bookingToSave.getDepartureDate() != null && !bookingToSave.getDepartureDate().isBlank()) {
            bookingToSave.setFlyDate(bookingToSave.getDepartureDate());
        } else if (bookingToSave.getFlyDate() == null || bookingToSave.getFlyDate().isBlank()) {
            bookingToSave.setFlyDate(LocalDate.now().plusDays(7).toString());
        }
        if (bookingToSave.getFare() <= 0) {
            bookingToSave.setFare(calculateFare(bookingToSave));
        }

        List<SeatInventory> seatsToMark = new ArrayList<>();
        if (bookingToSave.getSeat() != null && !bookingToSave.getSeat().isBlank()) {
            String[] parts = bookingToSave.getSeat().split(",");
            for (String p : parts) {
                String sNum = p == null ? null : p.trim();
                if (sNum == null || sNum.isBlank()) continue;

                Object fIdRaw = bookingToSave.getFlightId();
                Integer flightIdInt = null;
                if (fIdRaw != null) {
                    if (fIdRaw instanceof Number) {
                        flightIdInt = ((Number) fIdRaw).intValue();
                    } else {
                        try {
                            flightIdInt = Integer.valueOf(fIdRaw.toString());
                        } catch (NumberFormatException e) {
                            flightIdInt = null;
                        }
                    }
                }
                SeatInventory si = findSeat(flightIdInt, sNum);

                if (si != null && si.getSeatStatus() == SeatStatus.BOOKED) {
                    throw new IllegalStateException("Seat " + sNum + " is already booked. Please choose another seat.");
                }
                if (si != null) {
                    log.info("Adding seat to mark as booked: flightId={}, seatNumber={}, currentStatus={}", flightIdInt, sNum, si.getSeatStatus());
                    seatsToMark.add(si);
                }
            }
        }

        // Setup bidirectional reference links explicitly right before execution save
        if (bookingToSave.getPassengerDetails() != null) {
            bookingToSave.getPassengerDetails().forEach(p -> p.setBooking(bookingToSave));
        }

        Booking saved = bookingRepository.save(bookingToSave);

        log.info("Marking {} seats as booked", seatsToMark.size());
        for (SeatInventory si : seatsToMark) {
            si.setSeatStatus(SeatStatus.BOOKED);
            SeatInventory updated = seatInventoryRepository.save(si);
            log.info("Seat marked as booked: seatNumber={}, newStatus={}", updated.getSeatNumber(), updated.getSeatStatus());
        }

        awardDistanceMiles(saved);
        return saved;
    }

    private Flight resolveFlightReference(Booking booking) {
        Long flightId = booking.getFlightId();
        if (flightId == null && booking.getFlight() != null) {
            flightId = booking.getFlight().getFlightId();
        }
        if (flightId == null) {
            throw new IllegalArgumentException("Flight id is required for booking creation.");
        }
        final Long resolvedFlightId = flightId;
        Flight flight = flightRepository.findById(resolvedFlightId)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found for id: " + resolvedFlightId));
        booking.setFlight(flight);
        return flight;
    }

    // FIXED: Cleaned up logic to eliminate early .save() calls inside loops
    private List<Passenger> resolvePassengers(List<Passenger> incoming) {
        List<Passenger> resolved = new ArrayList<>();
        if (incoming == null) return resolved;
        for (Passenger p : incoming) {
            if (p == null) continue;
            Passenger existing = null;
            if (p.getId() != null) {
                existing = passengerRepository.findById(p.getId()).orElse(null);
            }
            if (existing == null && p.getEmail() != null && !p.getEmail().isBlank()) {
                existing = passengerRepository.findByEmailIgnoreCase(p.getEmail().trim()).orElse(null);
            }
            if (existing != null) {
                if (p.getFullName() != null && !p.getFullName().isBlank()) existing.setFullName(p.getFullName());
                if (p.getAge() != null) existing.setAge(p.getAge());
                if (p.getGender() != null && !p.getGender().isBlank()) existing.setGender(p.getGender());
                if (p.getEmail() != null && !p.getEmail().isBlank()) existing.setEmail(p.getEmail());
                if (p.getPhone() != null && !p.getPhone().isBlank()) existing.setPhone(p.getPhone());
                if (existing.getDistanceMiles() == null) existing.setDistanceMiles(0.0);
                resolved.add(existing);
            } else {
                if (p.getId() != null) p.setId(null);
                if (p.getDistanceMiles() == null) p.setDistanceMiles(0.0);
                resolved.add(p);
            }
        }
        return resolved;
    }

    private void enforceOnePassengerPerFlight(Booking booking) {
        Object rawFlightId = booking != null ? booking.getFlightId() : null;
        if (rawFlightId == null) return;

        List<Passenger> currentDetails = null;
        try { currentDetails = booking.getPassengerDetails(); } catch (Exception e) {}
        if (currentDetails == null) return;

        Set<Long> seenIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        for (Passenger p : currentDetails) {
            if (p == null) continue;
            Long id = p.getId();
            String email = (p.getEmail() != null && !p.getEmail().trim().isEmpty())
                    ? p.getEmail().trim().toLowerCase(Locale.ROOT) : null;

            if (id != null) {
                if (!seenIds.add(id)) {
                    throw new IllegalStateException("Passenger " + describePassenger(p) + " is duplicated in this booking request form.");
                }
            }
            if (email != null) {
                if (!seenEmails.add(email)) {
                    throw new IllegalStateException("Passenger with email " + email + " is duplicated in this booking request form.");
                }
            }
        }
    }

    private static String describePassenger(Passenger p) {
        if (p.getFullName() != null && !p.getFullName().isBlank()) {
            return p.getId() != null ? p.getFullName() + " (ID " + p.getId() + ")" : p.getFullName();
        }
        if (p.getId() != null) return "ID " + p.getId();
        if (p.getEmail() != null && !p.getEmail().isBlank()) return p.getEmail();
        return "provided";
    }

    @Override
    public void validateNoDuplicatePassengerOnFlight(Booking booking) {
        enforceOnePassengerPerFlight(booking);

        // Check if any passenger is already booked on the same flight
        Long flightId = booking.getFlightId();
        List<Passenger> currentPassengers = booking.getPassengerDetails();

        if (flightId == null || currentPassengers == null || currentPassengers.isEmpty()) {
            return;
        }

        for (Passenger passenger : currentPassengers) {
            if (passenger == null) continue;

            Long passengerId = passenger.getId();
            String email = (passenger.getEmail() != null && !passenger.getEmail().trim().isEmpty())
                    ? passenger.getEmail().trim() : null;

            // Check if this passenger already has bookings on this flight
            List<Passenger> existingBookings = passengerRepository.findPassengerBookingsOnFlight(
                    flightId,
                    passengerId,
                    email
            );

            if (!existingBookings.isEmpty()) {
                throw new IllegalStateException(
                        "Passenger " + describePassenger(passenger) + " is already booked on this flight. " +
                                "prA person cannot book multiple seats on the same flight."
                );
            }
        }
    }

    private void awardDistanceMiles(Booking booking) {
        try {
            if (booking.getFlightId() == null || booking.getPassengerDetails() == null || booking.getPassengerDetails().isEmpty()) {
                return;
            }
            Flight flight = flightRepository.findById(booking.getFlightId().longValue()).orElse(null);
            double routeMiles = (flight != null && flight.getDistanceMiles() != null) ? flight.getDistanceMiles() : 0.0;
            if (routeMiles <= 0) return;

            booking.getPassengerDetails().forEach(p -> {
                double current = (p.getDistanceMiles() != null) ? p.getDistanceMiles() : 0.0;
                p.setDistanceMiles(current + routeMiles);
            });
            passengerRepository.saveAll(booking.getPassengerDetails());
        } catch (Exception ex) {
            log.warn("Could not award distance miles ({})", ex.getMessage());
        }
    }

    private SeatInventory findSeat(Integer flightId, String seatNumber) {
        if (flightId == null || seatNumber == null || seatNumber.isBlank()) return null;
        return seatInventoryRepository.findByFlightIdAndSeatNumber(flightId, seatNumber).orElse(null);
    }

    @Override
    public Booking selectSeat(Long bookingId, String seat) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(BOOKING_NOT_FOUND + bookingId));
        if (seat != null && !seat.isBlank()) booking.setSeat(seat);
        return bookingRepository.save(booking);
    }

    @Override
    public Booking modifyBooking(Long bookingId, Booking updatedBooking) {
        Booking existing = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(BOOKING_NOT_FOUND + bookingId));

        if (updatedBooking != null) {
            if (updatedBooking.getFlightType() != null && !updatedBooking.getFlightType().isBlank()) existing.setFlightType(updatedBooking.getFlightType());
            if (updatedBooking.getFromLocation() != null && !updatedBooking.getFromLocation().isBlank()) existing.setFromLocation(updatedBooking.getFromLocation());
            if (updatedBooking.getToLocation() != null && !updatedBooking.getToLocation().isBlank()) existing.setToLocation(updatedBooking.getToLocation());
            if (updatedBooking.getAirline() != null && !updatedBooking.getAirline().isBlank()) existing.setAirline(updatedBooking.getAirline());
            if (updatedBooking.getSeat() != null && !updatedBooking.getSeat().isBlank()) existing.setSeat(updatedBooking.getSeat());
            if (updatedBooking.getDepartureDate() != null && !updatedBooking.getDepartureDate().isBlank()) {
                existing.setDepartureDate(updatedBooking.getDepartureDate());
                existing.setFlyDate(updatedBooking.getDepartureDate());
            } else if (updatedBooking.getFlyDate() != null && !updatedBooking.getFlyDate().isBlank()) {
                existing.setFlyDate(updatedBooking.getFlyDate());
            }
            if (updatedBooking.getPassengers() > 0) {
                int updatedCount = clampPassengers(updatedBooking.getPassengers());
                existing.setPassengers(updatedCount);
                ensurePassengerSlots(existing, updatedCount);
            }
        }
        existing.setFare(calculateFare(existing));
        return bookingRepository.save(existing);
    }

    @Override
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(BOOKING_NOT_FOUND + bookingId));
        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        if (booking.getFlightId() != null) {
            SeatInventory seat = findSeat(Math.toIntExact(booking.getFlightId()), booking.getSeat());
            if (seat != null && seat.getSeatStatus() == SeatStatus.BOOKED) {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seatInventoryRepository.save(seat);
            }
        }
        return saved;
    }

    @Override
    public Booking getBookingDetails() {
        return bookingRepository.findTopByOrderByIdDesc().orElseGet(this::createDefaultBookingPreview);
    }

    @Override
    public Booking getBookingById(Long bookingId) {
        if (bookingId == null) return createDefaultBookingPreview();
        return bookingRepository.findById(bookingId).orElseGet(this::createDefaultBookingPreview);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByIdDesc();
    }

    @Override
    public Passenger getPassengerById(Long passengerId) {
        if (passengerId == null) return null;
        return passengerRepository.findById(passengerId).orElse(null);
    }

    private Booking createDefaultBookingPreview() {
        Booking booking = new Booking();
        booking.setFlightType("domestic");
        booking.setFromLocation("CJB");
        booking.setToLocation("HYD");
        booking.setPassengers(1);
        booking.setPnr(mockPaymentService.generatePnr());
        booking.setStatus("CONFIRMED");
        booking.setAirline("IndiGo");
        booking.setSeat("12B");
        booking.setFare(5200.0);
        booking.setFlyDate("2026-06-15");
        return booking;
    }

    private double calculateFare(Booking booking) {
        double baseFare = "international".equalsIgnoreCase(booking.getFlightType()) ? 18500.0 : 5200.0;
        return baseFare * Math.max(booking.getPassengers(), 1);
    }

    private boolean isInternationalLocation(String location) {
        return location != null && INTERNATIONAL_AIRPORT_CODES.contains(location.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isInternationalFlight(Flight flight) {
        return isInternationalLocation(flight.getOrigin()) || isInternationalLocation(flight.getDestination());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private int clampPassengers(int requested) {
        return Math.min(Math.max(requested, 1), MAX_PASSENGERS);
    }

    private void ensurePassengerSlots(Booking booking, int passengerCount) {
        List<Passenger> currentPassengers = booking.getPassengerDetails();
        if (currentPassengers == null) currentPassengers = new ArrayList<>();
        while (currentPassengers.size() < passengerCount) currentPassengers.add(new Passenger());
        while (currentPassengers.size() > passengerCount) currentPassengers.removeLast();
        booking.setPassengerDetails(currentPassengers);
    }
}