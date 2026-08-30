package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningReservationServiceTest {

    @Mock
    private PlannedSlotRepository plannedSlotRepository;

    @InjectMocks
    private PlanningReservationService planningReservationService;

    @Test
    void treatsAcceptedSlotsFromNonCancellableSessionsAsHardReservations() {
        User user = User.builder().id(1L).email("user@example.com").name("Test User").build();
        when(plannedSlotRepository.findHardReservations(any(), any(), any())).thenReturn(List.of());

        planningReservationService.hardReservationsFor(user);

        ArgumentCaptor<Collection<PlanningSessionStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(plannedSlotRepository).findHardReservations(
                eq(user),
                eq(PlannedSlotStatus.ACCEPTED),
                statuses.capture()
        );
        assertEquals(
                List.of(
                        PlanningSessionStatus.APPROVED,
                        PlanningSessionStatus.APPLYING,
                        PlanningSessionStatus.APPLY_FAILED,
                        PlanningSessionStatus.APPLIED
                ),
                List.copyOf(statuses.getValue())
        );
        assertTrue(!statuses.getValue().contains(PlanningSessionStatus.DRAFT));
    }
}
