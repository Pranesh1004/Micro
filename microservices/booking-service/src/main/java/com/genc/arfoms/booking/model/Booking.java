package com.genc.arfoms.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_flight"))
    private Flight flight;

    @Column(name = "flightType")
    private String flightType;

    @Column(name = "fromLocation")
    private String fromLocation;

    @Column(name = "toLocation")
    private String toLocation;

    @Column(name = "departureDate")
    private String departureDate;

    @Column(name = "passengers")
    private int passengers;

    @Column(name = "pnr")
    private String pnr;

    @Column(name = "status")
    private String status;

    @Column(name = "airline")
    private String airline;

    @Column(name = "seat")
    private String seat;

    @Column(name = "fare")
    private double fare;

    @Column(name = "flyDate")
    private String flyDate;



    // Find this inside Booking.java:
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Passenger> passengerDetails = new ArrayList<>();

    // Custom setter kept (Lombok will not override it) to maintain the bidirectional link
    public void setPassengerDetails(List<Passenger> passengerDetails) {
        List<Passenger> incoming = (passengerDetails == null)
                ? new ArrayList<>()
                : new ArrayList<>(passengerDetails);
        this.passengerDetails.clear();
        incoming.forEach(this::addPassengerDetail);
    }

    public void addPassengerDetail(Passenger passenger) {
        if (passenger == null) {
            return;
        }
        passenger.setBooking(this);
        this.passengerDetails.add(passenger);
    }

    // Helper Compatibility Layer: Safely extracts flightId for systems expecting flat integers
    public Long getFlightId() {
        return (this.flight != null) ? this.flight.getFlightId() : null;
    }

    // Helper Compatibility Layer: Sets flight reference based on ID without breaking encapsulation
    public void setFlightId(Long flightId) {
        if (flightId != null) {
            if (this.flight == null) {
                this.flight = new Flight();
            }
            this.flight.setFlightId(flightId);
        } else {
            this.flight = null;
        }
    }
}