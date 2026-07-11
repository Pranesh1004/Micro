package com.genc.arfoms.crew.service;

import com.genc.arfoms.crew.client.FlightClient;
import com.genc.arfoms.crew.model.AssignmentStatus;
import com.genc.arfoms.crew.model.CrewAssignment;
import com.genc.arfoms.crew.repository.CrewAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.genc.arfoms.crew.exception.NoDataFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
public class CrewService {

    private final Logger logger = LoggerFactory.getLogger(CrewService.class);

    // Individual assignment safety threshold (per single assignment)
    private static final BigDecimal MAX_ASSIGNMENT_HOURS = new BigDecimal("12.0");
    // Global multi-flight fatigue limit (cumulative across scheduled assignments)
    private static final BigDecimal MAX_CUMULATIVE_HOURS = new BigDecimal("40.0");

    private final CrewAssignmentRepository repository;
    private final FlightClient flightClient;

    public CrewService(CrewAssignmentRepository repository, FlightClient flightClient) {
        this.repository = repository;
        this.flightClient = flightClient;
    }

    @Transactional
    public CrewAssignment assignCrew(CrewAssignment assignment) {
        Long flightId = assignment.getFlightId();
        logger.info("Attempting to assign crew member: '{}' with role: '{}' to Flight ID: {}", assignment.getCrewMemberName(), assignment.getRole(), flightId);

        // 1. Cross-module guard: verify the flight exists in flight-service before roster manipulation.
        try {
            flightClient.verifyFlightExists(flightId);
            logger.info("Flight ID {} verified successfully via Feign Client.", flightId);
        } catch (feign.FeignException.NotFound e) {
            logger.error("Crew assignment failed: Flight ID {} does not exist.", flightId);
            throw new IllegalArgumentException(
                    "Integration Guard: Flight ID " + flightId + " does not exist in the system.");
        }

        // 2. Per-assignment safety threshold limit.
        BigDecimal incomingHours = assignment.getDutyHours() != null ? assignment.getDutyHours() : BigDecimal.ZERO;
        if (incomingHours.compareTo(MAX_ASSIGNMENT_HOURS) > 0) {
            logger.warn("Crew assignment failed: duty hours {} exceeds maximum single assignment hours {}.", incomingHours, MAX_ASSIGNMENT_HOURS);
            throw new IllegalArgumentException(
                    "Duty hours violate safety rest limits (max 12 hours per assignment).");
        }

        // 3. Global multi-flight fatigue constraint (cumulative scheduled hours).
        BigDecimal currentHours = repository.findByCrewMemberName(assignment.getCrewMemberName()).stream()
                .filter(a -> a.getAssignmentStatus() == AssignmentStatus.SCHEDULED)
                .map(a -> a.getDutyHours() != null ? a.getDutyHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentHours.add(incomingHours).compareTo(MAX_CUMULATIVE_HOURS) > 0) {
            logger.warn("Crew assignment failed: cumulative scheduled hours {} + incoming {} exceeds fatigue limit {}.", currentHours, incomingHours, MAX_CUMULATIVE_HOURS);
            throw new IllegalArgumentException(
                    "Crew member exceeds maximum allowed cumulative duty hours limit (max 40 hours).");
        }

        // 4. Role duplication safety: prevent double booking of an identical operational slot.
        boolean roleTaken = repository.findByFlightId(flightId).stream()
                .anyMatch(a -> a.getRole() == assignment.getRole()
                        && a.getAssignmentStatus() == AssignmentStatus.SCHEDULED);
        if (roleTaken) {
            logger.warn("Crew assignment failed: Role '{}' already assigned for Flight ID {}.", assignment.getRole(), flightId);
            throw new IllegalStateException(
                    "Role " + assignment.getRole() + " is already assigned to Flight ID " + flightId);
        }

        assignment.setAssignmentStatus(AssignmentStatus.SCHEDULED);
        CrewAssignment saved = repository.save(assignment);
        logger.info("Crew member '{}' successfully assigned to Flight ID: {} with role '{}' (Assignment ID: {})", saved.getCrewMemberName(), flightId, saved.getRole(), saved.getAssignmentId());
        return saved;
    }

