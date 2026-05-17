# 核心类图设计 — 校园互助服务平台

**版本：** 2.0
**日期：** 2026-05-17
**团队：** true就是队

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
│  │          │           │+rewardAmt│         │ +proof   │               │
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
│  ┌──────────┐     ┌──────────────┐                              │      │
│  │  Message │     │PaymentRecord │                              │      │
│  │──────────│     │──────────────│                              │      │
│  │ +content │     │ +tradeNo     │                              │      │
│  │ +type    │     │ +amount      │                              │      │
│  └──────────┘     │ +status      │                              │      │
│                   │ +paidAt      │                              │      │
│                   └──────────────┘                              │      │
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
| certificationStatus | Enum | 未认证/已认证/审核中 |
| role | Enum | 普通用户/管理员 |
| createdAt | LocalDateTime | 注册时间 |

| 方法 | 说明 |
|------|------|
| register(phone, password) | 手机号注册 |
| login(phone, password) | 密码登录 |
| certify(studentId, realName) | 学号认证 |
| updateProfile(nickname, avatar, bio) | 编辑个人资料 |

#### Task（任务基类 — 抽象类）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| publisherId | Long | 发布者ID |
| type | TaskType(Enum) | 任务类型 |
| title | String | 任务标题 |
| description | String | 任务描述 |
| campus | String | 校区 |
| rewardAmount | BigDecimal | 报酬金额，单位：元（可为0，仅ERRAND类型有效） |
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
| confirmComplete() | 确认完成（触发支付宝转账至接单方） |
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

#### PaymentRecord（支付记录）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| orderId | Long | 关联订单ID（FK → Order） |
| payerId | Long | 付款方ID（发布者） |
| payeeId | Long | 收款方ID（接单者） |
| amount | BigDecimal | 支付金额，单位：元 |
| tradeNo | String | 支付宝交易号 |
| outTradeNo | String | 商户订单号（平台生成） |
| status | Enum | WAITING_PAY/PAID/TRANSFERRED/REFUNDED/FAILED |
| paidAt | LocalDateTime | 支付时间 |
| transferredAt | LocalDateTime | 转账时间 |
| refundedAt | LocalDateTime | 退款时间 |
| createdAt | LocalDateTime | 创建时间 |

| 方法 | 说明 |
|------|------|
| createPayment(orderId, amount) | 创建支付宝预支付 |
| handlePayCallback(notifyParams) | 处理支付宝异步回调 |
| transferToPayee() | 确认完成后转账至接单方 |
| refund(reason) | 取消订单时退款 |

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
| **S - 单一职责** | User类是否混入了支付管理逻辑？ | ⚠️ 是 | AI将支付计算（报酬冻结、转账规则）写死在User类中，支付规则变更需要修改User类 | 将支付逻辑独立为PaymentService，通过支付宝沙箱SDK处理支付/转账/退款，User类只负责身份认证和个人信息管理 |
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
  │通知 ││缓存││支付  ││评价提醒   │
  │推送 ││更新││转账  ││(48小时)   │
  │监听 ││监听││监听  ││监听      │
  └────┘└────┘└──────┘└──────────┘
```

**为什么用？**
- 事件发布者和监听者解耦，OrderService不需要知道"通知怎么发"或"缓存怎么刷新"
- 符合P2架构决策——进程内事件足够，无需引入MQ
- 未来新增监听器（如数据埋点、运营统计）无需修改OrderService

**不用会怎样？**
- OrderService中需要显式调用通知推送、缓存刷新、支付转账等多个方法
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
