package com.genc.arfoms.flight.model;

import com.genc.arfoms.flight.model.IndianAirports.Airport;

import java.util.List;

public final class InternationalAirports {

    private InternationalAirports() {}

    public static final List<Airport> AIRPORTS = List.of(
            new Airport("AMS", "Amsterdam (Schiphol)"),
            new Airport("AUH", "Abu Dhabi"),
            new Airport("BKK", "Bangkok"),
            new Airport("CDG", "Paris (Charles de Gaulle)"),
            new Airport("DOH", "Doha"),
            new Airport("DXB", "Dubai"),
            new Airport("FCO", "Rome (Fiumicino)"),
            new Airport("FRA", "Frankfurt"),
            new Airport("HKG", "Hong Kong"),
            new Airport("HND", "Tokyo (Haneda)"),
            new Airport("ICN", "Seoul (Incheon)"),
            new Airport("IST", "Istanbul"),
            new Airport("JFK", "New York (JFK)"),
            new Airport("KUL", "Kuala Lumpur"),
            new Airport("LAX", "Los Angeles"),
            new Airport("LHR", "London Heathrow"),
            new Airport("MAD", "Madrid"),
            new Airport("MEL", "Melbourne"),
            new Airport("NRT", "Tokyo (Narita)"),
            new Airport("ORD", "Chicago (O'Hare)"),
            new Airport("SFO", "San Francisco"),
            new Airport("SIN", "Singapore (Changi)"),
            new Airport("SYD", "Sydney"),
            new Airport("YVR", "Vancouver"),
            new Airport("YYZ", "Toronto (Pearson)"),
            new Airport("ZRH", "Zurich")
    );
}

