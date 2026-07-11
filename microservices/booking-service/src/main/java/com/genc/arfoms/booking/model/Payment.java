package com.genc.arfoms.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "base_fare")
    private String baseFare;
    @Column(name = "seat_charges")
    private String seatCharges;
    @Column(name = "taxes")
    private String taxes;
    @Column(name = "total_amount")
    private String totalAmount;
    @Column(name = "savings")
    private String savings;
    @Column(name = "flight_number")
    private String flightNumber;
    @Column(name = "source")
    private String source;
    @Column(name = "destination")
    private String destination;
    @Column(name = "departure_time")
    private String departureTime;
    @Column(name = "arrival_time")
    private String arrivalTime;
    @Column(name = "passenger_name")
    private String passengerName;

//    @ManyToOne
//    @JoinColumn(name = "flight_id", nullable = false, foreignKey = @ForeignKey(name = "flights"))
//    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false,foreignKey = @ForeignKey(name = "FK_payment_flight"))
    private Flight flight;

    @Column(name = "seat_number", length = 10)
    private String seatNumber;

    // Helper Compatibility Layer: Safely extracts flightId for systems expecting flat integers
    public Integer getFlightId() {
        return (this.flight != null) ? this.flight.getFlightId().intValue() : null;
    }

    // Helper Compatibility Layer: Sets flight reference based on ID without breaking encapsulation
    public void setFlightId(Integer flightId) {
        if (flightId != null) {
            if (this.flight == null) {
                this.flight = new Flight();
            }
            this.flight.setFlightId(flightId.longValue());
        } else {
            this.flight = null;
        }
    }
}
