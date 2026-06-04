-- CampusHub Database Schema (H2 Compatible)

-- 1. Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    student_id VARCHAR(20),
    real_name VARCHAR(50),
    nickname VARCHAR(50) NOT NULL DEFAULT '',
    avatar_file_id BIGINT,
    verification_file_id BIGINT,
    college VARCHAR(100),
    contact VARCHAR(100),
    auth_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Stored Files
CREATE TABLE IF NOT EXISTS stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    file_usage VARCHAR(30) NOT NULL,
    original_name VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Pickup Requests
CREATE TABLE IF NOT EXISTS pickup_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    acceptor_id BIGINT,
    campus VARCHAR(50) NOT NULL,
    pickup_location VARCHAR(200) NOT NULL,
    delivery_location VARCHAR(200) NOT NULL,
    item_description VARCHAR(500),
    pickup_credential_file_id BIGINT,
    reward_type VARCHAR(10) NOT NULL,
    reward_amount DECIMAL(10,2),
    payment_id BIGINT,
    status VARCHAR(20) NOT NULL,
    accept_deadline TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    completion_proof_file_id BIGINT,
    completed_at TIMESTAMP,
    cancel_reason VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Payment Records
CREATE TABLE IF NOT EXISTS payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_id BIGINT NOT NULL,
    receiver_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    business_type VARCHAR(20) NOT NULL,
    business_trace_no VARCHAR(64),
    out_trade_no VARCHAR(64) NOT NULL UNIQUE,
    trade_no VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING_PAY',
    expire_at TIMESTAMP,
    close_reason VARCHAR(200),
    status_changed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Evaluations
CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    business_type VARCHAR(20) NOT NULL,
    business_id BIGINT NOT NULL,
    reviewee_role_in_business VARCHAR(10) NOT NULL,
    rating_level VARCHAR(10) NOT NULL,
    content VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500) NOT NULL,
    business_type VARCHAR(20),
    business_id BIGINT,
    read_status VARCHAR(10) NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Verification Reviews
CREATE TABLE IF NOT EXISTS verification_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    reject_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);
