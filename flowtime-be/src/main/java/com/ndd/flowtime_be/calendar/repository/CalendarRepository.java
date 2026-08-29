package com.ndd.flowtime_be.calendar.repository;

import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    List<Calendar> findByUser(User user);

    Optional<Calendar> findByUserAndGoogleCalendarId(User user, String googleCalendarId);
}
