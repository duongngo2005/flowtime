package com.ndd.flowtime_be.planning.dto;

import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PlanningSessionResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        String timezone,
        PlanningSessionStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<PlannedSlotResponse> slots,
        List<PlanningUnscheduledTaskResponse> unscheduledTasks
) {
    public static PlanningSessionResponse from(
            PlanningSession session,
            List<PlannedSlotResponse> slots,
            List<PlanningUnscheduledTaskResponse> unscheduledTasks) {
        return new PlanningSessionResponse(
                session.getId(),
                session.getStartDate(),
                session.getEndDate(),
                session.getTimezone(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                slots,
                unscheduledTasks
        );
    }
}
