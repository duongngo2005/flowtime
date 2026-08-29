package com.ndd.flowtime_be.planning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "planned_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planning_session_id", nullable = false)
    private PlanningSession planningSession;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_title", nullable = false)
    private String taskTitle;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlannedSlotStatus status = PlannedSlotStatus.PROPOSED;

    @Column(name = "google_calendar_id", nullable = false, length = 512)
    @Builder.Default
    private String googleCalendarId = "primary";

    @Column(name = "google_event_id", nullable = false, updatable = false, length = 64)
    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_status", nullable = false, length = 20)
    @Builder.Default
    private PlannedSlotApplyStatus applyStatus = PlannedSlotApplyStatus.NOT_REQUESTED;

    @Column(name = "apply_error", columnDefinition = "TEXT")
    private String applyError;

    @Column(name = "apply_started_at")
    private Instant applyStartedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
