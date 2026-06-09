-- =====================================================================
-- CampusHub 测试环境种子数据脚本
-- 用途：清空 campushub 库并灌入一套较大规模、可重复的测试数据。
--
-- 危险：本脚本会 TRUNCATE 所有业务表，清空全部数据。
-- 仅用于 deploy/test 测试环境，严禁对生产或正式演示库执行。
--
-- 预置账号：
--   admin / Admin@123   ADMIN   APPROVED
--   alice / User@123    USER    APPROVED
--   bob   / User@123    USER    APPROVED
--   carol / User@123    USER    REVIEWING
--   demo_user_01 ~ demo_user_60 / User@123
--
-- 数据规模：
--   用户：64 个
--   实名认证审核：30 条
--   积分流水：50 条
--   代取请求：159 条
--   评价：70 条
--   通知：约 200+ 条
--
-- 说明：
--   - 表已由 01-schema.sql 建好，本脚本只清数据并插入测试数据。
--   - 学号、姓名、手机号、文件路径均为明显测试值。
--   - 文件记录用于满足外键和列表展示，placeholder 文件内容不一定真实存在。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 清空所有业务表
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE evaluations;
TRUNCATE TABLE point_transactions;
TRUNCATE TABLE pickup_requests;
TRUNCATE TABLE verification_reviews;
TRUNCATE TABLE stored_files;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 2. 构造临时序列表，供批量生成测试数据
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_numbers;
CREATE TEMPORARY TABLE seed_numbers (
                                        n INT PRIMARY KEY
) ENGINE=Memory;

INSERT INTO seed_numbers (n)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200
)
SELECT n FROM seq;

-- ---------------------------------------------------------------------
-- 3. users 用户
--    密码哈希已验证：
--      admin -> Admin@123
--      其他用户 -> User@123
-- ---------------------------------------------------------------------
INSERT INTO users
(id, username, password_hash, nickname, phone, student_id, real_name, college, contact, auth_status, role,
 point_balance, last_check_in_date, created_at, updated_at)
VALUES
    (1, 'admin',
     '$2a$10$vdgwQmli09Y1TCL.Uy/bIORIg0Y3bvZqShvhH2gW3OvSYwGdvpNme',
     '平台管理员', NULL, NULL, NULL, NULL, NULL, 'APPROVED', 'ADMIN', 0, NULL, NOW(), NOW()),
    (2, 'alice',
     '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
     '测试-Alice', '13800000002', '200000001', '测试用户Alice', '计算机学院', 'wx: alice_test', 'APPROVED', 'USER',
     100, NULL, NOW(), NOW()),
    (3, 'bob',
     '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
     '测试-Bob', '13800000003', '200000002', '测试用户Bob', '软件学院', 'wx: bob_test', 'APPROVED', 'USER',
     100, NULL, NOW(), NOW()),
    (4, 'carol',
     '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
     '测试-Carol', '13800000004', NULL, NULL, '电子学院', NULL, 'REVIEWING', 'USER',
     0, NULL, NOW(), NOW());

INSERT INTO users
(id, username, password_hash, nickname, phone, student_id, real_name, college, contact, auth_status, role,
 point_balance, last_check_in_date, created_at, updated_at)
SELECT
    10 + n AS id,
    CONCAT('demo_user_', LPAD(n, 2, '0')) AS username,
    '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO' AS password_hash,
    CONCAT('测试用户', LPAD(n, 2, '0')) AS nickname,
    CONCAT('1380010', LPAD(n, 4, '0')) AS phone,
    CASE
        WHEN n BETWEEN 1 AND 48 THEN CONCAT('2099', LPAD(n, 5, '0'))
        ELSE NULL
        END AS student_id,
    CASE
        WHEN n BETWEEN 1 AND 48 THEN CONCAT('测试实名', LPAD(n, 2, '0'))
        ELSE NULL
        END AS real_name,
    ELT(1 + MOD(n, 6), '计算机学院', '软件学院', '电子学院', '商学院', '外国语学院', '数学学院') AS college,
    CONCAT('wx: demo_user_', LPAD(n, 2, '0')) AS contact,
    CASE
        WHEN n BETWEEN 1 AND 48 THEN 'APPROVED'
        WHEN n BETWEEN 49 AND 53 THEN 'REVIEWING'
        WHEN n BETWEEN 54 AND 56 THEN 'UNVERIFIED'
        ELSE 'REJECTED'
        END AS auth_status,
    'USER' AS role,
    CASE WHEN n BETWEEN 1 AND 48 THEN 100 ELSE 0 END AS point_balance,
    NULL AS last_check_in_date,
    DATE_SUB(NOW(), INTERVAL n DAY) AS created_at,
    DATE_SUB(NOW(), INTERVAL MOD(n, 7) DAY) AS updated_at
