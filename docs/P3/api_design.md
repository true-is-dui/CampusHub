# API 接口规范 — 校园互助服务平台

**版本：** 2.0
**日期：** 2026-05-17
**团队：** true就是队

---

## 一、通用约定

| 项目 | 约定                                                                                    |
|------|---------------------------------------------------------------------------------------|
| 基础URL | `${API_BASE_URL}/v1`（通过环境变量配置，各环境不同）                                                  |
| 请求格式 | JSON (Content-Type: application/json)                                                 |
| 认证方式 | Bearer Token (JWT)，所有接口均需认证（注册/登录除外）                                                  |
| 通用响应格式 | `{ "code": 200, "message": "ok", "data": {...} }`                                     |
| 分页格式 | `{ "code": 200, "data": { "list": [...], "total": 100, "page": 1, "pageSize": 20 } }` |

**环境变量 `API_BASE_URL` 配置：**

| 环境 | 值 |
|------|------|
| 开发 | 待部署后配置（如 `http://localhost:8080/api`） |
| 测试 | 待申请域名后配置 |
| 生产 | 待申请域名后配置 |

---

## 二、错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未认证 / Token过期 |
| 404 | 资源不存在 |
| 409 | 业务冲突（如重复接单、学号已注册） |
| 422 | 支付失败（支付宝沙箱调用失败、余额不足等） |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

---

## 三、核心接口

### 接口 1：用户注册

```
POST /api/v1/auth/register
Content-Type: application/json

Request:
{
  "phone": "13812340001",
  "password": "Abc12345!",
  "smsCode": "123456"
}

Response (200):
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1001,
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}

Response (409):
{
  "code": 409,
  "message": "该手机号已注册"
}
```

### 接口 2：用户登录

```
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
  "phone": "13812340001",
  "password": "Abc12345!"
}

Response (200):
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "userId": 1001,
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "nickname": "张三",
    "avatar": "https://cdn.campushub.cn/avatars/1001.jpg",
    "certificationStatus": "CERTIFIED"
  }
}

Response (401):
{
  "code": 401,
  "message": "手机号或密码错误"
}
```

### 接口 3：学生认证

```
POST /api/v1/users/me/certify
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "studentId": "2024010001",
  "realName": "张三"
}

Response (200):
{
  "code": 200,
  "message": "认证成功",
  "data": {
    "studentId": "2024****0001",
    "certificationStatus": "CERTIFIED"
  }
}

Response (400):
{ "code": 400, "message": "学号格式不正确" }

Response (409):
{ "code": 409, "message": "该学号已被其他用户认证" }
```

### 接口 4：发布需求

```
POST /api/v1/tasks
Authorization: Bearer <token>
Content-Type: application/json
```

**通用字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | ERRAND / LOST_FOUND / MATCH / SECONDHAND / QA |
| title | String | 是 | 标题 |
| description | String | 否 | 描述 |
| campus | String | 否 | 校区（问答类型可不填） |
| rewardAmount | BigDecimal | 否 | 报酬金额（元），默认0（仅ERRAND类型有效，其他类型忽略。>0时需支付宝支付） |
| expiresInHours | Integer | 否 | 有效期（小时），默认24 |
| extra | Object | 是 | 类型特有字段，见下表 |

**各类型 extra 字段：**

| type | extra 字段 | 必填 |
|------|-----------|------|
| ERRAND | pickupLocation(String), deliveryLocation(String), itemDescription(String) | pickupLocation, deliveryLocation 必填 |
| LOST_FOUND | lostFoundType(LOST/FOUND), itemName(String), location(String), occurredAt(DateTime), contactInfo(String) | lostFoundType, itemName, contactInfo 必填 |
| MATCH | activityType(SPORTS/STUDY/GROUP_BUY/CARPOOL/GAMING/OTHER), activityTime(DateTime), location(String), maxParticipants(Integer), tags(String) | activityType, activityTime, maxParticipants 必填 |
| SECONDHAND | name(String), price(Decimal，商品价格，非积分), images(String), tradeLocation(String) | name, price 必填 |
| QA | category(COURSE/INTERNSHIP/LIFE/STUDY/OTHER) | category 必填 |

