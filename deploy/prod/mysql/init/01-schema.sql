-- =====================================================================
-- CampusHub 校园可信代取服务平台 — 数据库建表脚本
-- 严格对照 docs/P3/database_design.md 第六节建表 SQL
-- 字符集 utf8mb4，存储引擎 InnoDB
--
-- 使用方式（本地开发）：
--   mysql -u root -p < schema.sql
-- 或在客户端中先选库再执行本文件的 CREATE TABLE 部分。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. users 用户表
--    avatar_file_id 的外键在 stored_files 建表后通过 ALTER TABLE 回填，
--    解决 users 与 stored_files 互相引用的先后顺序问题。
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL COMMENT '登录账号，平台内唯一',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希，禁止明文存储',
    nickname VARCHAR(50) NOT NULL DEFAULT '' COMMENT '公开展示昵称',
    avatar_file_id BIGINT NULL COMMENT '头像文件ID，关联stored_files',
    phone VARCHAR(20) NULL COMMENT '手机号，普通选填资料字段，MVP不用于短信登录',
    student_id VARCHAR(32) NULL COMMENT '当前有效学号，认证通过后同步，普通用户页面不公开',
    real_name VARCHAR(50) NULL COMMENT '当前有效真实姓名，认证通过后同步',
    college VARCHAR(80) NULL COMMENT '学院，选填',
    contact VARCHAR(100) NULL COMMENT '用户主动填写的公开联系方式，选填',
    auth_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED' COMMENT 'UNVERIFIED/REVIEWING/APPROVED/REJECTED',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_student_id UNIQUE (student_id),
    CONSTRAINT chk_users_auth_status CHECK (auth_status IN ('UNVERIFIED','REVIEWING','APPROVED','REJECTED')),
    CONSTRAINT chk_users_role CHECK (role IN ('USER','ADMIN')),
    INDEX idx_users_auth_status (auth_status),
    INDEX idx_users_role (role),
    INDEX idx_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 2. stored_files 统一文件元数据表
-- ---------------------------------------------------------------------
CREATE TABLE stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id BIGINT NOT NULL COMMENT '上传者用户ID，仅用于上传溯源',
    file_usage VARCHAR(40) NOT NULL COMMENT 'AVATAR/VERIFICATION_MATERIAL/PICKUP_CREDENTIAL/COMPLETION_PROOF',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_path VARCHAR(500) NOT NULL COMMENT '本地存储路径或对象存储key',
    mime_type VARCHAR(100) NOT NULL COMMENT 'MIME类型',
    file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
    sha256 CHAR(64) NULL COMMENT '文件SHA-256哈希',
    business_type VARCHAR(40) NULL COMMENT '关联业务类型，仅用于追踪',
    business_id BIGINT NULL COMMENT '关联业务ID，仅用于追踪',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stored_files_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT chk_stored_files_usage CHECK (file_usage IN ('AVATAR','VERIFICATION_MATERIAL','PICKUP_CREDENTIAL','COMPLETION_PROOF')),
    INDEX idx_stored_files_uploader (uploader_id, created_at),
    INDEX idx_stored_files_usage (file_usage, created_at),
    INDEX idx_stored_files_business (business_type, business_id),
    INDEX idx_stored_files_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一文件元数据表';

-- 回填 users.avatar_file_id 外键
ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar_file
    FOREIGN KEY (avatar_file_id) REFERENCES stored_files(id);

-- ---------------------------------------------------------------------
-- 3. verification_reviews 实名认证审核记录表
-- ---------------------------------------------------------------------
CREATE TABLE verification_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '申请认证用户ID',
    material_file_id BIGINT NOT NULL COMMENT '认证材料文件ID',
    submitted_student_id VARCHAR(32) NOT NULL COMMENT '当次实名认证申请提交的学号快照',
    submitted_real_name VARCHAR(50) NOT NULL COMMENT '当次实名认证申请提交的真实姓名快照',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reviewer_id BIGINT NULL COMMENT '审核管理员ID',
    reject_reason VARCHAR(500) NULL COMMENT '驳回原因',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_verification_reviews_material_file FOREIGN KEY (material_file_id) REFERENCES stored_files(id),
    CONSTRAINT fk_verification_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_verification_reviews_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    INDEX idx_verification_reviews_status_created (status, created_at),
    INDEX idx_verification_reviews_user_status_created (user_id, status, created_at),
    INDEX idx_verification_reviews_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实名认证审核记录表';

