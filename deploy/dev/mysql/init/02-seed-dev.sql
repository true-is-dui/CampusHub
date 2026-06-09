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
--   - 有报酬代取以平台积分计价：alice/bob 预置 100 积分余额（认证赠送），
--     配 point_transactions 流水演示「我的积分」展示。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 清空所有业务表（外键存在，先关检查再按任意顺序 TRUNCATE）
--    TRUNCATE 同时把 AUTO_INCREMENT 重置为 1，保证固定主键可重复灌入。
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
-- 2. users 用户（固定 id 便于后续记录互相引用）
--    密码哈希由项目 BCryptPasswordEncoder 生成，已验证 matches=true。
--      admin → Admin@123
--      alice/bob/carol → User@123（同一密码复用同一哈希值即可）
-- ---------------------------------------------------------------------
INSERT INTO users
(id, username, password_hash, nickname, phone, student_id, real_name, college, contact, auth_status, role, point_balance, last_check_in_date, created_at, updated_at)
VALUES
    (1, 'admin',
     '$2a$10$vdgwQmli09Y1TCL.Uy/bIORIg0Y3bvZqShvhH2gW3OvSYwGdvpNme',
     '平台管理员', NULL, NULL, NULL, NULL, NULL, 'APPROVED', 'ADMIN', 0, NULL, NOW(), NOW()),
    (2, 'alice',
     '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
     '测试-Alice', '13800000002', '200000001', '测试用户Alice', '计算机学院', 'wx: alice_test', 'APPROVED', 'USER', 100, NULL, NOW(), NOW()),
    (3, 'bob',
     '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
     '测试-Bob', '13800000003', '200000002', '测试用户Bob', '软件学院', 'wx: bob_test', 'APPROVED', 'USER', 100, NULL, NOW(), NOW()),
    (4, 'carol',
     '$2a$10$SVacMEcEnqkXU9RancgTVOBdBHc5yQPXvo3MVQgH1Jw4JxYN7l/gO',
     '测试-Carol', '13800000004', NULL, NULL, '电子学院', NULL, 'REVIEWING', 'USER', 0, NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 3. stored_files 文件元数据
--    storage_path 为占位路径——联调时这些文件内容可能不存在于磁盘，
--    仅用于满足外键与列表展示；真正的图片读取请走前端实际上传产生的文件。
--      id=1  carol 的认证材料（VERIFICATION_MATERIAL）
--      id=2  WAITING_ACCEPT 代取的取件凭证（PICKUP_CREDENTIAL）
--      id=3  IN_PROGRESS  代取的取件凭证
--      id=4  COMPLETED    代取的取件凭证
--      id=5  COMPLETED    代取的完成凭证（COMPLETION_PROOF）
--      id=6  已双方互评的历史 COMPLETED 代取的取件凭证
--      id=7  已双方互评的历史 COMPLETED 代取的完成凭证
-- ---------------------------------------------------------------------
INSERT INTO stored_files
(id, uploader_id, file_usage, original_filename, storage_path, mime_type, file_size, sha256, business_type, business_id, created_at, updated_at)
VALUES
    (1, 4, 'VERIFICATION_MATERIAL', 'carol_id_card.jpg', 'seed/placeholder/carol_id_card.jpg', 'image/jpeg', 102400, NULL, 'VERIFICATION', NULL, NOW(), NOW()),
    (2, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_1.jpg', 'seed/placeholder/pickup_credential_1.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
    (3, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_2.jpg', 'seed/placeholder/pickup_credential_2.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
    (4, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_3.jpg', 'seed/placeholder/pickup_credential_3.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
    (5, 3, 'COMPLETION_PROOF',      'completion_proof_1.jpg',  'seed/placeholder/completion_proof_1.jpg',  'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
    (6, 2, 'PICKUP_CREDENTIAL',     'pickup_credential_4.jpg', 'seed/placeholder/pickup_credential_4.jpg', 'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW()),
    (7, 3, 'COMPLETION_PROOF',      'completion_proof_2.jpg',  'seed/placeholder/completion_proof_2.jpg',  'image/jpeg', 102400, NULL, 'PICKUP_REQUEST', NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 4. verification_reviews 实名认证审核
--    carol 一条 PENDING，reviewer 为空，供管理员审核流程演示。
-- ---------------------------------------------------------------------
INSERT INTO verification_reviews
(id, user_id, material_file_id, submitted_student_id, submitted_real_name, status, reviewer_id, reject_reason, submitted_at, reviewed_at, created_at, updated_at)
VALUES
    (1, 4, 1, '200000003', '测试用户Carol', 'PENDING', NULL, NULL, NOW(), NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 5. pickup_requests 代取请求（统一 UNPAID，规避有报酬金额约束，PAID 流转见 point_transactions 演示）
--      id=1  WAITING_ACCEPT  alice 发布，大厅可见，bob 可接单
--      id=2  IN_PROGRESS     alice 发布 / bob 接单
--      id=3  COMPLETED       alice 发布 / bob 接单
--                            （演示评价入口：仅灌 bob→alice 一条评价，
--                             alice→bob 的入口留空，可现场演示提交评价）
--      id=4  COMPLETED       alice 发布 / bob 接单
--                            （已双方互评的历史单，演示双方信誉/好评率展示）
-- ---------------------------------------------------------------------
INSERT INTO pickup_requests
(id, publisher_id, acceptor_id, campus, pickup_location, delivery_location, item_description,
 reward_type, reward_amount, pickup_credential_file_id, completion_proof_file_id,
 status, cancel_reason, cancel_detail, accept_deadline, accepted_at, completed_at, cancelled_at, created_at, updated_at)
VALUES
    (1, 2, NULL, 'XIANLIN', '菜鸟驿站(仙林)', '宿舍14栋', '一个快递盒，约2kg，请轻拿轻放',
     'UNPAID', NULL, 2, NULL,
     'WAITING_ACCEPT', NULL, NULL, DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, NOW(), NOW()),
    (2, 2, 3, 'XIANLIN', '京东快递点(仙林)', '教学楼A座', '文件袋，含书籍若干',
     'UNPAID', NULL, 3, NULL,
     'IN_PROGRESS', NULL, NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NULL, NULL, NOW(), NOW()),
    (3, 2, 3, 'GULOU', '丰巢柜(鼓楼)', '图书馆门口', '小型包裹，已完成代取',
     'UNPAID', NULL, 4, 5,
     'COMPLETED', NULL, NULL, DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -2 HOUR), DATE_ADD(NOW(), INTERVAL -1 HOUR), NULL, NOW(), NOW()),
    (4, 2, 3, 'XIANLIN', '中通快递(仙林)', '宿舍9栋', '一箱牛奶，已完成代取并双方互评',
     'UNPAID', NULL, 6, 7,
     'COMPLETED', NULL, NULL, DATE_ADD(NOW(), INTERVAL -3 DAY), DATE_ADD(NOW(), INTERVAL -50 HOUR), DATE_ADD(NOW(), INTERVAL -48 HOUR), NULL, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 6. evaluations 评价（仅针对 COMPLETED 代取，business_type=PICKUP_REQUEST）
--    唯一约束 uk_evaluations_once(business_type, business_id, reviewer_id)：
--      同一代取中同一评价者只能评一次。reviewee_role 记录【被评价人】在该单中的角色。
--      差评(BAD)的 content 业务层要求非空，种子里 BAD 均带原因说明。
--
--    代取 id=3：仅 bob→alice 一条，alice→bob 故意留空（演示提交评价入口）。
--    代取 id=4：alice↔bob 双方互评齐全（演示双方好评率/评价列表展示）。
-- ---------------------------------------------------------------------
INSERT INTO evaluations
(id, business_type, business_id, reviewer_id, reviewee_id, reviewee_role, rating_level, content, created_at, updated_at)
VALUES
    -- 代取 id=3：bob(接单方,3) 评 alice(发布方,2)，被评价人角色 PUBLISHER
    (1, 'PICKUP_REQUEST', 3, 3, 2, 'PUBLISHER', 'GOOD', '发布方沟通清晰，地址准确，合作愉快。',
     DATE_ADD(NOW(), INTERVAL -30 MINUTE), DATE_ADD(NOW(), INTERVAL -30 MINUTE)),
    -- 代取 id=4：bob(接单方,3) 评 alice(发布方,2)，被评价人角色 PUBLISHER
    (2, 'PICKUP_REQUEST', 4, 3, 2, 'PUBLISHER', 'GOOD', '物品好取，备注详细，发布方很靠谱。',
     DATE_ADD(NOW(), INTERVAL -47 HOUR), DATE_ADD(NOW(), INTERVAL -47 HOUR)),
    -- 代取 id=4：alice(发布方,2) 评 bob(接单方,3)，被评价人角色 ACCEPTOR
    (3, 'PICKUP_REQUEST', 4, 2, 3, 'ACCEPTOR', 'GOOD', '接单方送达及时，态度很好，下次还找他。',
     DATE_ADD(NOW(), INTERVAL -47 HOUR), DATE_ADD(NOW(), INTERVAL -47 HOUR));

-- ---------------------------------------------------------------------
-- 7. notifications 站内通知（与评价业务一致：评价创建后通知被评价人）
--    type=EVALUATION，关联 business_type=PICKUP_REQUEST + business_id=代取ID。
-- ---------------------------------------------------------------------
INSERT INTO notifications
(id, receiver_id, type, title, content, business_type, business_id, is_read, read_at, created_at, updated_at)
VALUES
    -- alice(2) 收到 bob 对 id=3 的评价（未读，配合 id=3 的演示）
    (1, 2, 'EVALUATION', '收到一条新评价', '您在一次代取服务中收到了新的评价。',
     'PICKUP_REQUEST', 3, 0, NULL, DATE_ADD(NOW(), INTERVAL -30 MINUTE), DATE_ADD(NOW(), INTERVAL -30 MINUTE)),
    -- alice(2) 收到 bob 对 id=4 的评价（已读，历史单）
    (2, 2, 'EVALUATION', '收到一条新评价', '您在一次代取服务中收到了新的评价。',
     'PICKUP_REQUEST', 4, 1, DATE_ADD(NOW(), INTERVAL -46 HOUR), DATE_ADD(NOW(), INTERVAL -47 HOUR), DATE_ADD(NOW(), INTERVAL -47 HOUR)),
    -- bob(3) 收到 alice 对 id=4 的评价（已读，历史单）
    (3, 3, 'EVALUATION', '收到一条新评价', '您在一次代取服务中收到了新的评价。',
     'PICKUP_REQUEST', 4, 1, DATE_ADD(NOW(), INTERVAL -46 HOUR), DATE_ADD(NOW(), INTERVAL -47 HOUR), DATE_ADD(NOW(), INTERVAL -47 HOUR));

-- ---------------------------------------------------------------------
-- 8. point_transactions 积分流水
--    alice / bob 各一条认证赠送 +100（balance_after=100，与 users.point_balance 一致）。
--    演示「我的积分」流水展示；relatedPickupId 为空（非代取相关）。
-- ---------------------------------------------------------------------
INSERT INTO point_transactions
(id, user_id, type, amount, balance_after, related_pickup_id, created_at, updated_at)
VALUES
    (1, 2, 'EARN_VERIFICATION', 100, 100, NULL, DATE_ADD(NOW(), INTERVAL -5 DAY), DATE_ADD(NOW(), INTERVAL -5 DAY)),
    (2, 3, 'EARN_VERIFICATION', 100, 100, NULL, DATE_ADD(NOW(), INTERVAL -5 DAY), DATE_ADD(NOW(), INTERVAL -5 DAY));

-- ---------------------------------------------------------------------
-- 完成。可执行以下查询确认：
--   SELECT id, username, role, auth_status, point_balance FROM users;
--   SELECT id, publisher_id, acceptor_id, status FROM pickup_requests;
--   SELECT id, user_id, status FROM verification_reviews;
--   SELECT id, business_id, reviewer_id, reviewee_id, reviewee_role, rating_level FROM evaluations;
--   SELECT id, receiver_id, type, business_id, is_read FROM notifications;
--   SELECT id, user_id, type, amount, balance_after FROM point_transactions;
-- ---------------------------------------------------------------------
