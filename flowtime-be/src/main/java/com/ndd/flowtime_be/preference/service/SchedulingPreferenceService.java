package com.ndd.flowtime_be.preference.service;

import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceRequest;
import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceResponse;
import com.ndd.flowtime_be.preference.entity.SchedulingPreference;
import com.ndd.flowtime_be.preference.repository.SchedulingPreferenceRepository;
import com.ndd.flowtime_be.user.entity.User;
import com.ndd.flowtime_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

@Service
@RequiredArgsConstructor
public class SchedulingPreferenceService {

    private final SchedulingPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SchedulingPreferenceResponse get(User user) {
        SchedulingPreference preference = preferenceRepository.findByUser(user)
                .orElseGet(() -> SchedulingPreference.builder().user(user).build());
        return SchedulingPreferenceResponse.from(user, preference);
    }

    @Transactional
    public SchedulingPreferenceResponse update(User user, SchedulingPreferenceRequest request) {
        validate(request);
        user.setTimezone(request.timezone().trim());
        userRepository.save(user);

        SchedulingPreference preference = preferenceRepository.findByUser(user)
                .orElseGet(() -> SchedulingPreference.builder().user(user).build());
        preference.setWorkdayStartTime(request.workdayStartTime());
        preference.setWorkdayEndTime(request.workdayEndTime());
        preference.setWorkingDays(request.workingDays());
        preference.setFocusDurationMinutes(request.focusDurationMinutes());
        preference.setBreakDurationMinutes(request.breakDurationMinutes());
        preference.setDailyFocusLimit(request.dailyFocusLimit());

        return SchedulingPreferenceResponse.from(user, preferenceRepository.save(preference));
    }

    private void validate(SchedulingPreferenceRequest request) {
        try {
            ZoneId.of(request.timezone().trim());
        } catch (ZoneRulesException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timezone must be a valid IANA timezone.");
        }

        if (!request.workdayStartTime().isBefore(request.workdayEndTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "workdayStartTime must be before workdayEndTime."
            );
        }
    }
}
