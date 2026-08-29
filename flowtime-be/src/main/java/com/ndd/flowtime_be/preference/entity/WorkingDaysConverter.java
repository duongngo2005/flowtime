package com.ndd.flowtime_be.preference.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class WorkingDaysConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> workingDays) {
        if (workingDays == null || workingDays.isEmpty()) {
            return "";
        }

        return workingDays.stream()
                .sorted()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        return Arrays.stream(databaseValue.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
