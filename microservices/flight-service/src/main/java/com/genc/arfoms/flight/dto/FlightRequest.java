package com.genc.arfoms.flight.dto;

import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {

   private String flightNumber;
   private String flightName;
   private String origin;
   private String destination;
   private String departureTime;
   private String arrivalTime;
   private Double economyFare;
   private Double premiumFare;
   private Double firstFare;
   private FlightStatus flightStatus;
   private Integer seatCount;
   private Integer seatRows;
   private Integer seatColumns;
   private Integer seatAisleAfter;

   public Flight toEntity() {
       Flight flight = new Flight();
       flight.setFlightNumber(flightNumber);
       flight.setFlightName(flightName);
       flight.setOrigin(origin);
       flight.setDestination(destination);
       flight.setDepartureTime(departureTime);
       flight.setArrivalTime(arrivalTime);
       flight.setEconomyFare(economyFare);
       flight.setPremiumFare(premiumFare);
       flight.setFirstFare(firstFare);
       flight.setSeatCount(seatCount);
       flight.setSeatRows(seatRows);
       flight.setSeatColumns(seatColumns);
       flight.setSeatAisleAfter(seatAisleAfter);
       flight.setFlightStatus(flightStatus != null ? flightStatus : FlightStatus.SCHEDULED);
       return flight;
   }
}

