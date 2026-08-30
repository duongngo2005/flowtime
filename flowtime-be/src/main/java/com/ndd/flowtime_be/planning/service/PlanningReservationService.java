package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningReservationService {

    private static final List<PlanningSessionStatus> HARD_RESERVATION_STATUSES = List.of(
            PlanningSessionStatus.APPROVED,
            PlanningSessionStatus.APPLYING,
            PlanningSessionStatus.APPLY_FAILED,
            PlanningSessionStatus.APPLIED
    );

    private final PlannedSlotRepository plannedSlotRepository;

    @Transactional(readOnly = true)
    public List<PlannedSlot> hardReservationsFor(User user) {
        return plannedSlotRepository.findHardReservations(
                user,
                PlannedSlotStatus.ACCEPTED,
                HARD_RESERVATION_STATUSES
        );
    }
}
