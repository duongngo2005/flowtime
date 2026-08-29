package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.planning.dto.PlanningSessionResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    @Mock
    private SchedulingEngine schedulingEngine;

    @Mock
    private PlanningSessionRepository planningSessionRepository;

    @Mock
    private PlannedSlotRepository plannedSlotRepository;

    @Mock
    private PlanningUnscheduledTaskRepository unscheduledTaskRepository;

    @InjectMocks
    private PlanningService planningService;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").timezone("UTC").build();

    @Test
    void createsDraftAndPersistsSlotsAndUnscheduledTasks() {
        SchedulingPreviewRequest request = new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 7);
        SchedulingPreviewResponse preview = preview();
        when(schedulingEngine.preview(user, request)).thenReturn(preview);
        when(planningSessionRepository.save(any(PlanningSession.class))).thenAnswer(invocation -> {
            PlanningSession session = invocation.getArgument(0);
            session.setId(10L);
            return session;
        });
        when(plannedSlotRepository.saveAll(any())).thenAnswer(invocation -> {
            List<PlannedSlot> slots = invocation.getArgument(0);
            slots.getFirst().setId(20L);
            return slots;
        });
        when(unscheduledTaskRepository.saveAll(any())).thenAnswer(invocation -> {
            List<PlanningUnscheduledTask> tasks = invocation.getArgument(0);
            tasks.getFirst().setId(30L);
            return tasks;
        });

        PlanningSessionResponse response = planningService.createDraft(user, request);

        ArgumentCaptor<PlanningSession> sessionCaptor = ArgumentCaptor.forClass(PlanningSession.class);
        verify(planningSessionRepository).save(sessionCaptor.capture());
        assertEquals(PlanningSessionStatus.DRAFT, sessionCaptor.getValue().getStatus());
        assertEquals(1, response.slots().size());
        assertEquals(20L, response.slots().getFirst().id());
        assertTrue(response.slots().getFirst().googleEventId().matches("[a-v0-9]{5,1024}"));
        assertEquals(PlanningSessionStatus.DRAFT, response.status());
        assertEquals(1, response.unscheduledTasks().size());
        assertEquals(60, response.unscheduledTasks().getFirst().unscheduledMinutes());
    }

    @Test
    void removesOnlyOwnedDraftSlotsWithoutDeletingTheSnapshot() {
        PlanningSession session = session(10L, PlanningSessionStatus.DRAFT);
        PlannedSlot slot = slot(20L, session, PlannedSlotStatus.PROPOSED);
        stubOwnedSession(session);
        when(plannedSlotRepository.findByIdAndPlanningSessionId(20L, 10L)).thenReturn(Optional.of(slot));
        when(plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(10L)).thenReturn(List.of(slot));
        when(unscheduledTaskRepository.findByPlanningSessionIdOrderByIdAsc(10L)).thenReturn(List.of());

        PlanningSessionResponse response = planningService.removeSlot(user, 10L, 20L);

        assertEquals(PlannedSlotStatus.REMOVED, response.slots().getFirst().status());
        verify(plannedSlotRepository).save(slot);
        verify(plannedSlotRepository, never()).delete(any());
    }

    @Test
    void approvesDraftAndAcceptsRemainingProposedSlotsWithoutRunningTheEngine() {
        PlanningSession session = session(10L, PlanningSessionStatus.DRAFT);
        PlannedSlot proposed = slot(20L, session, PlannedSlotStatus.PROPOSED);
        PlannedSlot removed = slot(21L, session, PlannedSlotStatus.REMOVED);
        stubOwnedSession(session);
        when(plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(10L)).thenReturn(List.of(proposed, removed));
        when(unscheduledTaskRepository.findByPlanningSessionIdOrderByIdAsc(10L)).thenReturn(List.of());

        PlanningSessionResponse response = planningService.approve(user, 10L);

        assertEquals(PlanningSessionStatus.APPROVED, response.status());
        assertEquals(PlannedSlotStatus.ACCEPTED, proposed.getStatus());
        assertEquals(PlannedSlotApplyStatus.PENDING, proposed.getApplyStatus());
        assertEquals(PlannedSlotStatus.REMOVED, removed.getStatus());
        verifyNoInteractions(schedulingEngine);
    }

    @Test
    void hidesPlanningSessionsOwnedByAnotherUser() {
        when(planningSessionRepository.findByIdAndUser(10L, user)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> planningService.approve(user, 10L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void cancelsDraftPlanningAndKeepsCancellationIdempotent() {
        PlanningSession session = session(10L, PlanningSessionStatus.DRAFT);
        stubOwnedSession(session);
        when(plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(10L)).thenReturn(List.of());
        when(unscheduledTaskRepository.findByPlanningSessionIdOrderByIdAsc(10L)).thenReturn(List.of());

        PlanningSessionResponse response = planningService.cancel(user, 10L);

        assertEquals(PlanningSessionStatus.CANCELLED, response.status());
        verify(planningSessionRepository).save(session);

        planningService.cancel(user, 10L);

        verify(planningSessionRepository, times(1)).save(session);
    }

    @Test
    void rejectsCancellingPlansWithApplySideEffectsOrUncertainApplyState() {
        for (PlanningSessionStatus status : List.of(
                PlanningSessionStatus.APPLYING,
                PlanningSessionStatus.APPLY_FAILED,
                PlanningSessionStatus.APPLIED
        )) {
            PlanningSession session = session(10L, status);
            when(planningSessionRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(session));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> planningService.cancel(user, 10L)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
        verify(planningSessionRepository, never()).save(any(PlanningSession.class));
    }

    private void stubOwnedSession(PlanningSession session) {
        when(planningSessionRepository.findByIdAndUser(session.getId(), user)).thenReturn(Optional.of(session));
    }

    private SchedulingPreviewResponse preview() {
        return new SchedulingPreviewResponse(
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 14),
                "UTC",
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(new ScheduledBlockSuggestion(
                        1L,
                        "Finish report",
                        Instant.parse("2026-09-07T09:00:00Z"),
                        Instant.parse("2026-09-07T10:00:00Z"),
                        60
                )),
                List.of(new UnscheduledTaskSuggestion(2L, "Read notes", 60, "Not enough free time."))
        );
    }

    private PlanningSession session(Long id, PlanningSessionStatus status) {
        return PlanningSession.builder()
                .id(id)
                .user(user)
                .startDate(LocalDate.of(2026, 9, 7))
                .endDate(LocalDate.of(2026, 9, 14))
                .timezone("UTC")
                .status(status)
                .build();
    }

    private PlannedSlot slot(Long id, PlanningSession session, PlannedSlotStatus status) {
        return PlannedSlot.builder()
                .id(id)
                .planningSession(session)
                .taskId(1L)
                .taskTitle("Finish report")
                .startAt(Instant.parse("2026-09-07T09:00:00Z"))
                .endAt(Instant.parse("2026-09-07T10:00:00Z"))
                .durationMinutes(60)
                .status(status)
                .build();
    }
}
