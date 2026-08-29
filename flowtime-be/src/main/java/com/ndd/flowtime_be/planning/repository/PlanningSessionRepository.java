package com.ndd.flowtime_be.planning.repository;

import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanningSessionRepository extends JpaRepository<PlanningSession, Long> {

    Optional<PlanningSession> findByIdAndUser(Long id, User user);
}
