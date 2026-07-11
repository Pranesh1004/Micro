package com.genc.arfoms.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FareUpdateRequest {
    private double economyFare;
    private double premiumFare;
    private double firstFare;
}
