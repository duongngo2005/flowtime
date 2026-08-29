package com.ndd.flowtime_be.calendar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateEventRequest(
        String summary,
        String description,
        EventListResponse.EventDateTimeDto start,
        EventListResponse.EventDateTimeDto end
) {}
