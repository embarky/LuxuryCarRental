-- -----------------------------------------------------------------------------
-- V2: email confirmation before the first payment
-- * customers get an emailVerified flag;
-- * password reset codes become general one-time codes with a purpose
--   (PASSWORD_RESET or EMAIL_CONFIRMATION). Open reset codes are short-lived
--   (10 minutes), so they are simply not carried over.
-- -----------------------------------------------------------------------------

ALTER TABLE customer
    ADD COLUMN emailVerified TINYINT(1) NOT NULL DEFAULT 0 AFTER dateOfBirth;

CREATE TABLE verification_code (
    id         VARCHAR(36)  NOT NULL,
    purpose    VARCHAR(32)  NOT NULL,
    role       VARCHAR(32)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    used       TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_verification_code_lookup ON verification_code (email, purpose, role, used);

DROP TABLE password_reset_code;
