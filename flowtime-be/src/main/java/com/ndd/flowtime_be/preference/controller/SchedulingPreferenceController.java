package com.ndd.flowtime_be.preference.controller;

import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceRequest;
import com.ndd.flowtime_be.preference.dto.SchedulingPreferenceResponse;
import com.ndd.flowtime_be.preference.service.SchedulingPreferenceService;
import com.ndd.flowtime_be.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduling-preferences")
@RequiredArgsConstructor
public class SchedulingPreferenceController {

    private final SchedulingPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<SchedulingPreferenceResponse> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferenceService.get(user));
    }

    @PutMapping
    public ResponseEntity<SchedulingPreferenceResponse> update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SchedulingPreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.update(user, request));
    }
}
