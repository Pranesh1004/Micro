package com.genc.arfoms.loyalty.service;

import com.genc.arfoms.loyalty.dto.LoyaltyFlightCreditResult;
import com.genc.arfoms.loyalty.dto.LoyaltyOffer;
import com.genc.arfoms.loyalty.dto.LoyaltyOffersResponse;
import com.genc.arfoms.loyalty.model.FrequentFlyer;
import com.genc.arfoms.loyalty.model.MemberStatus;
import com.genc.arfoms.loyalty.model.MembershipTier;
import com.genc.arfoms.loyalty.repository.FrequentFlyerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.genc.arfoms.loyalty.exception.NoDataFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class  LoyaltyService {

    private final Logger logger = LoggerFactory.getLogger(LoyaltyService.class);
    private final FrequentFlyerRepository repository;

    public LoyaltyService(FrequentFlyerRepository repository) {
        this.repository = repository;
    }

    public FrequentFlyer enrollFrequentFlyer(FrequentFlyer member) {
        logger.info("Enrolling new frequent flyer member: {}", member.getMemberName());
        member.setMilesBalance(0);
        member.setMembershipTier(MembershipTier.SILVER);
        FrequentFlyer enrolled = repository.save(member);
        logger.info("Successfully enrolled frequent flyer member. Assigned ID: {}", enrolled.getMemberId());
        return enrolled;
    }

    public FrequentFlyer creditMiles(Long memberId, int miles) {
        logger.info("Crediting {} miles to member ID: {}", miles, memberId);
        FrequentFlyer member = getMember(memberId);
        int oldBalance = member.getMilesBalance() != null ? member.getMilesBalance() : 0;
        member.setMilesBalance(oldBalance + miles);
        applyTier(member);
        FrequentFlyer saved = repository.save(member);
        logger.info("Miles credited. New balance: {}. Tier: {}", saved.getMilesBalance(), saved.getMembershipTier());
        return saved;
    }

    public FrequentFlyer redeemMiles(Long memberId, int miles) {
        logger.info("Attempting to redeem {} miles from member ID: {}", miles, memberId);
        FrequentFlyer member = getMember(memberId);
        int currentBalance = member.getMilesBalance() != null ? member.getMilesBalance() : 0;
        if (currentBalance < miles) {
            logger.warn("Redemption failed: Insufficient miles balance ({}) for member ID: {}", currentBalance, memberId);
            throw new IllegalArgumentException("Insufficient miles");
        }
        member.setMilesBalance(currentBalance - miles);
        applyTier(member);
        FrequentFlyer saved = repository.save(member);
        logger.info("Redemption successful. New balance: {}. Tier: {}", saved.getMilesBalance(), saved.getMembershipTier());
        return saved;
    }

    public FrequentFlyer upgradeTier(Long memberId) {
        logger.info("Manual tier upgrade trigger for member ID: {}", memberId);
        FrequentFlyer member = getMember(memberId);
        MembershipTier oldTier = member.getMembershipTier();
        applyTier(member);
        FrequentFlyer saved = repository.save(member);
        logger.info("Tier update applied. Old Tier: {}, New Tier: {}", oldTier, saved.getMembershipTier());
        return saved;
    }

    public FrequentFlyer getMember(Long memberId) {
        return repository.findById(memberId)
                .orElseThrow(() -> {
                    logger.warn("Member lookup failed: Member ID {} not found", memberId);
                    return new NoDataFoundException("Member not found");
                });
    }

    public List<FrequentFlyer> getAll() {
        logger.info("Fetching all frequent flyer members.");
        return repository.findAll();
    }

    /**
     * Credits loyalty miles for a completed flight (1 mile per distance-mile flown).
     * Self-contained: distance is supplied by the caller, so loyalty stays decoupled
     * from the Booking/Flight modules. Inactive accounts cannot earn miles.
     */
    public LoyaltyFlightCreditResult creditMilesForCompletedFlight(Long memberId, Long bookingId, String passengerName, double distanceMiles) {
        logger.info("Crediting miles for completed flight: Member: {}, Booking: {}, Distance: {}", memberId, bookingId, distanceMiles);
        FrequentFlyer member = getMember(memberId);
        if (member.getMemberStatus() == MemberStatus.INACTIVE) {
            logger.warn("Miles credit failed: Account is INACTIVE for member ID: {}", memberId);
            throw new IllegalArgumentException("Cannot credit miles to an inactive account");
        }
        int milesToAward = (int) Math.floor(Math.max(0, distanceMiles));
        int previousBalance = member.getMilesBalance() != null ? member.getMilesBalance() : 0;
        member.setMilesBalance(previousBalance + milesToAward);
        applyTier(member);
        FrequentFlyer saved = repository.save(member);
        logger.info("Successfully credited {} miles to member ID: {}. New balance: {}, New tier: {}", milesToAward, memberId, saved.getMilesBalance(), saved.getMembershipTier());
        return new LoyaltyFlightCreditResult(
                saved.getMemberId(), bookingId, passengerName, milesToAward, saved.getMilesBalance(), saved.getMembershipTier());
    }

    /**
     * Generates the set of offers the loyalty manager extends to a member based
     * on the distance flown (in miles). Longer journeys unlock richer offers,
     * and higher membership tiers receive an additional bonus-miles multiplier.
     *
     * @param memberId      optional member id; when supplied the member's tier
     *                      is used, otherwise SILVER (entry tier) is assumed
     * @param distanceMiles the distance of the flight in miles
     */
    public LoyaltyOffersResponse generateOffers(Long memberId, double distanceMiles) {
        if (distanceMiles < 0) {
            logger.error("Failed to generate offers: Negative distance: {}", distanceMiles);
            throw new IllegalArgumentException("Distance must not be negative");
        }

        MembershipTier tier = MembershipTier.SILVER;
        if (memberId != null) {
            tier = getMember(memberId).getMembershipTier();
        }

        logger.info("Generating offers for member ID: {} (Tier: {}) for distance: {} miles", memberId, tier, distanceMiles);
        int baseMiles = (int) Math.floor(distanceMiles);
        double tierMultiplier = tierBonusMultiplier(tier);
        List<LoyaltyOffer> offers = new ArrayList<>();

        // Every qualifying flight earns base miles.
        offers.add(new LoyaltyOffer(
                "BASE_MILES",
                "Earn flight miles",
                "Earn " + baseMiles + " miles for this " + baseMiles + "-mile journey.",
                baseMiles, 0, tier));

        // Distance-banded promotional offers.
        if (distanceMiles >= 6000) {
            int bonus = (int) Math.round(baseMiles * 0.50 * tierMultiplier);
            offers.add(new LoyaltyOffer("ULTRA_LONG_HAUL", "Ultra long-haul reward",
                    "50% bonus miles plus a complimentary companion award ticket on routes over 6000 miles.",
                    bonus, 15, tier));
            offers.add(new LoyaltyOffer("LOUNGE_PASS", "Premium lounge access",
                    "Two complimentary international lounge passes for your ultra long-haul flight.",
                    0, 0, tier));
        } else if (distanceMiles >= 2500) {
            int bonus = (int) Math.round(baseMiles * 0.30 * tierMultiplier);
            offers.add(new LoyaltyOffer("LONG_HAUL", "Long-haul bonus",
                    "30% bonus miles and a free seat upgrade coupon on routes over 2500 miles.",
                    bonus, 10, tier));
        } else if (distanceMiles >= 700) {
            int bonus = (int) Math.round(baseMiles * 0.20 * tierMultiplier);
            offers.add(new LoyaltyOffer("MEDIUM_HAUL", "Double-miles weekend",
                    "20% bonus miles on medium-haul routes over 700 miles.",
                    bonus, 5, tier));
        } else if (distanceMiles > 0) {
            int bonus = (int) Math.round(baseMiles * 0.10 * tierMultiplier);
            offers.add(new LoyaltyOffer("SHORT_HAUL", "Short-hop bonus",
                    "10% bonus miles on short-haul flights.",
                    bonus, 0, tier));
        }

        // Tier-exclusive perks.
        if (tier == MembershipTier.GOLD || tier == MembershipTier.PLATINUM) {
            offers.add(new LoyaltyOffer("PRIORITY_BOARDING", "Priority boarding",
                    "Complimentary priority boarding for " + tier + " members.", 0, 0, tier));
        }
        if (tier == MembershipTier.PLATINUM) {
            offers.add(new LoyaltyOffer("PLATINUM_CASHBACK", "Platinum fare cashback",
                    "An extra 10% fare discount exclusive to PLATINUM members.", 0, 10, tier));
        }

        logger.info("Generated {} offers for member ID: {}", offers.size(), memberId);
        return new LoyaltyOffersResponse(memberId, distanceMiles, baseMiles, tier, offers);
    }

    private double tierBonusMultiplier(MembershipTier tier) {
        return switch (tier) {
            case PLATINUM -> 1.5;
            case GOLD -> 1.2;
            case SILVER -> 1.0;
        };
    }

    private void applyTier(FrequentFlyer member) {
        int miles = member.getMilesBalance() != null ? member.getMilesBalance() : 0;
        MembershipTier oldTier = member.getMembershipTier();
        if (miles >= 50000) {
            member.setMembershipTier(MembershipTier.PLATINUM);
        } else if (miles >= 25000) {
            member.setMembershipTier(MembershipTier.GOLD);
        } else {
            member.setMembershipTier(MembershipTier.SILVER);
        }
        if (oldTier != member.getMembershipTier()) {
            logger.info("Member ID {} tier updated from {} to {}", member.getMemberId(), oldTier, member.getMembershipTier());
        }
    }
}

