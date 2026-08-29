ALTER TABLE planning_sessions
    ADD COLUMN apply_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN apply_started_at TIMESTAMP NULL,
    ADD COLUMN applied_at TIMESTAMP NULL,
    ADD COLUMN last_apply_error TEXT NULL;

ALTER TABLE planned_slots
    ADD COLUMN google_calendar_id VARCHAR(512) NOT NULL DEFAULT 'primary',
    ADD COLUMN google_event_id VARCHAR(64) NULL,
    ADD COLUMN apply_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN apply_error TEXT NULL,
    ADD COLUMN apply_started_at TIMESTAMP NULL,
    ADD COLUMN applied_at TIMESTAMP NULL;

UPDATE planned_slots
SET google_event_id = CONCAT('ftlegacy', id)
WHERE google_event_id IS NULL;

ALTER TABLE planned_slots
    MODIFY COLUMN google_event_id VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uq_planned_slots_google_event UNIQUE (google_event_id);
