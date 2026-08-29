package com.ndd.flowtime_be.calendar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateEventRequest(
        String id,
        String summary,
        String description,
        EventListResponse.EventDateTimeDto start,
        EventListResponse.EventDateTimeDto end,
        ExtendedProperties extendedProperties
) {
    public CreateEventRequest(
            String summary,
            String description,
            EventListResponse.EventDateTimeDto start,
            EventListResponse.EventDateTimeDto end) {
        this(null, summary, description, start, end, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtendedProperties(@JsonProperty("private") Map<String, String> privateProperties) {}
}
