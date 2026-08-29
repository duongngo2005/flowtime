package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.planning.dto.PlannedSlotResponse;
import com.ndd.flowtime_be.planning.dto.PlanningSessionResponse;
import com.ndd.flowtime_be.planning.dto.PlanningUnscheduledTaskResponse;
import com.ndd.flowtime_be.planning.entity.*;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.planning.repository.PlanningSessionRepository;
import com.ndd.flowtime_be.planning.repository.PlanningUnscheduledTaskRepository;
import com.ndd.flowtime_be.scheduling.dto.ScheduledBlockSuggestion;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewRequest;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewResponse;
import com.ndd.flowtime_be.scheduling.dto.UnscheduledTaskSuggestion;
import com.ndd.flowtime_be.scheduling.service.SchedulingEngine;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private final SchedulingEngine schedulingEngine;
    private final PlanningSessionRepository planningSessionRepository;
    private final PlannedSlotRepository plannedSlotRepository;
    private final PlanningUnscheduledTaskRepository unscheduledTaskRepository;

    @Transactional
    public PlanningSessionResponse createDraft(User user, SchedulingPreviewRequest request) {
        SchedulingPreviewResponse preview = schedulingEngine.preview(user, request);
        PlanningSession session = PlanningSession.builder()
                .user(user)
                .startDate(preview.startDate())
                .endDate(preview.endDate())
                .timezone(preview.timezone())
                .status(PlanningSessionStatus.DRAFT)
                .build();
        PlanningSession savedSession = planningSessionRepository.save(session);

        List<PlannedSlot> slots = preview.suggestions().stream()
                .map(suggestion -> plannedSlot(savedSession, suggestion))
                .toList();
        List<PlanningUnscheduledTask> unscheduledTasks = preview.unscheduledTasks().stream()
                .map(suggestion -> unscheduledTask(savedSession, suggestion))
                .toList();
        List<PlannedSlot> savedSlots = plannedSlotRepository.saveAll(slots);
        List<PlanningUnscheduledTask> savedUnscheduledTasks = unscheduledTaskRepository.saveAll(unscheduledTasks);
        return response(savedSession, savedSlots, savedUnscheduledTasks);
    }

    @Transactional(readOnly = true)
    public PlanningSessionResponse get(User user, Long planningId) {
        PlanningSession session = findOwnedSession(user, planningId);
        return response(
                session,
                plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(session.getId()),
                unscheduledTaskRepository.findByPlanningSessionIdOrderByIdAsc(session.getId())
        );
    }

    @Transactional
    public PlanningSessionResponse removeSlot(User user, Long planningId, Long slotId) {
        PlanningSession session = findOwnedSession(user, planningId);
        requireDraft(session);
        PlannedSlot slot = plannedSlotRepository.findByIdAndPlanningSessionId(slotId, session.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planned slot not found."));
        slot.setStatus(PlannedSlotStatus.REMOVED);
        plannedSlotRepository.save(slot);
        return get(user, planningId);
    }

    @Transactional
    public PlanningSessionResponse approve(User user, Long planningId) {
        PlanningSession session = findOwnedSession(user, planningId);
        requireDraft(session);
        List<PlannedSlot> slots = plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(session.getId());
        slots.stream()
                .filter(slot -> slot.getStatus() == PlannedSlotStatus.PROPOSED)
                .forEach(slot -> {
                    slot.setStatus(PlannedSlotStatus.ACCEPTED);
                    slot.setApplyStatus(PlannedSlotApplyStatus.PENDING);
                });
        plannedSlotRepository.saveAll(slots);
        session.setStatus(PlanningSessionStatus.APPROVED);
        planningSessionRepository.save(session);
        return response(
                session,
                slots,
                unscheduledTaskRepository.findByPlanningSessionIdOrderByIdAsc(session.getId())
        );
    }

    @Transactional
    public PlanningSessionResponse cancel(User user, Long planningId) {
        PlanningSession session = findOwnedSession(user, planningId);
        if (session.getStatus() == PlanningSessionStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Applied planning sessions cannot be cancelled.");
        }
        if (session.getStatus() != PlanningSessionStatus.CANCELLED) {
            session.setStatus(PlanningSessionStatus.CANCELLED);
            planningSessionRepository.save(session);
        }
        return get(user, planningId);
    }

    private PlanningSession findOwnedSession(User user, Long planningId) {
        return planningSessionRepository.findByIdAndUser(planningId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planning session not found."));
    }

    private void requireDraft(PlanningSession session) {
        if (session.getStatus() != PlanningSessionStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft planning sessions can be changed.");
        }
    }

    private PlannedSlot plannedSlot(PlanningSession session, ScheduledBlockSuggestion suggestion) {
        return PlannedSlot.builder()
                .planningSession(session)
                .taskId(suggestion.taskId())
                .taskTitle(suggestion.taskTitle())
                .startAt(suggestion.startAt())
                .endAt(suggestion.endAt())
                .durationMinutes(suggestion.durationMinutes())
                .status(PlannedSlotStatus.PROPOSED)
                .googleCalendarId("primary")
                .googleEventId(StableGoogleEventId.generate())
                .applyStatus(PlannedSlotApplyStatus.NOT_REQUESTED)
                .build();
    }

    private PlanningUnscheduledTask unscheduledTask(PlanningSession session, UnscheduledTaskSuggestion suggestion) {
        return PlanningUnscheduledTask.builder()
                .planningSession(session)
                .taskId(suggestion.taskId())
                .taskTitle(suggestion.taskTitle())
                .unscheduledMinutes(suggestion.unscheduledMinutes())
                .reason(suggestion.reason())
                .build();
    }

    private PlanningSessionResponse response(
            PlanningSession session,
            List<PlannedSlot> slots,
            List<PlanningUnscheduledTask> unscheduledTasks) {
        return PlanningSessionResponse.from(
                session,
                slots.stream().map(PlannedSlotResponse::from).toList(),
                unscheduledTasks.stream().map(PlanningUnscheduledTaskResponse::from).toList()
        );
    }
}
