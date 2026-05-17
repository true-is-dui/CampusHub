# 数据库设计 — 校园互助服务平台

**版本：** 2.0
**日期：** 2026-05-17
**团队：** true就是队

---

## 一、ER 图

```
┌──────────┐       ┌──────────┐       ┌──────────┐
│   User   │       │  Order   │       │  Review  │
│──────────│       │──────────│       │──────────│
│ PK id    │1    * │ PK id    │1    2 │ PK id    │
│ phone   │◄──────│ FK taskId│◄──────│ FK orderId│
│ password│       │ FK accept│       │ FK review│
│ studentI│       │ status   │       │ FK target│
│ realName│       │ accepted │       │ rating   │
│ nickname│       │ complete │       │ comment  │
│ avatar  │       │ proof    │       │ appealed │
│ certStat│       │ createdAt│       │ createdAt│
│ role    │       └──────┬───┘       └──────────┘
│ createdAt│              │
└────┬─────┘      ┌──────┴───────┐       ┌──────────┐
     │            │PaymentRecord │       │  Message │
     │ 1       *  │──────────────│       │──────────│
     └────────────│ PK id        │       │ PK id    │
                  │ FK orderId   │       │ FK orderId│
                  │ FK payerId   │       │ FK sender│
                  │ FK payeeId   │       │ FK receiv│
                  │ amount       │       │ content  │
                  │ tradeNo      │       │ type     │
                  │ status       │       │ isRead   │
                  │ paidAt       │       │ createdAt│
                  │ transferredAt│       └──────────┘
                  │ refundedAt   │
                  │ createdAt    │
                  └──────────────┘
     ┌──────────┐
     │  Match   │    ┌──────────┐
     │──────────│    │ SecondHnd│
     │ PK id    │    │──────────│
     │ FK pubId │    │ PK id    │
     │ actType  │    │ FK sellId│
     │ title    │    │ name     │
     │ desc     │    │ desc     │
     │ time     │    │ price    │
     │ location │    │ images   │
     │ maxNum   │    │ tradeLoc │
     │ tags     │    │ status   │
     │ status   │    │ createdAt│
     │ createdAt│    └──────────┘
     └──────────┘
     ┌──────────┐   └──────────┘   │ tradeLoc │
     │LostFound │                  │ status   │
     │──────────│   ┌──────────┐   │ createdAt│
     │ PK id    │   │ Question │   └──────────┘
     │ FK pubId │   │──────────│
     │ type     │   │ PK id    │
     │ itemName │   │ FK askId │
     │ desc     │   │ title    │
     │ location │   │ desc     │
     │ occurred │   │ category │
     │ contact  │   │ status   │
     │ status   │   │ createdAt│
     │ createdAt│   └──────────┘
     └──────────┘

     ┌──────────────┐
     │AdminAuditLog │
     │──────────────│
     │ PK id        │
     │ FK adminId   │
     │ actionType   │
     │ targetType   │
     │ targetId     │
     │ reason       │
     │ createdAt    │
     └──────────────┘

     ┌──────────────┐
     │ BanAppeal    │
     │──────────────│
     │ PK id        │
     │ FK userId    │
     │ reason       │
     │ status       │
     │ FK handlerId │
     │ handleReason │
     │ createdAt    │
     │ handledAt    │
     └──────────────┘

     ┌──────────────┐
     │   Comment    │
     │──────────────│
     │ PK id        │
     │ targetType   │
     │ targetId     │
     │ FK userId    │
     │ content      │
     │ isBest       │
     │ createdAt    │
     └──────────────┘
```

---

## 二、建表 SQL

