package com.ndd.flowtime_be.calendar.entity;

import com.ndd.flowtime_be.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "calendars",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calendars_user_google_calendar",
                columnNames = {"user_id", "google_calendar_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "google_calendar_id", nullable = false, length = 512)
    private String googleCalendarId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String timezone;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(name = "blocks_scheduling", nullable = false)
    @Builder.Default
    private boolean blocksScheduling = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
