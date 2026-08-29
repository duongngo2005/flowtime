package com.ndd.flowtime_be.planning.dto;

import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotApplyStatus;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;

import java.time.Instant;

public record PlannedSlotResponse(
        Long id,
        Long taskId,
        String taskTitle,
        Instant startAt,
        Instant endAt,
        Integer durationMinutes,
        PlannedSlotStatus status,
        String googleCalendarId,
        String googleEventId,
        PlannedSlotApplyStatus applyStatus,
        String applyError,
        Instant appliedAt
) {
    public static PlannedSlotResponse from(PlannedSlot slot) {
        return new PlannedSlotResponse(
                slot.getId(),
                slot.getTaskId(),
                slot.getTaskTitle(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getDurationMinutes(),
                slot.getStatus(),
                slot.getGoogleCalendarId(),
                slot.getGoogleEventId(),
                slot.getApplyStatus(),
                slot.getApplyError(),
                slot.getAppliedAt()
        );
    }
}
