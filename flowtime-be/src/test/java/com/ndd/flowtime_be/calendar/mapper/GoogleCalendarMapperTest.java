package com.ndd.flowtime_be.calendar.mapper;

import com.ndd.flowtime_be.calendar.dto.CalendarListResponse.CalendarEntryDto;
import com.ndd.flowtime_be.calendar.entity.Calendar;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleCalendarMapperTest {

    private final GoogleCalendarMapper mapper = new GoogleCalendarMapper();
    private final User user = User.builder().id(1L).email("user@example.com").name("Test User").build();

    @Test
    void keepsGoogleHolidayCalendarsVisibleButOptionalForScheduling() {
        Calendar calendar = mapper.apply(
                new CalendarEntryDto("vi.vietnamese#holiday@group.v.calendar.google.com", "Ngày lễ ở Việt Nam", null,
                        "Asia/Ho_Chi_Minh", false),
                user,
                new Calendar()
        );

        assertFalse(calendar.isBlocksScheduling());
    }

    @Test
    void keepsPersonalCalendarsBlockingByDefault() {
        Calendar calendar = mapper.apply(
                new CalendarEntryDto("user@example.com", "Lịch của tôi", null, "Asia/Ho_Chi_Minh", true),
                user,
                new Calendar()
        );

        assertTrue(calendar.isBlocksScheduling());
    }
}
