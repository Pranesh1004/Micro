package com.genc.arfoms.flight.controller;

import com.genc.arfoms.flight.dto.FareUpdateRequest;
import com.genc.arfoms.flight.dto.FlightRequest;
import com.genc.arfoms.flight.dto.ScheduleUpdateRequest;
import com.genc.arfoms.flight.exception.FlightException;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.InternationalAirports;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.model.IndianAirports;
import com.genc.arfoms.flight.service.FlightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/flights")
// Corrected to allow global cross-origin access safely alongside permitted endpoints
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<Flight> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{flightNumber}")
    public Flight getFlightByNumber(@PathVariable String flightNumber) {
        return flightService.getFlightDetails(flightNumber)
                .orElseThrow(() -> new FlightException(HttpStatus.NOT_FOUND, "Flight not found for number " + flightNumber));
    }

    @PostMapping
    public ResponseEntity<Flight> addFlight(@RequestBody FlightRequest request) {
        Flight flight = request.toEntity();
        flightService.addFlight(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(getFlightByNumber(flight.getFlightNumber()));
    }

    @PutMapping("/{flightNumber}/schedule")
    public Flight updateSchedule(@PathVariable String flightNumber,
                                 @RequestBody ScheduleUpdateRequest request) {
        flightService.updateSchedule(flightNumber, request.getDepartureTime(), request.getArrivalTime(), request.getFlightStatus());
        return getFlightByNumber(flightNumber);
    }

    @PutMapping("/{flightNumber}/fares")
    public Flight setFares(@PathVariable String flightNumber,
                           @RequestBody FareUpdateRequest request) {
        flightService.setFares(flightNumber, request.getEconomyFare(), request.getPremiumFare(), request.getFirstFare());
        return getFlightByNumber(flightNumber);
    }

    @DeleteMapping("/{flightNumber}")
    public ResponseEntity<Void> deleteFlight(@PathVariable String flightNumber) {
        flightService.deleteFlight(flightNumber);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/search")
    public ResponseEntity<List<Flight>> searchFlights(@RequestBody Map<String, Object> criteria) {
        String fromLocation = (String) criteria.get("fromLocation");
        String toLocation = (String) criteria.get("toLocation");
        String departureDate = (String) criteria.get("departureDate");
        String flightType = (String) criteria.get("flightType");
        Integer passengers = (Integer) criteria.get("passengers");

        List<Flight> flights = flightService.searchAvailableFlights(fromLocation, toLocation, departureDate, flightType, passengers);
        return ResponseEntity.ok(flights);
    }
    @GetMapping("/metadata")
    public Map<String, Object> getMetadata() {
        List<IndianAirports.Airport> indianAirports = IndianAirports.AIRPORTS;
        List<IndianAirports.Airport> internationalAirports = InternationalAirports.AIRPORTS;
        List<IndianAirports.Airport> allAirports = Stream.concat(indianAirports.stream(), internationalAirports.stream())
                .toList();

        return Map.of(
                "statuses", FlightStatus.values(),
                "airports", allAirports,
                "indianAirports", indianAirports,
                "internationalAirports", internationalAirports
        );
    }
}