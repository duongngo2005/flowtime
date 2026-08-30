UPDATE users
SET timezone = 'Asia/Ho_Chi_Minh';

ALTER TABLE users
    ALTER COLUMN timezone SET DEFAULT 'Asia/Ho_Chi_Minh';
