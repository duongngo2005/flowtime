package com.ndd.flowtime_be.calendar.mapper;

import com.ndd.flowtime_be.calendar.dto.EventListResponse.EventDateTimeDto;
import com.ndd.flowtime_be.calendar.dto.EventListResponse.GoogleEventDto;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeParseException;

@Component
public class GoogleCalendarEventMapper {

    public CalendarEvent apply(GoogleEventDto source, User user, Calendar calendar, CalendarEvent target) {
        if (source.id() == null || source.id().isBlank()) {
            throw new IllegalArgumentException("Google event ID is required.");
        }

        boolean allDay = isAllDay(source.start()) || isAllDay(source.end());
        ZoneId calendarZone = resolveZone(calendar.getTimezone());
        Instant startAt = toInstant(source.start(), calendarZone);
        Instant endAt = toInstant(source.end(), calendarZone);

        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Google event start time must be before end time.");
        }

        target.setUser(user);
        target.setCalendar(calendar);
        target.setGoogleEventId(source.id());
        target.setTitle(hasText(source.summary()) ? source.summary() : "Untitled event");
        target.setDescription(source.description());
        target.setStartAt(startAt);
        target.setEndAt(endAt);
        target.setAllDay(allDay);
        target.setStatus(hasText(source.status()) ? source.status() : "confirmed");
        return target;
    }

    private Instant toInstant(EventDateTimeDto value, ZoneId calendarZone) {
        if (value == null) {
            throw new IllegalArgumentException("Google event date/time is required.");
        }

        if (hasText(value.dateTime())) {
            try {
                return OffsetDateTime.parse(value.dateTime()).toInstant();
            } catch (DateTimeParseException ignored) {
                ZoneId eventZone = hasText(value.timeZone()) ? resolveZone(value.timeZone()) : calendarZone;
                return LocalDateTime.parse(value.dateTime()).atZone(eventZone).toInstant();
            }
        }

        if (hasText(value.date())) {
            ZoneId eventZone = hasText(value.timeZone()) ? resolveZone(value.timeZone()) : calendarZone;
            return LocalDate.parse(value.date()).atStartOfDay(eventZone).toInstant();
        }

        throw new IllegalArgumentException("Google event must contain dateTime or date.");
    }

    private ZoneId resolveZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid calendar timezone: " + timezone, exception);
        }
    }

    private boolean isAllDay(EventDateTimeDto value) {
        return value != null && hasText(value.date());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
