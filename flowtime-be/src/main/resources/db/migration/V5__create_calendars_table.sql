CREATE TABLE calendars (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    google_calendar_id VARCHAR(512) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    timezone           VARCHAR(100) NOT NULL,
    is_primary         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_synced_at     TIMESTAMP    NULL,
    CONSTRAINT uk_calendars_user_google_calendar UNIQUE (user_id, google_calendar_id),
    CONSTRAINT fk_calendars_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
