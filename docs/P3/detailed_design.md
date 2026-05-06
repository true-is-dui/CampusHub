# 详细设计文档 — 校园互助服务平台（整合版）

**版本：** 1.0
**日期：** 2026-05-01
**团队：** true就是队

> **独立交付物文件：**
> - [核心类图设计](class_design.md) — 类图 + SOLID 检查 + 设计模式
> - [API 接口规范](api_design.md) — 20 个接口 + 错误码 + AI 辅助实验
> - [数据库设计](database_design.md) — ER 图 + 建表 SQL + 索引 + 隐私处理

---

## 一、核心类图设计

### 1.1 类图总览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心类关系图                                     │
│                                                                          │
│  ┌──────────┐          ┌──────────┐         ┌──────────┐               │
│  │   User   │ 1     *  │   Task   │ 1     1 │  Order   │               │
│  │──────────│◄─────────│──────────│─────────│──────────│               │
│  │ +id      │ publisher │ +id      │         │ +id      │               │
│  │ +phone   │           │ +type    │         │ +status  │               │
│  │ +studentId│          │ +title   │         │ +acceptor│               │
│  │ +credits │           │ +credits │         │ +proof   │               │
│  └────┬─────┘           └────┬─────┘         └────┬─────┘               │
│       │                      │                    │                      │
│       │ 1                    │△                   │ 1                    │
│       │                      ├─────────────┐      │                      │
│       │              ┌───────┼───────┬─────┼──────┼───────┐            │
│       │              │       │       │     │      │       │            │
│       ▼              ▼       ▼       ▼     ▼      ▼       ▼            │
│  ┌──────────┐  ┌────────┐┌──────┐┌─────┐┌───────┐┌────────┐┌───────┐ │
│  │ LostFound│  │Errand  ││Match ││SecHd││Question││Comment ││Review │ │
│  │──────────│  │Task    ││Recruit││Item ││────────││────────││───────│ │
│  │ +itemName│  │────────││───────││─────││+title  ││+content││+rating│ │
│  │ +location│  │+pickup ││+actType││+name││+cate   ││+isBest ││+cmt   │ │
│  └──────────┘  │+deliver││+time  ││+price│└───────┘└────────┘└───┬───┘ │
│                └────────┘│+tags  ││+imgs │                       │      │
│                          └───────┘└──────┘                       │      │
│                                                                  │      │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                │      │
│  │  Message │     │CheckIn   │     │CreditLog │                │      │
│  │──────────│     │──────────│     │──────────│                │      │
│  │ +content │     │ +date    │     │ +amount  │                │      │
│  │ +type    │     │ +credits │     │ +reason  │                │      │
│  └──────────┘     └──────────┘     └──────────┘                │      │
│                                                                  │      │
│  ┌──────────────┐     ┌──────────────┐                          │      │
│  │ AdminAction  │     │ BrowseResult │                          │      │
│  │──────────────│     │──────────────│                          │      │
│  │ +actionType  │     │ +taskList    │                          │      │
│  │ +targetType  │     │ +total       │                          │      │
│  │ +reason      │     │ +filters     │                          │      │
│  └──────────────┘     └──────────────┘                          │      │
│                                                                  │      │
│  关系说明:                                                        │      │
│  ────  关联    ────▶ 依赖    ◇─── 聚合    ◆─── 组合    △ 继承    │      │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心类详细定义

#### User（用户）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| phone | String | 手机号（脱敏显示） |
| password | String | BCrypt加密存储 |
| studentId | String | 学号（脱敏显示，不公开） |
| realName | String | 真实姓名 |
| nickname | String | 昵称 |
| avatar | String | 头像URL |
| bio | String | 个性签名 |
| credits | Integer | 当前积分余额 |
| certificationStatus | Enum | 未认证/已认证/审核中 |
| role | Enum | 普通用户/管理员 |
| createdAt | LocalDateTime | 注册时间 |

| 方法 | 说明 |
|------|------|
| register(phone, password) | 手机号注册 |
| login(phone, password) | 密码登录 |
| certify(studentId, realName) | 学号认证 |
| updateProfile(nickname, avatar, bio) | 编辑个人资料 |
| addCredits(amount, reason) | 增加积分（记录日志） |
| deductCredits(amount, reason) | 扣减积分 |
| dailyCheckIn() | 每日签到 |

