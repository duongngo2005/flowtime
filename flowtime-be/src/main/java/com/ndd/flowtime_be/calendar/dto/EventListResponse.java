package com.ndd.flowtime_be.calendar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventListResponse(
        List<GoogleEventDto> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleEventDto(
            String id,
            String summary,
            String description,
            EventDateTimeDto start,
            EventDateTimeDto end,
            String status
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventDateTimeDto(
            String dateTime,
            String date,
            String timeZone
    ) {}
}