FROM seed_numbers
WHERE n <= 60;

-- ---------------------------------------------------------------------
-- 4. point_transactions 积分流水
--    已认证普通用户各一条认证赠送流水，与 users.point_balance 保持一致。
-- ---------------------------------------------------------------------
INSERT INTO point_transactions
(id, user_id, type, amount, balance_after, related_pickup_id, created_at, updated_at)
VALUES
    (1, 2, 'EARN_VERIFICATION', 100, 100, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (2, 3, 'EARN_VERIFICATION', 100, 100, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

INSERT INTO point_transactions
(id, user_id, type, amount, balance_after, related_pickup_id, created_at, updated_at)
SELECT
    100 + n AS id,
    10 + n AS user_id,
    'EARN_VERIFICATION' AS type,
    100 AS amount,
    100 AS balance_after,
    NULL AS related_pickup_id,
    DATE_SUB(NOW(), INTERVAL (n + 5) DAY) AS created_at,
    DATE_SUB(NOW(), INTERVAL (n + 5) DAY) AS updated_at
FROM seed_numbers
WHERE n <= 48;

-- ---------------------------------------------------------------------
-- 5. 实名认证审核临时数据
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_reviews;
CREATE TEMPORARY TABLE seed_reviews (
                                        id BIGINT PRIMARY KEY,
                                        user_id BIGINT NOT NULL,
                                        material_file_id BIGINT NOT NULL,
                                        submitted_student_id VARCHAR(32) NOT NULL,
                                        submitted_real_name VARCHAR(50) NOT NULL,
                                        status VARCHAR(20) NOT NULL,
                                        reviewer_id BIGINT NULL,
                                        reject_reason VARCHAR(500) NULL,
                                        submitted_at DATETIME NOT NULL,
                                        reviewed_at DATETIME NULL
) ENGINE=Memory;

INSERT INTO seed_reviews
(id, user_id, material_file_id, submitted_student_id, submitted_real_name, status, reviewer_id, reject_reason, submitted_at, reviewed_at)
VALUES
    (1, 4, 1, '200000003', '测试用户Carol', 'PENDING', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL);

INSERT INTO seed_reviews
(id, user_id, material_file_id, submitted_student_id, submitted_real_name, status, reviewer_id, reject_reason, submitted_at, reviewed_at)
SELECT
    1 + n AS id,
    CASE
        WHEN n <= 20 THEN 10 + n
        WHEN n <= 25 THEN 38 + n
        ELSE 41 + n
        END AS user_id,
    100 + n AS material_file_id,
    CONCAT('2098', LPAD(n, 5, '0')) AS submitted_student_id,
    CONCAT('认证测试', LPAD(n, 2, '0')) AS submitted_real_name,
    CASE
        WHEN n <= 20 THEN 'APPROVED'
        WHEN n <= 25 THEN 'PENDING'
        ELSE 'REJECTED'
        END AS status,
    CASE WHEN n BETWEEN 21 AND 25 THEN NULL ELSE 1 END AS reviewer_id,
    CASE WHEN n >= 26 THEN '测试材料信息不完整，请补充后重新提交。' ELSE NULL END AS reject_reason,
    DATE_SUB(NOW(), INTERVAL (n + 1) DAY) AS submitted_at,
    CASE WHEN n BETWEEN 21 AND 25 THEN NULL ELSE DATE_SUB(NOW(), INTERVAL n DAY) END AS reviewed_at
FROM seed_numbers
WHERE n <= 29;

-- ---------------------------------------------------------------------
-- 6. stored_files 文件元数据
-- ---------------------------------------------------------------------
INSERT INTO stored_files
(id, uploader_id, file_usage, original_filename, storage_path, mime_type, file_size, sha256, business_type, business_id, created_at, updated_at)
SELECT
    material_file_id AS id,
    user_id AS uploader_id,
    'VERIFICATION_MATERIAL' AS file_usage,
    CONCAT('verification_', LPAD(id, 3, '0'), '.jpg') AS original_filename,
    CONCAT('seed/placeholder/verification_', LPAD(id, 3, '0'), '.jpg') AS storage_path,
    'image/jpeg' AS mime_type,
    102400 + id AS file_size,
    NULL AS sha256,
    'VERIFICATION_REVIEW' AS business_type,
    id AS business_id,
    submitted_at AS created_at,
    submitted_at AS updated_at
FROM seed_reviews;

-- ---------------------------------------------------------------------
-- 7. verification_reviews 实名认证审核
-- ---------------------------------------------------------------------
INSERT INTO verification_reviews
(id, user_id, material_file_id, submitted_student_id, submitted_real_name, status, reviewer_id, reject_reason, submitted_at, reviewed_at, created_at, updated_at)
SELECT
    id, user_id, material_file_id, submitted_student_id, submitted_real_name, status, reviewer_id, reject_reason,
    submitted_at, reviewed_at, submitted_at, COALESCE(reviewed_at, submitted_at)
FROM seed_reviews;

-- ---------------------------------------------------------------------
-- 8. 构造代取请求临时数据
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_pickups;
CREATE TEMPORARY TABLE seed_pickups (
                                        id BIGINT PRIMARY KEY,
                                        publisher_id BIGINT NOT NULL,
                                        acceptor_id BIGINT NULL,
                                        campus VARCHAR(80) NOT NULL,
                                        pickup_location VARCHAR(200) NOT NULL,
                                        delivery_location VARCHAR(200) NOT NULL,
                                        item_description VARCHAR(500) NOT NULL,
                                        reward_type VARCHAR(20) NOT NULL,
                                        reward_amount DECIMAL(10,2) NULL,
                                        status VARCHAR(30) NOT NULL,
                                        cancel_reason VARCHAR(40) NULL,
                                        cancel_detail VARCHAR(500) NULL,
                                        accept_deadline DATETIME NOT NULL,
                                        accepted_at DATETIME NULL,
                                        completed_at DATETIME NULL,
                                        cancelled_at DATETIME NULL,
                                        created_at DATETIME NOT NULL
) ENGINE=Memory;

-- 4 条固定演示单：保留原有核心演示场景。
INSERT INTO seed_pickups
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at)
VALUES
    (1, 2, NULL, 'XIANLIN', '菜鸟驿站(仙林)', '宿舍14栋', '一个快递盒，约2kg，请轻拿轻放',
     'UNPAID', NULL, 'WAITING_ACCEPT', NULL, NULL, DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (2, 2, 3, 'XIANLIN', '京东快递点(仙林)', '教学楼A座', '文件袋，含书籍若干',
     'UNPAID', NULL, 'IN_PROGRESS', NULL, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (3, 2, 3, 'GULOU', '丰巢柜(鼓楼)', '图书馆门口', '小型包裹，已完成代取',
     'UNPAID', NULL, 'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (4, 2, 3, 'XIANLIN', '中通快递(仙林)', '宿舍9栋', '一箱牛奶，已完成代取并双方互评',
     'UNPAID', NULL, 'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 50 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 4 DAY));

-- id=5..64：60 条待接单，覆盖四个校区、无报酬和有报酬。
INSERT INTO seed_pickups
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at)
SELECT
    4 + n AS id,
    10 + MOD(n, 48) + 1 AS publisher_id,
    NULL AS acceptor_id,
    ELT(1 + MOD(n, 4), 'XIANLIN', 'GULOU', 'PUKOU', 'SUZHOU') AS campus,
    ELT(1 + MOD(n, 6), '菜鸟驿站', '京东快递点', '顺丰服务点', '丰巢柜', '中通快递', '校门口快递架') AS pickup_location,
    CONCAT(ELT(1 + MOD(n, 5), '宿舍', '教学楼', '图书馆', '实验楼', '食堂'), 1 + MOD(n, 18), '号点') AS delivery_location,
    CONCAT('待接单测试包裹 ', LPAD(n, 2, '0'), '，用于大厅分页和筛选测试') AS item_description,
    CASE WHEN MOD(n, 3) = 0 THEN 'PAID' ELSE 'UNPAID' END AS reward_type,
    CASE WHEN MOD(n, 3) = 0 THEN CAST(3 + MOD(n, 25) AS DECIMAL(10,2)) ELSE NULL END AS reward_amount,
    'WAITING_ACCEPT' AS status,
    NULL AS cancel_reason,
    NULL AS cancel_detail,
    DATE_ADD(NOW(), INTERVAL (12 + MOD(n, 72)) HOUR) AS accept_deadline,
    NULL AS accepted_at,
    NULL AS completed_at,
    NULL AS cancelled_at,
    DATE_SUB(NOW(), INTERVAL n HOUR) AS created_at
FROM seed_numbers
WHERE n <= 60;

-- id=65..94：30 条进行中。
INSERT INTO seed_pickups
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at)
SELECT
    64 + n AS id,
    10 + MOD(n, 48) + 1 AS publisher_id,
    10 + MOD(n + 17, 48) + 1 AS acceptor_id,
    ELT(1 + MOD(n, 4), 'XIANLIN', 'GULOU', 'PUKOU', 'SUZHOU') AS campus,
    ELT(1 + MOD(n, 6), '菜鸟驿站', '京东快递点', '顺丰服务点', '丰巢柜', '中通快递', '校门口快递架') AS pickup_location,
    CONCAT(ELT(1 + MOD(n, 5), '宿舍', '教学楼', '图书馆', '实验楼', '食堂'), 1 + MOD(n, 18), '号点') AS delivery_location,
    CONCAT('进行中测试包裹 ', LPAD(n, 2, '0'), '，用于我的代取记录测试') AS item_description,
    CASE WHEN MOD(n, 4) = 0 THEN 'PAID' ELSE 'UNPAID' END AS reward_type,
    CASE WHEN MOD(n, 4) = 0 THEN CAST(6 + MOD(n, 30) AS DECIMAL(10,2)) ELSE NULL END AS reward_amount,
    'IN_PROGRESS' AS status,
    NULL AS cancel_reason,
    NULL AS cancel_detail,
    DATE_ADD(NOW(), INTERVAL (6 + MOD(n, 48)) HOUR) AS accept_deadline,
    DATE_SUB(NOW(), INTERVAL (30 + n) MINUTE) AS accepted_at,
    NULL AS completed_at,
    NULL AS cancelled_at,
    DATE_SUB(NOW(), INTERVAL (n + 2) HOUR) AS created_at
FROM seed_numbers
WHERE n <= 30;

-- id=95..139：45 条已完成，供历史列表、评价列表和信誉统计测试。
INSERT INTO seed_pickups
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at)
SELECT
    94 + n AS id,
    10 + MOD(n, 48) + 1 AS publisher_id,
    10 + MOD(n + 11, 48) + 1 AS acceptor_id,
    ELT(1 + MOD(n, 4), 'XIANLIN', 'GULOU', 'PUKOU', 'SUZHOU') AS campus,
    ELT(1 + MOD(n, 6), '菜鸟驿站', '京东快递点', '顺丰服务点', '丰巢柜', '中通快递', '校门口快递架') AS pickup_location,
    CONCAT(ELT(1 + MOD(n, 5), '宿舍', '教学楼', '图书馆', '实验楼', '食堂'), 1 + MOD(n, 18), '号点') AS delivery_location,
    CONCAT('已完成测试包裹 ', LPAD(n, 2, '0'), '，用于评价与历史数据测试') AS item_description,
    CASE WHEN MOD(n, 5) IN (0, 1) THEN 'PAID' ELSE 'UNPAID' END AS reward_type,
    CASE WHEN MOD(n, 5) IN (0, 1) THEN CAST(8 + MOD(n, 40) AS DECIMAL(10,2)) ELSE NULL END AS reward_amount,
    'COMPLETED' AS status,
    NULL AS cancel_reason,
    NULL AS cancel_detail,
    DATE_SUB(NOW(), INTERVAL (n + 2) DAY) AS accept_deadline,
    DATE_SUB(NOW(), INTERVAL (n * 2 + 6) HOUR) AS accepted_at,
    DATE_SUB(NOW(), INTERVAL (n * 2 + 3) HOUR) AS completed_at,
    NULL AS cancelled_at,
    DATE_SUB(NOW(), INTERVAL (n + 5) DAY) AS created_at
FROM seed_numbers
WHERE n <= 45;

-- id=140..159：20 条已取消，覆盖取消原因。
INSERT INTO seed_pickups
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at)
SELECT
    139 + n AS id,
    10 + MOD(n, 48) + 1 AS publisher_id,
    NULL AS acceptor_id,
    ELT(1 + MOD(n, 4), 'XIANLIN', 'GULOU', 'PUKOU', 'SUZHOU') AS campus,
    ELT(1 + MOD(n, 6), '菜鸟驿站', '京东快递点', '顺丰服务点', '丰巢柜', '中通快递', '校门口快递架') AS pickup_location,
    CONCAT(ELT(1 + MOD(n, 5), '宿舍', '教学楼', '图书馆', '实验楼', '食堂'), 1 + MOD(n, 18), '号点') AS delivery_location,
    CONCAT('已取消测试包裹 ', LPAD(n, 2, '0'), '，用于异常状态展示测试') AS item_description,
    CASE WHEN MOD(n, 4) IN (0, 2) THEN 'PAID' ELSE 'UNPAID' END AS reward_type,
    CASE WHEN MOD(n, 4) IN (0, 2) THEN CAST(5 + MOD(n, 20) AS DECIMAL(10,2)) ELSE NULL END AS reward_amount,
    'CANCELLED' AS status,
    ELT(1 + MOD(n, 3), 'USER_CANCELLED', 'ACCEPT_DEADLINE_EXPIRED', 'SYSTEM_CANCELLED') AS cancel_reason,
    CONCAT('测试取消说明 ', LPAD(n, 2, '0')) AS cancel_detail,
    DATE_SUB(NOW(), INTERVAL (n + 1) DAY) AS accept_deadline,
    NULL AS accepted_at,
    NULL AS completed_at,
    DATE_SUB(NOW(), INTERVAL n DAY) AS cancelled_at,
    DATE_SUB(NOW(), INTERVAL (n + 2) DAY) AS created_at
