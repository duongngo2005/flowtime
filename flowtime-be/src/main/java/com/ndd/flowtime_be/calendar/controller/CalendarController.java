package com.ndd.flowtime_be.calendar.controller;

import com.ndd.flowtime_be.calendar.client.GoogleCalendarApiClient;
import com.ndd.flowtime_be.calendar.dto.*;
import com.ndd.flowtime_be.calendar.service.CalendarEventQueryService;
import com.ndd.flowtime_be.calendar.service.CalendarSyncService;
import com.ndd.flowtime_be.google_account.service.GoogleAccountService;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleAccountService googleAccountService;
    private final GoogleCalendarApiClient googleCalendarApiClient;
    private final CalendarSyncService calendarSyncService;
    private final CalendarEventQueryService calendarEventQueryService;

    @PostMapping("/calendars/sync")
    public ResponseEntity<CalendarSyncResponse> syncCalendars(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(calendarSyncService.sync(user));
    }

    @GetMapping("/calendars")
    public ResponseEntity<CalendarListResponse> listCalendars(@AuthenticationPrincipal User user) {
        String token = googleAccountService.getValidAccessToken(user);
        return ResponseEntity.ok(googleCalendarApiClient.listCalendars(token));
    }

    @GetMapping("/calendar/events")
    public ResponseEntity<java.util.List<CalendarEventResponse>> listEvents(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        if (from == null) from = Instant.now().truncatedTo(ChronoUnit.DAYS);
        if (to == null) to = from.plus(7, ChronoUnit.DAYS);

        return ResponseEntity.ok(calendarEventQueryService.listEvents(user, from, to));
    }

    @GetMapping("/calendar/events/{eventId}")
    public ResponseEntity<CalendarEventResponse> getEvent(
            @AuthenticationPrincipal User user,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(calendarEventQueryService.getEvent(user, eventId));
    }

    @GetMapping("/calendar/free-busy")
    public ResponseEntity<FreeBusyResponse> getFreeBusy(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        if (from == null) from = Instant.now().truncatedTo(ChronoUnit.DAYS);
        if (to == null) to = from.plus(7, ChronoUnit.DAYS);

        String token = googleAccountService.getValidAccessToken(user);
        return ResponseEntity.ok(googleCalendarApiClient.getFreeBusy(token, from, to));
    }

    @PostMapping("/calendar/events")
    public ResponseEntity<EventListResponse.GoogleEventDto> createEvent(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestBody CreateEventRequest request) {

        String token = googleAccountService.getValidAccessToken(user);
        return ResponseEntity.ok(googleCalendarApiClient.createEvent(token, calendarId, request));
    }
}
