package com.genc.arfoms.booking.repository;

import com.genc.arfoms.booking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT p FROM Payment p WHERE p.flight.flightId = :flightId AND p.seatNumber = :seatNumber")
    Optional<Payment> findByFlightIdAndSeatNumber(@Param("flightId") Integer flightId, @Param("seatNumber") String seatNumber);
}