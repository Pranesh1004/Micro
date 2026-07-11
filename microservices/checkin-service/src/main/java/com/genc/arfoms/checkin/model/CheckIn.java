package com.genc.arfoms.checkin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkInId;

    private Long bookingId;
    private LocalDateTime checkInTime;
    private Integer baggageCount;
    private BigDecimal baggageWeight;

    @Enumerated(EnumType.STRING)
    private CheckInStatus checkInStatus = CheckInStatus.CHECKED_IN;

    public Long getCheckInId() {
        return checkInId;
    }

    public void setCheckInId(Long checkInId) {
        this.checkInId = checkInId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Integer getBaggageCount() {
        return baggageCount;
    }

    public void setBaggageCount(Integer baggageCount) {
        this.baggageCount = baggageCount;
    }

    public BigDecimal getBaggageWeight() {
        return baggageWeight;
    }

    public void setBaggageWeight(BigDecimal baggageWeight) {
        this.baggageWeight = baggageWeight;
    }

    public CheckInStatus getCheckInStatus() {
        return checkInStatus;
    }

    public void setCheckInStatus(CheckInStatus checkInStatus) {
        this.checkInStatus = checkInStatus;
    }
}

