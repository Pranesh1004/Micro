package com.genc.arfoms.flight.service;

import com.genc.arfoms.flight.exception.FlightException;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.IndianAirports;
import com.genc.arfoms.flight.model.InternationalAirports;
import com.genc.arfoms.flight.model.SeatInventory;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.model.SeatStatus;
import com.genc.arfoms.flight.repository.FlightRepository;
import com.genc.arfoms.flight.repository.SeatInventoryRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class FlightServiceImpl implements FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightServiceImpl.class);
    private static final DateTimeFormatter INPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter OUTPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double EARTH_RADIUS_MILES = 3958.8;
    private static final double DEFAULT_CRUISE_SPEED_MPH = 510.0;

    private static final Set<String> VALID_AIRPORT_CODES = Stream.concat(
                    IndianAirports.AIRPORTS.stream(),
                    InternationalAirports.AIRPORTS.stream()
            )
            .map(IndianAirports.Airport::code)
            .collect(Collectors.toUnmodifiableSet());

    private static final Map<String, GeoPoint> AIRPORT_COORDINATES = Map.ofEntries(
            Map.entry("DEL", new GeoPoint(28.5562, 77.1000)),
            Map.entry("BOM", new GeoPoint(19.0896, 72.8656)),
            Map.entry("BLR", new GeoPoint(13.1986, 77.7066)),
            Map.entry("MAA", new GeoPoint(12.9941, 80.1709)),
            Map.entry("CCU", new GeoPoint(22.6547, 88.4467)),
            Map.entry("HYD", new GeoPoint(17.2403, 78.4294)),
            Map.entry("COK", new GeoPoint(10.1520, 76.4019)),
            Map.entry("AMD", new GeoPoint(23.0772, 72.6347)),
            Map.entry("PNQ", new GeoPoint(18.5821, 73.9197)),
            Map.entry("GOI", new GeoPoint(15.3808, 73.8314)),
            Map.entry("GOX", new GeoPoint(15.7368, 73.8606)),
            Map.entry("TRV", new GeoPoint(8.4821, 76.9201)),
            Map.entry("IXC", new GeoPoint(30.6735, 76.7885)),
            Map.entry("ATQ", new GeoPoint(31.7096, 74.7973)),
            Map.entry("SXR", new GeoPoint(33.9871, 74.7742)),
            Map.entry("IXJ", new GeoPoint(32.6891, 74.8374)),
            Map.entry("BBI", new GeoPoint(20.2444, 85.8178)),
            Map.entry("GAU", new GeoPoint(26.1061, 91.5859)),
            Map.entry("IXZ", new GeoPoint(11.6412, 92.7297)),
            Map.entry("DXB", new GeoPoint(25.2532, 55.3657)),
            Map.entry("AUH", new GeoPoint(24.4330, 54.6511)),
            Map.entry("DOH", new GeoPoint(25.2731, 51.6081)),
            Map.entry("LHR", new GeoPoint(51.4700, -0.4543)),
            Map.entry("CDG", new GeoPoint(49.0097, 2.5479)),
            Map.entry("JFK", new GeoPoint(40.6413, -73.7781)),
            Map.entry("SIN", new GeoPoint(1.3644, 103.9915)),
            Map.entry("HKG", new GeoPoint(22.3080, 113.9185)),
            Map.entry("BKK", new GeoPoint(13.6900, 100.7501)),
            Map.entry("FRA", new GeoPoint(50.0379, 8.5622)),
            Map.entry("AMS", new GeoPoint(52.3105, 4.7683)),
            Map.entry("MAD", new GeoPoint(40.4983, -3.5676)),
            Map.entry("FCO", new GeoPoint(41.8003, 12.2389)),
            Map.entry("IST", new GeoPoint(41.2753, 28.7519)),
            Map.entry("ZRH", new GeoPoint(47.4581, 8.5555)),
            Map.entry("NRT", new GeoPoint(35.7720, 140.3929)),
            Map.entry("HND", new GeoPoint(35.5494, 139.7798)),
            Map.entry("ICN", new GeoPoint(37.4602, 126.4407)),
            Map.entry("KUL", new GeoPoint(2.7456, 101.7099)),
            Map.entry("SYD", new GeoPoint(-33.9399, 151.1753)),
            Map.entry("MEL", new GeoPoint(-37.6690, 144.8410)),
            Map.entry("LAX", new GeoPoint(33.9416, -118.4085)),
            Map.entry("SFO", new GeoPoint(37.6213, -122.3790)),
            Map.entry("ORD", new GeoPoint(41.9742, -87.9073)),
            Map.entry("YYZ", new GeoPoint(43.6777, -79.6248)),
            Map.entry("YVR", new GeoPoint(49.1967, -123.1815))
    );

    private final FlightRepository flightRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    public FlightServiceImpl(FlightRepository flightRepository, SeatInventoryRepository seatInventoryRepository) {
        this.flightRepository = flightRepository;
        this.seatInventoryRepository = seatInventoryRepository;
    }

    @PostConstruct
    public void seedSeatInventoryIfRequested() {
        String seedFlag = System.getProperty("seed.seats");
        if (!"true".equalsIgnoreCase(seedFlag)) {
            return; // no-op unless explicitly requested
        }
        String fidStr = System.getProperty("seed.flightId", "1");
        Long flightId;
        try {
            flightId = Long.parseLong(fidStr);
        } catch (NumberFormatException ex) {
            log.warn("Invalid seed.flightId '{}', aborting seeding", fidStr);
            return;
        }

        Optional<Flight> flightOpt = flightRepository.findById(flightId);
        if (flightOpt.isEmpty()) {
            log.warn("No flight found for id {} - cannot seed seats", flightId);
            return;
        }
        Flight flight = flightOpt.get();
        int rows = flight.getSeatRows() != null ? flight.getSeatRows() : 0;
        int cols = flight.getSeatColumns() != null ? flight.getSeatColumns() : 0;
        if (rows < 1 || cols < 1) {
            log.warn("Flight {} has invalid seatRows/seatColumns ({} / {}) - skipping seeding", flight.getFlightNumber(), rows, cols);
            return;
        }

        // check existing inventory
        List<SeatInventory> existing = seatInventoryRepository.findByFlightIdOrderBySeatNumberAsc(flight.getFlightId().intValue());
        if (!existing.isEmpty()) {
            log.info("Flight {} already has {} seat inventory rows - skipping seeding", flight.getFlightNumber(), existing.size());
            return;
        }

        List<SeatInventory> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String rowLabel = rowLetter(r);
            for (int c = 1; c <= cols; c++) {
                SeatInventory seat = new SeatInventory();
                seat.setFlight(flight);
                seat.setSeatNumber(rowLabel + c);
                seat.setColumnLetter(rowLabel);
                seat.setSeatRow(r + 1);
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seats.add(seat);
            }
        }
        seatInventoryRepository.saveAll(seats);
        log.info("Seeded {} seats for flight {} (id={})", seats.size(), flight.getFlightNumber(), flightId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Flight> getFlightDetails(String flightNumber) {
        validateFlightNumber(flightNumber);
        return flightRepository.findByFlightNumberIgnoreCase(flightNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Flight getFlight(Long flightId) {
        if (flightId == null) {
            return null;
        }
        return flightRepository.findById(flightId).orElse(null);
    }

    @Override
    public void addFlight(Flight flight) {
        validateNewFlight(flight);
        String normalizedOrigin = normalizeAirportCode(flight.getOrigin(), "origin");
        String normalizedDestination = normalizeAirportCode(flight.getDestination(), "destination");
        validateDistinctRoute(normalizedOrigin, normalizedDestination);

        flight.setFlightNumber(flight.getFlightNumber().trim());
        flight.setOrigin(normalizedOrigin);
        flight.setDestination(normalizedDestination);

        flight.setDepartureTime(normalizeDateTime(flight.getDepartureTime(), "departureTime"));
        flight.setArrivalTime(normalizeDateTime(flight.getArrivalTime(), "arrivalTime"));

        validateNotInPast(flight.getDepartureTime(), "departureTime");
        validateScheduleOrder(flight.getDepartureTime(), flight.getArrivalTime());
        flight.setDistanceMiles(calculateRouteDistanceMiles(flight));

        if (flight.getFlightStatus() == null) {
            flight.setFlightStatus(FlightStatus.SCHEDULED);
        }
        if (flight.getEconomyFare() == null || flight.getPremiumFare() == null || flight.getFirstFare() == null) {
            throw new FlightException("All fare fields are required");
        }
        validateFare(flight.getEconomyFare(), "economyFare");
        validateFare(flight.getPremiumFare(), "premiumFare");
        validateFare(flight.getFirstFare(), "firstFare");
        flight.setFare(flight.getEconomyFare());

        flightRepository.findByFlightNumberIgnoreCase(flight.getFlightNumber())
                .ifPresent(existing -> {
                    throw new FlightException(HttpStatus.CONFLICT, "Flight with number " + flight.getFlightNumber() + " already exists");
                });

        Flight saved = flightRepository.save(flight);
        createSeatInventory(saved);
        log.info("Added flight {} ({} -> {})", saved.getFlightNumber(), saved.getOrigin(), saved.getDestination());
    }

    private void createSeatInventory(Flight flight) {
        Integer rows = flight.getSeatRows();
        Integer cols = flight.getSeatColumns();
        if (flight.getFlightId() == null || rows == null || cols == null || rows < 1 || cols < 1) {
            return;
        }
        List<SeatInventory> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String rowLabel = rowLetter(r);
            for (int c = 1; c <= cols; c++) {
                SeatInventory seat = new SeatInventory();
                seat.setFlight(flight);
                seat.setSeatNumber(rowLabel + c);
                seat.setColumnLetter(rowLabel);
                seat.setSeatRow(r + 1);
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seats.add(seat);
            }
        }
        seatInventoryRepository.saveAll(seats);
        log.info("Created {} seat(s) for flight {}", seats.size(), flight.getFlightNumber());
    }

    private static String rowLetter(int index) {
        StringBuilder label = new StringBuilder();
        int n = index;
        do {
            label.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return label.toString();
    }

    @Override
    public void deleteFlight(String flightNumber) {
        validateFlightNumber(flightNumber);
        Flight flight = findByFlightNumberOrThrow(flightNumber);
        if (flight.getFlightId() != null) {
            List<SeatInventory> seats =
                    seatInventoryRepository.findByFlightIdOrderBySeatNumberAsc(flight.getFlightId().intValue());
            if (!seats.isEmpty()) {
                seatInventoryRepository.deleteAll(seats);
            }
        }
        flightRepository.delete(flight);
        log.info("Deleted flight {}", flightNumber);
    }

    @Override
    public void updateSchedule(String flightNumber, String departureTime, String arrivalTime, FlightStatus flightStatus) {
        validateFlightNumber(flightNumber);
        Flight flight = findByFlightNumberOrThrow(flightNumber);

        if (departureTime != null && !departureTime.isBlank()) {
            flight.setDepartureTime(normalizeDateTime(departureTime, "departureTime"));
            validateNotInPast(flight.getDepartureTime(), "departureTime");
        }
        if (arrivalTime != null && !arrivalTime.isBlank()) {
            flight.setArrivalTime(normalizeDateTime(arrivalTime, "arrivalTime"));
        }
        validateScheduleOrder(flight.getDepartureTime(), flight.getArrivalTime());
        flight.setDistanceMiles(calculateRouteDistanceMiles(flight));
        if (flightStatus != null) {
            flight.setFlightStatus(flightStatus);
        }
        flightRepository.save(flight);
        log.info("Updated schedule for flight {}", flightNumber);
    }

    @Override
    public void setFares(String flightNumber, double economyFare, double premiumFare, double firstFare) {
        validateFlightNumber(flightNumber);
        validateFare(economyFare, "economyFare");
        validateFare(premiumFare, "premiumFare");
        validateFare(firstFare, "firstFare");

        Flight flight = findByFlightNumberOrThrow(flightNumber);
        flight.setEconomyFare(economyFare);
        flight.setPremiumFare(premiumFare);
        flight.setFirstFare(firstFare);
        flight.setFare(economyFare);
        flightRepository.save(flight);
        log.info("Updated fares for flight {}", flightNumber);
    }

    @Override
    public void setFareClass(String flightNumber, double fare, String fareClass) {
        validateFlightNumber(flightNumber);
        validateFare(fare, "fare");
        if (fareClass == null || fareClass.isBlank()) {
            throw new FlightException("fareClass is required");
        }

        Flight flight = findByFlightNumberOrThrow(flightNumber);
        flight.setFare(fare);
        flightRepository.save(flight);
        log.info("Updated fare class profile to {} for flight {}", fareClass, flightNumber);
    }

    private Flight findByFlightNumberOrThrow(String flightNumber) {
        return flightRepository.findByFlightNumberIgnoreCase(flightNumber)
                .orElseThrow(() -> new FlightException(HttpStatus.NOT_FOUND, "Flight not found for number " + flightNumber));
    }

    private void validateFlightNumber(String flightNumber) {
        if (flightNumber == null || flightNumber.isBlank()) {
            throw new FlightException("flightNumber is required");
        }
    }

    private void validateNewFlight(Flight flight) {
        if (flight == null) {
            throw new FlightException("Flight payload cannot be null");
        }
        validateFlightNumber(flight.getFlightNumber());
        if (flight.getOrigin() == null || flight.getOrigin().isBlank()) {
            throw new FlightException("origin is required");
        }
        if (flight.getDestination() == null || flight.getDestination().isBlank()) {
            throw new FlightException("destination is required");
        }
    }

    private String normalizeAirportCode(String airportCode, String field) {
        if (airportCode == null || airportCode.isBlank()) {
            throw new FlightException(field + " is required");
        }

        String normalizedCode = airportCode.trim().toUpperCase();
        if (!VALID_AIRPORT_CODES.contains(normalizedCode)) {
            throw new FlightException(field + " must be a valid Indian or international airport code");
        }
        return normalizedCode;
    }

    private void validateDistinctRoute(String origin, String destination) {
        if (origin.equals(destination)) {
            throw new FlightException("origin and destination cannot be the same");
        }
    }

    private void validateFare(double fare, String field) {
        if (Double.isNaN(fare) || Double.isInfinite(fare) || fare < 0) {
            throw new FlightException(field + " must be a non-negative number");
        }
    }

    private String normalizeDateTime(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new FlightException(field + " is required");
        }

        String value = raw.trim();
        if (value.contains("T")) {
            try {
                return LocalDateTime.parse(value, INPUT_DATE_TIME_FORMAT).format(OUTPUT_DATE_TIME_FORMAT);
            } catch (DateTimeParseException ignored) {
                throw new FlightException(field + " must be in yyyy-MM-ddTHH:mm format");
            }
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
            return value;
        }

        throw new FlightException(field + " must be in yyyy-MM-ddTHH:mm format");
    }

    private void validateScheduleOrder(String departureTime, String arrivalTime) {
        Optional<LocalDateTime> departure = parseStoredDateTime(departureTime);
        Optional<LocalDateTime> arrival = parseStoredDateTime(arrivalTime);

        if (departure.isPresent() && arrival.isPresent() && !arrival.get().isAfter(departure.get())) {
            throw new FlightException("arrivalTime must be after departureTime");
        }
    }

    private void validateNotInPast(String departureTime, String field) {
        parseStoredDateTime(departureTime).ifPresent(dep -> {
            // Added check to allow matching minute operations during rapid local UI execution passes
            if (dep.isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new FlightException(field + " cannot be in the past");
            }
        });
    }

    private Optional<LocalDateTime> parseStoredDateTime(String value) {
        try {
            return Optional.of(LocalDateTime.parse(value, OUTPUT_DATE_TIME_FORMAT));
        } catch (DateTimeParseException ex) {
            log.debug("Skipping schedule ordering validation for non-standard datetime: {}", value);
            return Optional.empty();
        }
    }

    private Double calculateRouteDistanceMiles(Flight flight) {
        GeoPoint originPoint = AIRPORT_COORDINATES.get(flight.getOrigin());
        GeoPoint destinationPoint = AIRPORT_COORDINATES.get(flight.getDestination());
        if (originPoint != null && destinationPoint != null) {
            return roundToOneDecimal(haversineMiles(originPoint, destinationPoint));
        }

        Optional<LocalDateTime> departure = parseStoredDateTime(flight.getDepartureTime());
        Optional<LocalDateTime> arrival = parseStoredDateTime(flight.getArrivalTime());
        if (departure.isPresent() && arrival.isPresent() && arrival.get().isAfter(departure.get())) {
            double durationHours = Duration.between(departure.get(), arrival.get()).toMinutes() / 60.0;
            return roundToOneDecimal(durationHours * DEFAULT_CRUISE_SPEED_MPH);
        }

        return null;
    }

    private double haversineMiles(GeoPoint from, GeoPoint to) {
        double lat1 = Math.toRadians(from.latitude());
        double lon1 = Math.toRadians(from.longitude());
        double lat2 = Math.toRadians(to.latitude());
        double lon2 = Math.toRadians(to.longitude());
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_MILES * c;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record GeoPoint(double latitude, double longitude) {}

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final Map<String, String> LOCATION_TO_CODE = buildLocationToCode();
    private static final Set<String> INTERNATIONAL_AIRPORT_CODES = buildInternationalAirportCodes();

    private static Map<String, String> buildLocationToCode() {
        Map<String, String> map = new HashMap<>();
        for (IndianAirports.Airport airport : IndianAirports.AIRPORTS) {
            String code = airport.code().toUpperCase(Locale.ROOT);
            map.put(code, code);
            map.put(airport.city().toUpperCase(Locale.ROOT), code);
        }
        for (IndianAirports.Airport airport : InternationalAirports.AIRPORTS) {
            String code = airport.code().toUpperCase(Locale.ROOT);
            map.put(code, code);
            map.put(airport.city().toUpperCase(Locale.ROOT), code);
        }
        return map;
    }

    private static Set<String> buildInternationalAirportCodes() {
        Set<String> codes = new HashSet<>();
        for (IndianAirports.Airport airport : InternationalAirports.AIRPORTS) {
            codes.add(airport.code().toUpperCase(Locale.ROOT));
            codes.add(airport.city().toUpperCase(Locale.ROOT));
        }
        return codes;
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

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean isInternationalLocation(String location) {
        return location != null && INTERNATIONAL_AIRPORT_CODES.contains(location.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isInternationalFlight(Flight flight) {
        return isInternationalLocation(flight.getOrigin()) || isInternationalLocation(flight.getDestination());
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

    @Override
    public List<Flight> searchAvailableFlights(String from, String to, String date, String type, Integer passengers) {
        String fromCanonical = canonicalLocation(from);
        String toCanonical = canonicalLocation(to);
        LocalDate departureDate = parseDate(date);
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

        if (type != null && !type.isBlank()) {
            boolean wantInternational = "international".equalsIgnoreCase(type);
            flights = flights.stream()
                    .filter(f -> isInternationalFlight(f) == wantInternational)
                    .collect(Collectors.toList());
        }

        flights.forEach(f -> f.setAirlineName(f.getFlightName() != null ? f.getFlightName() : "Unknown"));
        flights.forEach(this::attachAvailableSeats);
        return flights;
    }
}