#### Task（任务基类 — 抽象类）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| publisherId | Long | 发布者ID |
| type | TaskType(Enum) | 任务类型 |
| title | String | 任务标题 |
| description | String | 任务描述 |
| campus | String | 校区 |
| credits | Integer | 报酬积分（可为0） |
| status | TaskStatus(Enum) | 待接单/进行中/已完成/已取消 |
| expiresAt | LocalDateTime | 有效期 |
| createdAt | LocalDateTime | 创建时间 |

| 方法 | 说明 |
|------|------|
| publish() | 发布任务（模板方法） |
| validate() | 校验任务信息（抽象方法，子类实现） |
| cancel() | 取消任务 |

#### ErrandTask（跑腿任务 extends Task）

| 属性 | 类型 | 说明 |
|------|------|------|
| pickupLocation | String | 取件地点 |
| deliveryLocation | String | 送达地点 |
| itemDescription | String | 物品描述 |

| 方法 | 说明 |
|------|------|
| validate() | 校验取件/送达地点必填 |

#### LostFoundItem（失物招领）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| publisherId | Long | 发布者ID |
| type | Enum | 失物/招领 |
| itemName | String | 物品名称 |
| description | String | 描述 |
| location | String | 地点 |
| occurredAt | LocalDateTime | 丢失/捡到时间 |
| contactInfo | String | 联系方式 |
| status | Enum | 公开/已解决 |
| createdAt | LocalDateTime | 发布时间 |

#### MatchRecruit（搭子招募）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| publisherId | Long | 发布者ID |
| activityType | Enum | 运动/学习/拼单/拼车/游戏/其他 |
| title | String | 活动标题 |
| description | String | 活动描述 |
| time | LocalDateTime | 活动时间 |
| location | String | 活动地点 |
| maxParticipants | Integer | 最大人数 |
| tags | String | 兴趣标签（逗号分隔） |
| status | Enum | 招募中/已结束 |
| createdAt | LocalDateTime | 发布时间 |

#### SecondhandItem（二手商品）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| sellerId | Long | 卖家ID |
| name | String | 商品名称 |
| description | String | 商品描述 |
| price | BigDecimal | 价格 |
| images | String | 图片URL（JSON数组） |
| tradeLocation | String | 交易地点 |
| status | Enum | 在售/已出 |
| createdAt | LocalDateTime | 发布时间 |

#### Question（问题咨询）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| askerId | Long | 提问者ID |
| title | String | 问题标题 |
| description | String | 详细描述 |
| category | Enum | 选课/实习/学习/生活/其他 |
| status | Enum | 待回答/已解决 |
| createdAt | LocalDateTime | 发布时间 |

#### Comment（评论/回答）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| targetType | Enum | 评论目标类型（LOST_FOUND/MATCH/SECONDHAND/QA） |
| targetId | Long | 评论目标ID |
| userId | Long | 评论者ID |
| content | String | 评论内容 |
| isBest | Boolean | 是否为最佳回答（仅QA类型有效） |
| createdAt | LocalDateTime | 评论时间 |

#### Order（订单）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| taskId | Long | 任务ID（FK → Task） |
| acceptorId | Long | 接单者ID（FK → User） |
| status | Enum | 进行中/待确认/已完成/已取消 |
| acceptedAt | LocalDateTime | 接单时间 |
| completedAt | LocalDateTime | 完成时间 |
| completionProof | String | 完成凭证图片URL |

| 方法 | 说明 |
|------|------|
| accept(taskId, acceptorId) | 接单 |
| uploadProof(imageUrl) | 上传完成凭证 |
| confirmComplete() | 确认完成（转移积分） |
| cancel(reason) | 取消订单 |

#### Review（评价）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| orderId | Long | 订单ID |
| reviewerId | Long | 评价者ID |
| targetId | Long | 被评价者ID |
| rating | Enum | 好评/中评/差评 |
| comment | String | 评价内容 |
| isAppealed | Boolean | 是否已申诉 |
| createdAt | LocalDateTime | 评价时间 |

