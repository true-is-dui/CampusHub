-- =====================================================================
-- CampusHub 开发联调种子数据脚本 (seed-dev.sql)
-- 用途：清空 campushub 库并灌入一套可演示的联调数据。
--
-- ⚠️⚠️⚠️ 危险：本脚本会 TRUNCATE 所有业务表，清空全部数据！⚠️⚠️⚠️
--   仅用于本地 / 共享【开发】库 campushub。严禁对生产或正式演示库执行。
--
-- 使用方式（重置并灌入开发种子数据）：
--   mysql -u root -p campushub < backend/src/main/resources/seed-dev.sql
--
-- 预置账户（密码均为 BCrypt 哈希，非明文）：
--   admin / Admin@123   ADMIN   APPROVED   管理员
--   alice / User@123    USER    APPROVED   已认证（代取发布方）
--   bob   / User@123    USER    APPROVED   已认证（代取接单方）
--   carol / User@123    USER    REVIEWING  待审核（用于演示管理员审核流程）
--
-- 说明：
--   - 表已由 schema.sql 建好，本脚本不建表，只清数据 + 插数据。
--   - 学号 / 姓名均为明显的测试值，不含真实个人信息。
--   - PAID 支付流程留给前端走支付宝沙箱实跑，种子只造 UNPAID 代取，
--     规避半截支付态与跨字段约束。
-- =====================================================================

USE campushub;

-- ---------------------------------------------------------------------
-- 1. 清空所有业务表（外键存在，先关检查再按任意顺序 TRUNCATE）
--    TRUNCATE 同时把 AUTO_INCREMENT 重置为 1，保证固定主键可重复灌入。
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE evaluations;
TRUNCATE TABLE pickup_requests;
TRUNCATE TABLE payment_records;
TRUNCATE TABLE verification_reviews;
TRUNCATE TABLE stored_files;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 2. users 用户（固定 id 便于后续记录互相引用）
--    密码哈希由项目 BCryptPasswordEncoder 生成，已验证 matches=true。
--      admin → Admin@123
--      alice/bob/carol → User@123（同一密码复用同一哈希值即可）
-- ---------------------------------------------------------------------
INSERT INTO users
  (id, username, password_hash, nickname, phone, student_id, real_name, college, contact, auth_status, role, created_at, updated_at)
VALUES
  (1, 'admin',
      '$2a$10$vdgwQmli09Y1TCL.Uy/bIORIg0Y3bvZqShvhH2gW3OvSYwGdvpNme',
      '平台管理员', NULL, NULL, NULL, NULL, NULL, 'APPROVED', 'ADMIN', NOW(), NOW()),
  (2, 'alice',
      '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
      '测试-Alice', '13800000002', '200000001', '测试用户Alice', '计算机学院', 'wx: alice_test', 'APPROVED', 'USER', NOW(), NOW()),
  (3, 'bob',
      '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
      '测试-Bob', '13800000003', '200000002', '测试用户Bob', '软件学院', 'wx: bob_test', 'APPROVED', 'USER', NOW(), NOW()),
  (4, 'carol',
      '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
      '测试-Carol', '13800000004', NULL, NULL, '电子学院', NULL, 'REVIEWING', 'USER', NOW(), NOW());

-- ---------------------------------------------------------------------
-- 3. stored_files 文件元数据
--    storage_path 为占位路径——联调时这些文件内容可能不存在于磁盘，
--    仅用于满足外键与列表展示；真正的图片读取请走前端实际上传产生的文件。
--      id=1  carol 的认证材料（VERIFICATION_MATERIAL）
--      id=2  WAITING_ACCEPT 代取的取件凭证（PICKUP_CREDENTIAL）
--      id=3  IN_PROGRESS  代取的取件凭证
--      id=4  COMPLETED    代取的取件凭证
--      id=5  COMPLETED    代取的完成凭证（COMPLETION_PROOF）
-- ---------------------------------------------------------------------
INSERT INTO stored_files
  (id, uploader_id, file_usage, original_filename, storage_path, mime_type, file_size, sha256, business_type, business_id, created_at, updated_at)
VALUES
  (1, 4, 'VERIFICATION_MATERIAL', 'carol_id_card.jpg', 'seed/placeholder/carol_id_card.jpg', 'image/jpeg', 102400, NULL, 'VERIFICATION', NULL, NOW(), NOW()),
  (2, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_1.jpg', 'seed/placeholder/pickup_credential_1.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
  (3, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_2.jpg', 'seed/placeholder/pickup_credential_2.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
  (4, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_3.jpg', 'seed/placeholder/pickup_credential_3.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
  (5, 3, 'COMPLETION_PROOF',      'completion_proof_1.jpg',  'seed/placeholder/completion_proof_1.jpg',  'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 4. verification_reviews 实名认证审核
--    carol 一条 PENDING，reviewer 为空，供管理员审核流程演示。
-- ---------------------------------------------------------------------
INSERT INTO verification_reviews
  (id, user_id, material_file_id, submitted_student_id, submitted_real_name, status, reviewer_id, reject_reason, submitted_at, reviewed_at, created_at, updated_at)
VALUES
  (1, 4, 1, '200000003', '测试用户Carol', 'PENDING', NULL, NULL, NOW(), NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 5. pickup_requests 代取请求（统一 UNPAID，规避支付外键与金额约束）
--      id=1  WAITING_ACCEPT  alice 发布，大厅可见，bob 可接单
--      id=2  IN_PROGRESS     alice 发布 / bob 接单
--      id=3  COMPLETED       alice 发布 / bob 接单（可演示评价入口）
-- ---------------------------------------------------------------------
INSERT INTO pickup_requests
  (id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
   reward_type, reward_amount, pickup_credential_file_id, completion_proof_file_id, payment_id,
   status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at, updated_at)
VALUES
  (1, 2, NULL, 'XIANLIN', '菜鸟驿站(仙林)', '宿舍14栋', '一个快递盒，约2kg，请轻拿轻放',
   'UNPAID', NULL, 2, NULL, NULL,
   'WAITING_ACCEPT', NULL, NULL, DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, NOW(), NOW()),
  (2, 2, 3, 'XIANLIN', '京东快递点(仙林)', '教学楼A座', '文件袋，含书籍若干',
   'UNPAID', NULL, 3, NULL, NULL,
   'IN_PROGRESS', NULL, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NULL, NULL, NOW(), NOW()),
  (3, 2, 3, 'GULOU', '丰巢柜(鼓楼)', '图书馆门口', '小型包裹，已完成代取',
   'UNPAID', NULL, 4, 5, NULL,
   'COMPLETED', NULL, NULL, DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -2 HOUR), DATE_ADD(NOW(), INTERVAL -1 HOUR), NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 完成。可执行以下查询确认：
--   SELECT id, username, role, auth_status FROM users;
--   SELECT id, publisher_id, acceptor_id, status FROM pickup_requests;
--   SELECT id, user_id, status FROM verification_reviews;
-- ---------------------------------------------------------------------
