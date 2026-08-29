package com.ndd.flowtime_be.calendar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CalendarListResponse(
        List<CalendarEntryDto> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CalendarEntryDto(
            String id,
            String summary,
            String description,
            String timeZone,
            @JsonProperty("primary") Boolean primary
    ) {}
}