| 方法 | 说明 |
|------|------|
| submit() | 提交评价 |
| appeal(reason) | 提交申诉 |

#### Message（消息）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| orderId | Long | 关联订单ID |
| senderId | Long | 发送者ID |
| receiverId | Long | 接收者ID |
| content | String | 消息内容 |
| contentType | Enum | 文字/图片 |
| isRead | Boolean | 是否已读 |
| createdAt | LocalDateTime | 发送时间 |

#### CheckIn（签到记录）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户ID |
| checkInDate | LocalDate | 签到日期 |
| creditsEarned | Integer | 获得积分 |
| createdAt | LocalDateTime | 签到时间 |

#### CreditLog（积分日志）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| userId | Long | 用户ID |
| amount | Integer | 变动数量（正=获得，负=消耗） |
| reason | Enum | 认证奖励/签到/完成任务/发布任务冻结/回答被采纳/申诉扣分 |
| refId | Long | 关联业务ID |
| createdAt | LocalDateTime | 变动时间 |

#### AdminAction（管理操作记录）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| adminId | Long | 操作管理员ID |
| actionType | Enum | 封禁用户/解封用户/内容下线/申诉处理 |
| targetType | Enum | USER/CONTENT/APPEAL |
| targetId | Long | 被操作对象ID |
| reason | String | 操作原因 |
| createdAt | LocalDateTime | 操作时间 |

| 方法 | 说明 |
|------|------|
| banUser(userId, reason) | 封禁用户 |
| unbanUser(userId) | 解封用户 |
| removeContent(targetType, targetId, reason) | 内容下线 |
| handleAppeal(reviewId, result, reason) | 处理申诉 |

#### BrowseResult（浏览聚合结果 — 值对象）

| 属性 | 类型 | 说明 |
|------|------|------|
| list | List\<Object\> | 聚合结果列表 |
| total | Long | 总数 |
| page | Integer | 当前页 |
| pageSize | Integer | 每页数量 |

| 方法 | 说明 |
|------|------|
| aggregateByType(type, campus, sort, page, pageSize) | 按类型聚合查询 |
| aggregateByCampus(campus, page, pageSize) | 按校区聚合查询 |

---

## 二、SOLID 检查实验

### 2.1 实验流程

1. 向 AI（DeepSeek）提供 P1 需求文档和 P2 架构设计，让其生成完整类图
2. 团队逐条对照 SOLID 原则审查 AI 生成的设计
3. 记录违规问题和修正方案

### 2.2 SOLID 逐条检查清单

| SOLID 原则 | 检查问题 | AI 设计是否违反 | 违反说明 | 修正方案 |
|-----------|---------|--------------|---------|---------|
| **S - 单一职责** | Task类是否承担了过多职责？ | ⚠️ 是 | AI将所有任务类型的逻辑（跑腿、招募、商品）塞进一个Task类，通过type字段和if-else区分。Task类既要处理跑腿的地点验证，又要处理招募的标签匹配，还要处理商品的图片管理 | 将Task改为抽象基类，ErrandTask、LostFoundItem、MatchRecruit、SecondhandItem各自继承或独立实现，各自负责自己的业务逻辑 |
| **S - 单一职责** | User类是否混入了积分管理逻辑？ | ⚠️ 是 | AI将积分计算（签到判断、奖励规则）写死在User类中，积分规则变更需要修改User类 | 将CheckIn和CreditLog独立为单独类，积分规则通过CreditService统一管理 |
| **O - 开闭原则** | 新增需求类型是否需要修改现有代码？ | ⚠️ 是 | AI使用Task.type枚举+switch-case区分任务类型，每新增一种任务需修改枚举和所有switch分支 | 使用策略模式（TaskStrategy接口），每种任务类型一个策略实现。新增类型只需新增一个策略类，符合开闭原则 |
| **L - 里氏替换** | 子类是否可以替换父类使用？ | ✅ 否 | AI生成的ErrandTask继承Task，没有重写破坏父类契约的方法。validate()在子类中扩展而非覆盖 | — |
| **I - 接口隔离** | 有没有接口太"胖"？ | ⚠️ 是 | AI设计了一个ITaskService接口，包含发布跑腿、发布失物、发布搭子、发布二手、发布问答等10+方法。失物招领模块的实现者被迫依赖跑腿相关方法 | 按模块拆分为IErrandService、ILostFoundService、IMatchService、ISecondhandService、ICommentService等独立接口，每个实现类只关注自己的接口 |
| **D - 依赖倒转** | 高层模块是否直接依赖低层模块？ | ⚠️ 是 | AI的OrderService直接new了MySQLConnection获取数据库连接，TaskService直接依赖了具体的文件存储实现 | OrderService依赖IRepository接口（通过依赖注入），TaskService依赖IStorageService接口，具体实现由Spring容器管理 |

