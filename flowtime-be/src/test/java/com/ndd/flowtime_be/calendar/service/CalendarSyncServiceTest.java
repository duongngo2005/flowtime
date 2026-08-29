package com.ndd.flowtime_be.calendar.service;

import com.ndd.flowtime_be.calendar.client.GoogleCalendarApiClient;
import com.ndd.flowtime_be.calendar.dto.CalendarListResponse;
import com.ndd.flowtime_be.calendar.dto.CalendarSyncResponse;
import com.ndd.flowtime_be.calendar.dto.EventListResponse;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.calendar.mapper.GoogleCalendarEventMapper;
import com.ndd.flowtime_be.calendar.mapper.GoogleCalendarMapper;
import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.calendar.repository.CalendarRepository;
import com.ndd.flowtime_be.google_account.service.GoogleAccountService;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTest {

    @Mock
    private GoogleAccountService googleAccountService;

    @Mock
    private GoogleCalendarApiClient googleCalendarApiClient;

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    private CalendarSyncService calendarSyncService;
    private final User user = User.builder().email("user@example.com").name("Test User").build();

    @BeforeEach
    void setUp() {
        calendarSyncService = new CalendarSyncService(
                googleAccountService,
                googleCalendarApiClient,
                calendarRepository,
                calendarEventRepository,
                new GoogleCalendarMapper(),
                new GoogleCalendarEventMapper()
        );
    }

    @Test
    void syncsNewCalendarAndCancelledEvent() {
        CalendarListResponse.CalendarEntryDto calendarDto = new CalendarListResponse.CalendarEntryDto(
                "primary", "Primary", null, "Asia/Ho_Chi_Minh", true
        );
        EventListResponse.GoogleEventDto eventDto = new EventListResponse.GoogleEventDto(
                "event-1",
                "Cancelled meeting",
                null,
                new EventListResponse.EventDateTimeDto("2026-09-01T09:00:00+07:00", null, null),
                new EventListResponse.EventDateTimeDto("2026-09-01T10:00:00+07:00", null, null),
                "cancelled"
        );
        CalendarListResponse calendarResponse = new CalendarListResponse(List.of(calendarDto));
        EventListResponse eventResponse = new EventListResponse(List.of(eventDto));

        when(googleAccountService.getValidAccessToken(user)).thenReturn("google-token");
        when(googleCalendarApiClient.listCalendars("google-token")).thenReturn(calendarResponse);
        when(calendarRepository.findByUserAndGoogleCalendarId(user, "primary")).thenReturn(Optional.empty());
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(googleCalendarApiClient.listEvents(eq("google-token"), eq("primary"), any(Instant.class), any(Instant.class)))
                .thenReturn(eventResponse);
        when(calendarEventRepository.findByUserAndGoogleEventId(user, "event-1")).thenReturn(Optional.empty());
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CalendarSyncResponse result = calendarSyncService.sync(user);

        ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarEventRepository).save(eventCaptor.capture());
        assertEquals(1, result.calendarsSynced());
        assertEquals(1, result.eventsCreated());
        assertEquals(0, result.eventsUpdated());
        assertEquals("cancelled", eventCaptor.getValue().getStatus());
        verify(googleCalendarApiClient).listCalendars("google-token");
        verify(googleCalendarApiClient).listEvents(eq("google-token"), eq("primary"), any(Instant.class), any(Instant.class));
    }

    @Test
    void updatesExistingEventInsteadOfCreatingDuplicate() {
        Calendar existingCalendar = Calendar.builder()
                .id(1L)
                .user(user)
                .googleCalendarId("primary")
                .name("Primary")
                .timezone("Asia/Ho_Chi_Minh")
                .build();
        CalendarEvent existingEvent = CalendarEvent.builder()
                .id(2L)
                .user(user)
                .calendar(existingCalendar)
                .googleEventId("event-1")
                .title("Old title")
                .startAt(Instant.parse("2026-09-01T01:00:00Z"))
                .endAt(Instant.parse("2026-09-01T02:00:00Z"))
                .status("confirmed")
                .build();
        CalendarListResponse calendarResponse = new CalendarListResponse(List.of(
                new CalendarListResponse.CalendarEntryDto("primary", "Primary", null, "Asia/Ho_Chi_Minh", true)
        ));
        EventListResponse eventResponse = new EventListResponse(List.of(
                new EventListResponse.GoogleEventDto(
                        "event-1",
                        "Updated title",
                        null,
                        new EventListResponse.EventDateTimeDto("2026-09-01T09:00:00+07:00", null, null),
                        new EventListResponse.EventDateTimeDto("2026-09-01T10:00:00+07:00", null, null),
                        "cancelled"
                )
        ));

        when(googleAccountService.getValidAccessToken(user)).thenReturn("google-token");
        when(googleCalendarApiClient.listCalendars("google-token")).thenReturn(calendarResponse);
        when(calendarRepository.findByUserAndGoogleCalendarId(user, "primary"))
                .thenReturn(Optional.of(existingCalendar));
        when(calendarRepository.save(existingCalendar)).thenReturn(existingCalendar);
        when(googleCalendarApiClient.listEvents(eq("google-token"), eq("primary"), any(Instant.class), any(Instant.class)))
                .thenReturn(eventResponse);
        when(calendarEventRepository.findByUserAndGoogleEventId(user, "event-1"))
                .thenReturn(Optional.of(existingEvent));
        when(calendarEventRepository.save(existingEvent)).thenReturn(existingEvent);

        CalendarSyncResponse result = calendarSyncService.sync(user);

        assertEquals(0, result.eventsCreated());
        assertEquals(1, result.eventsUpdated());
        assertEquals("Updated title", existingEvent.getTitle());
        assertEquals("cancelled", existingEvent.getStatus());
        verify(calendarEventRepository).save(existingEvent);
    }
}
