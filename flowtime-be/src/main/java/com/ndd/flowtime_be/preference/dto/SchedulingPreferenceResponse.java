package com.ndd.flowtime_be.preference.dto;

import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.user.entity.User;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record SchedulingPreferenceResponse(
        LocalTime workdayStartTime,
        LocalTime workdayEndTime,
        Set<DayOfWeek> workingDays,
        Integer focusDurationMinutes,
        Integer breakDurationMinutes,
        Integer dailyFocusLimit,
        boolean configured
) {
    public static SchedulingPreferenceResponse from(User user, SchedulingPreference preference) {
        return new SchedulingPreferenceResponse(
                preference.getWorkdayStartTime(),
                preference.getWorkdayEndTime(),
                Set.copyOf(preference.getWorkingDays()),
                preference.getFocusDurationMinutes(),
                preference.getBreakDurationMinutes(),
                preference.getDailyFocusLimit(),
                preference.getId() != null
        );
    }
}
