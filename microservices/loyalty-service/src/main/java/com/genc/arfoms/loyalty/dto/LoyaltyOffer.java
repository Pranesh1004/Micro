package com.genc.arfoms.loyalty.dto;

import com.genc.arfoms.loyalty.model.MembershipTier;

/**
 * A single promotional offer the loyalty manager extends to a member based on
 * the distance flown (in miles) and their membership tier.
 */
public record LoyaltyOffer(
        String code,
        String title,
        String description,
        int bonusMiles,
        int discountPercent,
        MembershipTier eligibleTier) {
}

