ALTER TABLE calendars
    ADD COLUMN blocks_scheduling BOOLEAN NOT NULL DEFAULT TRUE AFTER is_primary;

UPDATE calendars
SET blocks_scheduling = FALSE
WHERE LOWER(google_calendar_id) LIKE '%#holiday@group.v.calendar.google.com';