    @Transactional
    public CrewAssignment swapCrew(Long assignmentId, String newCrewMemberName) {
        logger.info("Attempting crew member swap for assignment ID: {} to new member: '{}'", assignmentId, newCrewMemberName);
        CrewAssignment original = getAssignment(assignmentId);

        original.setAssignmentStatus(AssignmentStatus.SWAPPED);
        repository.save(original);
        logger.info("Original assignment ID {} marked as SWAPPED.", assignmentId);

        CrewAssignment newAssignment = new CrewAssignment();
        newAssignment.setCrewMemberName(newCrewMemberName);
        newAssignment.setFlightId(original.getFlightId());
        newAssignment.setRole(original.getRole());
        newAssignment.setDutyHours(original.getDutyHours());
        newAssignment.setAssignmentStatus(AssignmentStatus.SCHEDULED);
        CrewAssignment saved = repository.save(newAssignment);
        logger.info("Successfully created new assignment ID: {} for '{}' (Flight ID: {}, Role: {})", saved.getAssignmentId(), newCrewMemberName, saved.getFlightId(), saved.getRole());
        return saved;
    }

    @Transactional
    public CrewAssignment recordDutyHours(Long assignmentId, BigDecimal hours) {
        logger.info("Recording duty hours for assignment ID: {} (provided: {})", assignmentId, hours);
        CrewAssignment assignment = getAssignment(assignmentId);

        try {
            // Derive real operational duration from the flight's departure/arrival window.
            FlightClient.FlightView flight = null;
            try {
                flight = flightClient.getFlight(assignment.getFlightId());
                logger.info("Fetched flight details for Flight ID {} via Feign.", assignment.getFlightId());
            } catch (feign.FeignException.NotFound e) {
                logger.warn("Flight ID {} not found during duty hours resolution. Continuing with fallback.", assignment.getFlightId());
                // flight stays null
            }
            if (flight != null && flight.departureTime() != null && flight.arrivalTime() != null) {
                long minutes = Duration.between(flight.departureTime(), flight.arrivalTime()).toMinutes();
                hours = BigDecimal.valueOf(minutes / 60.0).setScale(1, RoundingMode.HALF_UP);
                logger.info("Derived duty hours from flight schedule departure/arrival window: {} hours", hours);
            }
        } catch (Exception ex) {
            logger.warn("Dynamic time tracking failed: {}. Falling back to manual entry.", ex.getMessage());
            // Fallback: use manual user entry if dynamic time tracking fails.
            if (hours == null) {
                throw new IllegalArgumentException(
                        "Dynamic time tracking failed, and no manual hours fallback was provided.");
            }
        }

        if (hours == null) {
            logger.error("Failed to resolve duty hours for assignment ID: {}", assignmentId);
            throw new IllegalArgumentException(
                    "No duty hours could be determined for this assignment.");
        }

        assignment.setDutyHours(hours);
        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        CrewAssignment saved = repository.save(assignment);
        logger.info("Successfully recorded duty hours for assignment ID: {} as {} hours. Status: COMPLETED.", assignmentId, hours);
        return saved;
    }

    @Transactional(readOnly = true)
    public CrewAssignment getAssignment(Long assignmentId) {
        return repository.findById(assignmentId)
                .orElseThrow(() -> {
                    logger.warn("Crew assignment lookup failed: Assignment ID {} not found", assignmentId);
                    return new NoDataFoundException("Assignment ID not found.");
                });
    }

    @Transactional(readOnly = true)
    public List<CrewAssignment> getCrewRoster() {
        logger.info("Fetching complete crew roster.");
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CrewAssignment> getCrewRoster(String crewMemberName) {
        logger.info("Fetching crew roster for crew member: '{}'", crewMemberName);
        return repository.findByCrewMemberName(crewMemberName);
    }

    @Transactional
    public void deleteCrew(Long assignmentId) {
        logger.info("Attempting to delete crew assignment ID: {}", assignmentId);
        if (!repository.existsById(assignmentId)) {
            logger.warn("Crew assignment deletion failed: Assignment ID {} not found.", assignmentId);
            throw new NoDataFoundException(
                    "Crew assignment not found with id: " + assignmentId);
        }
        repository.deleteById(assignmentId);
        logger.info("Successfully deleted crew assignment ID: {}", assignmentId);
    }
}
