-- CampusHub Database Schema
-- Based on api_design.yaml and class_design.md

CREATE DATABASE IF NOT EXISTS campushub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campushub;

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
    auth_status ENUM('UNVERIFIED','REVIEWING','APPROVED','REJECTED') NOT NULL DEFAULT 'UNVERIFIED',
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_student_id (student_id),
    INDEX idx_auth_status (auth_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Stored Files
CREATE TABLE IF NOT EXISTS stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    file_usage ENUM('AVATAR','VERIFICATION_MATERIAL','PICKUP_CREDENTIAL','COMPLETION_PROOF') NOT NULL,
    original_name VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uploader (uploader_id),
    INDEX idx_usage (file_usage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    reward_type ENUM('PAID','UNPAID') NOT NULL,
    reward_amount DECIMAL(10,2),
    payment_id BIGINT,
    status ENUM('WAITING_PAYMENT','WAITING_ACCEPT','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL,
    accept_deadline DATETIME NOT NULL,
    accepted_at DATETIME,
    completion_proof_file_id BIGINT,
    completed_at DATETIME,
    cancel_reason ENUM('USER_CANCELLED','PAYMENT_EXPIRED','ACCEPT_DEADLINE_EXPIRED','SYSTEM_CANCELLED'),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_campus (campus),
    INDEX idx_reward_type (reward_type),
    INDEX idx_created (created_at DESC),
    INDEX idx_publisher (publisher_id),
    INDEX idx_acceptor (acceptor_id),
    INDEX idx_accept_deadline (accept_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Payment Records
CREATE TABLE IF NOT EXISTS payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_id BIGINT NOT NULL,
    receiver_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    business_type ENUM('PICKUP_REQUEST') NOT NULL,
    business_trace_no VARCHAR(64),
    out_trade_no VARCHAR(64) NOT NULL UNIQUE,
    trade_no VARCHAR(64),
    status ENUM('WAITING_PAY','PAID','CLOSED','REFUNDED','SETTLED') NOT NULL DEFAULT 'WAITING_PAY',
    expire_at DATETIME,
    close_reason VARCHAR(200),
    status_changed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (payer_id) REFERENCES users(id),
    INDEX idx_out_trade_no (out_trade_no),
    INDEX idx_trade_no (trade_no),
    INDEX idx_payer (payer_id),
    INDEX idx_status (status),
    INDEX idx_business_trace (business_trace_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Evaluations
CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    business_type ENUM('PICKUP_REQUEST') NOT NULL,
    business_id BIGINT NOT NULL,
    reviewee_role_in_business ENUM('PUBLISHER','ACCEPTOR') NOT NULL,
    rating_level ENUM('GOOD','NEUTRAL','BAD') NOT NULL,
    content VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    FOREIGN KEY (reviewee_id) REFERENCES users(id),
    UNIQUE KEY uk_business_reviewer (business_type, business_id, reviewer_id),
    INDEX idx_reviewee (reviewee_id),
    INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    type ENUM('SYSTEM','PICKUP','PAYMENT') NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500) NOT NULL,
    business_type ENUM('PICKUP_REQUEST'),
    business_id BIGINT,
    read_status ENUM('UNREAD','READ') NOT NULL DEFAULT 'UNREAD',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    INDEX idx_receiver_read (receiver_id, read_status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Verification Reviews
CREATE TABLE IF NOT EXISTS verification_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    reject_reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
