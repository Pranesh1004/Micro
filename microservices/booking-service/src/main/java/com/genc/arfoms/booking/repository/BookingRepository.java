package com.genc.arfoms.booking.repository;

import com.genc.arfoms.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findTopByOrderByIdDesc();

    List<Booking> findAllByOrderByIdDesc();

    Optional<Booking> findByPnr(String pnr);
}