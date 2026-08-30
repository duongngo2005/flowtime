package com.ndd.flowtime_be.planning.service;

import com.ndd.flowtime_be.calendar.client.GoogleCalendarApiClient;
import com.ndd.flowtime_be.calendar.dto.CreateEventRequest;
import com.ndd.flowtime_be.calendar.dto.EventListResponse;
import com.ndd.flowtime_be.calendar.dto.FreeBusyResponse;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.calendar.repository.CalendarRepository;
import com.ndd.flowtime_be.calendar.service.CalendarEventUpsertService;
import com.ndd.flowtime_be.google_account.service.GoogleAccountService;
import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotApplyStatus;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.planning.repository.PlannedSlotRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningApplyServiceTest {

    @Mock
    private PlanningApplyStateService applyStateService;

    @Mock
    private PlannedSlotRepository plannedSlotRepository;

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private GoogleAccountService googleAccountService;

    @Mock
    private GoogleCalendarApiClient googleCalendarApiClient;

    @Mock
    private CalendarEventUpsertService calendarEventUpsertService;

    @InjectMocks
    private PlanningApplyService planningApplyService;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").build();
    private final Calendar primaryCalendar = Calendar.builder()
            .id(7L)
            .user(user)
            .googleCalendarId("primary@example.com")
            .name("Primary")
            .timezone("UTC")
            .primary(true)
            .build();

    @BeforeEach
    void setUp() {
        when(googleAccountService.getValidAccessToken(user)).thenReturn("access-token");
        when(calendarRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(primaryCalendar));
    }

    @Test
    void appliesMissingGoogleEventAndUpsertsLocalCalendarEvent() {
        PlannedSlot slot = applyingSlot(1L, "ftabcde123");
        EventListResponse.GoogleEventDto remoteEvent = googleEvent(slot);
        stubApplyingSlots(slot);
        when(googleCalendarApiClient.getEvent(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(googleCalendarApiClient.getFreeBusy(anyString(), anyList(), any(), any())).thenReturn(emptyFreeBusy());
        when(googleCalendarApiClient.createEvent(anyString(), anyString(), any())).thenReturn(remoteEvent);

        planningApplyService.apply(user, 10L);

        ArgumentCaptor<CreateEventRequest> requestCaptor = ArgumentCaptor.forClass(CreateEventRequest.class);
        verify(googleCalendarApiClient).createEvent(eq("access-token"), eq("primary@example.com"), requestCaptor.capture());
        assertEquals(slot.getGoogleEventId(), requestCaptor.getValue().id());
        verify(calendarEventUpsertService).upsert(user, primaryCalendar, remoteEvent);
        verify(applyStateService).markSlotApplied(10L, 1L);
        verify(applyStateService).markApplied(user, 10L);
    }

    @Test
    void recoversExistingGoogleEventWithoutInsertingDuplicate() {
        PlannedSlot slot = applyingSlot(1L, "ftabcde123");
        EventListResponse.GoogleEventDto remoteEvent = googleEvent(slot);
        stubApplyingSlots(slot);
        when(googleCalendarApiClient.getEvent("access-token", "primary@example.com", "ftabcde123"))
                .thenReturn(Optional.of(remoteEvent));

        planningApplyService.apply(user, 10L);

        verify(googleCalendarApiClient, never()).createEvent(anyString(), anyString(), any());
        verify(calendarEventUpsertService).upsert(user, primaryCalendar, remoteEvent);
        verify(applyStateService).markSlotApplied(10L, 1L);
        verify(applyStateService).markApplied(user, 10L);
    }

    @Test
    void retryRecoversRemoteEventAfterLocalPersistenceFailedWithoutDuplicate() {
        PlannedSlot slot = applyingSlot(1L, "ftabcde123");
        EventListResponse.GoogleEventDto remoteEvent = googleEvent(slot);
        stubApplyingSlots(slot);
        when(googleCalendarApiClient.getEvent(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty(), Optional.of(remoteEvent));
        when(calendarRepository.findByUserAndGoogleCalendarId(user, "primary@example.com"))
                .thenReturn(Optional.of(primaryCalendar));
        when(googleCalendarApiClient.getFreeBusy(anyString(), anyList(), any(), any())).thenReturn(emptyFreeBusy());
        when(googleCalendarApiClient.createEvent(anyString(), anyString(), any())).thenReturn(remoteEvent);
        when(calendarEventUpsertService.upsert(user, primaryCalendar, remoteEvent))
                .thenThrow(new IllegalStateException("Local database unavailable"))
                .thenReturn(new CalendarEvent());

        planningApplyService.apply(user, 10L);
        planningApplyService.apply(user, 10L);

        verify(googleCalendarApiClient, times(1)).createEvent(anyString(), anyString(), any());
        verify(applyStateService).markSlotFailed(10L, 1L, "Local database unavailable");
        verify(applyStateService).markApplyFailed(user, 10L, "Local database unavailable");
        verify(applyStateService).markSlotApplied(10L, 1L);
    }

    @Test
    void recordsPartialFailureWithoutTouchingAlreadyAppliedSlots() {
        PlannedSlot first = applyingSlot(1L, "ftabcde123");
        PlannedSlot second = applyingSlot(2L, "ftabcde456");
        stubApplyingSlots(first, second);
        when(googleCalendarApiClient.getEvent(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(googleCalendarApiClient.getFreeBusy(anyString(), anyList(), any(), any())).thenReturn(emptyFreeBusy());
        when(googleCalendarApiClient.createEvent(eq("access-token"), eq("primary@example.com"), any()))
                .thenReturn(googleEvent(first))
                .thenThrow(new IllegalStateException("Google request failed"));

        planningApplyService.apply(user, 10L);

        verify(applyStateService).markSlotApplied(10L, 1L);
        verify(applyStateService).markSlotFailed(10L, 2L, "Google request failed");
        verify(applyStateService).markApplyFailed(user, 10L, "Google request failed");
        verify(applyStateService, never()).markApplied(user, 10L);
    }

    @Test
    void rejectsNewGoogleCalendarConflictBeforeCreatingEvents() {
        PlannedSlot slot = applyingSlot(1L, "ftabcde123");
        stubApplyingSlots(slot);
        when(googleCalendarApiClient.getEvent(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        FreeBusyResponse conflict = new FreeBusyResponse(Map.of(
                "primary@example.com",
                new FreeBusyResponse.CalendarBusy(
                        List.of(new FreeBusyResponse.BusyPeriod("2026-09-07T09:30:00Z", "2026-09-07T10:30:00Z")),
                        List.of()
                )
        ));
        when(googleCalendarApiClient.getFreeBusy(anyString(), anyList(), any(), any())).thenReturn(conflict);

        planningApplyService.apply(user, 10L);

        verify(googleCalendarApiClient, never()).createEvent(anyString(), anyString(), any());
        verify(applyStateService).markApplyFailed(
                eq(user),
                eq(10L),
                contains("xung đột với khung giờ đã lên lịch")
        );
    }

    @Test
    void revalidatesOnlyTargetCalendarAndDoesNotReadAllSyncedCalendars() {
        PlannedSlot slot = applyingSlot(1L, "ftabcde123");
        EventListResponse.GoogleEventDto remoteEvent = googleEvent(slot);
        stubApplyingSlots(slot);
        when(googleCalendarApiClient.getEvent(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(googleCalendarApiClient.getFreeBusy(anyString(), anyList(), any(), any())).thenReturn(emptyFreeBusy());
        when(googleCalendarApiClient.createEvent(anyString(), anyString(), any())).thenReturn(remoteEvent);

        planningApplyService.apply(user, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> calendarIds = ArgumentCaptor.forClass(List.class);
        verify(googleCalendarApiClient).getFreeBusy(eq("access-token"), calendarIds.capture(), any(), any());
        assertEquals(List.of("primary@example.com"), calendarIds.getValue());
        verify(calendarRepository, never()).findByUser(user);
        verify(googleCalendarApiClient).createEvent(eq("access-token"), eq("primary@example.com"), any());
        verify(applyStateService).markApplied(user, 10L);
    }

    private void stubApplyingSlots(PlannedSlot... slots) {
        doNothing().when(applyStateService).claim(user, 10L);
        when(plannedSlotRepository.findByPlanningSessionIdOrderByStartAtAsc(10L)).thenReturn(List.of(slots));
    }

    private PlannedSlot applyingSlot(Long id, String googleEventId) {
        return PlannedSlot.builder()
                .id(id)
                .planningSession(PlanningSession.builder().id(10L).user(user).build())
                .taskId(id)
                .taskTitle("Task " + id)
                .startAt(Instant.parse("2026-09-07T09:00:00Z"))
                .endAt(Instant.parse("2026-09-07T10:00:00Z"))
                .durationMinutes(60)
                .status(PlannedSlotStatus.ACCEPTED)
                .googleCalendarId("primary")
                .googleEventId(googleEventId)
                .applyStatus(PlannedSlotApplyStatus.APPLYING)
                .build();
    }

    private EventListResponse.GoogleEventDto googleEvent(PlannedSlot slot) {
        return new EventListResponse.GoogleEventDto(
                slot.getGoogleEventId(),
                slot.getTaskTitle(),
                "Scheduled by FlowTime.",
                new EventListResponse.EventDateTimeDto(slot.getStartAt().toString(), null, "UTC"),
                new EventListResponse.EventDateTimeDto(slot.getEndAt().toString(), null, "UTC"),
                "confirmed"
        );
    }

    private FreeBusyResponse emptyFreeBusy() {
        return new FreeBusyResponse(Map.of(
                "primary@example.com",
                new FreeBusyResponse.CalendarBusy(List.of(), List.of())
        ));
    }
}
