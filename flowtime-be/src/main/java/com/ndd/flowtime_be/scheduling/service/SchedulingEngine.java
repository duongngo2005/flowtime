package com.ndd.flowtime_be.scheduling.service;

import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.shared.time.VietnamTime;
import com.ndd.flowtime_be.planning.entity.PlannedSlot;
import com.ndd.flowtime_be.planning.service.PlanningReservationService;
import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.preference.repository.SchedulingPreferenceRepository;
import com.ndd.flowtime_be.scheduling.dto.ScheduledBlockSuggestion;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewRequest;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewResponse;
import com.ndd.flowtime_be.scheduling.dto.UnscheduledTaskReason;
import com.ndd.flowtime_be.scheduling.dto.UnscheduledTaskSuggestion;
import com.ndd.flowtime_be.task.entity.Task;
import com.ndd.flowtime_be.task.entity.TaskPriority;
import com.ndd.flowtime_be.task.entity.TaskStatus;
import com.ndd.flowtime_be.task.repository.TaskRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SchedulingEngine {

    private final TaskRepository taskRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final SchedulingPreferenceRepository preferenceRepository;
    private final PlanningReservationService planningReservationService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public SchedulingPreviewResponse preview(User user, SchedulingPreviewRequest request) {
        ZoneId timezone = VietnamTime.ZONE;
        Instant now = Instant.now(clock);
        LocalDate startDate = request.startDate() == null ? LocalDate.ofInstant(now, timezone) : request.startDate();
        LocalDate endDate = startDate.plusDays(request.days());
        Instant from = startDate.atStartOfDay(timezone).toInstant();
        Instant to = endDate.atStartOfDay(timezone).toInstant();
        SchedulingPreference preference = preferenceRepository.findByUser(user)
                .orElseGet(() -> SchedulingPreference.builder().user(user).build());

        List<BusyInterval> busyIntervals = calendarEventRepository
                .findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(user, to, from)
                .stream()
                .filter(event -> event.getCalendar() == null || event.getCalendar().isBlocksScheduling())
                .filter(event -> !"cancelled".equalsIgnoreCase(event.getStatus()))
                .map(event -> new BusyInterval(event.getStartAt(), event.getEndAt()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<PlannedSlot> hardReservations = planningReservationService.hardReservationsFor(user);
        hardReservations.stream()
                .map(slot -> new BusyInterval(slot.getStartAt(), slot.getEndAt()))
                .forEach(busyIntervals::add);
        busyIntervals.sort(Comparator.comparing(BusyInterval::startAt));

        List<FreeSlot> freeSlots = buildFreeSlots(startDate, request.days(), timezone, preference, busyIntervals, now);
        Map<LocalDate, Integer> scheduledMinutesByDay = scheduledMinutesByDay(hardReservations, timezone);
        Map<TaskDay, Integer> scheduledMinutesByTaskAndDay = scheduledMinutesByTaskAndDay(hardReservations, timezone);
        List<ScheduledBlockSuggestion> suggestions = new ArrayList<>();
        List<UnscheduledTaskSuggestion> unscheduled = new ArrayList<>();

        for (SchedulableTask schedulableTask : schedulableTasks(user, hardReservations)) {
            Task task = schedulableTask.task();
            int remaining = schedulableTask.remainingDuration();
            if (task.isSplitAllowed()) {
                remaining = scheduleSplitTask(task, remaining, freeSlots, scheduledMinutesByDay, scheduledMinutesByTaskAndDay, preference,
                        timezone, suggestions);
            } else {
                remaining = scheduleWholeTask(task, remaining, freeSlots, scheduledMinutesByDay, scheduledMinutesByTaskAndDay, preference,
                        timezone, suggestions);
            }

            if (remaining > 0) {
                unscheduled.add(new UnscheduledTaskSuggestion(
                        task.getId(),
                        task.getTitle(),
                        remaining,
                        unscheduledReason(task, now)
                ));
            }
        }

        suggestions.sort(Comparator.comparing(ScheduledBlockSuggestion::startAt));
        return new SchedulingPreviewResponse(
                startDate,
                endDate,
                timezone.getId(),
                now,
                List.copyOf(suggestions),
                List.copyOf(unscheduled)
        );
    }

    private List<SchedulableTask> schedulableTasks(User user, List<PlannedSlot> hardReservations) {
        Map<Long, Integer> committedMinutesByTask = hardReservations.stream()
                .collect(HashMap::new, (minutes, slot) -> minutes.merge(slot.getTaskId(), slot.getDurationMinutes(), Integer::sum), HashMap::putAll);
        return taskRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(task -> task.getStatus() == TaskStatus.TODO || task.getStatus() == TaskStatus.IN_PROGRESS)
                .map(task -> new SchedulableTask(
                        task,
                        Math.max(0, task.getEstimatedDuration() - committedMinutesByTask.getOrDefault(task.getId(), 0))
                ))
                .filter(task -> task.remainingDuration() > 0)
                .sorted(Comparator
                        .comparing((SchedulableTask task) -> task.task().getDeadline(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(task -> priorityRank(task.task().getPriority()))
                        .thenComparing(task -> task.task().getCreatedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private int scheduleWholeTask(
            Task task,
            int remaining,
            List<FreeSlot> freeSlots,
            Map<LocalDate, Integer> scheduledMinutesByDay,
            Map<TaskDay, Integer> scheduledMinutesByTaskAndDay,
            SchedulingPreference preference,
            ZoneId timezone,
            List<ScheduledBlockSuggestion> suggestions) {
        for (int index = 0; index < freeSlots.size(); index++) {
            FreeSlot slot = freeSlots.get(index);
            FreeSlot eligibleSlot = constrainToTaskConstraints(slot, task, timezone);
            if (eligibleSlot == null || availableMinutes(task, slot, scheduledMinutesByDay, scheduledMinutesByTaskAndDay, preference) < remaining) {
                continue;
            }

            if (minutesBetween(eligibleSlot.startAt(), eligibleSlot.endAt()) >= remaining) {
                Instant endAt = eligibleSlot.startAt().plus(remaining, ChronoUnit.MINUTES);
                addSuggestion(task, eligibleSlot.startAt(), endAt, remaining, suggestions);
                scheduledMinutesByDay.merge(slot.date(), remaining, Integer::sum);
                scheduledMinutesByTaskAndDay.merge(new TaskDay(task.getId(), slot.date()), remaining, Integer::sum);
                reserveSlot(freeSlots, index, eligibleSlot.startAt(), endAt, preference.getBreakDurationMinutes());
                return 0;
            }
        }
        return remaining;
    }

    private int scheduleSplitTask(
            Task task,
            int remaining,
            List<FreeSlot> freeSlots,
            Map<LocalDate, Integer> scheduledMinutesByDay,
            Map<TaskDay, Integer> scheduledMinutesByTaskAndDay,
            SchedulingPreference preference,
            ZoneId timezone,
            List<ScheduledBlockSuggestion> suggestions) {
        int minimumSession = task.getMinSessionDuration() == null ? 1 : task.getMinSessionDuration();

        for (int index = 0; index < freeSlots.size() && remaining > 0; index++) {
            if (remaining < minimumSession) {
                break;
            }
            FreeSlot slot = freeSlots.get(index);
            FreeSlot eligibleSlot = constrainToTaskConstraints(slot, task, timezone);
            if (eligibleSlot == null) {
                continue;
            }

            int available = Math.min(
                    minutesBetween(eligibleSlot.startAt(), eligibleSlot.endAt()),
                    availableMinutes(task, slot, scheduledMinutesByDay, scheduledMinutesByTaskAndDay, preference)
            );
            int desired = Math.min(
                    desiredSessionMinutes(remaining, minimumSession, preference.getFocusDurationMinutes()),
                    available
            );
            if (desired < minimumSession) {
                continue;
            }

            Instant endAt = eligibleSlot.startAt().plus(desired, ChronoUnit.MINUTES);
            addSuggestion(task, eligibleSlot.startAt(), endAt, desired, suggestions);
            scheduledMinutesByDay.merge(slot.date(), desired, Integer::sum);
            scheduledMinutesByTaskAndDay.merge(new TaskDay(task.getId(), slot.date()), desired, Integer::sum);
            reserveSlot(freeSlots, index, eligibleSlot.startAt(), endAt, preference.getBreakDurationMinutes());
            remaining -= desired;
            index--;
        }
        return remaining;
    }

    private List<FreeSlot> buildFreeSlots(
            LocalDate startDate,
            int days,
            ZoneId timezone,
            SchedulingPreference preference,
            List<BusyInterval> busyIntervals,
            Instant now) {
        List<FreeSlot> slots = new ArrayList<>();
        LocalDate today = LocalDate.ofInstant(now, timezone);
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = startDate.plusDays(offset);
            if (date.isBefore(today)) {
                continue;
            }
            if (!preference.getWorkingDays().contains(date.getDayOfWeek())) {
                continue;
            }

            Instant workdayStart = date.atTime(preference.getWorkdayStartTime()).atZone(timezone).toInstant();
            Instant workdayEnd = date.atTime(preference.getWorkdayEndTime()).atZone(timezone).toInstant();
            Instant effectiveStart = date.equals(today) && now.isAfter(workdayStart)
                    ? ceilToMinute(now)
                    : workdayStart;
            if (effectiveStart.isBefore(workdayEnd)) {
                slots.addAll(subtractBusyIntervals(date, effectiveStart, workdayEnd, busyIntervals));
            }
        }
        return slots;
    }

    private List<FreeSlot> subtractBusyIntervals(
            LocalDate date,
            Instant workdayStart,
            Instant workdayEnd,
            List<BusyInterval> busyIntervals) {
        List<FreeSlot> result = new ArrayList<>();
        Instant cursor = workdayStart;
        for (BusyInterval interval : busyIntervals) {
            if (!interval.startAt().isBefore(workdayEnd)) {
                break;
            }
            if (!interval.endAt().isAfter(workdayStart)) {
                continue;
            }
            Instant eventStart = interval.startAt().isBefore(workdayStart) ? workdayStart : interval.startAt();
            Instant eventEnd = interval.endAt().isAfter(workdayEnd) ? workdayEnd : interval.endAt();
            if (!eventEnd.isAfter(cursor)) {
                continue;
            }
            if (eventStart.isAfter(cursor)) {
                result.add(new FreeSlot(cursor, eventStart, date));
            }
            if (eventEnd.isAfter(cursor)) {
                cursor = eventEnd;
            }
        }
        if (workdayEnd.isAfter(cursor)) {
            result.add(new FreeSlot(cursor, workdayEnd, date));
        }
        return result;
    }

    private FreeSlot constrainToTaskConstraints(FreeSlot slot, Task task, ZoneId timezone) {
        Instant startAt = slot.startAt();
        Instant endAt = slot.endAt();
        if (task.getPreferredStartTime() != null) {
            Instant preferredStart = slot.date().atTime(task.getPreferredStartTime()).atZone(timezone).toInstant();
            if (preferredStart.isAfter(startAt)) {
                startAt = preferredStart;
            }
        }
        if (task.getPreferredEndTime() != null) {
            Instant preferredEnd = slot.date().atTime(task.getPreferredEndTime()).atZone(timezone).toInstant();
            if (preferredEnd.isBefore(endAt)) {
                endAt = preferredEnd;
            }
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(endAt)) {
            endAt = task.getDeadline();
        }
        return endAt.isAfter(startAt) ? new FreeSlot(startAt, endAt, slot.date()) : null;
    }

    private Instant ceilToMinute(Instant instant) {
        Instant truncated = instant.truncatedTo(ChronoUnit.MINUTES);
        return instant.equals(truncated) ? instant : truncated.plus(1, ChronoUnit.MINUTES);
    }

    private UnscheduledTaskReason unscheduledReason(Task task, Instant now) {
        if (task.getDeadline() != null && !task.getDeadline().isAfter(now)) {
            return UnscheduledTaskReason.DEADLINE_PASSED;
        }
        return task.isSplitAllowed()
                ? UnscheduledTaskReason.INSUFFICIENT_DURATION
                : UnscheduledTaskReason.NO_AVAILABLE_SLOT;
    }

    private void reserveSlot(
            List<FreeSlot> freeSlots,
            int index,
            Instant scheduledStart,
            Instant scheduledEnd,
            int breakDurationMinutes) {
        FreeSlot original = freeSlots.remove(index);
        List<FreeSlot> replacement = new ArrayList<>();
        if (scheduledStart.isAfter(original.startAt())) {
            replacement.add(new FreeSlot(original.startAt(), scheduledStart, original.date()));
        }
        Instant nextAvailable = scheduledEnd.plus(breakDurationMinutes, ChronoUnit.MINUTES);
        if (original.endAt().isAfter(nextAvailable)) {
            replacement.add(new FreeSlot(nextAvailable, original.endAt(), original.date()));
        }
        freeSlots.addAll(index, replacement);
    }

    private int desiredSessionMinutes(int remaining, int minimumSession, int focusDuration) {
        return Math.min(remaining, Math.max(minimumSession, Math.min(remaining, focusDuration)));
    }

    private int availableMinutes(
            Task task,
            FreeSlot slot,
            Map<LocalDate, Integer> scheduledMinutesByDay,
            Map<TaskDay, Integer> scheduledMinutesByTaskAndDay,
            SchedulingPreference preference) {
        int globalAvailable = preference.getDailyFocusLimit() - scheduledMinutesByDay.getOrDefault(slot.date(), 0);
        if (task.getMaxDailyMinutes() == null) {
            return globalAvailable;
        }
        int taskAvailable = task.getMaxDailyMinutes()
                - scheduledMinutesByTaskAndDay.getOrDefault(new TaskDay(task.getId(), slot.date()), 0);
        return Math.min(globalAvailable, taskAvailable);
    }

    private Map<LocalDate, Integer> scheduledMinutesByDay(List<PlannedSlot> hardReservations, ZoneId timezone) {
        Map<LocalDate, Integer> minutes = new HashMap<>();
        for (PlannedSlot slot : hardReservations) {
            LocalDate date = LocalDate.ofInstant(slot.getStartAt(), timezone);
            minutes.merge(date, slot.getDurationMinutes(), Integer::sum);
        }
        return minutes;
    }

    private Map<TaskDay, Integer> scheduledMinutesByTaskAndDay(List<PlannedSlot> hardReservations, ZoneId timezone) {
        Map<TaskDay, Integer> minutes = new HashMap<>();
        for (PlannedSlot slot : hardReservations) {
            TaskDay taskDay = new TaskDay(slot.getTaskId(), LocalDate.ofInstant(slot.getStartAt(), timezone));
            minutes.merge(taskDay, slot.getDurationMinutes(), Integer::sum);
        }
        return minutes;
    }

    private int minutesBetween(Instant start, Instant end) {
        return Math.toIntExact(ChronoUnit.MINUTES.between(start, end));
    }

    private void addSuggestion(
            Task task,
            Instant startAt,
            Instant endAt,
            int duration,
            List<ScheduledBlockSuggestion> suggestions) {
        suggestions.add(new ScheduledBlockSuggestion(task.getId(), task.getTitle(), startAt, endAt, duration));
    }

    private int priorityRank(TaskPriority priority) {
        return switch (priority) {
            case URGENT -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private record FreeSlot(Instant startAt, Instant endAt, LocalDate date) {}

    private record BusyInterval(Instant startAt, Instant endAt) {}

    private record SchedulableTask(Task task, int remainingDuration) {}

    private record TaskDay(Long taskId, LocalDate date) {}
}
