ALTER TABLE tasks
    ADD COLUMN max_daily_minutes INT NULL AFTER min_session_duration;
