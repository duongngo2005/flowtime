package com.ndd.flowtime_be.calendar.dto;

import java.time.Instant;

public record CalendarSyncResponse(
        int calendarsSynced,
        int eventsCreated,
        int eventsUpdated,
        Instant syncedFrom,
        Instant syncedTo
) {}
