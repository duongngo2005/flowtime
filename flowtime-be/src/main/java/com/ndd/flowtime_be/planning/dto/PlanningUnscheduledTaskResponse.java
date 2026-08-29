package com.ndd.flowtime_be.planning.dto;

import com.ndd.flowtime_be.planning.entity.PlanningUnscheduledTask;

public record PlanningUnscheduledTaskResponse(
        Long id,
        Long taskId,
        String taskTitle,
        Integer unscheduledMinutes,
        String reason
) {
    public static PlanningUnscheduledTaskResponse from(PlanningUnscheduledTask task) {
        return new PlanningUnscheduledTaskResponse(
                task.getId(),
                task.getTaskId(),
                task.getTaskTitle(),
                task.getUnscheduledMinutes(),
                task.getReason()
        );
    }
}
