package com.genc.arfoms.loyalty.controller;

import com.genc.arfoms.loyalty.client.FlightDistanceClient;
import com.genc.arfoms.loyalty.dto.LoyaltyFlightCreditRequest;
import com.genc.arfoms.loyalty.dto.LoyaltyFlightCreditResult;
import com.genc.arfoms.loyalty.dto.LoyaltyOffersResponse;
import com.genc.arfoms.loyalty.model.FrequentFlyer;
import com.genc.arfoms.loyalty.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final Logger logger = LoggerFactory.getLogger(LoyaltyController.class);
    private final LoyaltyService loyaltyService;
    private final FlightDistanceClient flightDistanceClient;

    public LoyaltyController(LoyaltyService loyaltyService, FlightDistanceClient flightDistanceClient) {
        this.loyaltyService = loyaltyService;
        this.flightDistanceClient = flightDistanceClient;
    }

    @PostMapping("/enroll")
    public FrequentFlyer enrollFrequentFlyer(@RequestBody FrequentFlyer member) {
        logger.info("Received request to enroll member: {}", member);
        return loyaltyService.enrollFrequentFlyer(member);
    }

    @PatchMapping("/{memberId}/credit")
    public FrequentFlyer creditMiles(@PathVariable Long memberId, @RequestBody MilesRequest request) {
        logger.info("Received request to credit {} miles to member ID: {}", request.miles(), memberId);
        return loyaltyService.creditMiles(memberId, request.miles());
    }

    @PatchMapping("/{memberId}/redeem")
    public FrequentFlyer redeemMiles(@PathVariable Long memberId, @RequestBody MilesRequest request) {
        logger.info("Received request to redeem {} miles from member ID: {}", request.miles(), memberId);
        return loyaltyService.redeemMiles(memberId, request.miles());
    }

    @PatchMapping("/{memberId}/tier")
    public FrequentFlyer upgradeTier(@PathVariable Long memberId) {
        logger.info("Received request to upgrade tier for member ID: {}", memberId);
        return loyaltyService.upgradeTier(memberId);
    }

    @GetMapping("/{memberId}")
    public FrequentFlyer getMember(@PathVariable Long memberId) {
        logger.info("Received request to fetch member ID: {}", memberId);
        return loyaltyService.getMember(memberId);
    }

    @GetMapping
    public List<FrequentFlyer> getAllMembers() {
        logger.info("Received request to fetch all members");
        return loyaltyService.getAll();
    }

    // ------------------------------------------------------------------
    // Loyalty Manager portal endpoints (param-based) - additive, the
    // existing endpoints above are left untouched so nothing else changes.
    // ------------------------------------------------------------------

    @GetMapping("/member/{memberId}")
    public FrequentFlyer getMemberForPortal(@PathVariable Long memberId) {
        logger.info("Received request to fetch member ID: {} (portal)", memberId);
        return loyaltyService.getMember(memberId);
    }

    @GetMapping("/members")
    public List<FrequentFlyer> getMembersForPortal() {
        logger.info("Received request to fetch all members (portal)");
        return loyaltyService.getAll();
    }

    @PostMapping("/credit")
    public FrequentFlyer creditMilesByParam(@RequestParam Long memberId, @RequestParam int miles) {
        logger.info("Received request to credit {} miles to member ID: {} (param)", miles, memberId);
        return loyaltyService.creditMiles(memberId, miles);
    }

    @PostMapping("/redeem")
    public FrequentFlyer redeemMilesByParam(@RequestParam Long memberId, @RequestParam int miles) {
        logger.info("Received request to redeem {} miles from member ID: {} (param)", miles, memberId);
        return loyaltyService.redeemMiles(memberId, miles);
    }

    @PostMapping("/credit-flight")
    public LoyaltyFlightCreditResult creditForCompletedFlight(@RequestBody LoyaltyFlightCreditRequest request) {
        logger.info("Received request to credit miles for completed flight: member ID {}, booking ID {}, passenger '{}', distance: {} miles", request.memberId(), request.bookingId(), request.passengerName(), request.distanceMiles());
        return loyaltyService.creditMilesForCompletedFlight(
                request.memberId(), request.bookingId(), request.passengerName(), request.distanceMiles());
    }

    // ------------------------------------------------------------------
    // Distance-based offers: the loyalty manager tailors offers to how far
    // a member has flown (in miles).
    // ------------------------------------------------------------------

    /** Generate offers directly from a known distance in miles. */
    @GetMapping("/offers")
    public LoyaltyOffersResponse getOffers(@RequestParam double distanceMiles,
                                           @RequestParam(required = false) Long memberId) {
        logger.info("Received request to get offers for member ID: {} based on distance: {} miles", memberId, distanceMiles);
        return loyaltyService.generateOffers(memberId, distanceMiles);
    }

    /**
     * Generate offers for a flight: the distance in miles is fetched from the
     * flight-service (which computes it from the airport coordinates) and then
     * used to build the offers.
     */
    @GetMapping("/offers/by-flight")
    public LoyaltyOffersResponse getOffersForFlight(@RequestParam Long flightId,
                                                    @RequestParam(required = false) Long memberId) {
        logger.info("Received request to get offers for member ID: {} based on Flight ID: {}", memberId, flightId);
        double distanceMiles = 0;
        try {
            distanceMiles = flightDistanceClient.distanceForFlight(flightId).distanceMiles();
            logger.info("Fetched flight distance via Feign client: {} miles for Flight ID {}", distanceMiles, flightId);
        } catch (feign.FeignException e) {
            logger.error("Failed to fetch distance for Flight ID {} from flight service.", flightId);
            throw new IllegalStateException(
                "Flight service returned no distance or failed for flight " + flightId
            );
        }
        return loyaltyService.generateOffers(memberId, distanceMiles);
    }

    public record MilesRequest(int miles) {
    }
}