### 2.3 修正统计

| 统计项 | 数量 |
|--------|------|
| AI 原始设计中的类数 | 8 |
| 违反 SOLID 原则的类 | 5 |
| 发现的问题点 | 6 |
| 已修正 | 6 |

### 2.4 重点问题分析

**最严重问题：** AI使用type字段 + switch-case区分所有业务类型，严重违反开闭原则。

```
AI原始设计（违反OCP）:
  class Task {
    TaskType type;  // ERRAND, LOST_FOUND, MATCH, SECONDHAND, QA
    // 所有类型共用一个字段集合，很多字段可能为null
    String pickupLocation;      // 仅跑腿用
    String activityType;       // 仅搭子用
    BigDecimal price;          // 仅二手用
    ...
  }

人工修正（策略模式）:
  interface Postable {          // 可发布的内容
    void publish();
    void validate();
  }
  class ErrandTask implements Postable { ... }
  class LostFoundItem implements Postable { ... }
  class MatchRecruit implements Postable { ... }
  class SecondhandItem implements Postable { ... }
  class Question implements Postable { ... }
```

---

## 三、设计模式应用

### 3.1 策略模式 — 任务类型差异化处理

**应用场景：** 平台有5种业务类型（跑腿、失物、搭子、二手、问答），每种类型有独立的校验规则和发布流程。P1阶段定义了30+功能需求分布在五大板块。

**类结构：**
```
  ┌──────────────┐
  │  PostStrategy │  (接口)
  │──────────────│
  │ +validate()  │
  │ +publish()   │
  │ +getType()   │
  └──────┬───────┘
         │
    ┌────┼────────┬──────────┬──────────┐
    ▼    ▼        ▼          ▼          ▼
┌──────┐┌─────┐┌──────┐┌──────┐┌──────┐
│Errand││Lost ││Match ││SecHnd││ QA   │
│Post  ││Found││Post  ││Post  ││Post  │
│Stgy  ││Post ││Stgy  ││Stgy  ││Stgy  │
└──────┘└─────┘└──────┘└──────┘└──────┘
```

**为什么用？**
- 每种任务类型有独立的校验规则（跑腿要验证地点，搭子要验证时间，二手要验证价格格式）
- 未来新增业务类型只需加一个策略类，符合OCP
- PostService通过策略工厂获取对应实现，无需switch-case

**不用会怎样？**
- 所有校验逻辑堆在TaskService中，if-else超过20行
- 新增任务类型需要修改TaskService + 枚举 + 前端表单，改3个以上的地方

### 3.2 观察者模式 — 任务状态变更通知

**应用场景：** 任务状态变更时需要触发多个操作——推送给相关用户、更新需求大厅缓存、更新用户统计数据。P1的用例UC-04（接单）中明确要求"系统向发布方推送通知"。

**实现方式：** Spring Event（进程内事件，无需MQ）

```
  ┌─────────────────┐
  │  OrderService   │  (事件发布者)
  │  ─────────────  │
  │  acceptOrder()  │──发布 OrderAcceptedEvent
  │  completeOrder()│──发布 OrderCompletedEvent
  │  cancelOrder()  │──发布 OrderCancelledEvent
  └────────┬────────┘
           │ Spring EventBus
     ┌─────┼─────┬──────────┐
     ▼     ▼     ▼          ▼
  ┌────┐┌────┐┌──────┐┌──────────┐
  │通知 ││缓存││积分  ││评价提醒   │
  │推送 ││更新││转移  ││(48小时)   │
  │监听 ││监听││监听  ││监听      │
  └────┘└────┘└──────┘└──────────┘
```