```sql
-- 1. 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    student_id VARCHAR(20) UNIQUE COMMENT '学号，认证后填写',
    real_name VARCHAR(50) COMMENT '真实姓名',
    nickname VARCHAR(50) NOT NULL DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    bio VARCHAR(200) COMMENT '个性签名',
    certification_status ENUM('UNCERTIFIED','PENDING','CERTIFIED') NOT NULL DEFAULT 'UNCERTIFIED',
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    banned_until DATETIME COMMENT '封禁截止时间，NULL表示未封禁',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_student_id (student_id),
    INDEX idx_certification (certification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 跑腿任务表
CREATE TABLE errand_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    campus VARCHAR(50) NOT NULL COMMENT '校区',
    reward_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '报酬金额（元），可为0，>0时需支付宝支付',
    pickup_location VARCHAR(200) NOT NULL COMMENT '取件地点',
    delivery_location VARCHAR(200) NOT NULL COMMENT '送达地点',
    item_description VARCHAR(300) COMMENT '物品描述',
    status ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL COMMENT '失效时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_campus (campus),
    INDEX idx_created (created_at DESC),
    INDEX idx_publisher (publisher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿任务表';

-- 3. 失物招领表
CREATE TABLE lost_found_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    type ENUM('LOST','FOUND') NOT NULL COMMENT '失物/招领',
    item_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    location VARCHAR(200),
    occurred_at DATETIME COMMENT '丢失/捡到时间',
    contact_info VARCHAR(100) NOT NULL COMMENT '联系方式',
    status ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_type_status (type, status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领表';

-- 4. 搭子招募表
CREATE TABLE match_recruits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    activity_type ENUM('SPORTS','STUDY','GROUP_BUY','CARPOOL','GAMING','OTHER') NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    activity_time DATETIME NOT NULL,
    location VARCHAR(200),
    max_participants INT NOT NULL DEFAULT 10,
    tags VARCHAR(200) COMMENT '兴趣标签，逗号分隔',
    status ENUM('RECRUITING','ENDED') NOT NULL DEFAULT 'RECRUITING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_status (status),
    INDEX idx_time (activity_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子招募表';

-- 5. 搭子报名表
CREATE TABLE match_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruit_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status ENUM('JOINED','CANCELLED') NOT NULL DEFAULT 'JOINED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruit_id) REFERENCES match_recruits(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_recruit_user (recruit_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子报名表';

-- 6. 二手商品表
CREATE TABLE secondhand_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    images VARCHAR(1000) COMMENT '图片URL，JSON数组',
    trade_location VARCHAR(200) COMMENT '交易地点',
    status ENUM('ON_SALE','SOLD') NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_price (price),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手商品表';

-- 7. 咨询问题表
CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asker_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category ENUM('COURSE','INTERNSHIP','STUDY','LIFE','OTHER') NOT NULL,
    status ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (asker_id) REFERENCES users(id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询问题表（回答通过comments表实现）';

-- 8. 订单表
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联跑腿任务',
    acceptor_id BIGINT NOT NULL COMMENT '接单者',
    status ENUM('IN_PROGRESS','WAITING_CONFIRM','COMPLETED','CANCELLED') NOT NULL DEFAULT 'IN_PROGRESS',
    accepted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    completion_proof VARCHAR(500) COMMENT '完成凭证图片URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES errand_tasks(id),
    FOREIGN KEY (acceptor_id) REFERENCES users(id),
    UNIQUE KEY uk_task (task_id),
    INDEX idx_acceptor (acceptor_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表（仅跑腿业务有订单流）';

-- 9. 评价表
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL COMMENT '评价者',
    target_id BIGINT NOT NULL COMMENT '被评价者',
    rating ENUM('GOOD','NEUTRAL','BAD') NOT NULL,
    comment VARCHAR(500) COMMENT '评价内容，差评必填',
    is_appealed TINYINT(1) NOT NULL DEFAULT 0,
    appeal_reason VARCHAR(500) COMMENT '申诉理由',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    FOREIGN KEY (target_id) REFERENCES users(id),
    UNIQUE KEY uk_order_reviewer (order_id, reviewer_id),
    INDEX idx_target (target_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 10. 消息表
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    content_type ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT',
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    INDEX idx_order_time (order_id, created_at),
    INDEX idx_receiver_read (receiver_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='即时通讯消息表';

-- 11. 支付记录表（支付宝沙箱）
CREATE TABLE payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    payer_id BIGINT NOT NULL COMMENT '付款方（发布者）',
    payee_id BIGINT NOT NULL COMMENT '收款方（接单者）',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
    out_trade_no VARCHAR(64) NOT NULL UNIQUE COMMENT '商户订单号（平台生成）',
    trade_no VARCHAR(64) COMMENT '支付宝交易号',
    status ENUM('WAITING_PAY','PAID','TRANSFERRED','REFUNDED','FAILED') NOT NULL DEFAULT 'WAITING_PAY',
    paid_at DATETIME COMMENT '支付时间',
    transferred_at DATETIME COMMENT '转账至接单方时间',
    refund_no VARCHAR(64) COMMENT '退款交易号',
    refunded_at DATETIME COMMENT '退款时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (payer_id) REFERENCES users(id),
    FOREIGN KEY (payee_id) REFERENCES users(id),
    INDEX idx_out_trade_no (out_trade_no),
    INDEX idx_trade_no (trade_no),
    INDEX idx_payer (payer_id),
    INDEX idx_payee (payee_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付宝沙箱支付记录表';

-- 12. 管理操作日志表
CREATE TABLE admin_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL COMMENT '操作管理员ID',
    action_type ENUM('BAN_USER','UNBAN_USER','REMOVE_CONTENT','HANDLE_APPEAL') NOT NULL,
    target_type ENUM('USER','TASK','LOST_FOUND','MATCH','SECONDHAND','QUESTION','REVIEW','COMMENT') NOT NULL,
    target_id BIGINT NOT NULL COMMENT '被操作对象ID',
    reason VARCHAR(500) COMMENT '操作原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_admin (admin_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理操作日志表';

-- 14. 封禁申诉表
CREATE TABLE ban_appeals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '申诉用户ID',
    reason VARCHAR(500) NOT NULL COMMENT '申诉理由',
    status ENUM('PENDING','UPHELD','OVERTURNED') NOT NULL DEFAULT 'PENDING' COMMENT '待处理/维持封禁/解封',
    handler_id BIGINT COMMENT '处理管理员ID',
    handle_reason VARCHAR(500) COMMENT '处理理由',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (handler_id) REFERENCES users(id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='封禁申诉表';

-- 15. 评论表（兼问答回答）
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type ENUM('LOST_FOUND','MATCH','SECONDHAND','QA') NOT NULL COMMENT '评论目标类型',
    target_id BIGINT NOT NULL COMMENT '评论目标ID',
    user_id BIGINT NOT NULL COMMENT '评论者ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    is_best TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为最佳回答（仅QA类型有效）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_target (target_type, target_id, created_at),
    INDEX idx_user (user_id),
    INDEX idx_qa_best (target_type, target_id, is_best)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表（兼问答回答）';
```

