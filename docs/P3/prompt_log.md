# Prompt 日志 — Phase 3 详细设计

**团队：** true就是队
**日期：** 2026-05-17

---

## Prompt #1：AI 类图生成

**工具：** DeepSeek
**日期：** 2026-04-30

**Prompt：**
```
请根据以下需求文档和架构设计，为校园互助平台生成完整的类图。

需求摘要：
- 用户管理：注册、登录、学号认证
- 跑腿代取：发布任务（取件/送达地点、报酬金额）、接单、确认完成、凭证上传
- 失物招领：发布失物/招领信息、筛选、标记已解决
- 找搭子：发布招募（活动类型/时间/标签）、报名、标记结束
- 二手交易：发布商品（名称/价格/图片）、标记已出
- 咨询问答：发布问题、回答、采纳最佳答案
- 信用评价：交易互评（好评/中评/差评）、好评率计算、申诉
- 即时通讯：任务内1对1文字/图片聊天
- 后台管理：用户封禁、内容审核、申诉处理
- 支付：接入支付宝沙箱，跑腿报酬通过支付宝转账

架构约束：前后端分离 + 后端单体分层（Spring Boot），接入支付宝沙箱SDK。

请生成类图，包含类的属性、方法、类间关系。
```

**AI 初稿：**

```java
// ===== 用户类 =====
public class User {
    private Long id;
    private String phone;
    private String password;
    private String studentId;
    private String realName;
    private String nickname;
    private String avatar;
    private String bio;
    private CertificationStatus certificationStatus; // UNCERTIFIED, PENDING, CERTIFIED
    private UserRole role; // USER, ADMIN
    private LocalDateTime createdAt;

    // 注册
    public void register(String phone, String password) { ... }
    // 登录
    public User login(String phone, String password) { ... }
    // 学号认证
    public void certify(String studentId, String realName) { ... }
    // 编辑资料
    public void updateProfile(String nickname, String avatar, String bio) { ... }
}

// ===== 任务类（统一模型） =====
public class Task {
    private Long id;
    private Long publisherId;
    private TaskType type; // ERRAND, LOST_FOUND, MATCH, SECONDHAND, QA
    private String title;
    private String description;
    private String campus;
    private BigDecimal rewardAmount; // 报酬金额（元），通过支付宝沙箱支付
    private TaskStatus status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    // 跑腿专用字段
    private String pickupLocation;
    private String deliveryLocation;
    private String itemDescription;

    // 失物招领专用字段
    private String itemName;
    private String location;
    private LocalDateTime occurredAt;
    private String contactInfo;
    private LostFoundType lostFoundType; // LOST, FOUND

    // 搭子招募专用字段
    private ActivityType activityType; // SPORTS, STUDY, GROUP_BUY, CARPOOL, GAMING, OTHER
    private LocalDateTime activityTime;
    private String activityLocation;
    private Integer maxParticipants;
    private String tags;

    // 二手商品专用字段
    private String productName;
    private BigDecimal price;
    private String images;
    private String tradeLocation;

    // 问答专用字段
    private QuestionCategory category; // COURSE, INTERNSHIP, STUDY, LIFE, OTHER
    private Long bestAnswerId;

    // 发布任务
    public void publish() {
        switch (this.type) {
            case ERRAND:
                validateErrand();
                break;
            case LOST_FOUND:
                validateLostFound();
                break;
            case MATCH:
                validateMatch();
                break;
            case SECONDHAND:
                validateSecondhand();
                break;
            case QA:
                validateQA();
                break;
        }
        this.status = TaskStatus.PENDING;
    }

    private void validateErrand() {
        if (pickupLocation == null || deliveryLocation == null)
            throw new BusinessException("取件和送达地点不能为空");
    }
    private void validateLostFound() {
        if (itemName == null || contactInfo == null)
            throw new BusinessException("物品名称和联系方式不能为空");
    }
    private void validateMatch() {
        if (activityTime == null || maxParticipants == null)
            throw new BusinessException("活动时间和人数上限不能为空");
    }
    private void validateSecondhand() {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("价格必须大于0");
    }
    private void validateQA() {
        if (category == null)
            throw new BusinessException("问题分类不能为空");
    }

    // 取消任务
    public void cancel() { this.status = TaskStatus.CANCELLED; }
}

// ===== 订单类 =====
public class Order {
    private Long id;
    private Long taskId;
    private Long acceptorId;
    private OrderStatus status; // IN_PROGRESS, WAITING_CONFIRM, COMPLETED, CANCELLED
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private String completionProof;

    // 接单
    public void accept(Long taskId, Long acceptorId) {
        // 直接获取数据库连接
        Connection conn = new MySQLConnection("jdbc:mysql://localhost:3306/campushub");
        // 检查任务状态...
        // 更新任务状态为IN_PROGRESS...
        // 创建订单记录...
    }

    // 上传凭证
    public void uploadProof(String imageUrl) { this.completionProof = imageUrl; }

    // 确认完成
    public void confirmComplete() {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        // 通过支付宝沙箱转账至接单方
        Connection conn = new MySQLConnection("jdbc:mysql://localhost:3306/campushub");
        // 调用支付宝转账接口...
    }

    // 取消订单
    public void cancel(String reason) { this.status = OrderStatus.CANCELLED; }
}

// ===== 评价类 =====
public class Review {
    private Long id;
    private Long orderId;
    private Long reviewerId;
    private Long targetId;
    private RatingType rating; // GOOD, NEUTRAL, BAD
    private String comment;
    private LocalDateTime createdAt;

    public void submit() { ... }
    public void appeal(String reason) { ... }
}

// ===== 消息类 =====
public class Message {
    private Long id;
    private Long orderId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private ContentType contentType; // TEXT, IMAGE
    private Boolean isRead;
    private LocalDateTime createdAt;
}

// ===== 支付记录（支付宝沙箱） =====
public class PaymentRecord {
    private Long id;
    private Long orderId;
    private Long payerId;      // 付款方（发布者）
    private Long payeeId;      // 收款方（接单者）
    private BigDecimal amount; // 支付金额（元）
    private String tradeNo;    // 支付宝交易号
    private String outTradeNo; // 商户订单号
    private PaymentStatus status; // WAITING_PAY, PAID, TRANSFERRED, REFUNDED, FAILED
    private LocalDateTime paidAt;
    private LocalDateTime transferredAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;

    // 创建支付宝预支付
    public String createPayment(Long orderId, BigDecimal amount) { ... }
    // 处理支付宝异步回调
    public void handlePayCallback(Map<String, String> notifyParams) { ... }
    // 转账至接单方
    public void transferToPayee() { ... }
    // 退款
    public void refund(String reason) { ... }
}

// ===== 回答类 =====
public class Answer {
    private Long id;
    private Long questionId;
    private Long answererId;
    private String content;
    private Boolean isAdopted;
    private LocalDateTime createdAt;
}

// ===== 管理操作 =====
public class AdminAction {
    private Long id;
    private Long adminId;
    private ActionType actionType; // BAN_USER, UNBAN_USER, REMOVE_CONTENT, HANDLE_APPEAL
    private String targetType;
    private Long targetId;
    private String reason;
    private LocalDateTime createdAt;

    public void banUser(Long userId, String reason) { ... }
    public void unbanUser(Long userId) { ... }
    public void removeContent(String targetType, Long targetId, String reason) { ... }
    public void handleAppeal(Long reviewId, String result, String reason) { ... }
}

// ===== 浏览结果 =====
public class BrowseResult {
    private List<Object> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public BrowseResult aggregateByType(TaskType type, String campus, String sort, int page, int pageSize) { ... }
    public BrowseResult aggregateByCampus(String campus, int page, int pageSize) { ... }
}

// ===== 服务接口（统一接口） =====
public interface ITaskService {
    // 跑腿
    Task publishErrand(Long publisherId, String title, String pickup, String delivery, int credits);
    Order acceptErrand(Long taskId, Long acceptorId);
    void confirmErrandComplete(Long orderId);

    // 失物招领
    Task publishLostFound(Long publisherId, String itemName, String location, LostFoundType type);
    void markLostFoundResolved(Long taskId);

    // 搭子
    Task publishMatchRecruit(Long publisherId, ActivityType type, String title, LocalDateTime time, int max);
    void joinMatch(Long taskId, Long userId);
    void endMatch(Long taskId);

    // 二手
    Task publishSecondhand(Long publisherId, String name, BigDecimal price, String images);
    void markSecondhandSold(Long taskId);

    // 问答
    Task publishQuestion(Long askerId, String title, String description, QuestionCategory category);
    Answer postAnswer(Long questionId, Long answererId, String content);
    void adoptAnswer(Long questionId, Long answerId);

    // 通用
    BrowseResult browseTasks(TaskType type, String campus, String sort, int page, int pageSize);
    Task getTaskDetail(Long taskId);
}
```

