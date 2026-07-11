package com.genc.arfoms.flight.dto;

import com.genc.arfoms.flight.model.FlightStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleUpdateRequest {
    private String departureTime;
    private String arrivalTime;
    private FlightStatus flightStatus;
}
