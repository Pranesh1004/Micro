package com.genc.arfoms.flight.service;

import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the flight service
 */
@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightServiceImpl flightService;

    private Flight sampleFlight;

    @BeforeEach
    void setUp() {
        sampleFlight = buildFlight(1L, "AI2051", "DEL", "DXB",
                "2026-07-15 02:30",
                "2026-07-15 05:10");
    }

    @Test
    @DisplayName("getAllFlights returns all flights from the repository")
    void getAllFlights_returnsList() {
        Flight second = buildFlight(2L, "6E1002", "BOM", "BLR",
                "2026-07-15 09:00",
                "2026-07-15 10:45");
        when(flightRepository.findAll()).thenReturn(List.of(sampleFlight, second));

        List<Flight> flights = flightService.getAllFlights();

        assertThat(flights).hasSize(2).containsExactly(sampleFlight, second);
        verify(flightRepository).findAll();
    }

    @Test
    @DisplayName("getFlightDetails returns Optional with flight when it exists")
    void getFlightDetails_found() {
        when(flightRepository.findByFlightNumberIgnoreCase("AI2051")).thenReturn(Optional.of(sampleFlight));

        Optional<Flight> found = flightService.getFlightDetails("AI2051");

        assertThat(found).isPresent().contains(sampleFlight);
        verify(flightRepository).findByFlightNumberIgnoreCase("AI2051");
    }

    @Test
    @DisplayName("getFlightDetails returns empty Optional when flight not found")
    void getFlightDetails_notFound() {
        when(flightRepository.findByFlightNumberIgnoreCase("NONE")).thenReturn(Optional.empty());

        Optional<Flight> found = flightService.getFlightDetails("NONE");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("getFlight returns flight for valid flightId")
    void getFlight_found() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        Flight found = flightService.getFlight(1L);

        assertThat(found).isSameAs(sampleFlight);
    }

    @Test
    @DisplayName("getFlight returns null when flightId is null")
    void getFlight_nullId() {
        Flight found = flightService.getFlight(null);
        assertThat(found).isNull();
    }

    // Helper method
    private Flight buildFlight(Long id, String number, String origin, String destination,
                              String depTime, String arrTime) {
        Flight f = new Flight();
        f.setFlightId(id);
        f.setFlightNumber(number);
        f.setOrigin(origin);
        f.setDestination(destination);
        f.setDepartureTime(depTime);
        f.setArrivalTime(arrTime);
        f.setFlightStatus(FlightStatus.SCHEDULED);
        f.setEconomyFare(5000.0);
        f.setPremiumFare(9000.0);
        f.setFirstFare(15000.0);
        return f;
    }
}

