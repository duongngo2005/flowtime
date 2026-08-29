package com.ndd.flowtime_be.planning.repository;

import com.ndd.flowtime_be.planning.entity.PlanningUnscheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanningUnscheduledTaskRepository extends JpaRepository<PlanningUnscheduledTask, Long> {

    List<PlanningUnscheduledTask> findByPlanningSessionIdOrderByIdAsc(Long planningSessionId);
}
