package com.ndd.flowtime_be.calendar.service;

import com.ndd.flowtime_be.calendar.dto.EventListResponse;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.calendar.entity.CalendarEvent;
import com.ndd.flowtime_be.calendar.mapper.GoogleCalendarEventMapper;
import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarEventUpsertService {

    private final CalendarEventRepository calendarEventRepository;
    private final GoogleCalendarEventMapper googleCalendarEventMapper;

    @Transactional
    public CalendarEvent upsert(User user, Calendar calendar, EventListResponse.GoogleEventDto eventDto) {
        CalendarEvent event = calendarEventRepository.findByUserAndGoogleEventId(user, eventDto.id())
                .orElseGet(CalendarEvent::new);
        return calendarEventRepository.save(googleCalendarEventMapper.apply(eventDto, user, calendar, event));
    }
}