**人工评估：** 质量3/5。框架完整但SOLID违规严重：
- Task类把5种业务类型的所有字段塞在一起，大量字段可能为null（违反SRP）
- Task.publish()用switch-case区分类型，新增任务类型需修改枚举+所有分支（违反OCP）
- AI未设计支付模块，将报酬逻辑内嵌在Task类中（违反SRP）
- Order.accept()直接`new MySQLConnection()`获取数据库连接（违反DIP）
- ITaskService接口包含10+方法，失物招领模块被迫依赖跑腿相关方法（违反ISP）

后续通过SOLID检查清单逐条修正。

---

## Prompt #2：SOLID 审查

**工具：** DeepSeek
**日期：** 2026-05-01

**Prompt：**
```
请审查以下类图设计是否符合SOLID原则，逐条检查：
1. 单一职责：每个类是否只负责一个变化维度
2. 开闭原则：新增任务类型是否需要修改已有代码
3. 里氏替换：子类是否能替换父类
4. 接口隔离：接口是否只包含使用者需要的方法
5. 依赖倒转：是否依赖抽象而非具体实现

[附AI生成的类图]
```

**AI 初稿：**

```
SOLID 审查报告

1. 单一职责原则（SRP）— ⚠️ 存在问题

   Task类：❌ 违反
   - Task类包含了5种业务类型的所有字段（跑腿的pickupLocation、搭子的activityType、二手的price等）
   - publish()方法中用switch-case区分5种类型的校验逻辑
   - 建议：将Task拆分为基类+子类，或使用策略模式

   User类：⚠️ 轻微违反
   - AI将支付/报酬逻辑内嵌在User类中
   - 这些业务规则应该放在单独的Service类中
   - 建议：将支付逻辑提取到PaymentService，通过支付宝沙箱SDK处理

   其他类：✅ 符合
   - Order、Review、Message等类职责单一

2. 开闭原则（OCP）— ✅ 基本符合

   Task类的publish()方法虽然用了switch-case，但这是常见的分支逻辑处理方式。
   如果新增任务类型，确实需要修改Task类，但在单体架构下这是可接受的。
   建议：可以考虑用工厂模式优化，但不是必须的。

3. 里氏替换原则（LSP）— ✅ 符合

   当前设计没有使用继承关系，不存在LSP问题。
   Task是统一类，没有子类，所以不存在子类替换父类的问题。

4. 接口隔离原则（ISP）— ❌ 违反

   ITaskService接口：❌ 严重违反
   - 接口包含10+方法，涵盖了5种业务类型的发布、接单、查询等操作
   - 失物招领模块的实现者被迫依赖跑腿相关的接口方法
   - 建议：按业务模块拆分为IErrandService、ILostFoundService、IMatchService等

5. 依赖倒转原则（DIP）— ✅ 基本符合

   在Spring Boot框架下，Service层通过@Autowired注入Repository，符合DIP。
   Order类中的数据库连接操作，在实际项目中会通过Repository处理，不存在DIP问题。

总结：
- 发现问题：2处（SRP + ISP）
- 严重程度：中等
- 建议优先修复ISP问题（拆分接口）
```

