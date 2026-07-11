package com.genc.arfoms.booking.repository;

import com.genc.arfoms.booking.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    Optional<Passenger> findByEmailIgnoreCase(String email);

    @Query("SELECT p FROM Passenger p WHERE p.booking.flight.flightId = :flightId AND " +
           "(p.id = :passengerId OR (p.email IS NOT NULL AND LOWER(p.email) = LOWER(:email)))")
    List<Passenger> findPassengerBookingsOnFlight(@Param("flightId") Long flightId,
                                                    @Param("passengerId") Long passengerId,
                                                    @Param("email") String email);
}

