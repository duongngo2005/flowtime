package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.planning.entity.PlannedSlotApplyStatus;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.planning.repository.PlanningSessionRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningApplyStateServiceTest {

    @Mock
    private PlanningSessionRepository planningSessionRepository;

    @Mock
    private PlannedSlotRepository plannedSlotRepository;

    @InjectMocks
    private PlanningApplyStateService applyStateService;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").build();

    @Test
    void atomicallyClaimsApprovedOrFailedPlansBeforeAnyRemoteWork() {
        when(planningSessionRepository.claimForApply(eq(10L), eq(user), any(), any(), any())).thenReturn(1);

        applyStateService.claim(user, 10L);

        verify(planningSessionRepository).claimForApply(
                eq(10L),
                eq(user),
                argThat(statuses -> statuses.equals(
                        java.util.List.of(PlanningSessionStatus.APPROVED, PlanningSessionStatus.APPLY_FAILED)
                )),
                eq(PlanningSessionStatus.APPLYING),
                any(Instant.class)
        );
        verify(plannedSlotRepository).claimEligibleSlotsForApply(
                eq(10L),
                eq(PlannedSlotStatus.ACCEPTED),
                eq(java.util.List.of(PlannedSlotApplyStatus.PENDING, PlannedSlotApplyStatus.FAILED)),
                eq(PlannedSlotApplyStatus.APPLYING),
                any(Instant.class)
        );
    }

    @Test
    void rejectsApplyWhenAnotherRequestAlreadyClaimedThePlan() {
        when(planningSessionRepository.claimForApply(eq(10L), eq(user), any(), any(), any())).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applyStateService.claim(user, 10L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(plannedSlotRepository);
    }

    @Test
    void marksAnApplyingPlanFailedAndAllowsTheErrorToBeRecorded() {
        PlanningSession session = session(PlanningSessionStatus.APPLYING);
        when(planningSessionRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(session));

        applyStateService.markApplyFailed(user, 10L, "Google request timed out");

        assertEquals(PlanningSessionStatus.APPLY_FAILED, session.getStatus());
        assertEquals("Google request timed out", session.getLastApplyError());
        verify(plannedSlotRepository).releaseInFlightSlotsForRetry(
                10L,
                PlannedSlotStatus.ACCEPTED,
                PlannedSlotApplyStatus.APPLYING,
                PlannedSlotApplyStatus.PENDING
        );
        verify(planningSessionRepository).save(session);
    }

    @Test
    void completesOnlyWhenEveryAcceptedSlotWasApplied() {
        PlanningSession session = session(PlanningSessionStatus.APPLYING);
        when(planningSessionRepository.findByIdAndUser(10L, user)).thenReturn(Optional.of(session));
        when(plannedSlotRepository.countByPlanningSessionIdAndStatusAndApplyStatusNot(
                10L, PlannedSlotStatus.ACCEPTED, PlannedSlotApplyStatus.APPLIED
        )).thenReturn(0L);

        applyStateService.markApplied(user, 10L);

        assertEquals(PlanningSessionStatus.APPLIED, session.getStatus());
        assertNotNull(session.getAppliedAt());
    }

    private PlanningSession session(PlanningSessionStatus status) {
        return PlanningSession.builder().id(10L).user(user).status(status).build();
    }
}
