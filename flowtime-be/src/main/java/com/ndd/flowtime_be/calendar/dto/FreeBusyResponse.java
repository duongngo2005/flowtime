package com.ndd.flowtime_be.calendar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FreeBusyResponse(
        Map<String, CalendarBusy> calendars
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CalendarBusy(
            List<BusyPeriod> busy,
            List<CalendarError> errors
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CalendarError(
            String domain,
            String reason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BusyPeriod(
            String start,
            String end
    ) {}
}
