package com.genc.arfoms.flight.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Computes the great-circle distance (in statute miles) between two airports
 * using the Haversine formula. Airport coordinates are held in a small,
 * self-contained registry keyed by IATA code so the flight service does not
 * depend on any external data source.
 */
@Service
public class AirportDistanceService {

    /** Mean radius of the Earth in miles. */
    private static final double EARTH_RADIUS_MILES = 3958.7613;

    /** IATA code -> { latitude, longitude } in decimal degrees. */
    private static final Map<String, double[]> AIRPORTS = Map.ofEntries(
            // --- India ---
            Map.entry("DEL", new double[]{28.5562, 77.1000}),  // Delhi
            Map.entry("BOM", new double[]{19.0896, 72.8656}),  // Mumbai
            Map.entry("BLR", new double[]{13.1986, 77.7066}),  // Bengaluru
            Map.entry("MAA", new double[]{12.9941, 80.1709}),  // Chennai
            Map.entry("HYD", new double[]{17.2403, 78.4294}),  // Hyderabad
            Map.entry("CCU", new double[]{22.6547, 88.4467}),  // Kolkata
            Map.entry("COK", new double[]{10.1520, 76.4019}),  // Kochi
            Map.entry("GOI", new double[]{15.3808, 73.8314}),  // Goa
            Map.entry("PNQ", new double[]{18.5793, 73.9089}),  // Pune
            Map.entry("AMD", new double[]{23.0772, 72.6347}),  // Ahmedabad
            // --- North America ---
            Map.entry("JFK", new double[]{40.6413, -73.7781}), // New York
            Map.entry("LAX", new double[]{33.9416, -118.4085}),// Los Angeles
            Map.entry("ORD", new double[]{41.9742, -87.9073}), // Chicago
            Map.entry("ATL", new double[]{33.6407, -84.4277}), // Atlanta
            Map.entry("DFW", new double[]{32.8998, -97.0403}), // Dallas
            Map.entry("SFO", new double[]{37.6213, -122.3790}),// San Francisco
            Map.entry("SEA", new double[]{47.4502, -122.3088}),// Seattle
            Map.entry("MIA", new double[]{25.7959, -80.2870}), // Miami
            Map.entry("BOS", new double[]{42.3656, -71.0096}), // Boston
            Map.entry("YYZ", new double[]{43.6777, -79.6248}), // Toronto
            // --- Europe ---
            Map.entry("LHR", new double[]{51.4700, -0.4543}),  // London Heathrow
            Map.entry("CDG", new double[]{49.0097, 2.5479}),   // Paris
            Map.entry("FRA", new double[]{50.0379, 8.5622}),   // Frankfurt
            Map.entry("AMS", new double[]{52.3105, 4.7683}),   // Amsterdam
            Map.entry("MAD", new double[]{40.4839, -3.5680}),  // Madrid
            Map.entry("FCO", new double[]{41.8003, 12.2389}),  // Rome
            // --- Middle East / Asia / Oceania ---
            Map.entry("DXB", new double[]{25.2532, 55.3657}),  // Dubai
            Map.entry("DOH", new double[]{25.2731, 51.6080}),  // Doha
            Map.entry("SIN", new double[]{1.3644, 103.9915}),  // Singapore
            Map.entry("HKG", new double[]{22.3080, 113.9185}), // Hong Kong
            Map.entry("NRT", new double[]{35.7720, 140.3929}), // Tokyo Narita
            Map.entry("ICN", new double[]{37.4602, 126.4407}), // Seoul
            Map.entry("SYD", new double[]{-33.9399, 151.1753}) // Sydney
    );

    /**
     * Returns the distance between two airports in miles, rounded to one decimal.
     *
     * @param origin      origin IATA code (case-insensitive)
     * @param destination destination IATA code (case-insensitive)
     */
    public double distanceMiles(String origin, String destination) {
        double[] from = coordinatesFor(origin);
        double[] to = coordinatesFor(destination);

        double lat1 = Math.toRadians(from[0]);
        double lat2 = Math.toRadians(to[0]);
        double deltaLat = Math.toRadians(to[0] - from[0]);
        double deltaLon = Math.toRadians(to[1] - from[1]);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double miles = EARTH_RADIUS_MILES * c;
        return Math.round(miles * 10.0) / 10.0;
    }

    public boolean isKnownAirport(String code) {
        return code != null && AIRPORTS.containsKey(code.trim().toUpperCase());
    }

    private double[] coordinatesFor(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Airport code must not be null");
        }
        double[] coords = AIRPORTS.get(code.trim().toUpperCase());
        if (coords == null) {
            throw new IllegalArgumentException(
                    "Unknown airport code '" + code + "'. Coordinates are not available.");
        }
        return coords;
    }
}

