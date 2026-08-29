package com.ndd.flowtime_be.planning.entity;

import com.ndd.flowtime_be.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "planning_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlanningSessionStatus status = PlanningSessionStatus.DRAFT;

    @Column(name = "apply_attempts", nullable = false)
    @Builder.Default
    private Integer applyAttempts = 0;

    @Column(name = "apply_started_at")
    private Instant applyStartedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "last_apply_error", columnDefinition = "TEXT")
    private String lastApplyError;

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
