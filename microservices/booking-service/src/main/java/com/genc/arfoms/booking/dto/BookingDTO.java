package com.genc.arfoms.booking.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class BookingDTO {

    private Long id;
    private String flightType;
    private String fromLocation;
    private String toLocation;
    private String departureDate;
    private int passengers;
    private String pnr;
    private String status;
    private String airline;
    private String seat;
    private double fare;
    private String flyDate;

    private List<PassengerDTO> passengerDetails = new ArrayList<>();
}