**示例 — 跑腿：**
```json
{
  "type": "ERRAND",
  "title": "帮取韵达快递",
  "description": "3号韵达快递站，小件",
  "campus": "南校区",
  "rewardAmount": 5.00,
  "expiresInHours": 24,
  "extra": {
    "pickupLocation": "韵达快递站",
    "deliveryLocation": "12栋宿舍",
    "itemDescription": "衣服包裹，约1kg"
  }
}
```

**示例 — 失物招领：**
```json
{
  "type": "LOST_FOUND",
  "title": "丢失校园卡",
  "description": "今天下午在图书馆丢的",
  "campus": "南校区",
  "extra": {
    "lostFoundType": "LOST",
    "itemName": "校园卡",
    "location": "图书馆二楼",
    "occurredAt": "2026-05-01T14:00:00",
    "contactInfo": "微信: zhangsan123"
  }
}
```

**示例 — 搭子招募：**
```json
{
  "type": "MATCH",
  "title": "周末羽毛球约起来",
  "description": "找2-3个人一起打羽毛球",
  "campus": "北校区",
  "extra": {
    "activityType": "SPORTS",
    "activityTime": "2026-05-03T14:00:00",
    "location": "体育馆3号场",
    "maxParticipants": 4,
    "tags": "羽毛球,运动,周末"
  }
}
```

**示例 — 二手商品：**
```json
{
  "type": "SECONDHAND",
  "title": "出二手数据结构教材",
  "description": "九成新，没怎么写",
  "campus": "南校区",
  "extra": {
    "name": "数据结构（C语言版）",
    "price": 15.00,
    "tradeLocation": "南校区食堂门口"
  }
}
```

**示例 — 咨询问答：**
```json
{
  "type": "QA",
  "title": "大二选课有什么建议？",
  "description": "软件工程方向，想选一些实用的课",
  "extra": {
    "category": "COURSE"
  }
}
```

**响应：**
```
Response (200):
{
  "code": 200,
  "message": "发布成功",
  "data": {
    "taskId": 2001,
    "status": "PENDING",
    "createdAt": "2026-05-01T10:30:00"
  }
}

Response (200, 含支付):
{
  "code": 200,
  "message": "发布成功，需完成支付",
  "data": {
    "taskId": 2001,
    "status": "PENDING_PAYMENT",
    "paymentUrl": "https://openapi.alipay.com/gateway.do?...",
    "outTradeNo": "CH20260501103000001",
    "createdAt": "2026-05-01T10:30:00"
  }
}

说明：rewardAmount > 0 时，发布后需跳转支付宝沙箱完成付款，
付款成功后任务状态变为 PENDING（待接单）。rewardAmount = 0 时直接发布成功。
```

### 接口 5：浏览需求列表

```
GET /api/v1/tasks?type=ERRAND&campus=南校区&sort=latest&page=1&pageSize=20
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "list": [
      {
        "taskId": 2001,
        "type": "ERRAND",
        "title": "帮取韵达快递",
        "description": "3号韵达快递站，小件",
        "campus": "南校区",
        "rewardAmount": 5.00,
        "status": "PENDING",
        "publisher": {
          "userId": 1001,
          "nickname": "张三",
          "avatar": "https://cdn.../1001.jpg",
          "goodRate": 0.96
        },
        "createdAt": "2026-05-01T10:30:00",
        "expiresAt": "2026-05-02T10:30:00"
      }
    ],
    "total": 42,
    "page": 1,
    "pageSize": 20
  }
}

查询参数:
  type      可选  ERRAND / LOST_FOUND / MATCH / SECONDHAND / QA
  campus    可选  校区名称
  keyword   可选  搜索关键词
  sort      可选  latest(默认) / reward_desc / expiring_soon
  page      可选  页码(默认1)
  pageSize  可选  每页数量(默认20, 最大50)
```

### 接口 6：接单