FROM seed_numbers
WHERE n <= 20;

-- ---------------------------------------------------------------------
-- 9. 代取相关文件：每条代取一条取件凭证，已完成代取一条完成凭证
-- ---------------------------------------------------------------------
INSERT INTO stored_files
(id, uploader_id, file_usage, original_filename, storage_path, mime_type, file_size, sha256, business_type, business_id, created_at, updated_at)
SELECT
    1000 + id AS id,
    publisher_id AS uploader_id,
    'PICKUP_CREDENTIAL' AS file_usage,
    CONCAT('pickup_credential_', LPAD(id, 4, '0'), '.jpg') AS original_filename,
    CONCAT('seed/placeholder/pickup_credential_', LPAD(id, 4, '0'), '.jpg') AS storage_path,
    'image/jpeg' AS mime_type,
    120000 + id AS file_size,
    NULL AS sha256,
    'PICKUP_REQUEST' AS business_type,
    id AS business_id,
    created_at AS created_at,
    created_at AS updated_at
FROM seed_pickups;

INSERT INTO stored_files
(id, uploader_id, file_usage, original_filename, storage_path, mime_type, file_size, sha256, business_type, business_id, created_at, updated_at)
SELECT
    2000 + id AS id,
    acceptor_id AS uploader_id,
    'COMPLETION_PROOF' AS file_usage,
    CONCAT('completion_proof_', LPAD(id, 4, '0'), '.jpg') AS original_filename,
    CONCAT('seed/placeholder/completion_proof_', LPAD(id, 4, '0'), '.jpg') AS storage_path,
    'image/jpeg' AS mime_type,
    150000 + id AS file_size,
    NULL AS sha256,
    'PICKUP_REQUEST' AS business_type,
    id AS business_id,
    completed_at AS created_at,
    completed_at AS updated_at
