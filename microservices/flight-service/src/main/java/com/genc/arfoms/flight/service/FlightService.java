package com.genc.arfoms.flight.service;

import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;

import java.util.List;
import java.util.Optional;

public interface FlightService {

    List<Flight> getAllFlights();

    Optional<Flight> getFlightDetails(String flightNumber);

    Flight getFlight(Long flightId);

    void addFlight(Flight flight);

    void deleteFlight(String flightNumber);

    void updateSchedule(String flightNumber, String departureTime, String arrivalTime, FlightStatus flightStatus);

    void setFares(String flightNumber, double economyFare, double premiumFare, double firstFare);

    void setFareClass(String flightNumber, double fare, String fareClass);

    List<Flight> searchAvailableFlights(String from, String to, String date, String type, Integer passengers);
}