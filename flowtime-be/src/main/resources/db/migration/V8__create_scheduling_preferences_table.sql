CREATE TABLE scheduling_preferences (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT      NOT NULL,
    workday_start_time      TIME        NOT NULL DEFAULT '09:00:00',
    workday_end_time        TIME        NOT NULL DEFAULT '17:00:00',
    working_days            VARCHAR(80) NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    focus_duration_minutes  INT         NOT NULL DEFAULT 50,
    break_duration_minutes  INT         NOT NULL DEFAULT 10,
    daily_focus_limit       INT         NOT NULL DEFAULT 480,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_scheduling_preferences_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_scheduling_preferences_user UNIQUE (user_id)
);