```
POST /api/v1/tasks/{taskId}/accept
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "message": "接单成功",
  "data": {
    "orderId": 3001,
    "taskId": 2001,
    "status": "IN_PROGRESS",
    "acceptedAt": "2026-05-01T11:00:00",
    "chatSessionId": "chat_3001"
  }
}

Response (409):
{ "code": 409, "message": "任务已被接走" }

Response (400):
{ "code": 400, "message": "不能接自己的任务" }

Response (400):
{ "code": 400, "message": "请先完成学号认证" }
```

### 接口 7：查看订单详情

```
GET /api/v1/orders/{orderId}
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "orderId": 3001,
    "status": "IN_PROGRESS",
    "task": {
      "taskId": 2001,
      "type": "ERRAND",
      "title": "帮取韵达快递",
      "rewardAmount": 5.00
    },
    "publisher": {
      "userId": 1001,
      "nickname": "张三",
      "goodRate": 0.96
    },
    "acceptor": {
      "userId": 1002,
      "nickname": "李四",
      "goodRate": 0.98
    },
    "timeline": {
      "createdAt": "2026-05-01T10:30:00",
      "acceptedAt": "2026-05-01T11:00:00",
      "completedAt": null
    },
    "completionProof": null
  }
}
```

### 接口 8：提交评价

```
POST /api/v1/reviews
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "orderId": 3001,
  "rating": "GOOD",
  "comment": "取件速度快，态度好"
}

Response (200):
{
  "code": 200,
  "message": "评价提交成功",
  "data": {
    "reviewId": 4001,
    "targetNewGoodRate": 0.97
  }
}

Response (400):
{ "code": 400, "message": "差评需填写理由" }

Response (409):
{ "code": 409, "message": "已对该订单提交过评价" }
```

### 接口 9：用户申诉

```
POST /api/v1/reviews/{reviewId}/appeal
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "reason": "已完成任务，评价与事实不符"
}

Response (200):
{
  "code": 200,
  "message": "申诉已提交，等待管理员处理"
}

Response (400):
{ "code": 400, "message": "申诉理由不能为空" }

Response (409):
{ "code": 409, "message": "该评价已提交过申诉" }

Response (400):
{ "code": 400, "message": "只能申诉自己收到的评价" }
```

### 接口 10：提交评论

```
POST /api/v1/comments
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "targetType": "LOST_FOUND",
  "targetId": 5001,
  "content": "我下午在图书馆一楼看到过这张校园卡"
}

targetType: LOST_FOUND / MATCH / SECONDHAND / QA

Response (200):
{
  "code": 200,
  "message": "评论成功",
  "data": {
    "commentId": 8001,
    "createdAt": "2026-05-01T15:00:00"
  }
}

Response (400):
{ "code": 400, "message": "评论内容不能为空" }
```

### 接口 11：获取评论列表

```
GET /api/v1/comments?targetType=LOST_FOUND&targetId=5001&page=1&pageSize=20
Authorization: Bearer <token>

targetType: LOST_FOUND / MATCH / SECONDHAND / QA

Response (200):
{
  "code": 200,
  "data": {
    "list": [
      {
        "commentId": 8001,
        "content": "我下午在图书馆一楼看到过这张校园卡",
        "user": {
          "userId": 1002,
          "nickname": "李四"
        },
        "isBest": false,
        "createdAt": "2026-05-01T15:00:00"
      }
    ],
    "total": 5,
    "page": 1,
    "pageSize": 20
  }
}
```

说明：当 targetType=QA 时，最佳评论（isBest=true）始终排在第一位，其余按时间倒序。

### 接口 12：删除自己的评论

```
DELETE /api/v1/comments/{commentId}
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "message": "删除成功"
}

Response (400):
{ "code": 400, "message": "只能删除自己的评论" }
```

### 接口 13：选择最佳评论（仅 QA 类型）

```
POST /api/v1/comments/{commentId}/best
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "message": "已选为最佳回答",
  "data": {
    "commentId": 8001,
    "targetId": 6001
  }
}

Response (400):
{ "code": 400, "message": "只能选择自己问题下的评论" }

Response (409):
{ "code": 409, "message": "该问题已选过最佳回答" }
```

### 接口 14：用户封禁申诉

