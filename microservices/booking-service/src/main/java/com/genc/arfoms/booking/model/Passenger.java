package com.genc.arfoms.booking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "passenger")
@Data
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private Integer age;
    private String gender;
    private String email;
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "booking_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Booking booking;

    /** Transient attribute reserved for mileage accumulation inside the booking system. */
    @Transient
    private Double distanceMiles = 0.0;

    /** Convenience alias used by the seat/payment module. */
    @Transient
    public String getName() {
        return fullName;
    }
}