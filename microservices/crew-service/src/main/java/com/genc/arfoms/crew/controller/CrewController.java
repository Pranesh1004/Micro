package com.genc.arfoms.crew.controller;

import com.genc.arfoms.crew.model.CrewAssignment;
import com.genc.arfoms.crew.service.CrewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
@CrossOrigin("*")
@RestController
@RequestMapping("/api/crew")
public class CrewController {

    private final Logger logger = LoggerFactory.getLogger(CrewController.class);
    private final CrewService crewService;

    public CrewController(CrewService crewService) {
        this.crewService = crewService;
    }

    @PostMapping("/assign")
    public CrewAssignment assignCrew(@Valid @RequestBody CrewAssignment assignment) {
        logger.info("Received request to assign crew: {}", assignment);
        return crewService.assignCrew(assignment);
    }

    @PatchMapping("/{assignmentId}/swap")
    public CrewAssignment swapCrew(@PathVariable Long assignmentId, @RequestBody SwapCrewRequest request) {
        logger.info("Received request to swap crew member to '{}' for assignment ID: {}", request.crewMemberName(), assignmentId);
        return crewService.swapCrew(assignmentId, request.crewMemberName());
    }

    @PatchMapping("/{assignmentId}/duty-hours")
    public CrewAssignment recordDutyHours(@PathVariable Long assignmentId, @RequestBody DutyHoursRequest request) {
        logger.info("Received request to record duty hours '{}' for assignment ID: {}", request.dutyHours(), assignmentId);
        return crewService.recordDutyHours(assignmentId, request.dutyHours());
    }

    @GetMapping("/{assignmentId}")
    public CrewAssignment getAssignment(@PathVariable Long assignmentId) {
        logger.info("Received request to fetch assignment ID: {}", assignmentId);
        return crewService.getAssignment(assignmentId);
    }

    @GetMapping("/roster")
    public List<CrewAssignment> getCrewRoster() {
        logger.info("Received request to fetch all rosters");
        return crewService.getCrewRoster();
    }

    @GetMapping("/roster/{crewMemberName}")
    public List<CrewAssignment> getCrewRosterByName(@PathVariable String crewMemberName) {
        logger.info("Received request to fetch roster for crew member: '{}'", crewMemberName);
        return crewService.getCrewRoster(crewMemberName);
    }

    public record SwapCrewRequest(String crewMemberName) {
    }

    public record DutyHoursRequest(BigDecimal dutyHours) {
    }
}

