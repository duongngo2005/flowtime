ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);

CREATE TABLE google_accounts
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL UNIQUE,
    google_account_id VARCHAR(255) NOT NULL,
    access_token      TEXT         NOT NULL,
    refresh_token     TEXT,
    expires_at        TIMESTAMP    NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_google_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
