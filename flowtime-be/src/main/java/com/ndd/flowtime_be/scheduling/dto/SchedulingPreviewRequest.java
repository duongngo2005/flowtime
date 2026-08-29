package com.ndd.flowtime_be.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SchedulingPreviewRequest(
        LocalDate startDate,
        @NotNull @Min(1) @Max(14) Integer days
) {}
