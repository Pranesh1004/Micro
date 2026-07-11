package com.genc.arfoms.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ground-staff lookup result: tells which passenger(s) belong to a booking and
 * which flight they are travelling on. Assembled read-only from the shared
 * {@code bookings}, {@code passenger} and {@code flights} tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerFlightInfo {

    private Long bookingId;
    private String pnr;

    /** Primary passenger name (first on the booking). */
    private String passengerName;
    /** All passenger names on the booking. */
    private List<String> passengerNames;

    private Long flightId;
    private String flightNumber;
    private String flightName;

    private String origin;
    private String destination;
    private String route;

    private String seat;
    /** Travel class / fare type from the booking (e.g. ECONOMY, BUSINESS). */
    private String travelClass;

    /** Current check-in status for ground staff ("Not Checked In" if none yet). */
    private String status;
}

