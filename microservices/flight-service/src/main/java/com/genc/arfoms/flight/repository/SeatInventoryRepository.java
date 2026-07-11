package com.genc.arfoms.flight.repository;

import com.genc.arfoms.flight.model.SeatInventory;
import com.genc.arfoms.flight.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    @Query("SELECT s FROM SeatInventory s WHERE s.flight.flightId = :flightId")
    List<SeatInventory> findByFlightId(@Param("flightId") Integer flightId);

    @Query("SELECT s FROM SeatInventory s WHERE s.flight.flightId = :flightId ORDER BY s.seatNumber ASC")
    List<SeatInventory> findByFlightIdOrderBySeatNumberAsc(@Param("flightId") Integer flightId);

    @Query("SELECT s FROM SeatInventory s WHERE s.flight.flightId = :flightId AND s.seatStatus = :seatStatus")
    List<SeatInventory> findByFlightIdAndSeatStatus(@Param("flightId") Integer flightId, @Param("seatStatus") SeatStatus seatStatus);

    @Query("SELECT s FROM SeatInventory s WHERE s.flight.flightId = :flightId AND s.seatNumber = :seatNumber")
    Optional<SeatInventory> findByFlightIdAndSeatNumber(@Param("flightId") Integer flightId, @Param("seatNumber") String seatNumber);
}
