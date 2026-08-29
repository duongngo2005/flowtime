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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarSyncService {

    private static final long HISTORY_DAYS = 30;
    private static final long FUTURE_DAYS = 90;

    private final GoogleAccountService googleAccountService;
    private final GoogleCalendarApiClient googleCalendarApiClient;
    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final GoogleCalendarMapper googleCalendarMapper;
    private final GoogleCalendarEventMapper googleCalendarEventMapper;

    @Transactional
    public CalendarSyncResponse sync(User user) {
        Instant now = Instant.now();
        Instant from = now.minus(HISTORY_DAYS, ChronoUnit.DAYS);
        Instant to = now.plus(FUTURE_DAYS, ChronoUnit.DAYS);
        String accessToken = googleAccountService.getValidAccessToken(user);

        List<Calendar> localCalendars = syncCalendars(user, accessToken);
        SyncEventCounts eventCounts = syncEvents(user, accessToken, localCalendars, from, to);

        return new CalendarSyncResponse(
                localCalendars.size(),
                eventCounts.created(),
                eventCounts.updated(),
                from,
                to
        );
    }

    private List<Calendar> syncCalendars(User user, String accessToken) {
        CalendarListResponse response = googleCalendarApiClient.listCalendars(accessToken);
        List<Calendar> localCalendars = new ArrayList<>();

        for (CalendarListResponse.CalendarEntryDto entry : calendarEntries(response)) {
            Calendar calendar = calendarRepository.findByUserAndGoogleCalendarId(user, entry.id())
                    .orElseGet(Calendar::new);
            localCalendars.add(calendarRepository.save(googleCalendarMapper.apply(entry, user, calendar)));
        }

        return localCalendars;
    }

    private SyncEventCounts syncEvents(User user, String accessToken, List<Calendar> calendars,
                                        Instant from, Instant to) {
        int created = 0;
        int updated = 0;

        for (Calendar calendar : calendars) {
            EventListResponse response = googleCalendarApiClient.listEvents(
                    accessToken,
                    calendar.getGoogleCalendarId(),
                    from,
                    to
            );

            for (EventListResponse.GoogleEventDto eventDto : eventEntries(response)) {
                CalendarEvent event = calendarEventRepository.findByUserAndGoogleEventId(user, eventDto.id())
                        .orElseGet(CalendarEvent::new);
                boolean isNew = event.getId() == null;
                calendarEventRepository.save(googleCalendarEventMapper.apply(eventDto, user, calendar, event));

                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            }
        }

        return new SyncEventCounts(created, updated);
    }

    private List<CalendarListResponse.CalendarEntryDto> calendarEntries(CalendarListResponse response) {
        return response == null || response.items() == null ? List.of() : response.items();
    }

    private List<EventListResponse.GoogleEventDto> eventEntries(EventListResponse response) {
        return response == null || response.items() == null ? List.of() : response.items();
    }

    private record SyncEventCounts(int created, int updated) {}
}