FROM seed_pickups
WHERE status = 'COMPLETED';

-- ---------------------------------------------------------------------
-- 10. pickup_requests 代取请求
-- ---------------------------------------------------------------------
INSERT INTO pickup_requests
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, pickup_credential_file_id, completion_proof_file_id,
 status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at, updated_at)
SELECT
    id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
    reward_type, reward_amount, 1000 + id AS pickup_credential_file_id,
    CASE WHEN status = 'COMPLETED' THEN 2000 + id ELSE NULL END AS completion_proof_file_id,
    status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at,
    created_at, COALESCE(completed_at, cancelled_at, accepted_at, created_at) AS updated_at
FROM seed_pickups;

-- ---------------------------------------------------------------------
-- 11. evaluations 评价
-- ---------------------------------------------------------------------
INSERT INTO evaluations
(id, business_type, business_id, reviewer_id, reviewee_id, reviewee_role, rating_level, content, created_at, updated_at)
VALUES
    (1, 'PICKUP_REQUEST', 3, 3, 2, 'PUBLISHER', 'GOOD', '发布方沟通清晰，地址准确，合作愉快。',
     DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (2, 'PICKUP_REQUEST', 4, 3, 2, 'PUBLISHER', 'GOOD', '物品好取，备注详细，发布方很靠谱。',
     DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR)),
    (3, 'PICKUP_REQUEST', 4, 2, 3, 'ACCEPTOR', 'GOOD', '接单方送达及时，态度很好，下次还找他。',
     DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR));

