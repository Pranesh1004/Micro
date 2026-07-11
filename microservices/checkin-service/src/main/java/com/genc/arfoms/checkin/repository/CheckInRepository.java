package com.genc.arfoms.checkin.repository;

import com.genc.arfoms.checkin.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    Optional<CheckIn> findFirstByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}

