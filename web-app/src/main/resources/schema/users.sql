CREATE TABLE IF NOT EXISTS users
(
    id                 BIGSERIAL PRIMARY KEY,
    full_name          VARCHAR(150),
    mobile_number      VARCHAR(15) UNIQUE,
    email              VARCHAR(150) UNIQUE,
    password           VARCHAR(255),
    role               VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'PARENT', 'TUTOR')),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED')),
    status_reason      VARCHAR(500),
    gender             VARCHAR(10),
    preferred_language VARCHAR(20),
    city               VARCHAR(100),
    pincode            VARCHAR(6),
    profile_photo_url  VARCHAR(500),
    mobile_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at      TIMESTAMP(6),
    created_at         TIMESTAMP(6),
    last_updated_at    TIMESTAMP(6),
    CONSTRAINT users_login_id_present CHECK (mobile_number IS NOT NULL OR email IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_users_role_status ON users (role, status);

CREATE TABLE IF NOT EXISTS otp_requests
(
    id            BIGSERIAL PRIMARY KEY,
    mobile_number VARCHAR(15)  NOT NULL,
    otp_hash      VARCHAR(100) NOT NULL,
    expires_at    TIMESTAMP(6) NOT NULL,
    attempts      INTEGER      NOT NULL DEFAULT 0,
    consumed      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_requests_mobile_created ON otp_requests (mobile_number, created_at DESC);