**人工评估：** 质量3.5/5。AI能发现明显违规（Task类职责过多、ITaskService接口过胖），但遗漏了关键问题：

- **DIP违规未发现：** Order.accept()中直接`new MySQLConnection()`获取数据库连接，AI认为"在Spring框架下会通过Repository处理"，但实际上AI生成的代码确实写了`new MySQLConnection()`。AI不理解"直接new具体实现"是不好的，因为AI的训练数据中示例代码常这样做。
- **OCP违规轻描淡写：** AI认为switch-case是"常见的分支逻辑处理方式"，在单体架构下"可接受"。但实际上新增任务类型需要修改Task类+枚举+所有switch分支，改3个以上的地方，这在工程上是不可接受的。

人工补充了DIP和OCP的违规分析，修正方案见最终设计文档。

---

## Prompt #3：API 文档生成

**工具：** DeepSeek
**日期：** 2026-05-02

**Prompt：**
```
校园互助平台，请生成RESTful API文档，至少覆盖：
- 用户注册 / 登录
- 发布需求（跑腿代取）
- 浏览需求列表（含筛选和分页）
- 接单
- 查看订单详情
- 提交评价

每个接口包含：URL路径、HTTP方法、请求参数（类型/必填/说明）、成功响应格式、失败响应格式、HTTP状态码。

基础URL: /api/v1，认证方式: Bearer Token (JWT)
```

