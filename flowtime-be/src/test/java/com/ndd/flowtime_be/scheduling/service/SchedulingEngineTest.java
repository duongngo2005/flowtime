package com.ndd.flowtime_be.scheduling.service;

import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.entity.PlannedSlotStatus;
import com.ndd.flowtime_be.planning.entity.PlanningSession;
import com.ndd.flowtime_be.planning.entity.PlanningSessionStatus;
import com.ndd.flowtime_be.planning.service.PlanningReservationService;
import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.preference.repository.SchedulingPreferenceRepository;
import com.ndd.flowtime_be.scheduling.dto.ScheduledBlockSuggestion;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewRequest;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewResponse;
import com.ndd.flowtime_be.scheduling.dto.UnscheduledTaskReason;
import com.ndd.flowtime_be.task.entity.Task;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;
import com.ndd.flowtime_be.task.repository.TaskRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulingEngineTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private SchedulingPreferenceRepository preferenceRepository;

    @Mock
    private PlanningReservationService planningReservationService;

    private SchedulingEngine schedulingEngine;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").timezone("UTC").build();

    @BeforeEach
    void setUp() {
        schedulingEngine = schedulingEngineAt(Instant.parse("2026-09-01T00:00:00Z"));
    }

    @Test
    void createsSplitSuggestionsAroundBusyCalendarEvents() {
        Task task = task(1L, "Write report", 100, TaskPriority.HIGH, true, 50, null, null);
        CalendarEvent meeting = CalendarEvent.builder()
                .startAt(Instant.parse("2026-09-07T03:00:00Z"))
                .endAt(Instant.parse("2026-09-07T04:00:00Z"))
                .status("confirmed")
                .build();
        stubData(List.of(task), List.of(meeting), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(2, response.suggestions().size());
        assertEquals(Instant.parse("2026-09-07T02:00:00Z"), response.suggestions().get(0).startAt());
        assertEquals(Instant.parse("2026-09-07T02:50:00Z"), response.suggestions().get(0).endAt());
        assertEquals(Instant.parse("2026-09-07T04:00:00Z"), response.suggestions().get(1).startAt());
        assertEquals(100, response.suggestions().stream().mapToInt(ScheduledBlockSuggestion::durationMinutes).sum());
    }

    @Test
    void doesNotTreatAnOptionalHolidayCalendarAsBusyTime() {
        Task task = task(1L, "Plan project", 60, TaskPriority.HIGH, false, null, null, null);
        Calendar holidayCalendar = Calendar.builder().blocksScheduling(false).build();
        CalendarEvent holiday = CalendarEvent.builder()
                .calendar(holidayCalendar)
                .startAt(Instant.parse("2026-09-07T02:00:00Z"))
                .endAt(Instant.parse("2026-09-07T10:00:00Z"))
                .status("confirmed")
                .allDay(true)
                .build();
        stubData(List.of(task), List.of(holiday), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(1, response.suggestions().size());
        assertEquals(Instant.parse("2026-09-07T02:00:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void respectsDailyFocusLimitAndReportsUnscheduledWork() {
        Task firstTask = task(1L, "Urgent task", 60, TaskPriority.URGENT, false, null, null, null);
        Task secondTask = task(2L, "Later task", 60, TaskPriority.LOW, false, null, null, null);
        stubData(List.of(secondTask, firstTask), List.of(), preference(60));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(List.of(1L), response.suggestions().stream().map(ScheduledBlockSuggestion::taskId).toList());
        assertEquals(1, response.unscheduledTasks().size());
        assertEquals(2L, response.unscheduledTasks().getFirst().taskId());
        assertEquals(60, response.unscheduledTasks().getFirst().unscheduledMinutes());
    }

    @Test
    void appliesTaskPreferredTimeAndIgnoresCancelledEvents() {
        Task task = task(1L, "Deep work", 60, TaskPriority.HIGH, false, null,
                LocalTime.of(13, 0), LocalTime.of(16, 0));
        CalendarEvent cancelled = CalendarEvent.builder()
                .startAt(Instant.parse("2026-09-07T13:00:00Z"))
                .endAt(Instant.parse("2026-09-07T15:00:00Z"))
                .status("cancelled")
                .build();
        stubData(List.of(task), List.of(cancelled), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(1, response.suggestions().size());
        assertEquals(Instant.parse("2026-09-07T06:00:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void doesNotLetFutureDayEventsExtendAnEarlierWorkday() {
        Task task = task(1L, "Long task", 500, TaskPriority.HIGH, true, 50, null, null);
        CalendarEvent nextDayMeeting = CalendarEvent.builder()
                .startAt(Instant.parse("2026-09-08T03:00:00Z"))
                .endAt(Instant.parse("2026-09-08T04:00:00Z"))
                .status("confirmed")
                .build();
        SchedulingPreference preference = preference(1_000);
        preference.setWorkingDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        stubData(List.of(task), List.of(nextDayMeeting), preference);

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 2));

        assertEquals(400, response.suggestions().stream()
                .filter(suggestion -> suggestion.startAt().isBefore(Instant.parse("2026-09-08T00:00:00Z")))
                .mapToInt(ScheduledBlockSuggestion::durationMinutes)
                .sum());
        assertEquals(100, response.suggestions().stream()
                .filter(suggestion -> !suggestion.startAt().isBefore(Instant.parse("2026-09-08T00:00:00Z")))
                .mapToInt(ScheduledBlockSuggestion::durationMinutes)
                .sum());
    }

    @Test
    void schedulesOnlyTheRemainingDurationAfterHardCommitments() {
        Task task = task(1L, "Already scheduled", 120, TaskPriority.HIGH, false, null, null, null);
        PlannedSlot reservation = reservation(1L, Instant.parse("2026-09-07T09:00:00Z"), Instant.parse("2026-09-07T10:00:00Z"), 60);
        stubData(List.of(task), List.of(), preference(240), List.of(reservation));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(60, response.suggestions().stream().mapToInt(ScheduledBlockSuggestion::durationMinutes).sum());
        assertEquals(Instant.parse("2026-09-07T02:00:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void respectsPerTaskDailyCapAcrossMultipleDays() {
        Task task = task(1L, "SIMI", 720, TaskPriority.HIGH, true, null, null, null);
        task.setMaxDailyMinutes(180);
        SchedulingPreference preference = preference(480);
        preference.setWorkingDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));
        stubData(List.of(task), List.of(), preference);

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 4));

        assertEquals(720, response.suggestions().stream().mapToInt(ScheduledBlockSuggestion::durationMinutes).sum());
        assertTrue(response.suggestions().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        suggestion -> LocalDate.ofInstant(suggestion.startAt(), ZoneId.of("Asia/Ho_Chi_Minh")),
                        java.util.stream.Collectors.summingInt(ScheduledBlockSuggestion::durationMinutes)
                ))
                .values()
                .stream()
                .allMatch(minutes -> minutes <= 180));
    }

    @Test
    void leavesResidualWorkUnscheduledWhenItIsShorterThanMinimumSession() {
        Task task = task(1L, "Read chapter", 100, TaskPriority.HIGH, true, 60, null, null);
        stubData(List.of(task), List.of(), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(60, response.suggestions().stream().mapToInt(ScheduledBlockSuggestion::durationMinutes).sum());
        assertEquals(40, response.unscheduledTasks().getFirst().unscheduledMinutes());
        assertTrue(response.suggestions().stream().allMatch(slot -> slot.durationMinutes() >= 60));
    }

    @Test
    void doesNotSchedulePastTimeTodayAndRoundsUpToTheNextMinute() {
        Task task = task(1L, "Write summary", 30, TaskPriority.HIGH, false, null, null, null);
        stubData(List.of(task), List.of(), preference(240));
        SchedulingEngine engine = schedulingEngineAt(Instant.parse("2026-09-07T09:00:30Z"));

        SchedulingPreviewResponse response = engine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1)
        );

        assertEquals(Instant.parse("2026-09-07T09:01:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void doesNotLetCurrentTimeChangeFutureWorkdayStart() {
        Task task = task(1L, "Write summary", 30, TaskPriority.HIGH, false, null, null, null);
        SchedulingPreference preference = preference(240);
        preference.setWorkingDays(EnumSet.of(DayOfWeek.TUESDAY));
        stubData(List.of(task), List.of(), preference);
        SchedulingEngine engine = schedulingEngineAt(Instant.parse("2026-09-07T16:50:30Z"));

        SchedulingPreviewResponse response = engine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 8), 1)
        );

        assertEquals(Instant.parse("2026-09-08T02:00:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void skipsPastDaysWhenTheRequestedWindowStartsBeforeToday() {
        Task task = task(1L, "Write summary", 30, TaskPriority.HIGH, false, null, null, null);
        SchedulingPreference preference = preference(240);
        preference.setWorkingDays(EnumSet.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY));
        stubData(List.of(task), List.of(), preference);
        SchedulingEngine engine = schedulingEngineAt(Instant.parse("2026-09-07T09:00:00Z"));

        SchedulingPreviewResponse response = engine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 6), 2)
        );

        assertEquals(Instant.parse("2026-09-07T09:00:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void doesNotScheduleWholeTaskWhenDeadlineLeavesTooLittleTime() {
        Task task = task(1L, "Send report", 60, TaskPriority.HIGH, false, null, null, null);
        task.setDeadline(Instant.parse("2026-09-07T02:30:00Z"));
        stubData(List.of(task), List.of(), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1)
        );

        assertEquals(0, response.suggestions().size());
        assertEquals(UnscheduledTaskReason.NO_AVAILABLE_SLOT, response.unscheduledTasks().getFirst().reason());
    }

    @Test
    void schedulesTaskWhenItsSlotEndsExactlyAtDeadline() {
        Task task = task(1L, "Send report", 60, TaskPriority.HIGH, false, null, null, null);
        task.setDeadline(Instant.parse("2026-09-07T03:00:00Z"));
        stubData(List.of(task), List.of(), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1)
        );

        assertEquals(1, response.suggestions().size());
        assertEquals(Instant.parse("2026-09-07T03:00:00Z"), response.suggestions().getFirst().endAt());
    }

    @Test
    void marksTaskWithElapsedDeadlineUsingDomainReason() {
        Task task = task(1L, "Send report", 60, TaskPriority.HIGH, false, null, null, null);
        task.setDeadline(Instant.parse("2026-09-07T08:59:00Z"));
        stubData(List.of(task), List.of(), preference(240));
        SchedulingEngine engine = schedulingEngineAt(Instant.parse("2026-09-07T09:00:00Z"));

        SchedulingPreviewResponse response = engine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1)
        );

        assertEquals(0, response.suggestions().size());
        assertEquals(UnscheduledTaskReason.DEADLINE_PASSED, response.unscheduledTasks().getFirst().reason());
    }

    @Test
    void neverCreatesSplitSlotAfterDeadlineAndReportsRemainingWork() {
        Task task = task(1L, "Prepare slides", 120, TaskPriority.HIGH, true, 30, null, null);
        task.setDeadline(Instant.parse("2026-09-07T03:10:00Z"));
        stubData(List.of(task), List.of(), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(
                user,
                new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1)
        );

        assertEquals(1, response.suggestions().size());
        assertEquals(Instant.parse("2026-09-07T02:50:00Z"), response.suggestions().getFirst().endAt());
        assertEquals(UnscheduledTaskReason.INSUFFICIENT_DURATION, response.unscheduledTasks().getFirst().reason());
        assertEquals(70, response.unscheduledTasks().getFirst().unscheduledMinutes());
        assertTrue(response.suggestions().stream()
                .allMatch(suggestion -> !suggestion.endAt().isAfter(task.getDeadline())));
    }

    private void stubData(List<Task> tasks, List<CalendarEvent> events, SchedulingPreference preference) {
        stubData(tasks, events, preference, List.of());
    }

    private void stubData(
            List<Task> tasks,
            List<CalendarEvent> events,
            SchedulingPreference preference,
            List<PlannedSlot> hardReservations) {
        when(taskRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(tasks);
        when(calendarEventRepository.findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(any(), any(), any()))
                .thenReturn(events);
        when(preferenceRepository.findByUser(user)).thenReturn(Optional.of(preference));
        when(planningReservationService.hardReservationsFor(user)).thenReturn(hardReservations);
    }

    private SchedulingPreference preference(int dailyLimit) {
        return SchedulingPreference.builder()
                .user(user)
                .workdayStartTime(LocalTime.of(9, 0))
                .workdayEndTime(LocalTime.of(17, 0))
                .workingDays(EnumSet.of(DayOfWeek.MONDAY))
                .focusDurationMinutes(50)
                .breakDurationMinutes(10)
                .dailyFocusLimit(dailyLimit)
                .build();
    }

    private SchedulingEngine schedulingEngineAt(Instant now) {
        return new SchedulingEngine(
                taskRepository,
                calendarEventRepository,
                preferenceRepository,
                planningReservationService,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    private Task task(
            Long id,
            String title,
            int duration,
            TaskPriority priority,
            boolean splitAllowed,
            Integer minSession,
            LocalTime preferredStart,
            LocalTime preferredEnd) {
        return Task.builder()
                .id(id)
                .user(user)
                .title(title)
                .estimatedDuration(duration)
                .priority(priority)
                .status(TaskStatus.TODO)
                .splitAllowed(splitAllowed)
                .minSessionDuration(minSession)
                .preferredStartTime(preferredStart)
                .preferredEndTime(preferredEnd)
                .createdAt(Instant.parse("2026-09-01T09:00:00Z"))
                .build();
    }

    private PlannedSlot reservation(Long taskId, Instant startAt, Instant endAt, int durationMinutes) {
        return PlannedSlot.builder()
                .id(100L)
                .planningSession(PlanningSession.builder().status(PlanningSessionStatus.APPROVED).build())
                .taskId(taskId)
                .taskTitle("Reserved")
                .startAt(startAt)
                .endAt(endAt)
                .durationMinutes(durationMinutes)
                .status(PlannedSlotStatus.ACCEPTED)
                .build();
    }
}
