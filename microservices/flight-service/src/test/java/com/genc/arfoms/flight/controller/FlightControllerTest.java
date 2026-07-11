package com.genc.arfoms.flight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit 5 + Mockito web-layer tests for the flight endpoints, driving the
 * controller through a standalone {@link MockMvc} (no full Spring context / DB).
 * Verifies both the fetching (GET) and posting (POST) HTTP flows.
 */
@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    @Mock
    private FlightService flightService;

    @InjectMocks
    private FlightController flightController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(flightController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("GET /api/flights returns the list of flights (fetching)")
    void getAllFlights_returnsJsonArray() throws Exception {
        when(flightService.getAllFlights()).thenReturn(List.of(
                flight(1L, "AI2051", "DEL", "DXB"),
                flight(2L, "6E1002", "BOM", "BLR")));

        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].flightNumber").value("AI2051"))
                .andExpect(jsonPath("$[0].origin").value("DEL"))
                .andExpect(jsonPath("$[1].destination").value("BLR"));

        verify(flightService).getAllFlights();
    }

    @Test
    @DisplayName("GET /api/flights/{flightNumber} returns a single flight (fetching)")
    void getFlightDetails_returnsFlight() throws Exception {
        when(flightService.getFlightDetails("AI2051")).thenReturn(java.util.Optional.of(flight(1L, "AI2051", "DEL", "DXB")));

        mockMvc.perform(get("/api/flights/AI2051"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1))
                .andExpect(jsonPath("$.flightNumber").value("AI2051"));

        verify(flightService).getFlightDetails("AI2051");
    }

    @Test
    @DisplayName("POST /api/flights creates a flight (posting)")
    void addFlight_createsFlight() throws Exception {
        // Create a proper FlightRequest DTO
        String requestBody = """
            {
                "flightNumber": "AI9001",
                "flightName": "Air India 9001",
                "origin": "DEL",
                "destination": "SIN",
                "departureTime": "2026-07-15T02:30",
                "arrivalTime": "2026-07-15T05:10",
                "economyFare": 28000.0,
                "premiumFare": 50400.0,
                "firstFare": 84000.0,
                "seatRows": 3,
                "seatColumns": 6
            }
            """;
        
        Flight saved = flight(10L, "AI9001", "DEL", "SIN");
        saved.setSeatRows(3);
        saved.setSeatColumns(6);
        doNothing().when(flightService).addFlight(any(Flight.class));
        when(flightService.getFlightDetails("AI9001")).thenReturn(java.util.Optional.of(saved));

        mockMvc.perform(post("/api/flights")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("AI9001"));

        verify(flightService).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("PUT /api/flights/{flightNumber}/schedule updates the schedule (posting)")
    void updateSchedule_changesSchedule() throws Exception {
        Flight updated = flight(1L, "AI2051", "DEL", "DXB");
        updated.setFlightStatus(FlightStatus.BOARDING);
        doNothing().when(flightService).updateSchedule(eq("AI2051"), anyString(), anyString(), eq(FlightStatus.BOARDING));
        when(flightService.getFlightDetails("AI2051")).thenReturn(java.util.Optional.of(updated));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/flights/AI2051/schedule")
                        .contentType("application/json")
                        .content("{\"departureTime\":\"2026-07-15T10:00\",\"arrivalTime\":\"2026-07-15T12:00\",\"flightStatus\":\"BOARDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightStatus").value("BOARDING"));

        verify(flightService).updateSchedule(eq("AI2051"), anyString(), anyString(), eq(FlightStatus.BOARDING));
    }

    @Test
    @DisplayName("DELETE /api/flights/{flightNumber} removes a flight (posting)")
    void deleteFlight_invokesService() throws Exception {
        doNothing().when(flightService).deleteFlight("AI2051");
        
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/flights/AI2051"))
                .andExpect(status().isNoContent());

        verify(flightService).deleteFlight("AI2051");
    }

    private Flight flight(Long id, String number, String origin, String destination) {
        Flight f = new Flight();
        f.setFlightId(id);
        f.setFlightNumber(number);
        f.setOrigin(origin);
        f.setDestination(destination);
        f.setDepartureTime("2026-07-15 02:30");
        f.setArrivalTime("2026-07-15 05:10");
        f.setFlightStatus(FlightStatus.SCHEDULED);
        f.setEconomyFare(28000.0);
        f.setPremiumFare(50400.0);
        f.setFirstFare(84000.0);
        return f;
    }
}

