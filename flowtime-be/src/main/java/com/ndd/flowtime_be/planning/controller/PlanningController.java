package com.ndd.flowtime_be.planning.controller;

import com.ndd.flowtime_be.planning.dto.PlanningSessionResponse;
import com.ndd.flowtime_be.planning.service.PlanningApplyService;
import com.ndd.flowtime_be.planning.service.PlanningService;
import com.ndd.flowtime_be.scheduling.dto.SchedulingPreviewRequest;
import com.ndd.flowtime_be.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningService planningService;
    private final PlanningApplyService planningApplyService;

    @PostMapping
    public ResponseEntity<PlanningSessionResponse> createDraft(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SchedulingPreviewRequest request) {
        return ResponseEntity.ok(planningService.createDraft(user, request));
    }

    @GetMapping("/{planningId}")
    public ResponseEntity<PlanningSessionResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long planningId) {
        return ResponseEntity.ok(planningService.get(user, planningId));
    }

    @DeleteMapping("/{planningId}/slots/{slotId}")
    public ResponseEntity<PlanningSessionResponse> removeSlot(
            @AuthenticationPrincipal User user,
            @PathVariable Long planningId,
            @PathVariable Long slotId) {
        return ResponseEntity.ok(planningService.removeSlot(user, planningId, slotId));
    }

    @PostMapping("/{planningId}/approve")
    public ResponseEntity<PlanningSessionResponse> approve(
            @AuthenticationPrincipal User user,
            @PathVariable Long planningId) {
        return ResponseEntity.ok(planningService.approve(user, planningId));
    }

    @PostMapping("/{planningId}/apply")
    public ResponseEntity<PlanningSessionResponse> apply(
            @AuthenticationPrincipal User user,
            @PathVariable Long planningId) {
        planningApplyService.apply(user, planningId);
        return ResponseEntity.ok(planningService.get(user, planningId));
    }

    @PostMapping("/{planningId}/cancel")
    public ResponseEntity<PlanningSessionResponse> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable Long planningId) {
        return ResponseEntity.ok(planningService.cancel(user, planningId));
    }
}
