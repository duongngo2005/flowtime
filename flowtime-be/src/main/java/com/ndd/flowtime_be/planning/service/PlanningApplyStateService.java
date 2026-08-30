package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotApplyStatus;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.planning.repository.PlanningSessionRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanningApplyStateService {

    private final PlanningSessionRepository planningSessionRepository;
    private final PlannedSlotRepository plannedSlotRepository;

    /**
     * Atomically reserves a plan for an apply attempt. This transaction ends before any Google API call.
     */
    @Transactional
    public void claim(User user, Long planningId) {
        Instant now = Instant.now();
        int claimed = planningSessionRepository.claimForApply(
                planningId,
                user,
                List.of(PlanningSessionStatus.APPROVED, PlanningSessionStatus.APPLY_FAILED),
                PlanningSessionStatus.APPLYING,
                now
        );
        if (claimed != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kế hoạch hiện không thể được áp dụng."
            );
        }

        plannedSlotRepository.claimEligibleSlotsForApply(
                planningId,
                PlannedSlotStatus.ACCEPTED,
                List.of(PlannedSlotApplyStatus.PENDING, PlannedSlotApplyStatus.FAILED),
                PlannedSlotApplyStatus.APPLYING,
                now
        );
    }

    @Transactional
    public void markApplyFailed(User user, Long planningId, String error) {
        PlanningSession session = findOwnedSession(user, planningId);
        requireApplying(session);
        plannedSlotRepository.releaseInFlightSlotsForRetry(
                planningId,
                PlannedSlotStatus.ACCEPTED,
                PlannedSlotApplyStatus.APPLYING,
                PlannedSlotApplyStatus.PENDING
        );
        session.setStatus(PlanningSessionStatus.APPLY_FAILED);
        session.setLastApplyError(error);
        planningSessionRepository.save(session);
    }

    @Transactional
    public void markApplied(User user, Long planningId) {
        PlanningSession session = findOwnedSession(user, planningId);
        requireApplying(session);
        long unappliedSlots = plannedSlotRepository.countByPlanningSessionIdAndStatusAndApplyStatusNot(
                planningId,
                PlannedSlotStatus.ACCEPTED,
                PlannedSlotApplyStatus.APPLIED
        );
        if (unappliedSlots > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mọi khung giờ đã chấp nhận phải được áp dụng trước khi hoàn tất kế hoạch."
            );
        }

        session.setStatus(PlanningSessionStatus.APPLIED);
        session.setAppliedAt(Instant.now());
        session.setLastApplyError(null);
        planningSessionRepository.save(session);
    }

    @Transactional
    public void markSlotApplied(Long planningId, Long slotId) {
        PlannedSlot slot = findApplyingSlot(planningId, slotId);
        slot.setApplyStatus(PlannedSlotApplyStatus.APPLIED);
        slot.setAppliedAt(Instant.now());
        slot.setApplyError(null);
        plannedSlotRepository.save(slot);
    }

    @Transactional
    public void markSlotFailed(Long planningId, Long slotId, String error) {
        PlannedSlot slot = findApplyingSlot(planningId, slotId);
        slot.setApplyStatus(PlannedSlotApplyStatus.FAILED);
        slot.setApplyError(error);
        plannedSlotRepository.save(slot);
    }

    private PlanningSession findOwnedSession(User user, Long planningId) {
        return planningSessionRepository.findByIdAndUser(planningId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch."));
    }

    private void requireApplying(PlanningSession session) {
        if (session.getStatus() != PlanningSessionStatus.APPLYING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kế hoạch này không ở trạng thái đang áp dụng."
            );
        }
    }

    private PlannedSlot findApplyingSlot(Long planningId, Long slotId) {
        PlannedSlot slot = plannedSlotRepository.findByIdAndPlanningSessionId(slotId, planningId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khung giờ đã lên lịch."));
        if (slot.getApplyStatus() != PlannedSlotApplyStatus.APPLYING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ này không ở trạng thái đang áp dụng.");
        }
        return slot;
    }
}
