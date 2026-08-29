package com.ndd.flowtime_be.calendar.service;

import com.ndd.flowtime_be.calendar.dto.CalendarEventResponse;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarEventQueryServiceTest {

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @InjectMocks
    private CalendarEventQueryService calendarEventQueryService;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").build();
    private final Calendar calendar = Calendar.builder()
            .id(2L)
            .user(user)
            .googleCalendarId("primary")
            .name("Primary")
            .timezone("Asia/Ho_Chi_Minh")
            .build();

    @Test
    void listsOnlyOverlappingEventsForCurrentUser() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");
        CalendarEvent event = event(3L, "event-1", "Project meeting");
        when(calendarEventRepository.findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(user, to, from))
                .thenReturn(List.of(event));

        List<CalendarEventResponse> events = calendarEventQueryService.listEvents(user, from, to);

        assertEquals(1, events.size());
        assertEquals(3L, events.getFirst().id());
        assertEquals(2L, events.getFirst().calendarId());
        assertEquals("Project meeting", events.getFirst().title());
        verify(calendarEventRepository)
                .findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(user, to, from);
    }

    @Test
    void rejectsAnInvalidDateRange() {
        Instant instant = Instant.parse("2026-09-01T00:00:00Z");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> calendarEventQueryService.listEvents(user, instant, instant)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void hidesEventsThatDoNotBelongToCurrentUser() {
        when(calendarEventRepository.findByIdAndUser(3L, user)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> calendarEventQueryService.getEvent(user, 3L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private CalendarEvent event(Long id, String googleEventId, String title) {
        return CalendarEvent.builder()
                .id(id)
                .user(user)
                .calendar(calendar)
                .googleEventId(googleEventId)
                .title(title)
                .startAt(Instant.parse("2026-09-01T09:00:00Z"))
                .endAt(Instant.parse("2026-09-01T10:00:00Z"))
                .status("confirmed")
                .build();
    }
}
