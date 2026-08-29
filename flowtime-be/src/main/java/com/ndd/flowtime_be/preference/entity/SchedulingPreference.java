package com.ndd.flowtime_be.preference.entity;

import com.ndd.flowtime_be.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "scheduling_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingPreference {

    public static final LocalTime DEFAULT_WORKDAY_START = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_WORKDAY_END = LocalTime.of(17, 0);
    public static final int DEFAULT_FOCUS_DURATION_MINUTES = 50;
    public static final int DEFAULT_BREAK_DURATION_MINUTES = 10;
    public static final int DEFAULT_DAILY_FOCUS_LIMIT = 480;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "workday_start_time", nullable = false)
    @Builder.Default
    private LocalTime workdayStartTime = DEFAULT_WORKDAY_START;

    @Column(name = "workday_end_time", nullable = false)
    @Builder.Default
    private LocalTime workdayEndTime = DEFAULT_WORKDAY_END;

    @Convert(converter = WorkingDaysConverter.class)
    @Column(name = "working_days", nullable = false, length = 80)
    @Builder.Default
    private Set<DayOfWeek> workingDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    @Column(name = "focus_duration_minutes", nullable = false)
    @Builder.Default
    private Integer focusDurationMinutes = DEFAULT_FOCUS_DURATION_MINUTES;

    @Column(name = "break_duration_minutes", nullable = false)
    @Builder.Default
    private Integer breakDurationMinutes = DEFAULT_BREAK_DURATION_MINUTES;

    @Column(name = "daily_focus_limit", nullable = false)
    @Builder.Default
    private Integer dailyFocusLimit = DEFAULT_DAILY_FOCUS_LIMIT;

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
