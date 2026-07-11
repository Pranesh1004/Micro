package com.genc.arfoms.loyalty.dto;

import com.genc.arfoms.loyalty.model.MembershipTier;

/**
 * Result of crediting loyalty miles for a completed flight.
 */
public record LoyaltyFlightCreditResult(
        Long memberId,
        Long bookingId,
        String passengerName,
        int milesAwarded,
        int newBalance,
        MembershipTier membershipTier) {
}

