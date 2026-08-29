package com.ndd.flowtime_be.planning.repository;

import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotApplyStatus;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlannedSlotRepository extends JpaRepository<PlannedSlot, Long> {

    List<PlannedSlot> findByPlanningSessionIdOrderByStartAtAsc(Long planningSessionId);

    Optional<PlannedSlot> findByIdAndPlanningSessionId(Long id, Long planningSessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PlannedSlot slot
            SET slot.applyStatus = :applyingStatus,
                slot.applyStartedAt = :startedAt,
                slot.applyError = NULL
            WHERE slot.planningSession.id = :planningId
              AND slot.status = :acceptedStatus
              AND slot.applyStatus IN :claimableStatuses
            """)
    int claimEligibleSlotsForApply(
            @Param("planningId") Long planningId,
            @Param("acceptedStatus") PlannedSlotStatus acceptedStatus,
            @Param("claimableStatuses") Collection<PlannedSlotApplyStatus> claimableStatuses,
            @Param("applyingStatus") PlannedSlotApplyStatus applyingStatus,
            @Param("startedAt") Instant startedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PlannedSlot slot
            SET slot.applyStatus = :pendingStatus
            WHERE slot.planningSession.id = :planningId
              AND slot.status = :acceptedStatus
              AND slot.applyStatus = :applyingStatus
            """)
    int releaseInFlightSlotsForRetry(
            @Param("planningId") Long planningId,
            @Param("acceptedStatus") PlannedSlotStatus acceptedStatus,
            @Param("applyingStatus") PlannedSlotApplyStatus applyingStatus,
            @Param("pendingStatus") PlannedSlotApplyStatus pendingStatus
    );

    long countByPlanningSessionIdAndStatusAndApplyStatusNot(
            Long planningSessionId,
            PlannedSlotStatus status,
            PlannedSlotApplyStatus applyStatus
    );
}
