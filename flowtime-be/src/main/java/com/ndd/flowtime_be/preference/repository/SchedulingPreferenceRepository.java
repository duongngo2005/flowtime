package com.ndd.flowtime_be.preference.repository;

import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulingPreferenceRepository extends JpaRepository<SchedulingPreference, Long> {

    Optional<SchedulingPreference> findByUser(User user);
}
