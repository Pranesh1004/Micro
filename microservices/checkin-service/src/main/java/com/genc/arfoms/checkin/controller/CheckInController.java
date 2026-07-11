package com.genc.arfoms.checkin.controller;

import com.genc.arfoms.checkin.dto.BoardingPassView;
import com.genc.arfoms.checkin.dto.BookingLookupResult;
import com.genc.arfoms.checkin.dto.CheckInDetailsView;
import com.genc.arfoms.checkin.model.CheckIn;
import com.genc.arfoms.checkin.service.CheckInService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkin")
public class CheckInController {

    private final Logger logger = LoggerFactory.getLogger(CheckInController.class);
    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /** Live booking + flight lookup used by the check-in screen before check-in. */
    @GetMapping("/booking/{bookingId}")
    public BookingLookupResult lookupBooking(@PathVariable Long bookingId) {
        logger.info("Received request to look up booking ID: {}", bookingId);
        return checkInService.lookupBooking(bookingId);
    }

    /** Enriched registry (passenger names + routes pulled from the DB). */
    @GetMapping("/details")
    public List<CheckInDetailsView> getAllDetails() {
        logger.info("Received request to fetch all check-in details");
        return checkInService.getAllDetails();
    }

    @PostMapping
    public CheckIn checkInPassenger(@RequestBody CheckIn checkIn) {
        logger.info("Received request to check in passenger for booking ID: {}", checkIn.getBookingId());
        return checkInService.checkInPassenger(checkIn);
    }

    @PatchMapping("/{checkInId}/board")
    public CheckIn boardPassenger(@PathVariable Long checkInId) {
        logger.info("Received request to board passenger for check-in ID: {}", checkInId);
        return checkInService.boardPassenger(checkInId);
    }

    /** Issues a boarding pass (assembled from live booking + flight data). */
    @GetMapping("/{checkInId}/boarding-pass")
    public BoardingPassView issueBoardingPass(@PathVariable Long checkInId) {
        logger.info("Received request to issue boarding pass for check-in ID: {}", checkInId);
        return checkInService.issueBoardingPass(checkInId);
    }

    @PatchMapping("/{checkInId}/baggage")
    public CheckIn tagBaggage(@PathVariable Long checkInId, @RequestBody TagBaggageRequest request) {
        logger.info("Received request to tag {} bags for check-in ID: {}", request.baggageCount(), checkInId);
        return checkInService.tagBaggage(checkInId, request.baggageCount());
    }

    @GetMapping("/{checkInId}")
    public CheckIn getCheckIn(@PathVariable Long checkInId) {
        logger.info("Received request to fetch check-in ID: {}", checkInId);
        return checkInService.getCheckIn(checkInId);
    }

    @GetMapping
    public List<CheckIn> getAll() {
        logger.info("Received request to fetch all check-ins");
        return checkInService.getAll();
    }

    public record TagBaggageRequest(int baggageCount) {
    }
}