**为什么用？**
- 事件发布者和监听者解耦，OrderService不需要知道"通知怎么发"或"缓存怎么刷新"
- 符合P2架构决策——进程内事件足够，无需引入MQ
- 未来新增监听器（如数据埋点、运营统计）无需修改OrderService

**不用会怎样？**
- OrderService中需要显式调用通知推送、缓存刷新、积分转移等多个方法
- 每新增一个副作用就要改一次OrderService
- 方法体膨胀到50+行，测试困难

### 3.3 WebSocket + STOMP — 即时通讯

**应用场景：** 任务双方需要在订单内进行1对1文字/图片聊天。P2架构决策（ADR-004）明确选择Spring WebSocket + STOMP协议自建IM，不引入第三方SDK。

**实现方式：** Spring WebSocket + STOMP协议，消息持久化到MySQL

```
  ┌──────────────┐          ┌──────────────┐
  │  H5 客户端   │◄─WebSocket─▶│  Spring Boot │
  │  (Vue 3)    │  STOMP     │  WebSocket   │
  └──────┬───────┘          │  服务端      │
         │                  └──────┬───────┘
         │ subscribe                │
         │ /user/queue/messages     │
         ▼                          ▼
  ┌──────────────┐          ┌──────────────┐
  │  消息接收     │          │  Message     │
  │  实时渲染     │          │  Repository  │
  └──────────────┘          │  (MySQL持久化)│
                            └──────────────┘

  消息流程:
  1. 客户端连接 → STOMP握手 → 订阅 /user/queue/messages
  2. 发送消息 → POST /app/chat.send (STOMP帧)
  3. 服务端路由 → 查找接收者WebSocketSession → 推送
  4. 离线消息 → 存入DB，用户上线时拉取未读
```

**关键设计：**

| 设计点 | 方案 |
|--------|------|
| 连接认证 | WebSocket握手时校验JWT Token，拒绝未认证连接 |
| 消息路由 | STOMP的`/user/queue/messages`端点，Spring自动按用户ID路由 |
| 离线消息 | 消息始终写入DB；用户上线时查询is_read=0的记录 |
| 断线重连 | 客户端实现心跳检测（30s间隔），断线后自动重连 |
| 图片消息 | 图片先上传至OSS/本地存储，消息体中传递图片URL |
| 消息关联 | 每条消息绑定orderId，聊天窗口按订单维度隔离 |

**为什么用？**
- P2 ADR-004决策：MVP仅需1对1聊天，Spring WebSocket原生支持足够，零额外成本
- STOMP协议简化消息路由和订阅管理，无需手动管理WebSocket帧
- 聊天数据存储在自有数据库，纠纷仲裁时可直接查询

**不用会怎样？**
- 使用轮询：实时性差，频繁请求浪费服务器资源
- 使用第三方IM SDK：免费额度MAU≤100，200目标用户即超出；数据存储在第三方，纠纷仲裁不便

---

## 四、API 接口规范

### 4.1 通用约定

| 项目 | 约定 |
|------|------|
| 基础URL | `${API_BASE_URL}/v1`（通过环境变量配置，各环境不同） |
| 请求格式 | JSON (Content-Type: application/json) |
| 认证方式 | Bearer Token (JWT)，所有接口均需认证（注册/登录除外） |
| 通用响应格式 | `{ "code": 200, "message": "ok", "data": {...} }` |
| 分页格式 | `{ "code": 200, "data": { "list": [...], "total": 100, "page": 1, "pageSize": 20 } }` |

**环境变量 `API_BASE_URL` 配置：**

| 环境 | 值 |
|------|------|
| 开发 | 待部署后配置（如 `http://localhost:8080/api`） |
| 测试 | 待申请域名后配置 |
| 生产 | 待申请域名后配置 |

