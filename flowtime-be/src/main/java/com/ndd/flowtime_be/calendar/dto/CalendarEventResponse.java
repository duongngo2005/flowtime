package com.ndd.flowtime_be.calendar.dto;

import com.ndd.flowtime_be.calendar.entity.CalendarEvent;

import java.time.Instant;

public record CalendarEventResponse(
        Long id,
        Long calendarId,
        String googleEventId,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        boolean allDay,
        String status
) {
    public static CalendarEventResponse from(CalendarEvent event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getCalendar().getId(),
                event.getGoogleEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.isAllDay(),
                event.getStatus()
        );
    }
}
