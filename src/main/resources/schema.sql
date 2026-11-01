-- =========================================
-- USERS TABLE
-- =========================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at DATETIME,
    updated_at DATETIME
);

-- =========================================
-- DOCUMENTS TABLE
-- =========================================
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    original_name VARCHAR(255),
    stored_name VARCHAR(255),
    file_type VARCHAR(100),
    file_size BIGINT,
    storage_path VARCHAR(500),
    status VARCHAR(50),

    -- Phase 5 fields
    page_count INT DEFAULT 0,
    is_temporary BOOLEAN DEFAULT FALSE,
    processing_status VARCHAR(50) DEFAULT 'READY',

    created_at DATETIME,
    updated_at DATETIME
);

-- =========================================
-- PROCESSING HISTORY (PDF/AI JOBS)
-- =========================================
CREATE TABLE IF NOT EXISTS processing_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    tool_type VARCHAR(100),
    input_file_id BIGINT,
    output_file_id BIGINT,
    status VARCHAR(50),
    error_message TEXT,
    created_at DATETIME,
    updated_at DATETIME
);

-- =========================================
-- JOBS (tool processing requests, guest or user)
-- public_id is a random UUID used in URLs; it is the
-- access capability for guest jobs.
-- =========================================
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    client_ip VARCHAR(64),
    tool_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    options_json TEXT,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    result_summary VARCHAR(500),
    created_at DATETIME NOT NULL,
    started_at DATETIME,
    completed_at DATETIME,
    expires_at DATETIME NOT NULL,
    INDEX idx_jobs_user (user_id, created_at),
    INDEX idx_jobs_expires (status, expires_at)
);

-- =========================================
-- JOB FILES (inputs and outputs of a job)
-- =========================================
CREATE TABLE IF NOT EXISTS job_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    created_at DATETIME NOT NULL,
    INDEX idx_job_files_job (job_id)
);

-- =========================================
-- jobs.attempts (added for retrying job engine).
-- MySQL has no ADD COLUMN IF NOT EXISTS, so check information_schema first.
-- =========================================
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE jobs ADD COLUMN attempts INT NOT NULL DEFAULT 0',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'jobs' AND column_name = 'attempts');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =========================================
-- CREDIT LEDGER (FR-023). Append-only; balance = SUM(delta).
-- idempotency_key makes grants, charges and refunds happen at most once
-- (e.g. 'charge:<jobId>', 'refund:<jobId>', 'signup:<userId>').
-- =========================================
CREATE TABLE IF NOT EXISTS credit_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    delta INT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    related_job_id BIGINT NULL,
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    reason VARCHAR(200),
    created_at DATETIME NOT NULL,
    INDEX idx_credit_ledger_user (user_id, created_at)
);

-- =========================================
-- PASSWORD RESET TOKENS. Only a SHA-256 hash of the emailed token is stored;
-- a token works once and expires after docsaas.password-reset.valid-minutes.
-- =========================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_password_reset_user (user_id, created_at)
);
