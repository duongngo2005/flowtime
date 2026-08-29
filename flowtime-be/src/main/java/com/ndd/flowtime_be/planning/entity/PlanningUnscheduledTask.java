package com.ndd.flowtime_be.planning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "planning_unscheduled_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningUnscheduledTask {

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

    @Column(name = "unscheduled_minutes", nullable = false)
    private Integer unscheduledMinutes;

    @Column(nullable = false, length = 500)
    private String reason;
}