---

## 三、索引设计说明

| 表 | 索引 | 为什么需要 |
|-----|------|----------|
| users | `idx_phone` | 登录时按手机号查询，最频繁的查询 |
| users | `idx_student_id` | 认证时校验学号唯一性 |
| errand_tasks | `idx_status + idx_campus` | 需求大厅按状态+校区联合筛选，高频查询 |
| errand_tasks | `idx_created DESC` | 按时间倒序排列，默认排序 |
| orders | `uk_task` | 一个任务只对应一个订单，防重复接单 |
| orders | `idx_acceptor` | 查看"我接的单"列表 |
| messages | `idx_order_time` | 按订单查询聊天记录，按时间排序 |
| messages | `idx_receiver_read` | 查询未读消息数量 |
| reviews | `uk_order_reviewer` | 一个订单每人只能评价一次 |
| payment_records | `idx_out_trade_no` | 按商户订单号查询支付状态（支付宝回调时用） |
| payment_records | `idx_trade_no` | 按支付宝交易号查询 |
| payment_records | `idx_payer / idx_payee` | 查看"我发起的支付"/"我收到的转账" |
| admin_audit_log | `idx_admin` | 按管理员查询操作记录 |
| admin_audit_log | `idx_target` | 按目标查询被操作历史 |

---

## 四、隐私数据处理

| 数据 | 存储方式 | 前端展示 |
|------|---------|---------|
| 密码 | BCrypt加密存储 | 不展示 |
| 手机号 | 明文存储（脱敏展示） | `138****0001` |
| 学号 | 明文存储（脱敏展示） | 仅显示后4位 |
| JWT Token | Redis存储（含过期） | 前端localStorage |

---

## 五、AI 辅助审查记录

**审查输入：** 将建表SQL和ER图提交给AI，要求审查是否满足3NF、索引是否合理、有无性能瓶颈。

**AI 发现的问题：**

| 问题 | AI判断 | 人工判断 | 处理 |
|------|--------|---------|------|
| orders表通过task_id间接关联publisher | AI建议冗余publisher_id | 接受 | 冗余publisher_id可避免join查询 |
| messages表消息量可能很大 | AI建议按月分表 | 暂不接受 | MVP阶段消息量可控，预留归档策略 |
| review表缺少is_appealed字段 | AI建议补充申诉标记 | 接受 | 已补充 |
| payment_records需支持幂等 | AI建议out_trade_no加唯一索引防重复支付 | 接受 | out_trade_no已设UNIQUE，支付宝回调需幂等处理 |
| errand_tasks的campus字段 | AI建议独立为campus表+N对1 | 不接受 | 校区数量极少（2-3个），枚举或字符串即可 |
