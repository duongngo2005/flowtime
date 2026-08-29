package com.ndd.flowtime_be.planning.repository;

import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlannedSlotRepository extends JpaRepository<PlannedSlot, Long> {

    List<PlannedSlot> findByPlanningSessionIdOrderByStartAtAsc(Long planningSessionId);

    Optional<PlannedSlot> findByIdAndPlanningSessionId(Long id, Long planningSessionId);
}
