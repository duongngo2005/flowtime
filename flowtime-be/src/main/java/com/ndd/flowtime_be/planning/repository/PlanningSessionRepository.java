package com.ndd.flowtime_be.planning.repository;

import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface PlanningSessionRepository extends JpaRepository<PlanningSession, Long> {

    Optional<PlanningSession> findByIdAndUser(Long id, User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PlanningSession session
            SET session.status = :applyingStatus,
                session.applyAttempts = session.applyAttempts + 1,
                session.applyStartedAt = :startedAt,
                session.lastApplyError = NULL
            WHERE session.id = :planningId
              AND session.user = :user
              AND session.status IN :claimableStatuses
            """)
    int claimForApply(
            @Param("planningId") Long planningId,
            @Param("user") User user,
            @Param("claimableStatuses") Collection<PlanningSessionStatus> claimableStatuses,
            @Param("applyingStatus") PlanningSessionStatus applyingStatus,
            @Param("startedAt") Instant startedAt
    );
}