-- 所有批量已完成单：接单方评价发布方。
INSERT INTO evaluations
(id, business_type, business_id, reviewer_id, reviewee_id, reviewee_role, rating_level, content, created_at, updated_at)
SELECT
    1000 + id AS id,
    'PICKUP_REQUEST' AS business_type,
    id AS business_id,
    acceptor_id AS reviewer_id,
    publisher_id AS reviewee_id,
    'PUBLISHER' AS reviewee_role,
    CASE WHEN MOD(id, 10) = 0 THEN 'BAD' WHEN MOD(id, 4) = 0 THEN 'NEUTRAL' ELSE 'GOOD' END AS rating_level,
    CASE
        WHEN MOD(id, 10) = 0 THEN '测试差评：发布信息不够完整，影响代取效率。'
        WHEN MOD(id, 4) = 0 THEN '测试中评：整体顺利，但沟通可以更及时。'
        ELSE '测试好评：发布信息清晰，取件过程顺利。'
        END AS content,
    DATE_ADD(completed_at, INTERVAL 10 MINUTE) AS created_at,
    DATE_ADD(completed_at, INTERVAL 10 MINUTE) AS updated_at
FROM seed_pickups
WHERE status = 'COMPLETED' AND id >= 95;

-- 偶数批量已完成单：发布方评价接单方，保留部分未评价场景用于演示提交入口。
INSERT INTO evaluations
(id, business_type, business_id, reviewer_id, reviewee_id, reviewee_role, rating_level, content, created_at, updated_at)
SELECT
    2000 + id AS id,
    'PICKUP_REQUEST' AS business_type,
    id AS business_id,
    publisher_id AS reviewer_id,
    acceptor_id AS reviewee_id,
    'ACCEPTOR' AS reviewee_role,
    CASE WHEN MOD(id, 12) = 0 THEN 'BAD' WHEN MOD(id, 6) = 0 THEN 'NEUTRAL' ELSE 'GOOD' END AS rating_level,
    CASE
        WHEN MOD(id, 12) = 0 THEN '测试差评：送达时间较晚，需要改进。'
        WHEN MOD(id, 6) = 0 THEN '测试中评：服务完成，但沟通一般。'
        ELSE '测试好评：接单方准时送达，服务可靠。'
        END AS content,
    DATE_ADD(completed_at, INTERVAL 20 MINUTE) AS created_at,
    DATE_ADD(completed_at, INTERVAL 20 MINUTE) AS updated_at
