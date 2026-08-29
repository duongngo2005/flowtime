CREATE TABLE calendar_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    calendar_id     BIGINT       NOT NULL,
    google_event_id VARCHAR(512) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    start_at        TIMESTAMP    NOT NULL,
    end_at          TIMESTAMP    NOT NULL,
    all_day         BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(50)  NOT NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_calendar_events_user_google_event UNIQUE (user_id, google_event_id),
    CONSTRAINT fk_calendar_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_calendar_events_calendar FOREIGN KEY (calendar_id) REFERENCES calendars (id) ON DELETE CASCADE
);