```
POST /api/v1/users/me/ban-appeal
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "reason": "本人未发布违规内容，请求复核"
}

Response (200):
{
  "code": 200,
  "message": "申诉已提交，等待管理员处理"
}

Response (400):
{ "code": 400, "message": "申诉理由不能为空" }

Response (409):
{ "code": 409, "message": "已提交过封禁申诉，请等待处理" }
```

### 接口 15：发送聊天消息

```
POST /api/v1/messages
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "orderId": 3001,
  "receiverId": 1002,
  "content": "快递已经到了，方便来取吗？",
  "contentType": "TEXT"
}

Response (200):
{
  "code": 200,
  "message": "发送成功",
  "data": {
    "messageId": 5001,
    "sentAt": "2026-05-01T14:30:00"
  }
}

Response (400):
{ "code": 400, "message": "只能向订单关联用户发送消息" }
```

### 接口 16：获取聊天记录

```
GET /api/v1/orders/{orderId}/messages?before=5001&pageSize=50
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "list": [
      {
        "messageId": 5001,
        "senderId": 1001,
        "receiverId": 1002,
        "content": "快递已经到了，方便来取吗？",
        "contentType": "TEXT",
        "isRead": true,
        "sentAt": "2026-05-01T14:30:00"
      }
    ],
    "hasMore": true
  }
}

查询参数:
  before    可选  消息ID，用于向前翻页（加载更早的消息）
  pageSize  可选  每页数量(默认50, 最大100)
```

### 接口 17：获取未读消息数

```
GET /api/v1/messages/unread-count
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "totalCount": 5,
    "orders": [
      { "orderId": 3001, "unreadCount": 3 },
      { "orderId": 3005, "unreadCount": 2 }
    ]
  }
}
```

### 接口 18：管理员 — 封禁用户

```
POST /api/v1/admin/users/{userId}/ban
Authorization: Bearer <admin-token>
Content-Type: application/json

Request:
{
  "reason": "发布违规内容",
  "duration": 7
}

Response (200):
{
  "code": 200,
  "message": "用户已封禁",
  "data": {
    "userId": 1003,
    "bannedUntil": "2026-05-08T14:30:00"
  }
}
```

### 接口 19：管理员 — 内容审核/下线

```
POST /api/v1/admin/content/{targetType}/{targetId}/remove
Authorization: Bearer <admin-token>
Content-Type: application/json

Request:
{
  "reason": "包含不当内容"
}

targetType: TASK / LOST_FOUND / MATCH / SECONDHAND / QUESTION / COMMENT

Response (200):
{
  "code": 200,
  "message": "内容已下线"
}
```

### 接口 20：管理员 — 处理申诉

```
POST /api/v1/admin/appeals/{reviewId}
Authorization: Bearer <admin-token>
Content-Type: application/json

Request:
{
  "result": "UPHELD",
  "reason": "经核实，服务完成质量未达要求，维持原评"
}

result: UPHELD(维持原评) / OVERTURNED(撤销差评)

Response (200):
{
  "code": 200,
  "message": "申诉处理完成",
  "data": {
    "reviewId": 4001,
    "result": "UPHELD",
    "targetNewGoodRate": 0.96
  }
}

Response (400):
{ "code": 400, "message": "处理理由不能为空" }
```

### 接口 21：支付宝异步回调（无需认证）

```
POST /api/v1/payments/alipay/notify
Content-Type: application/x-www-form-urlencoded

参数（支付宝标准异步通知参数）:
  out_trade_no   商户订单号
  trade_no       支付宝交易号
  trade_status   交易状态（TRADE_SUCCESS / TRADE_FINISHED）
  total_amount   交易金额
  sign           签名
  sign_type      RSA2

处理逻辑:
1. 验证签名（防伪造）
2. 根据 out_trade_no 找到对应订单
3. 更新 PaymentRecord 状态为 PAID
4. 更新 Task 状态为 PENDING（待接单）
5. 返回 "success" 给支付宝（必须，否则会重复通知）

Response: 返回纯文本 "success"
```

### 接口 22：查询支付状态

