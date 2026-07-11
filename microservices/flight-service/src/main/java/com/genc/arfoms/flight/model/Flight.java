package com.genc.arfoms.flight.model;

import com.genc.arfoms.flight.model.FlightStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Flights") // Matches the SQL case-sensitivity schema
@Data
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flightId") // Aligns with flightId (PK) in schema doc
    private Long flightId;

    @Column(name = "flightNumber", nullable = false, unique = true, length = 20)
    private String flightNumber;

    @Column(name = "flight_name")
    private String flightName;

    @Column(name = "origin", length = 50)
    private String origin;

    @Column(name = "destination", length = 50)
    private String destination;

    @Column(name = "distance_miles")
    private Double distanceMiles;

    @Column(name = "departureTime") // Aligns with departureTime DATETIME schema field
    private String departureTime;

    @Column(name = "arrivalTime") // Aligns with arrivalTime DATETIME schema field
    private String arrivalTime;

    // Standardized to 'Double' object wrapper to safely allow handling NULL values
    @Column(name = "fare")
    private Double fare;

    @Column(name = "economy_fare")
    private Double economyFare;

    @Column(name = "premium_fare")
    private Double premiumFare;

    @Column(name = "first_fare")
    private Double firstFare;

    @Enumerated(EnumType.STRING)
    @Column(name = "flightStatus")
    private FlightStatus flightStatus;

    // Cabin and seating layout components
    @Column(name = "seat_count")
    private Integer seatCount;

    @Column(name = "seat_rows")
    private Integer seatRows;

    @Column(name = "seat_columns")
    private Integer seatColumns;

    @Column(name = "seat_aisle_after")
    private Integer seatAisleAfter;

    // Transient attributes: excluded from DB storage, reserved for external system integrations
    @Transient
    private Integer airlineId;

    @Transient
    private String airlineName;

    @Transient
    private Integer availableSeats; // Added to resolve BookingServiceImpl calculation requirements

    // Default Constructor
    public Flight() {
        this.flightStatus = FlightStatus.SCHEDULED;
    }

    // Comprehensive Argument Constructor for service initialization tasks
    public Flight(String flightNumber, String origin, String destination,
                  String departureTime, String arrivalTime, Double fare, FlightStatus flightStatus) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.fare = fare;
        this.flightStatus = (flightStatus != null) ? flightStatus : FlightStatus.SCHEDULED;
    }
}