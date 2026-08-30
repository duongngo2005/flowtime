package com.ndd.flowtime_be.preference.service;

import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceRequest;
import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceResponse;
import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.preference.repository.SchedulingPreferenceRepository;
import com.ndd.flowtime_be.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingPreferenceServiceTest {

    @Mock
    private SchedulingPreferenceRepository preferenceRepository;

    @InjectMocks
    private SchedulingPreferenceService preferenceService;

    private final User user = User.builder()
            .id(1L)
            .email("user@example.com")
            .name("Test User")
            .timezone("Asia/Ho_Chi_Minh")
            .build();

    @Test
    void returnsSafeDefaultsBeforeUserConfiguresPreferences() {
        when(preferenceRepository.findByUser(user)).thenReturn(Optional.empty());

        SchedulingPreferenceResponse response = preferenceService.get(user);

        assertFalse(response.configured());
        assertEquals(LocalTime.of(9, 0), response.workdayStartTime());
        assertEquals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), response.workingDays());
        assertEquals(480, response.dailyFocusLimit());
    }

    @Test
    void storesSchedulingPreferencesInVietnamTime() {
        SchedulingPreferenceRequest request = request(LocalTime.of(8, 30), LocalTime.of(17, 30));
        when(preferenceRepository.findByUser(user)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(SchedulingPreference.class))).thenAnswer(invocation -> {
            SchedulingPreference preference = invocation.getArgument(0);
            preference.setId(1L);
            return preference;
        });

        SchedulingPreferenceResponse response = preferenceService.update(user, request);

        ArgumentCaptor<SchedulingPreference> captor = ArgumentCaptor.forClass(SchedulingPreference.class);
        verify(preferenceRepository).save(captor.capture());
        assertEquals("Asia/Ho_Chi_Minh", user.getTimezone());
        assertEquals(LocalTime.of(8, 30), captor.getValue().getWorkdayStartTime());
        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), response.workingDays());
        assertEquals(300, response.dailyFocusLimit());
        assertEquals(true, response.configured());
    }

    @Test
    void rejectsAnInvalidWorkdayRange() {
        SchedulingPreferenceRequest invalidRange = request(LocalTime.of(17, 0), LocalTime.of(9, 0));

        ResponseStatusException rangeException = assertThrows(
                ResponseStatusException.class,
                () -> preferenceService.update(user, invalidRange)
        );

        assertEquals(HttpStatus.BAD_REQUEST, rangeException.getStatusCode());
    }

    private SchedulingPreferenceRequest request(LocalTime start, LocalTime end) {
        return new SchedulingPreferenceRequest(
                start,
                end,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                60,
                15,
                300
        );
    }
}