-- ---------------------------------------------------------------------
-- 4. payment_records 支付记录表
-- ---------------------------------------------------------------------
CREATE TABLE payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_id BIGINT NOT NULL COMMENT '付款方，代取发布方',
    receiver_id BIGINT NULL COMMENT '收款方，代取接单方，结算前可为空',
    business_type VARCHAR(40) NOT NULL DEFAULT 'PICKUP_REQUEST' COMMENT '当前仅PICKUP_REQUEST，仅用于支付追踪',
    business_trace_no VARCHAR(80) NOT NULL COMMENT '业务追踪号，用于幂等、回调定位和异常排查',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额，单位元',
    out_trade_no VARCHAR(64) NOT NULL COMMENT '商户订单号，必须唯一',
    trade_no VARCHAR(128) NULL COMMENT '第三方交易号',
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING_PAY' COMMENT 'WAITING_PAY/PAID/CLOSED/REFUNDED/SETTLED',
    pay_entry TEXT NULL COMMENT '支付宝沙箱支付入口',
    expire_at DATETIME NOT NULL COMMENT '支付过期时间',
    paid_at DATETIME NULL COMMENT '支付成功时间',
    refunded_at DATETIME NULL COMMENT '退款成功时间',
    settled_at DATETIME NULL COMMENT '结算成功时间',
    closed_at DATETIME NULL COMMENT '待支付关闭时间',
    close_reason VARCHAR(100) NULL COMMENT '关闭原因',
    failure_reason VARCHAR(500) NULL COMMENT '失败原因',
    status_changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近状态变化时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_records_out_trade_no UNIQUE (out_trade_no),
    CONSTRAINT uk_payment_records_business_trace_no UNIQUE (business_trace_no),
    CONSTRAINT fk_payment_records_payer FOREIGN KEY (payer_id) REFERENCES users(id),
    CONSTRAINT fk_payment_records_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT chk_payment_records_business_type CHECK (business_type IN ('PICKUP_REQUEST')),
    CONSTRAINT chk_payment_records_status CHECK (status IN ('WAITING_PAY','PAID','CLOSED','REFUNDED','SETTLED')),
    INDEX idx_payment_records_payer_created (payer_id, created_at),
    INDEX idx_payment_records_receiver_created (receiver_id, created_at),
    INDEX idx_payment_records_status_expire (status, expire_at),
    INDEX idx_payment_records_trade_no (trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ---------------------------------------------------------------------
-- 5. pickup_requests 代取请求主表
-- ---------------------------------------------------------------------
CREATE TABLE pickup_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL COMMENT '发布方用户ID',
    acceptor_id BIGINT NULL COMMENT '接单方用户ID',
    campus VARCHAR(80) NOT NULL COMMENT '校区',
    pickup_location VARCHAR(200) NOT NULL COMMENT '取件地点',
    delivery_location VARCHAR(200) NOT NULL COMMENT '送达地点',
    item_description VARCHAR(500) NOT NULL COMMENT '物品说明',
    reward_type VARCHAR(20) NOT NULL COMMENT 'PAID/UNPAID',
    reward_amount DECIMAL(10,2) NULL COMMENT '有报酬金额，1-200元；无报酬为空',
    pickup_credential_file_id BIGINT NOT NULL COMMENT '取件凭证文件ID',
    completion_proof_file_id BIGINT NULL COMMENT '完成凭证文件ID',
    payment_id BIGINT NULL COMMENT '支付记录ID；无报酬为空',
    status VARCHAR(30) NOT NULL COMMENT 'WAITING_PAYMENT/WAITING_ACCEPT/IN_PROGRESS/COMPLETED/CANCELLED',
    cancel_reason VARCHAR(40) NULL COMMENT 'USER_CANCELLED/PAYMENT_EXPIRED/ACCEPT_DEADLINE_EXPIRED/SYSTEM_CANCELLED',
    cancel_detail VARCHAR(500) NULL COMMENT '取消补充说明',
    accept_deadline DATETIME NOT NULL COMMENT '接单截止时间',
    accepted_at DATETIME NULL COMMENT '接单时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pickup_requests_publisher FOREIGN KEY (publisher_id) REFERENCES users(id),
    CONSTRAINT fk_pickup_requests_acceptor FOREIGN KEY (acceptor_id) REFERENCES users(id),
    CONSTRAINT fk_pickup_requests_pickup_file FOREIGN KEY (pickup_credential_file_id) REFERENCES stored_files(id),
    CONSTRAINT fk_pickup_requests_completion_file FOREIGN KEY (completion_proof_file_id) REFERENCES stored_files(id),
    CONSTRAINT fk_pickup_requests_payment FOREIGN KEY (payment_id) REFERENCES payment_records(id),
    CONSTRAINT uk_pickup_requests_payment_id UNIQUE (payment_id),
    CONSTRAINT chk_pickup_requests_reward_type CHECK (reward_type IN ('PAID','UNPAID')),
    CONSTRAINT chk_pickup_requests_status CHECK (status IN ('WAITING_PAYMENT','WAITING_ACCEPT','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_pickup_requests_cancel_reason CHECK (cancel_reason IS NULL OR cancel_reason IN ('USER_CANCELLED','PAYMENT_EXPIRED','ACCEPT_DEADLINE_EXPIRED','SYSTEM_CANCELLED')),
    CONSTRAINT chk_pickup_requests_reward_amount CHECK ((reward_type = 'PAID' AND reward_amount BETWEEN 1.00 AND 200.00) OR (reward_type = 'UNPAID' AND reward_amount IS NULL)),
    CONSTRAINT chk_pickup_requests_not_self_accept CHECK (acceptor_id IS NULL OR acceptor_id <> publisher_id),
    INDEX idx_pickup_hall (status, campus, reward_type, created_at),
    INDEX idx_pickup_status_created (status, created_at),
    INDEX idx_pickup_publisher_status (publisher_id, status, created_at),
    INDEX idx_pickup_acceptor_status (acceptor_id, status, created_at),
    INDEX idx_pickup_accept_deadline (status, accept_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代取请求主表';

-- ---------------------------------------------------------------------
-- 6. evaluations 评价表
-- ---------------------------------------------------------------------
CREATE TABLE evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_type VARCHAR(40) NOT NULL DEFAULT 'PICKUP_REQUEST' COMMENT '当前仅PICKUP_REQUEST',
    business_id BIGINT NOT NULL COMMENT '当前对应代取请求ID',
    reviewer_id BIGINT NOT NULL COMMENT '评价者ID',
    reviewee_id BIGINT NOT NULL COMMENT '被评价者ID',
    reviewee_role VARCHAR(20) NOT NULL COMMENT 'PUBLISHER/ACCEPTOR',
    rating_level VARCHAR(20) NOT NULL COMMENT 'GOOD/NEUTRAL/BAD',
    content VARCHAR(1000) NULL COMMENT '评价内容；rating_level=BAD时业务层要求非空，用于承载差评原因说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_evaluations_once UNIQUE (business_type, business_id, reviewer_id),
    CONSTRAINT fk_evaluations_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT fk_evaluations_reviewee FOREIGN KEY (reviewee_id) REFERENCES users(id),
    CONSTRAINT chk_evaluations_business_type CHECK (business_type IN ('PICKUP_REQUEST')),
    CONSTRAINT chk_evaluations_reviewee_role CHECK (reviewee_role IN ('PUBLISHER','ACCEPTOR')),
    CONSTRAINT chk_evaluations_rating_level CHECK (rating_level IN ('GOOD','NEUTRAL','BAD')),
    INDEX idx_evaluations_business (business_type, business_id),
    INDEX idx_evaluations_reviewee_role_created (reviewee_id, reviewee_role, created_at),
    INDEX idx_evaluations_reviewer_created (reviewer_id, created_at),
    INDEX idx_evaluations_rating_level (rating_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ---------------------------------------------------------------------
-- 7. notifications 站内通知表
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL COMMENT '接收人用户ID',
    type VARCHAR(40) NOT NULL COMMENT 'SYSTEM/VERIFICATION/PICKUP/PAYMENT/EVALUATION',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    business_type VARCHAR(40) NULL COMMENT '关联业务类型，例如PICKUP_REQUEST',
    business_id BIGINT NULL COMMENT '关联业务ID',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0未读，1已读',
    read_at DATETIME NULL COMMENT '已读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_receiver FOREIGN KEY (receiver_id) REFERENCES users(id),
    CONSTRAINT chk_notifications_type CHECK (type IN ('SYSTEM','VERIFICATION','PICKUP','PAYMENT','EVALUATION')),
    INDEX idx_notifications_receiver_read_created (receiver_id, is_read, created_at),
    INDEX idx_notifications_receiver_created (receiver_id, created_at),
    INDEX idx_notifications_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知表';