### 4.2 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未认证 / Token过期 |
| 404 | 资源不存在 |
| 409 | 业务冲突（如重复接单、学号已注册） |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

### 4.3 核心接口

#### 接口 1：用户注册

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

#### 接口 2：用户登录

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
    "credits": 150,
    "certificationStatus": "CERTIFIED"
  }
}

Response (401):
{
  "code": 401,
  "message": "手机号或密码错误"
}
```

#### 接口 3：学生认证

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
    "certificationStatus": "CERTIFIED",
    "creditsReward": 10
  }
}

Response (400):
{ "code": 400, "message": "学号格式不正确" }

Response (409):
{ "code": 409, "message": "该学号已被其他用户认证" }
```

#### 接口 4：发布需求

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
| credits | Integer | 否 | 报酬积分，默认0（仅ERRAND类型有效，其他类型忽略） |
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
  "credits": 10,
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

Response (400):
{
  "code": 400,
  "message": "积分不足",
  "data": { "required": 10, "available": 5 }
}
```

#### 接口 5：浏览需求列表

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
        "credits": 10,
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
  sort      可选  latest(默认) / credits_desc / expiring_soon
  page      可选  页码(默认1)
  pageSize  可选  每页数量(默认20, 最大50)
```

#### 接口 6：接单

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

#### 接口 7：查看订单详情

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
      "credits": 10
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

#### 接口 8：提交评价

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

#### 接口 9：用户申诉

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

#### 接口 10：提交评论

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

#### 接口 11：获取评论列表

```
GET /api/v1/comments?targetType=LOST_FOUND&targetId=5001&page=1&pageSize=20
Authorization: Bearer <token>

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

#### 接口 12：删除自己的评论

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

#### 接口 13：选择最佳评论（仅 QA 类型）

```
POST /api/v1/comments/{commentId}/best
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "message": "已选为最佳回答",
  "data": {
    "commentId": 8001,
    "targetId": 6001,
    "answererCreditsReward": 10
  }
}

Response (400):
{ "code": 400, "message": "只能选择自己问题下的评论" }

