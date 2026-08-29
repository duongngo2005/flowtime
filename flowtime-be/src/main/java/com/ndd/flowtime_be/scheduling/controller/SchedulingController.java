package com.ndd.flowtime_be.scheduling.controller;

import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewRequest;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewResponse;
import com.ndd.flowtime_be.scheduling.service.SchedulingEngine;
import com.ndd.flowtime_be.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduling")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingEngine schedulingEngine;

    @PostMapping("/preview")
    public ResponseEntity<SchedulingPreviewResponse> preview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SchedulingPreviewRequest request) {
        return ResponseEntity.ok(schedulingEngine.preview(user, request));
    }
}
