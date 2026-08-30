package com.ndd.flowtime_be.calendar.mapper;

import com.ndd.flowtime_be.calendar.dto.CalendarListResponse.CalendarEntryDto;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class GoogleCalendarMapper {

    public Calendar apply(CalendarEntryDto source, User user, Calendar target) {
        if (source.id() == null || source.id().isBlank()) {
            throw new IllegalArgumentException("Google calendar ID is required.");
        }

        target.setUser(user);
        target.setGoogleCalendarId(source.id());
        target.setName(hasText(source.summary()) ? source.summary() : "Untitled calendar");
        target.setTimezone(hasText(source.timeZone()) ? source.timeZone() : "UTC");
        target.setPrimary(Boolean.TRUE.equals(source.primary()));
        if (target.getId() == null) {
            target.setBlocksScheduling(!isGoogleHolidayCalendar(source.id()));
        }
        target.setLastSyncedAt(Instant.now());
        return target;
    }

    private boolean isGoogleHolidayCalendar(String googleCalendarId) {
        return googleCalendarId != null
                && googleCalendarId.toLowerCase().contains("#holiday@group.v.calendar.google.com");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
