package com.ndd.flowtime_be.calendar.repository;

import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    Optional<CalendarEvent> findByUserAndGoogleEventId(User user, String googleEventId);

    Optional<CalendarEvent> findByIdAndUser(Long id, User user);

    List<CalendarEvent> findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
            User user,
            Instant to,
            Instant from
    );
}
