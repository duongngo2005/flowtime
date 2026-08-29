package com.ndd.flowtime_be.preference.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record SchedulingPreferenceRequest(
        @NotBlank String timezone,
        @NotNull LocalTime workdayStartTime,
        @NotNull LocalTime workdayEndTime,
        @NotEmpty Set<DayOfWeek> workingDays,
        @NotNull @Min(5) @Max(240) Integer focusDurationMinutes,
        @NotNull @Min(0) @Max(240) Integer breakDurationMinutes,
        @NotNull @Min(15) @Max(1_440) Integer dailyFocusLimit
) {}