FROM seed_pickups
WHERE status = 'COMPLETED' AND id >= 95 AND MOD(id, 2) = 0;

-- ---------------------------------------------------------------------
-- 12. notifications 站内通知
-- ---------------------------------------------------------------------
-- 系统欢迎通知。
INSERT INTO notifications
(id, receiver_id, type, title, content, business_type, business_id, is_read, read_at, created_at, updated_at)
SELECT
    1000 + id AS id,
    id AS receiver_id,
    'SYSTEM' AS type,
    '欢迎使用 CampusHub' AS title,
    '这是测试环境生成的系统欢迎通知。' AS content,
    NULL AS business_type,
    NULL AS business_id,
    CASE WHEN MOD(id, 3) = 0 THEN 1 ELSE 0 END AS is_read,
    CASE WHEN MOD(id, 3) = 0 THEN DATE_SUB(NOW(), INTERVAL 1 DAY) ELSE NULL END AS read_at,
    created_at,
    created_at AS updated_at
FROM users
WHERE role = 'USER';

-- 实名认证处理结果通知。
INSERT INTO notifications
(id, receiver_id, type, title, content, business_type, business_id, is_read, read_at, created_at, updated_at)
SELECT
    2000 + id AS id,
    user_id AS receiver_id,
    'VERIFICATION' AS type,
    CASE WHEN status = 'APPROVED' THEN '实名认证已通过' ELSE '实名认证未通过' END AS title,
    CASE WHEN status = 'APPROVED' THEN '您的实名认证已通过，可以发布和接受代取请求。' ELSE '您的实名认证未通过，请根据原因重新提交材料。' END AS content,
    NULL AS business_type,
    NULL AS business_id,
    CASE WHEN MOD(id, 2) = 0 THEN 1 ELSE 0 END AS is_read,
    CASE WHEN MOD(id, 2) = 0 THEN reviewed_at ELSE NULL END AS read_at,
    reviewed_at AS created_at,
    reviewed_at AS updated_at
