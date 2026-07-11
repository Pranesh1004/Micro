package com.genc.arfoms.crew.repository;

import com.genc.arfoms.crew.model.CrewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewAssignmentRepository extends JpaRepository<CrewAssignment, Long> {

    List<CrewAssignment> findByCrewMemberName(String crewMemberName);

    List<CrewAssignment> findByFlightId(Long flightId);
}
