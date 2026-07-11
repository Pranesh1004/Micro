package com.genc.arfoms.crew.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
public class CrewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @NotBlank(message = "Crew member name cannot be blank.")
    @Size(max = 100, message = "Crew member name must not exceed 100 characters.")
    private String crewMemberName;

    @NotNull(message = "Flight id is required.")
    private Long flightId;

    @NotNull(message = "Operational role is required.")
    @Enumerated(EnumType.STRING)
    private CrewRole role;

    @DecimalMin(value = "0.0", message = "Duty hours cannot be negative.")
    @DecimalMax(value = "24.0", message = "Duty hours cannot exceed 24 hours.")
    private BigDecimal dutyHours;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus assignmentStatus = AssignmentStatus.SCHEDULED;

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getCrewMemberName() {
        return crewMemberName;
    }

    public void setCrewMemberName(String crewMemberName) {
        this.crewMemberName = crewMemberName;
    }

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
    }

    public CrewRole getRole() {
        return role;
    }

    public void setRole(CrewRole role) {
        this.role = role;
    }

    public BigDecimal getDutyHours() {
        return dutyHours;
    }

    public void setDutyHours(BigDecimal dutyHours) {
        this.dutyHours = dutyHours;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }
}