**AI 初稿：**

```markdown
# 校园互助平台 API 文档

Base URL: /api/v1
认证: Bearer Token (JWT)

---

## 1. 用户注册

POST /api/v1/auth/register

请求参数:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号 |
| password | string | 是 | 密码 |
| sms_code | string | 是 | 短信验证码 |

成功响应 (200):
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "user_id": 1001,
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}

---

## 2. 用户登录

POST /api/v1/auth/login

请求参数:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | string | 是 | 手机号 |
| password | string | 是 | 密码 |

成功响应 (200):
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user_id": 1001,
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "nickname": "张三",
    "avatar": "https://example.com/avatar.jpg"
  }
}

---

## 3. 发布需求

POST /api/v1/tasks
Authorization: Bearer <token>

请求参数:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | string | 是 | 任务类型 |
| title | string | 是 | 标题 |
| description | string | 否 | 描述 |
| campus | string | 是 | 校区 |
| credits | int | 是 | 报酬积分 |
| pickup_location | string | 是 | 取件地点 |
| delivery_location | string | 是 | 送达地点 |

成功响应 (200):
{
  "code": 200,
  "message": "发布成功",
  "data": {
    "task_id": 2001,
    "status": "PENDING"
  }
}

---

## 4. 浏览需求列表

GET /api/v1/tasks

请求参数:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | string | 否 | 任务类型筛选 |
| campus | string | 否 | 校区筛选 |

成功响应 (200):
{
  "code": 200,
  "data": [
    {
      "task_id": 2001,
      "type": "ERRAND",
      "title": "帮取快递",
      "campus": "南校区",
      "credits": 10,
      "status": "PENDING",
      "created_at": "2026-05-01T10:30:00"
    }
  ]
}

---

## 5. 接单

POST /api/v1/tasks/{taskId}/accept
Authorization: Bearer <token>

成功响应 (200):
{
  "code": 200,
  "message": "接单成功",
  "data": {
    "order_id": 3001,
    "status": "IN_PROGRESS"
  }
}

---

## 6. 查看订单详情

GET /api/v1/orders/{orderId}
Authorization: Bearer <token>

成功响应 (200):
{
  "code": 200,
  "data": {
    "order_id": 3001,
    "status": "IN_PROGRESS",
    "task": {
      "task_id": 2001,
      "title": "帮取快递"
    }
  }
}

---

## 7. 提交评价

POST /api/v1/reviews
Authorization: Bearer <token>

请求参数:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| order_id | long | 是 | 订单ID |
| rating | string | 是 | 评价等级 |
| comment | string | 否 | 评价内容 |

成功响应 (200):
{
  "code": 200,
  "message": "评价提交成功"
}
```

**人工评估：** 质量3/5。格式规范，但存在以下问题：

| 问题 | 具体表现 | 修正 |
|------|---------|------|
| 命名不一致 | `sms_code`(snake_case) vs `createdAt`(camelCase) | 统一为camelCase |
| 缺少错误响应 | 只有成功响应，没有定义错误码和失败响应格式 | 补充6种错误码（400/401/403/404/409/500） |
| 接单缺少校验 | 未校验"不能接自己的任务" | 补充400响应 |
| 列表缺少分页 | 无page/pageSize参数，返回格式是数组而非分页结构 | 补充分页参数和total |
| 参数表不完整 | 发布需求接口只列了跑腿字段，缺少通用字段 | 用extra对象封装类型特有参数 |
| 评价缺少约束 | 未说明差评需填理由、不能重复评价 | 补充400和409响应 |

AI把API文档写成了"理想情况下的接口描述"，没有考虑边界条件和错误场景。

---

## Prompt #4：建表 SQL 生成

**工具：** DeepSeek
**日期：** 2026-05-03

