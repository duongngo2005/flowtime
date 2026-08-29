package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.calendar.client.GoogleCalendarApiClient;
import com.ndd.flowtime_be.calendar.dto.CreateEventRequest;
import com.ndd.flowtime_be.calendar.dto.EventListResponse;
import com.ndd.flowtime_be.calendar.dto.FreeBusyResponse;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.repository.CalendarRepository;
import com.ndd.flowtime_be.calendar.service.CalendarEventUpsertService;
import com.ndd.flowtime_be.google_account.service.GoogleAccountService;
import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotApplyStatus;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlanningApplyService {

    private static final String PRIMARY_CALENDAR_ALIAS = "primary";

    private final PlanningApplyStateService applyStateService;
    private final PlannedSlotRepository plannedSlotRepository;
    private final CalendarRepository calendarRepository;
    private final GoogleAccountService googleAccountService;
    private final GoogleCalendarApiClient googleCalendarApiClient;
    private final CalendarEventUpsertService calendarEventUpsertService;

    /**
     * Orchestrates short database transactions around remote calls; it deliberately is not transactional itself.
     */
    public void apply(User user, Long planningId) {
        applyStateService.claim(user, planningId);
        try {
            String accessToken = googleAccountService.getValidAccessToken(user);
            List<PlannedSlot> applyingSlots = applyingSlots(planningId);
            Map<Long, Calendar> targetCalendars = resolveTargetCalendars(user, applyingSlots);

            List<PlannedSlot> missingRemoteEvents = recoverExistingEvents(
                    user,
                    accessToken,
                    applyingSlots,
                    targetCalendars,
                    planningId
            );
            revalidateAvailability(accessToken, user, missingRemoteEvents, targetCalendars);

            for (PlannedSlot slot : missingRemoteEvents) {
                applyMissingSlot(user, accessToken, slot, targetCalendars.get(slot.getId()), planningId);
            }

            applyStateService.markApplied(user, planningId);
        } catch (RuntimeException exception) {
            applyStateService.markApplyFailed(user, planningId, errorMessage(exception));
        }
    }

    private List<PlannedSlot> applyingSlots(Long planningId) {
        return plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(planningId).stream()
                .filter(slot -> slot.getApplyStatus() == PlannedSlotApplyStatus.APPLYING)
                .toList();
    }

    private Map<Long, Calendar> resolveTargetCalendars(User user, List<PlannedSlot> slots) {
        Map<Long, Calendar> targets = new HashMap<>();
        for (PlannedSlot slot : slots) {
            Calendar target = resolveTargetCalendar(user, slot.getGoogleCalendarId());
            if (!target.getGoogleCalendarId().equals(slot.getGoogleCalendarId())) {
                slot.setGoogleCalendarId(target.getGoogleCalendarId());
                plannedSlotRepository.save(slot);
            }
            targets.put(slot.getId(), target);
        }
        return targets;
    }

    private Calendar resolveTargetCalendar(User user, String calendarId) {
        if (PRIMARY_CALENDAR_ALIAS.equals(calendarId)) {
            return calendarRepository.findByUserAndPrimaryTrue(user)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A synced primary calendar is required before applying a plan."
                    ));
        }
        return calendarRepository.findByUserAndGoogleCalendarId(user, calendarId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Target calendar is not available locally."));
    }

    private List<PlannedSlot> recoverExistingEvents(
            User user,
            String accessToken,
            List<PlannedSlot> slots,
            Map<Long, Calendar> targetCalendars,
            Long planningId) {
        List<PlannedSlot> missing = new ArrayList<>();
        for (PlannedSlot slot : slots) {
            try {
                Optional<EventListResponse.GoogleEventDto> existing = googleCalendarApiClient.getEvent(
                        accessToken,
                        targetCalendars.get(slot.getId()).getGoogleCalendarId(),
                        slot.getGoogleEventId()
                );
                if (existing.isPresent()) {
                    if ("cancelled".equalsIgnoreCase(existing.get().status())) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "A previously created FlowTime event was cancelled in Google Calendar."
                        );
                    }
                    calendarEventUpsertService.upsert(user, targetCalendars.get(slot.getId()), existing.get());
                    applyStateService.markSlotApplied(planningId, slot.getId());
                } else {
                    missing.add(slot);
                }
            } catch (RuntimeException exception) {
                applyStateService.markSlotFailed(planningId, slot.getId(), errorMessage(exception));
                throw exception;
            }
        }
        return missing;
    }

    private void revalidateAvailability(
            String accessToken,
            User user,
            List<PlannedSlot> slots,
            Map<Long, Calendar> targetCalendars) {
        if (slots.isEmpty()) {
            return;
        }

        Instant from = slots.stream().map(PlannedSlot::getStartAt).min(Instant::compareTo).orElseThrow();
        Instant to = slots.stream().map(PlannedSlot::getEndAt).max(Instant::compareTo).orElseThrow();
        List<String> calendarIds = calendarRepository.findByUser(user).stream()
                .map(Calendar::getGoogleCalendarId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        if (calendarIds.isEmpty()) {
            calendarIds = targetCalendars.values().stream()
                    .map(Calendar::getGoogleCalendarId)
                    .distinct()
                    .toList();
        }

        FreeBusyResponse response = googleCalendarApiClient.getFreeBusy(accessToken, calendarIds, from, to);
        if (response == null || response.calendars() == null) {
            throw new IllegalStateException("Google FreeBusy returned no calendar availability data.");
        }
        for (Map.Entry<String, FreeBusyResponse.CalendarBusy> entry : response.calendars().entrySet()) {
            if (entry.getValue().errors() != null && !entry.getValue().errors().isEmpty()) {
                throw new IllegalStateException("Google FreeBusy could not check calendar " + entry.getKey() + ".");
            }
        }

        for (PlannedSlot slot : slots) {
            if (hasConflict(slot, response)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A newly busy Google Calendar period conflicts with planned slot " + slot.getId() + "."
                );
            }
        }
    }

    private boolean hasConflict(PlannedSlot slot, FreeBusyResponse response) {
        return response.calendars().values().stream()
                .filter(Objects::nonNull)
                .flatMap(calendar -> calendar.busy() == null ? java.util.stream.Stream.empty() : calendar.busy().stream())
                .anyMatch(period -> overlaps(slot, period));
    }

    private boolean overlaps(PlannedSlot slot, FreeBusyResponse.BusyPeriod period) {
        try {
            Instant busyStart = Instant.parse(period.start());
            Instant busyEnd = Instant.parse(period.end());
            return busyStart.isBefore(slot.getEndAt()) && busyEnd.isAfter(slot.getStartAt());
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("Google FreeBusy returned an invalid busy interval.", exception);
        }
    }

    private void applyMissingSlot(
            User user,
            String accessToken,
            PlannedSlot slot,
            Calendar targetCalendar,
            Long planningId) {
        try {
            EventListResponse.GoogleEventDto event = createOrRecoverEvent(accessToken, slot, targetCalendar);
            calendarEventUpsertService.upsert(user, targetCalendar, event);
            applyStateService.markSlotApplied(planningId, slot.getId());
        } catch (RuntimeException exception) {
            applyStateService.markSlotFailed(planningId, slot.getId(), errorMessage(exception));
            throw exception;
        }
    }

    private EventListResponse.GoogleEventDto createOrRecoverEvent(
            String accessToken,
            PlannedSlot slot,
            Calendar targetCalendar) {
        try {
            return googleCalendarApiClient.createEvent(
                    accessToken,
                    targetCalendar.getGoogleCalendarId(),
                    eventRequest(slot, targetCalendar)
            );
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 409) {
                throw exception;
            }
            return googleCalendarApiClient.getEvent(
                    accessToken,
                    targetCalendar.getGoogleCalendarId(),
                    slot.getGoogleEventId()
            ).orElseThrow(() -> exception);
        }
    }

    private CreateEventRequest eventRequest(PlannedSlot slot, Calendar targetCalendar) {
        EventListResponse.EventDateTimeDto start = new EventListResponse.EventDateTimeDto(
                slot.getStartAt().toString(),
                null,
                targetCalendar.getTimezone()
        );
        EventListResponse.EventDateTimeDto end = new EventListResponse.EventDateTimeDto(
                slot.getEndAt().toString(),
                null,
                targetCalendar.getTimezone()
        );
        return new CreateEventRequest(
                slot.getGoogleEventId(),
                slot.getTaskTitle(),
                "Scheduled by FlowTime.",
                start,
                end,
                new CreateEventRequest.ExtendedProperties(Map.of("flowtimeSlotId", slot.getId().toString()))
        );
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
