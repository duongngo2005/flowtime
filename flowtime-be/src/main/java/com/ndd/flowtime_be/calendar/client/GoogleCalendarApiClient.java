package com.ndd.flowtime_be.calendar.client;

import com.ndd.flowtime_be.calendar.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarApiClient {

    private static final String BASE_URL = "https://www.googleapis.com/calendar/v3";

    private final RestClient restClient;

    public CalendarListResponse listCalendars(String accessToken) {
        log.debug("Fetching calendar list from Google API");
        return restClient.get()
                .uri(BASE_URL + "/users/me/calendarList")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(CalendarListResponse.class);
    }

    public EventListResponse listEvents(String accessToken, String calendarId, Instant from, Instant to) {
        log.debug("Fetching events for calendar {} from {} to {}", calendarId, from, to);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("www.googleapis.com")
                        .path("/calendar/v3/calendars/{calendarId}/events")
                        .queryParam("timeMin", from.toString())
                        .queryParam("timeMax", to.toString())
                        .queryParam("singleEvents", "true")
                        .queryParam("orderBy", "startTime")
                        .build(calendarId))
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(EventListResponse.class);
    }

    public FreeBusyResponse getFreeBusy(String accessToken, Instant from, Instant to) {
        return getFreeBusy(accessToken, List.of("primary"), from, to);
    }

    public FreeBusyResponse getFreeBusy(String accessToken, List<String> calendarIds, Instant from, Instant to) {
        log.debug("Fetching free/busy from {} to {}", from, to);

        Map<String, Object> requestBody = Map.of(
                "timeMin", from.toString(),
                "timeMax", to.toString(),
                "items", calendarIds.stream().map(id -> Map.of("id", id)).toList()
        );

        return restClient.post()
                .uri(BASE_URL + "/freeBusy")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(FreeBusyResponse.class);
    }

    public Optional<EventListResponse.GoogleEventDto> getEvent(
            String accessToken,
            String calendarId,
            String eventId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri(BASE_URL + "/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(EventListResponse.GoogleEventDto.class));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    public EventListResponse.GoogleEventDto createEvent(String accessToken, String calendarId,
                                                        CreateEventRequest event) {
        log.debug("Creating event '{}' in calendar {}", event.summary(), calendarId);
        return restClient.post()
                .uri(BASE_URL + "/calendars/{calendarId}/events", calendarId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .body(EventListResponse.GoogleEventDto.class);
    }
}