**Prompt：**
```
根据以下ER图和字段说明，为校园互助平台生成MySQL 8.0建表SQL。

要求：
- 所有表使用InnoDB，utf8mb4字符集
- 字段需有COMMENT注释
- 包含主键、外键约束、必要的索引
- password字段使用BCrypt存储
- 状态字段使用ENUM类型

表：users, errand_tasks, lost_found_items, match_recruits, match_participants, secondhand_items, questions, orders, reviews, messages, payment_records
```

**AI 初稿：**

```sql
-- 1. 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(11) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    student_id VARCHAR(20) UNIQUE,
    real_name VARCHAR(50),
    nickname VARCHAR(50) NOT NULL DEFAULT '',
    avatar VARCHAR(255),
    bio VARCHAR(200),
    certification_status ENUM('UNCERTIFIED','PENDING','CERTIFIED') NOT NULL DEFAULT 'UNCERTIFIED',
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 跑腿任务表
CREATE TABLE errand_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    campus VARCHAR(50) NOT NULL,
    reward_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '报酬金额（元），通过支付宝沙箱支付',
    pickup_location VARCHAR(200) NOT NULL,
    delivery_location VARCHAR(200) NOT NULL,
    item_description VARCHAR(300),
    status ENUM('PENDING','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_campus (campus),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 失物招领表
CREATE TABLE lost_found_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    type ENUM('LOST','FOUND') NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    location VARCHAR(200),
    occurred_at DATETIME,
    contact_info VARCHAR(100) NOT NULL,
    status ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_type_status (type, status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    tags VARCHAR(200),
    status ENUM('RECRUITING','ENDED') NOT NULL DEFAULT 'RECRUITING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES users(id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_status (status),
    INDEX idx_time (activity_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 二手商品表
CREATE TABLE secondhand_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    images VARCHAR(1000),
    trade_location VARCHAR(200),
    status ENUM('ON_SALE','SOLD') NOT NULL DEFAULT 'ON_SALE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_price (price),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 咨询问题表
CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asker_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category ENUM('COURSE','INTERNSHIP','STUDY','LIFE','OTHER') NOT NULL,
    status ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
    best_answer_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (asker_id) REFERENCES users(id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 回答表
CREATE TABLE answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    answerer_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_adopted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (answerer_id) REFERENCES users(id),
    INDEX idx_question (question_id),
    INDEX idx_answerer (answerer_id),
    INDEX idx_adopted (is_adopted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 订单表
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    acceptor_id BIGINT NOT NULL,
    status ENUM('IN_PROGRESS','WAITING_CONFIRM','COMPLETED','CANCELLED') NOT NULL DEFAULT 'IN_PROGRESS',
    accepted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    completion_proof VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES errand_tasks(id),
    FOREIGN KEY (acceptor_id) REFERENCES users(id),
    UNIQUE KEY uk_task (task_id),
    INDEX idx_acceptor (acceptor_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 评价表
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    rating ENUM('GOOD','NEUTRAL','BAD') NOT NULL,
    comment VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    FOREIGN KEY (target_id) REFERENCES users(id),
    UNIQUE KEY uk_order_reviewer (order_id, reviewer_id),
    INDEX idx_target (target_id),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 消息表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 支付记录表（支付宝沙箱）
CREATE TABLE payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL COMMENT '付款方（发布者）',
    payee_id BIGINT NOT NULL COMMENT '收款方（接单者）',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
    out_trade_no VARCHAR(64) NOT NULL UNIQUE COMMENT '商户订单号',
    trade_no VARCHAR(64) COMMENT '支付宝交易号',
    status ENUM('WAITING_PAY','PAID','TRANSFERRED','REFUNDED','FAILED') NOT NULL DEFAULT 'WAITING_PAY',
    paid_at DATETIME,
    transferred_at DATETIME,
    refund_no VARCHAR(64),
    refunded_at DATETIME,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**AI回答中的主动建议：**
- AI主动为users表添加了`updated_at`字段（更新时间），理由是"用户资料会频繁修改，需要记录最后更新时间"
- AI主动为orders表添加了`completion_proof`字段（凭证图片URL），理由是"跑腿任务需要上传完成凭证"

**人工评估：** 质量4/5。建表SQL是AI表现最好的任务——语法正确、约束合理、索引设计基本到位。人工补充了：
- 所有表/字段的COMMENT注释（AI未添加）
- users表的`banned_until`字段（封禁功能需要）
- payment_records表（支付宝沙箱支付记录，AI未生成）
- reviews表的`is_appealed`字段（申诉功能需要）
- admin_audit_log表（管理操作日志，AI未生成）
- 索引设计说明（为什么需要这个索引）
- 隐私数据处理说明

---

## Prompt #5：数据库设计审查

**工具：** DeepSeek
**日期：** 2026-05-03

**Prompt：**
```
请审查以下数据库设计：

