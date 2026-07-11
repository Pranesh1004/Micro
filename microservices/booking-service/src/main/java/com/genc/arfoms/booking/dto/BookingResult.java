package com.genc.arfoms.booking.dto;

public class BookingResult {
    private final boolean success;
    private final String message;
    private final Integer bookingId;
    private final String seatNumber;
    private final String totalAmount;

    public BookingResult(boolean success, String message, Integer bookingId, String seatNumber, String totalAmount) {
        this.success = success;
        this.message = message;
        this.bookingId = bookingId;
        this.seatNumber = seatNumber;
        this.totalAmount = totalAmount;
    }

    public static BookingResult success(Integer bookingId, String seatNumber, String totalAmount) {
        return new BookingResult(true, "Success", bookingId, seatNumber, totalAmount);
    }

    public static BookingResult failure(String message) {
        return new BookingResult(false, message, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getTotalAmount() {
        return totalAmount;
    }
}