```
GET /api/v1/payments/{outTradeNo}
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "outTradeNo": "CH20260501103000001",
    "tradeNo": "2026050122001400001",
    "amount": 5.00,
    "status": "PAID",
    "paidAt": "2026-05-01T10:35:00"
  }
}

status: WAITING_PAY / PAID / TRANSFERRED / REFUNDED / FAILED
```

### 接口 23：确认完成并转账

```
POST /api/v1/orders/{orderId}/confirm
Authorization: Bearer <token>

说明: 发布方确认任务完成。若报酬>0，系统通过支付宝沙箱
      将报酬从平台中间账户转账至接单方支付宝账户。

Response (200):
{
  "code": 200,
  "message": "确认完成，转账已发起",
  "data": {
    "orderId": 3001,
    "status": "COMPLETED",
    "transferStatus": "PROCESSING",
    "amount": 5.00
  }
}

Response (400):
{ "code": 400, "message": "订单状态不允许确认完成" }

Response (400):
{ "code": 400, "message": "只有发布方可以确认完成" }
```

### 接口 24：退款（取消订单时）

```
POST /api/v1/payments/{outTradeNo}/refund
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "reason": "任务取消，申请退款"
}

说明: 接单前取消订单时，将已支付的报酬原路退回至发布方支付宝账户。

Response (200):
{
  "code": 200,
  "message": "退款已发起",
  "data": {
    "outTradeNo": "CH20260501103000001",
    "refundAmount": 5.00,
    "refundStatus": "PROCESSING"
  }
}

Response (400):
{ "code": 400, "message": "该订单不支持退款" }
```

---

## 四、AI 辅助实验记录

**AI 生成 API 的典型问题：**

| 问题 | AI 表现 | 修正 |
|------|---------|------|
| 接口命名不一致 | 混用 camelCase 和 snake_case（`pickup_location` vs `createdAt`） | 统一为 camelCase |
| 缺少错误处理 | 只有成功响应，未定义错误码和错误响应格式 | 补充了6种错误码及对应的响应体 |
| 参数校验不完整 | 接单接口未校验"不能接自己的任务" | 补充了400响应 |
| 认证缺失 | 部分接口未标注是否需要Token | 每个接口明确标注认证要求 |
| 分页缺失 | 列表接口未设计分页参数 | 补充了page/pageSize和total |
| 缺少问答模块接口 | AI只生成了发布问题的通用接口，未设计回答、查看回答列表、采纳最佳答案接口 | 问答功能合并至评论系统（接口10-13），通过targetType=QA区分，isBest标记最佳回答 |
| 缺少学生认证接口 | User模型有studentId和certificationStatus字段，但未生成对应API | 补充了接口3（POST /api/v1/users/me/certify） |
| 缺少用户申诉接口 | 只有管理员处理申诉（原接口14），没有用户提交差评申诉的入口 | 补充了接口9（POST /api/v1/reviews/{reviewId}/appeal） |
| 缺少封禁申诉接口 | 用户被封禁后无法申诉，缺少封禁申诉流程 | 补充了接口14（POST /api/v1/users/me/ban-appeal）+ ban_appeals表 |
| 基础URL硬编码 | AI直接写死`https://api.campushub.cn/v1`，未考虑多环境 | 改为`${API_BASE_URL}/v1`环境变量，各环境独立配置 |
| 错误码403滥用 | 业务规则校验（学号未认证、非订单关联用户等）错误使用403 | 403全部改为400，从错误码表中移除403 |
| 游客浏览逻辑矛盾 | 部分接口标注"游客可浏览"，但通用约定要求所有接口需认证 | 删除游客浏览标注，统一要求认证 |
| 接口职责重复 | AI同时生成了`/tasks`（接口5）和`/browse`（接口15）两个列表接口，参数和返回结构几乎完全相同 | 删除`/browse`，将`expiring_soon`排序合并到接口5，统一由一个接口承担所有列表浏览 |
| 问答模块独立建表 | AI为问答设计了独立的answers表和3个专属接口（查看回答、提交回答、采纳最佳答案） | 问答功能合并至评论系统，answers表删除，通过comments表的targetType=QA和isBest字段实现，减少1张表和2个接口 |