1. 是否满足第三范式（3NF）？
2. 索引设计是否合理？是否有遗漏或多余的索引？
3. 是否有潜在的性能瓶颈？

[附建表SQL]
```

**AI 初稿：**

```
数据库设计审查报告

一、第三范式（3NF）检查

1. orders表：⚠️ 建议优化
   - orders表通过task_id关联errand_tasks，要获取发布者信息需要JOIN errand_tasks.publisher_id
   - 建议：在orders表中冗余publisher_id字段，避免频繁JOIN查询
   - 这是典型的"反范式化"优化，以空间换时间

2. messages表：⚠️ 注意
   - 消息表数据量会随时间快速增长
   - 建议：按月分表（messages_202605, messages_202606...），或预留归档策略

3. reviews表：❌ 缺少字段
   - 缺少is_appealed字段，无法标记该评价是否已被申诉
   - 建议：添加is_appealed TINYINT(1) NOT NULL DEFAULT 0

4. payment_records表：⚠️ 建议补充
   - out_trade_no需加唯一索引防重复支付
   - 建议：添加幂等处理逻辑，支付宝回调可能重复通知

5. errand_tasks.campus字段：⚠️ 建议优化
   - campus字段存储校区名称（字符串），存在以下问题：
     - 数据冗余：同一个校区名称在每条记录中重复存储
     - 一致性风险：可能出现"南校区"和"南院"等不一致写法
   - 建议：独立为campus表（id, name），errand_tasks通过campus_id外键关联

二、索引设计检查

1. errand_tasks表：✅ 合理
   - idx_status + idx_campus：需求大厅筛选，高频查询
   - idx_created DESC：默认排序

2. orders表：✅ 合理
   - uk_task：防重复接单
   - idx_acceptor：查看"我接的单"

3. messages表：✅ 合理
   - idx_order_time：按订单查聊天记录
   - idx_receiver_read：查询未读消息

4. payment_records表：✅ 合理
   - idx_out_trade_no：支付宝回调查询
   - idx_payer/idx_payee：用户支付/收款记录

5. 缺少的索引：⚠️
   - users表缺少idx_certification索引，认证状态筛选可能较慢

三、性能瓶颈分析

1. messages表：❌ 高风险
   - 消息表是写入最频繁的表，每条聊天记录都会写入
   - 随着用户增长，messages表可能成为性能瓶颈
   - 建议：按月分表，或使用Redis缓存最近消息

2. orders表JOIN查询：⚠️ 中风险
   - 查看订单详情需要JOIN errand_tasks和users表
   - 建议：冗余publisher_id减少JOIN

3. browse查询：⚠️ 中风险
   - 聚合浏览需要跨多张表查询（errand_tasks + lost_found_items + match_recruits + ...）
   - 建议：考虑使用视图或物化视图

总结：
- 3NF问题：2处（orders冗余、campus独立表）
- 缺少字段：2处（is_appealed、ref_id）
- 性能风险：3处（messages分表、orders JOIN、browse聚合）
- 建议索引：1处（users.certification_status）
```

**人工评估：** 质量3.5/5。AI的审查有一定价值，但倾向于"教科书式优化"：

| AI建议 | 人工判断 | 处理 |
|--------|---------|------|
| orders表冗余publisher_id | ✅ 接受 | 避免频繁JOIN，MVP阶段数据量不大但查询频率高 |
| messages表按月分表 | ❌ 不接受 | MVP阶段200用户，消息量可控，预留归档策略即可 |
| reviews表补充is_appealed | ✅ 接受 | 申诉功能必须字段 |
| payment_records幂等处理 | ✅ 接受 | 防止支付宝回调重复处理 |
| campus独立成表 | ❌ 不接受 | 校区数量极少（2-3个），枚举或字符串即可，独立成表是过度设计 |
| users补充idx_certification | ✅ 接受 | 认证状态筛选是高频操作 |

AI倾向于"教科书式优化"——建议campus独立成表（对2-3个校区过度设计）、建议按月分表（MVP阶段消息量可控）。人工采纳了3条，拒绝了2条。