Response (409):
{ "code": 409, "message": "该问题已选过最佳回答" }
```

#### 接口 14：用户封禁申诉

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

#### 接口 15：发送聊天消息

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

#### 接口 16：获取聊天记录

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

#### 接口 17：获取未读消息数

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

#### 接口 18：管理员 — 封禁用户

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

#### 接口 19：管理员 — 内容审核/下线

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

#### 接口 20：管理员 — 处理申诉

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

### 4.4 AI 辅助实验记录

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
| 缺少用户申诉接口 | 只有管理员处理申诉，没有用户提交差评申诉的入口 | 补充了接口9（POST /api/v1/reviews/{reviewId}/appeal） |
| 缺少封禁申诉接口 | 用户被封禁后无法申诉，缺少封禁申诉流程 | 补充了接口14（POST /api/v1/users/me/ban-appeal）+ ban_appeals表 |
| 基础URL硬编码 | AI直接写死URL，未考虑多环境 | 改为环境变量，各环境独立配置 |
| 错误码403滥用 | 业务规则校验错误使用403 | 403全部改为400，从错误码表中移除 |
| 游客浏览逻辑矛盾 | 部分接口标注"游客可浏览"，但通用约定要求所有接口需认证 | 删除游客浏览标注，统一要求认证 |
| 接口职责重复 | AI同时生成了`/tasks`（接口5）和`/browse`（接口15）两个列表接口，参数和返回结构几乎完全相同 | 删除`/browse`，将`expiring_soon`排序合并到接口5，统一由一个接口承担所有列表浏览 |
| 问答模块独立建表 | AI为问答设计了独立的answers表和3个专属接口（查看回答、提交回答、采纳最佳答案） | 问答功能合并至评论系统，answers表删除，通过comments表的targetType=QA和isBest字段实现，减少1张表和2个接口 |

---

## 五、数据库设计

### 5.1 ER 图

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
│ credits │       │ createdAt│      │ createdAt│
│ certStat│       └──────────┘       └──────────┘
│ role    │
│ createdAt│      ┌──────────┐       ┌──────────┐
└────┬─────┘      │  Message │       │ CheckIn  │
     │            │──────────│       │──────────│
     │            │ PK id    │       │ PK id    │
     │ 1       *  │ FK orderId│      │ FK userId│
     └────────────│ FK sender│       │ checkDate│
                  │ FK receiv│       │ credits  │
                  │ content  │       │ createdAt│
                  │ type     │       └──────────┘
                  │ isRead   │
                  │ createdAt│       ┌──────────┐
                  └──────────┘       │CreditLog │
                                     │──────────│
     ┌──────────┐                    │ PK id    │
     │  Match   │                    │ FK userId│
     │──────────│                    │ amount   │
     │ PK id    │                    │ reason   │
     │ FK pubId │                    │ refId    │
     │ actType  │                    │ createdAt│
     │ title    │                    └──────────┘
     │ desc     │
     │ time     │   ┌──────────┐    ┌──────────┐
     │ location │   │  Match   │    │ SecondHnd│
     │ maxNum   │   │Participant│   │──────────│
     │ tags     │   │──────────│    │ PK id    │
     │ status   │   │ PK id    │    │ FK sellId│
     │ createdAt│   │ FK recruit│   │ name     │
     └──────────┘   │ FK userId│    │ desc     │
                    │ status   │    │ price    │
                    │ createdAt│   │ images   │
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

### 5.2 建表 SQL

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
    credits INT NOT NULL DEFAULT 0 COMMENT '积分余额',
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
    credits INT NOT NULL DEFAULT 0 COMMENT '报酬积分，可为0',
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

-- 11. 签到记录表
CREATE TABLE check_ins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL COMMENT '签到日期',
    credits_earned INT NOT NULL DEFAULT 5,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_date (user_id, check_in_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- 12. 积分日志表
CREATE TABLE credit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL COMMENT '变动数量，正为获得，负为消耗',
    reason ENUM('CERTIFY','CHECK_IN','TASK_REWARD','TASK_FREEZE','TASK_UNFREEZE','ANSWER_ADOPTED','APPEAL_PENALTY') NOT NULL,
    ref_id BIGINT COMMENT '关联业务ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_time (user_id, created_at DESC),
    INDEX idx_reason (reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志表';

-- 13. 管理操作日志表
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

### 5.3 索引设计说明

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
| check_ins | `uk_user_date` | 同一天不可重复签到 |
| credit_logs | `idx_user_time` | 查看"我的积分流水" |
| admin_audit_log | `idx_admin` | 按管理员查询操作记录 |
| admin_audit_log | `idx_target` | 按目标查询被操作历史 |

### 5.4 隐私数据处理

| 数据 | 存储方式 | 前端展示 |
|------|---------|---------|
| 密码 | BCrypt加密存储 | 不展示 |
| 手机号 | 明文存储（脱敏展示） | `138****0001` |
| 学号 | 明文存储（脱敏展示） | 仅显示后4位 |
| JWT Token | Redis存储（含过期） | 前端localStorage |

### 5.5 AI 辅助审查记录

**审查输入：** 将建表SQL和ER图提交给AI，要求审查是否满足3NF、索引是否合理、有无性能瓶颈。

**AI 发现的问题：**

| 问题 | AI判断 | 人工判断 | 处理 |
|------|--------|---------|------|
| orders表通过task_id间接关联publisher | AI建议冗余publisher_id | 接受 | 冗余publisher_id可避免join查询 |
| messages表消息量可能很大 | AI建议按月分表 | 暂不接受 | MVP阶段消息量可控，预留归档策略 |
| review表缺少is_appealed字段 | AI建议补充申诉标记 | 接受 | 已补充 |
| credit_logs缺少refId | AI建议补充关联ID | 接受 | 已补充，方便追溯积分变动来源 |
| errand_tasks的campus字段 | AI建议独立为campus表+N对1 | 不接受 | 校区数量极少（2-3个），枚举或字符串即可 |
| answers表独立存在 | AI为问答模块设计了独立的answers表 | 不接受 | 问答回答合并至comments表，通过targetType=QA区分，is_best标记最佳回答，减少表数量 |
