package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.model.Booking;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class MockPaymentService {

    private static final char[] ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public MockPaymentResult processBookingPayment(Booking booking) {
        return new MockPaymentResult(generatePnr(), "CONFIRMED");
    }

    public String generatePnr() {
        return generateToken(8);
    }

    private String generateToken(int length) {
        StringBuilder token = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            token.append(ALPHANUMERIC[RANDOM.nextInt(ALPHANUMERIC.length)]);
        }
        return token.toString();
    }

    public record MockPaymentResult(String pnr, String bookingStatus) {
    }
}