FROM seed_reviews
WHERE status IN ('APPROVED', 'REJECTED');

-- 代取状态通知。
INSERT INTO notifications
(id, receiver_id, type, title, content, business_type, business_id, is_read, read_at, created_at, updated_at)
SELECT
    3000 + id AS id,
    publisher_id AS receiver_id,
    'PICKUP' AS type,
    CASE
        WHEN status = 'IN_PROGRESS' THEN '代取请求已被接单'
        WHEN status = 'COMPLETED' THEN '代取请求已完成'
        WHEN status = 'CANCELLED' THEN '代取请求已取消'
        ELSE '代取请求状态更新'
        END AS title,
    CASE
        WHEN status = 'IN_PROGRESS' THEN '您发布的代取请求已被同学接单。'
        WHEN status = 'COMPLETED' THEN '您发布的代取请求已完成，请及时评价。'
        WHEN status = 'CANCELLED' THEN '您的代取请求已取消。'
        ELSE '您的代取请求状态已更新。'
        END AS content,
    'PICKUP_REQUEST' AS business_type,
    id AS business_id,
    CASE WHEN MOD(id, 4) = 0 THEN 1 ELSE 0 END AS is_read,
    CASE WHEN MOD(id, 4) = 0 THEN COALESCE(completed_at, cancelled_at, accepted_at, created_at) ELSE NULL END AS read_at,
    COALESCE(completed_at, cancelled_at, accepted_at, created_at) AS created_at,
    COALESCE(completed_at, cancelled_at, accepted_at, created_at) AS updated_at
FROM seed_pickups
WHERE status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED');

-- 评价通知。
INSERT INTO notifications
(id, receiver_id, type, title, content, business_type, business_id, is_read, read_at, created_at, updated_at)
SELECT
    5000 + id AS id,
    reviewee_id AS receiver_id,
    'EVALUATION' AS type,
    '收到一条新评价' AS title,
    '您在一次代取服务中收到了新的评价。' AS content,
    'PICKUP_REQUEST' AS business_type,
    business_id AS business_id,
    CASE WHEN MOD(id, 5) = 0 THEN 1 ELSE 0 END AS is_read,
    CASE WHEN MOD(id, 5) = 0 THEN created_at ELSE NULL END AS read_at,
    created_at,
    created_at AS updated_at
FROM evaluations;

-- ---------------------------------------------------------------------
-- 13. 清理临时表并输出检查提示
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_pickups;
DROP TEMPORARY TABLE IF EXISTS seed_reviews;
DROP TEMPORARY TABLE IF EXISTS seed_numbers;

-- 常用检查 SQL：
--   SELECT COUNT(*) FROM users;
--   SELECT status, COUNT(*) FROM pickup_requests GROUP BY status ORDER BY status;
--   SELECT reward_type, COUNT(*) FROM pickup_requests GROUP BY reward_type;
--   SELECT type, COUNT(*) FROM point_transactions GROUP BY type ORDER BY type;
--   SELECT rating_level, COUNT(*) FROM evaluations GROUP BY rating_level ORDER BY rating_level;
--   SELECT type, COUNT(*) FROM notifications GROUP BY type ORDER BY type;
-- =====================================================================
