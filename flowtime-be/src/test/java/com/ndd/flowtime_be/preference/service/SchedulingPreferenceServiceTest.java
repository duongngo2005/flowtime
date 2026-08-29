package com.ndd.flowtime_be.preference.service;

import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceRequest;
import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceResponse;
import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.preference.repository.SchedulingPreferenceRepository;
import com.ndd.flowtime_be.user.entity.User;
import com.ndd.flowtime_be.user.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

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
        assertEquals("Asia/Ho_Chi_Minh", response.timezone());
        assertEquals(LocalTime.of(9, 0), response.workdayStartTime());
        assertEquals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), response.workingDays());
        assertEquals(480, response.dailyFocusLimit());
    }

    @Test
    void storesSchedulingPreferencesAndUpdatesUserTimezone() {
        SchedulingPreferenceRequest request = request("America/New_York", LocalTime.of(8, 30), LocalTime.of(17, 30));
        when(preferenceRepository.findByUser(user)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(SchedulingPreference.class))).thenAnswer(invocation -> {
            SchedulingPreference preference = invocation.getArgument(0);
            preference.setId(1L);
            return preference;
        });

        SchedulingPreferenceResponse response = preferenceService.update(user, request);

        ArgumentCaptor<SchedulingPreference> captor = ArgumentCaptor.forClass(SchedulingPreference.class);
        verify(preferenceRepository).save(captor.capture());
        verify(userRepository).save(user);
        assertEquals("America/New_York", user.getTimezone());
        assertEquals(LocalTime.of(8, 30), captor.getValue().getWorkdayStartTime());
        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), response.workingDays());
        assertEquals(300, response.dailyFocusLimit());
        assertEquals(true, response.configured());
    }

    @Test
    void rejectsInvalidTimezoneAndWorkdayRange() {
        SchedulingPreferenceRequest invalidTimezone = request("Mars/Olympus_Mons", LocalTime.of(9, 0), LocalTime.of(17, 0));
        SchedulingPreferenceRequest invalidRange = request("UTC", LocalTime.of(17, 0), LocalTime.of(9, 0));

        ResponseStatusException timezoneException = assertThrows(
                ResponseStatusException.class,
                () -> preferenceService.update(user, invalidTimezone)
        );
        ResponseStatusException rangeException = assertThrows(
                ResponseStatusException.class,
                () -> preferenceService.update(user, invalidRange)
        );

        assertEquals(HttpStatus.BAD_REQUEST, timezoneException.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, rangeException.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    private SchedulingPreferenceRequest request(String timezone, LocalTime start, LocalTime end) {
        return new SchedulingPreferenceRequest(
                timezone,
                start,
                end,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                60,
                15,
                300
        );
    }
}
