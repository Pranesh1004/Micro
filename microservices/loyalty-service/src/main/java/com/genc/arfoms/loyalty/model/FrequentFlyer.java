package com.genc.arfoms.loyalty.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class FrequentFlyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    private String memberName;
    private Integer milesBalance = 0;

    @Enumerated(EnumType.STRING)
    private MembershipTier membershipTier = MembershipTier.SILVER;

    private LocalDate enrollmentDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public Integer getMilesBalance() {
        return milesBalance;
    }

    public void setMilesBalance(Integer milesBalance) {
        this.milesBalance = milesBalance;
    }

    public MembershipTier getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public MemberStatus getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(MemberStatus memberStatus) {
        this.memberStatus = memberStatus;
    }
}

