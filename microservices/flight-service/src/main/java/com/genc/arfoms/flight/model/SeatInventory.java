package com.genc.arfoms.flight.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false, foreignKey = @ForeignKey(name = "FK_seat_inventory_flight_fs"))
    private Flight flight;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Column(name = "column_letter", length = 5, nullable = false)
    private String columnLetter;

    @Column(name = "seat_row", nullable = false)
    private Integer seatRow;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", length = 20, nullable = false)
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
