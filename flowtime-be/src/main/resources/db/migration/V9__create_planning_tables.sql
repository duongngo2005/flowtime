CREATE TABLE planning_sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    timezone    VARCHAR(64) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_planning_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_planning_sessions_user_created (user_id, created_at)
);

CREATE TABLE planned_slots (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    planning_session_id BIGINT       NOT NULL,
    task_id             BIGINT       NOT NULL,
    task_title          VARCHAR(255) NOT NULL,
    start_at            TIMESTAMP    NOT NULL,
    end_at              TIMESTAMP    NOT NULL,
    duration_minutes    INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_planned_slots_session FOREIGN KEY (planning_session_id) REFERENCES planning_sessions (id) ON DELETE CASCADE,
    INDEX idx_planned_slots_session_start (planning_session_id, start_at)
);

CREATE TABLE planning_unscheduled_tasks (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    planning_session_id BIGINT       NOT NULL,
    task_id             BIGINT       NOT NULL,
    task_title          VARCHAR(255) NOT NULL,
    unscheduled_minutes INT          NOT NULL,
    reason              VARCHAR(500) NOT NULL,
    CONSTRAINT fk_planning_unscheduled_tasks_session FOREIGN KEY (planning_session_id) REFERENCES planning_sessions (id) ON DELETE CASCADE,
    INDEX idx_planning_unscheduled_tasks_session (planning_session_id)
);
