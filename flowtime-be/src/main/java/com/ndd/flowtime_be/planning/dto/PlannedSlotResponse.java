package com.ndd.flowtime_be.planning.dto;

import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;

import java.time.Instant;

public record PlannedSlotResponse(
        Long id,
        Long taskId,
        String taskTitle,
        Instant startAt,
        Instant endAt,
        Integer durationMinutes,
        PlannedSlotStatus status
) {
    public static PlannedSlotResponse from(PlannedSlot slot) {
        return new PlannedSlotResponse(
                slot.getId(),
                slot.getTaskId(),
                slot.getTaskTitle(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getDurationMinutes(),
                slot.getStatus()
        );
    }
}
