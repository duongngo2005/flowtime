package com.ndd.flowtime_be.calendar.mapper;

import com.ndd.flowtime_be.calendar.dto.EventListResponse.EventDateTimeDto;
import com.ndd.flowtime_be.calendar.dto.EventListResponse.GoogleEventDto;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleCalendarEventMapperTest {

    private final GoogleCalendarEventMapper mapper = new GoogleCalendarEventMapper();
    private final User user = User.builder().email("user@example.com").name("Test User").build();
    private final Calendar calendar = Calendar.builder()
            .user(user)
            .googleCalendarId("primary")
            .name("Primary")
            .timezone("Asia/Ho_Chi_Minh")
            .build();

    @Test
    void mapsTimedEventUsingGoogleOffset() {
        GoogleEventDto source = new GoogleEventDto(
                "timed-event",
                "Project meeting",
                "Discuss MVP",
                new EventDateTimeDto("2026-09-01T09:00:00+07:00", null, null),
                new EventDateTimeDto("2026-09-01T10:30:00+07:00", null, null),
                "confirmed"
        );

        CalendarEvent event = mapper.apply(source, user, calendar, new CalendarEvent());

        assertFalse(event.isAllDay());
        assertEquals(Instant.parse("2026-09-01T02:00:00Z"), event.getStartAt());
        assertEquals(Instant.parse("2026-09-01T03:30:00Z"), event.getEndAt());
        assertEquals("confirmed", event.getStatus());
    }

    @Test
    void mapsAllDayEventUsingCalendarTimezone() {
        GoogleEventDto source = new GoogleEventDto(
                "all-day-event",
                "Public holiday",
                null,
                new EventDateTimeDto(null, "2026-09-02", null),
                new EventDateTimeDto(null, "2026-09-03", null),
                "confirmed"
        );

        CalendarEvent event = mapper.apply(source, user, calendar, new CalendarEvent());

        assertTrue(event.isAllDay());
        assertEquals(Instant.parse("2026-09-01T17:00:00Z"), event.getStartAt());
        assertEquals(Instant.parse("2026-09-02T17:00:00Z"), event.getEndAt());
    }

    @Test
    void preservesCancelledEventStatus() {
        GoogleEventDto source = new GoogleEventDto(
                "cancelled-event",
                "Cancelled meeting",
                null,
                new EventDateTimeDto("2026-09-04T09:00:00Z", null, null),
                new EventDateTimeDto("2026-09-04T10:00:00Z", null, null),
                "cancelled"
        );

        CalendarEvent event = mapper.apply(source, user, calendar, new CalendarEvent());

        assertEquals("cancelled", event.getStatus());
    }
}
