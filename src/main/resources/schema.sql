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
