package com.ndd.flowtime_be.scheduling.service;

import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.preference.repository.SchedulingPreferenceRepository;
import com.ndd.flowtime_be.scheduling.dto.ScheduledBlockSuggestion;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewRequest;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewResponse;
import com.ndd.flowtime_be.task.entity.Task;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;
import com.ndd.flowtime_be.task.repository.TaskRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @InjectMocks
    private SchedulingEngine schedulingEngine;

    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").timezone("UTC").build();

    @Test
    void createsSplitSuggestionsAroundBusyCalendarEvents() {
        Task task = task(1L, "Write report", 100, TaskPriority.HIGH, true, 50, null, null);
        CalendarEvent meeting = CalendarEvent.builder()
                .startAt(Instant.parse("2026-09-07T10:00:00Z"))
                .endAt(Instant.parse("2026-09-07T11:00:00Z"))
                .status("confirmed")
                .build();
        stubData(List.of(task), List.of(meeting), preference(240));

        SchedulingPreviewResponse response = schedulingEngine.preview(user, new SchedulingPreviewRequest(LocalDate.of(2026, 9, 7), 1));

        assertEquals(2, response.suggestions().size());
        assertEquals(Instant.parse("2026-09-07T09:00:00Z"), response.suggestions().get(0).startAt());
        assertEquals(Instant.parse("2026-09-07T09:50:00Z"), response.suggestions().get(0).endAt());
        assertEquals(Instant.parse("2026-09-07T11:00:00Z"), response.suggestions().get(1).startAt());
        assertEquals(100, response.suggestions().stream().mapToInt(ScheduledBlockSuggestion::durationMinutes).sum());
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
        assertEquals(Instant.parse("2026-09-07T13:00:00Z"), response.suggestions().getFirst().startAt());
    }

    @Test
    void doesNotLetFutureDayEventsExtendAnEarlierWorkday() {
        Task task = task(1L, "Long task", 500, TaskPriority.HIGH, true, 50, null, null);
        CalendarEvent nextDayMeeting = CalendarEvent.builder()
                .startAt(Instant.parse("2026-09-08T10:00:00Z"))
                .endAt(Instant.parse("2026-09-08T11:00:00Z"))
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

    private void stubData(List<Task> tasks, List<CalendarEvent> events, SchedulingPreference preference) {
        when(taskRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(tasks);
        when(calendarEventRepository.findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(any(), any(), any()))
                .thenReturn(events);
        when(preferenceRepository.findByUser(user)).thenReturn(Optional.of(preference));
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
}
