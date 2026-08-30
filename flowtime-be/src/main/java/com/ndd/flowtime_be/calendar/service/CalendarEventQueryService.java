package com.ndd.flowtime_be.calendar.service;

import com.ndd.flowtime_be.calendar.dto.CalendarEventResponse;
import com.ndd.flowtime_be.calendar.repository.CalendarEventRepository;
import com.ndd.flowtime_be.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarEventQueryService {

    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> listEvents(User user, Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời điểm bắt đầu phải trước thời điểm kết thúc.");
        }

        return calendarEventRepository
                .findByUserAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(user, to, from)
                .stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse getEvent(User user, Long eventId) {
        return calendarEventRepository.findByIdAndUser(eventId, user)
                .map(CalendarEventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện lịch."));
    }
}
