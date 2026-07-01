# 详细设计文档 — 校园可信代取服务闭环

## 一、文档定位与版本说明

本文是 P3 阶段详细设计整合版，用于汇总说明当前系统的详细设计范围、三份独立设计文档之间的一致性、核心业务闭环和 AI 辅助设计记录。版本 3.1 按 P4 实际实现收敛。

最新权威设计分别来自：

- `class_design.md`：核心类图、类属性与方法、SOLID 检查、设计模式应用。
- `api_design.yaml`：OpenAPI 3.0 接口规范、统一响应结构、错误码和认证方式。
- `database_design.md`：ER 图、核心表结构、建表 SQL、索引、隐私与安全数据处理。

本文不重复维护完整 OpenAPI YAML 和完整建表 SQL，只对关键设计进行整合说明。若本文摘要与以上三份独立文档存在差异，以 `class_design.md`、`api_design.yaml`、`database_design.md` 为准。

## 二、P3 作业要求覆盖情况

| P3 要求 | 对应文件 | 整合版说明 |
|---|---|---|
| 类图，含类、属性、方法、关系 | `class_design.md` | 本文摘要核心领域类与服务协作 |
| SOLID 检查清单 | `class_design.md` | 本文摘要 AI 初稿问题与人工修正 |
| 至少两种设计模式 | `class_design.md` | 本文摘要设计模式应用场景 |
| API 规范 | `api_design.yaml` | 本文摘要接口分组与核心流程 |
| ER 图 + 建表 SQL | `database_design.md` | 本文摘要核心表与关系 |
| 索引与隐私数据处理 | `database_design.md` | 本文摘要高频查询索引与隐私处理 |
| AI 审查数据库设计 | `database_design.md` | 本文摘要第三范式、索引、性能瓶颈检查 |
| 详细设计整合版 | `detailed_design.md` | 当前文档承担 |

## 三、设计范围与前置文档承接

当前系统范围已经收敛为“校园可信代取服务闭环”，承接 P1/P2/P3 最新设计。系统围绕真实学生身份、代取需求发布、平台积分报酬、接单、履约、确认、评价和通知形成最小可实现闭环。

当前纳入范围：

- 用户注册登录。
- 个人资料。
- 学生实名认证与管理员审核。
- 文件上传与访问。
- 代取需求发布。
- 代取大厅浏览与筛选。
- 平台积分报酬（认证赠送、每日签到、发布扣减、完成转移、取消退回）。
- 接单。
- 上传完成凭证。
- 发布方确认完成。
- 主动取消与超时取消。
- 我的发布 / 我的接单。
- 双方评价。
- 站内通知。

已从当前设计中删除或不纳入的旧范围：

- 失物招领。
- 搭子招募。
- 二手交易。
- 问答评论。
- 私聊 / WebSocket。
- 复杂内容治理。
- 封禁申诉。
- 独立订单表模型。
- 旧的 Task + Order 大而全模型。

## 四、核心类设计摘要

最新版类设计以 `PickupRequest` 为代取业务主聚合对象，不再使用旧版跨业务 `Task` 基类和独立 `Order` 主对象。

| 核心类 | 职责摘要 | 关键关系 |
|---|---|---|
| `User` | 保存登录账号、公开资料、当前有效实名信息、认证状态和角色 | 发布或承接代取；提交实名认证；上传文件；接收通知；参与评价 |
| `VerificationReview` | 类层面表达实名认证审核流程记录 | 关联申请用户和审核管理员；具体持久化字段以 `database_design.md` 为准 |
| `StoredFile` | 保存文件元数据、用途和上传溯源 | 被头像、认证材料、取件凭证、完成凭证等业务对象通过 `fileId` 引用 |
| `PickupRequest` | 代取业务主聚合，承载发布、接单、完成、取消等主状态 | 关联发布方、接单方、取件凭证和完成凭证 |
| `PointTransaction` | 保存平台积分流水（认证赠送、签到、发布扣减、取消退回、完成入账） | 通过 `userId` 关联用户，`relatedPickupId` 关联代取请求用于溯源 |
| `Evaluation` | 支持代取完成后的双方评价和好评率统计 | 通过 `businessType=PICKUP_REQUEST`、`businessId` 定位代取请求 |
| `NotificationRecord` | 保存站内通知、已读状态和业务回溯信息 | 由认证、接单、完成、积分、评价等业务事件触发 |

分层协作摘要：

| 层次 | 主要对象 | 说明 |
|---|---|---|
| Controller | Auth、Users、Admin、PickupRequests、Points、Evaluations、Notifications 控制器 | 接收 HTTP 请求、解析参数、返回统一响应，不直接处理业务状态流转 |
| Service | `AuthService`、`UserService`、`VerificationReviewService`、`FileStorageService`、`PickupService`、`PointService`、`EvaluationService`、`NotificationService` | 负责权限校验、状态校验、跨模块协作和事务边界 |
| Repository | 用户、审核、文件、代取、积分流水、评价、通知仓储 | 面向核心表进行持久化，不承载业务流程判断 |
| Domain / Entity | `User`、`VerificationReview`、`StoredFile`、`PickupRequest`、`PointTransaction`、`Evaluation`、`NotificationRecord` | 表达领域状态和必要行为 |

`PickupRequest` 是代取业务主聚合对象，统一承载 `WAITING_ACCEPT`、`IN_PROGRESS`、`COMPLETED`、`CANCELLED` 等主业务状态。文件、积分、评价、通知分别作为独立模块与代取业务协作，避免在代取对象中堆积所有实现细节。

当前系统不再保留旧版 `LostFoundItem`、`MatchRecruit`、`SecondhandItem`、`Question`、`Message`、`Comment`、`Order` 等类作为核心类。

## 五、SOLID 检查与设计模式摘要

### 5.1 SOLID 检查摘要

| AI 初稿问题 | 人工判断 | 修正结果 |
|---|---|---|
| 沿用旧范围，错误保留失物招领、搭子、二手、问答、私聊等模块 | 与最新版需求不一致，会导致类设计过大、职责不清 | 收缩到校园可信代取服务闭环，仅保留当前范围内核心对象 |
| 容易将代取主流程拆成 `Task + Order` 双主对象 | 双主对象会造成发布状态、接单状态和履约状态分裂 | 使用 `PickupRequest` 统一承载主业务状态，积分作为独立流水被引用 |
| 容易把文件、积分、评价的追踪字段理解成双向强外键 | 追踪字段的目标是溯源、扩展，不应反向驱动业务状态 | `StoredFile.businessType/businessId`、`PointTransaction.relatedPickupId`、`Evaluation.businessType/businessId` 均明确边界 |
| Service 职责边界容易混乱 | 一个大服务同时处理认证、文件、代取、积分、评价、通知会破坏单一职责 | 拆分为认证、用户、实名审核、文件、代取、积分、评价、通知服务 |

SOLID 修正后的结果：

- 单一职责：用户、文件、代取、积分、评价、通知分开建模。
- 开闭原则：文件上传处理器、积分变动模板方法支持后续扩展。
- 里氏替换：不再强行使用跨业务 `Task` 继承层级。
- 接口隔离：Controller / Service 按业务模块拆分。
- 依赖倒转：报酬流转收口到 `PointService` 接口，代取服务只依赖该接口，不直接操作积分余额或流水表。

### 5.2 设计模式摘要

| 设计模式 | 使用场景 | 使用理由 | 不使用的影响 |
|---|---|---|---|
| 模板方法模式 | 头像、实名认证材料、取件凭证、完成凭证等图片上传流程 | 上传流程都需要格式校验、大小校验、路径生成、本地保存、元数据写入；差异主要在文件用途、大小限制和存储路径 | 上传逻辑会散落在用户、审核和代取模块中，容易出现校验规则不一致和元数据遗漏 |
| 模板方法模式 | 积分变动流程（认证赠送、签到、发布扣减、取消退回、完成转移） | 各类变动共享「前置校验 → 改余额 → 写流水并记 balanceAfter」骨架，差异只在校验规则、流水类型和金额正负 | 余额与流水的读写会散落在用户、代取模块中，容易出现余额与流水不一致、重复赠送或重复签到 |

当前类设计没有额外引入状态模式。代取状态数量有限，状态流转由 `PickupService` 方法和 `PickupRequest` 行为共同约束即可。

## 六、API 设计摘要

`api_design.yaml` 使用 OpenAPI 3.0.3 描述接口。接口默认使用 JWT Bearer 认证，注册、登录、公开用户主页、头像、代取大厅、代取详情等接口按业务需要开放匿名访问。成功响应统一使用 `ApiResponse`，错误响应统一使用 `ErrorResponse`，当前错误码包括参数错误、认证失败、权限不足、资源不存在、业务冲突、系统异常和第三方服务异常。

| 接口分组 | 核心接口摘要 | 设计说明 |
|---|---|---|
| Auth | `POST /auth/register`、`POST /auth/login` | 注册不签发 token，登录成功返回 JWT |
| Users | `GET /users/me`、`PUT /users/me/profile`、`POST /users/me/verification`、`GET /users/{userId}/profile`、`GET /users/{userId}/avatar` | 支持个人资料、头像、实名认证提交和公开主页；实名信息最小暴露 |
| Admin | `GET /admin/verification-reviews`、`POST /admin/verification-reviews/{reviewId}/handle`、`GET /admin/verification-reviews/{reviewId}/image` | 管理员查询、审核实名认证，并读取认证材料 |
| File Access | 头像、认证材料、取件凭证、完成凭证分别通过用户、审核、代取等业务接口上传或读取 | 文件能力不作为独立通用资源开放；访问权限由对应业务模块控制 |
| Pickup Requests | `GET /pickup-requests`、`POST /pickup-requests`、`GET /pickup-requests/{pickupId}`、`GET /pickup-requests/{pickupId}/credential`、`POST /pickup-requests/{pickupId}/accept`、`POST /pickup-requests/{pickupId}/completion-proof`、`GET /pickup-requests/{pickupId}/completion-proof`、`POST /pickup-requests/{pickupId}/completion-confirmation`、`POST /pickup-requests/{pickupId}/cancel`、`GET /users/me/pickup-requests` | 覆盖发布、列表、详情、接单、完成凭证、确认完成、取消、我的发布、我的接单 |
| Points | `GET /users/me/point-balance`、`POST /users/me/check-in`、`GET /users/me/point-transactions` | 查询积分余额、每日签到、积分流水（支持按类型筛选）；积分为平台内虚拟资产，不可充值提现 |
| Evaluations | `POST /pickup-requests/{pickupId}/evaluations`、`GET /pickup-requests/{pickupId}/evaluation-eligibility`、`GET /users/{userId}/rating-summary`、`GET /users/{userId}/evaluations` | 代取完成后双方评价，支持评价资格查询、评价列表和好评率摘要 |
| Notifications | `GET /users/me/notifications`、`GET /users/me/notifications/unread-count`、`POST /users/me/notifications/{notificationId}/read` | 站内通知采用 HTTP 查询/轮询方式，不使用 WebSocket |

有偿代取的关键 API 规则：

- 发布有偿代取（`rewardType=PAID`，报酬 ≥1）时从发布方积分余额中扣减报酬；扣减成功后代取直接进入 `WAITING_ACCEPT`，可在大厅展示并被接单；余额不足则发布失败返回 409。
- 无偿代取（`rewardType=UNPAID`）不涉及积分，发布成功直接进入 `WAITING_ACCEPT`。
- 发布方主动取消待接单服务时，代取进入 `CANCELLED`，有偿服务将扣减的积分退回发布方。
- 接单截止超时未被接单时，代取进入 `CANCELLED`（`cancel_reason=ACCEPT_DEADLINE_EXPIRED`），有偿服务退回发布方积分。
- 发布方确认完成后，代取进入 `COMPLETED`，有偿服务将报酬积分转入接单方账户。

旧版任务、订单、私聊、评论和聚合浏览等接口不属于当前 API 设计。

## 七、数据库设计摘要

`database_design.md` 是数据库设计的权威文件，包含 ER 图、字段说明、完整建表 SQL、索引、约束、隐私与安全处理。本节只摘要核心表和设计结论。

| 核心表 | 职责摘要 | 关键说明 |
|---|---|---|
| `users` | 用户登录信息、公开资料、当前有效实名信息、认证状态、角色 | 密码只保存 BCrypt 哈希；学号唯一；公开主页不暴露实名敏感信息 |
| `verification_reviews` | 实名认证审核记录 | 保存当次提交快照、材料文件、审核状态、审核管理员和驳回原因 |
| `stored_files` | 统一文件元数据 | 只保存路径、MIME、大小、上传者、用途和上传溯源；不负责业务权限判断 |
| `pickup_requests` | 代取请求主表 | 替代旧版双表模型，统一维护发布、待接单、接单、完成、取消和超时状态 |
| `point_transactions` | 积分流水 | 记录认证赠送、签到、发布扣减、取消退回、完成入账；用户余额存于 `users.point_balance` |
| `evaluations` | 双方评价 | 通过 `business_type + business_id` 定位代取请求，通过唯一约束避免重复评价 |
| `notifications` | 站内通知 | 支撑通知列表、未读查询、已读标记和业务回溯 |

数据库状态与约束摘要：

- `pickup_requests` 是代取主业务表，不再使用旧版 `errand_tasks + orders` 双表模型。
- `pickup_requests.status` 取值为 `WAITING_ACCEPT`、`IN_PROGRESS`、`COMPLETED`、`CANCELLED`。
- `pickup_requests.cancel_reason` 取值为 `USER_CANCELLED`、`ACCEPT_DEADLINE_EXPIRED`、`SYSTEM_CANCELLED`。
- `users.point_balance` 保存积分余额（`CHECK >= 0`）；`point_transactions.type` 取值为 `EARN_VERIFICATION`、`EARN_CHECK_IN`、`SPEND_PUBLISH`、`REFUND_CANCEL`、`INCOME_COMPLETE`。
- `point_transactions.related_pickup_id` 仅用于代取相关流水的溯源，不作为反向业务外键。
- `stored_files` 只保存文件元数据和上传溯源；头像、认证材料、取件凭证、完成凭证的真实业务关系由对应业务字段引用。
- `evaluations` 支持双方评价，`uk_evaluations_once (business_type, business_id, reviewer_id)` 保证同一业务对象中同一评价者只能评价一次。
- `notifications` 支撑站内通知和未读查询，不建设独立实时消息系统。

数据库规范化、索引和性能结论：

- 当前数据库基本满足第三范式；用户、审核、文件、代取、积分、评价、通知职责拆分清晰。
- 少数快照、追踪、扩展字段有明确业务语义，不属于无意义冗余。
- 索引覆盖登录、学号唯一、认证审核、代取大厅、我的发布、我的接单、积分流水查询、接单截止超时扫描、通知未读、评价统计等高频路径。
- 当前 MVP 数据量下暂不需要分表、缓存统计表或独立消息队列。

不保留的旧版数据库内容包括失物招领、搭子招募、二手交易、问答评论、私聊消息、订单、封禁申诉、复杂审计日志等旧范围表，也不保留支付记录表。

## 八、核心业务流程闭环

### 8.1 注册登录与实名认证

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 用户注册 | `POST /auth/register` | `AuthService`、`User` | 新增 `users`，密码保存哈希 |
| 用户登录 | `POST /auth/login` | `AuthService`、`CurrentUserContext` | 不改变业务表，签发 JWT |
| 提交认证 | `POST /users/me/verification` | `UserService`、`VerificationReviewService`、`StoredFile` | 写入认证材料 `stored_files`，新增 `verification_reviews`，用户状态进入 `REVIEWING` |
| 管理员审核 | `POST /admin/verification-reviews/{reviewId}/handle` | `VerificationReviewService`、`NotificationService` | 审核记录变为 `APPROVED` 或 `REJECTED`，回写 `users.auth_status`，新增通知 |

### 8.2 发布无偿代取

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 上传取件凭证并发布 | `POST /pickup-requests` | `PickupService`、`FileStorageService`、`PickupRequest` | 写入 `stored_files`，新增 `pickup_requests`，`reward_type=UNPAID`，`status=WAITING_ACCEPT` |
| 大厅可见 | `GET /pickup-requests` | `PickupService` | 查询 `status=WAITING_ACCEPT` 的代取请求 |

### 8.3 发布有偿代取（积分扣减）

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 发布有偿代取 | `POST /pickup-requests` | `PickupService`、`PointService`、`PickupRequest` | 校验发布方积分余额充足后扣减 `users.point_balance`，写入 `point_transactions(type=SPEND_PUBLISH)`；新增 `pickup_requests.status=WAITING_ACCEPT`；余额不足则发布失败返回 409 |
| 大厅可见 | `GET /pickup-requests` | `PickupService` | 查询 `status=WAITING_ACCEPT` 的代取请求 |
| 查询积分余额 | `GET /users/me/point-balance` | `PointService` | 读取 `users.point_balance` |
| 查询积分流水 | `GET /users/me/point-transactions` | `PointService` | 分页读取 `point_transactions`，支持按类型筛选 |

### 8.4 接单

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 浏览大厅 | `GET /pickup-requests` | `PickupService` | 查询 `WAITING_ACCEPT` 服务 |
| 查看详情 | `GET /pickup-requests/{pickupId}` | `PickupService` | 不暴露敏感凭证 |
| 接单 | `POST /pickup-requests/{pickupId}/accept` | `PickupService`、`NotificationService` | 写入 `acceptor_id`、`accepted_at`，状态变为 `IN_PROGRESS`，新增通知 |
| 接单方读取凭证 | `GET /pickup-requests/{pickupId}/credential` | `PickupService`、`FileStorageService` | 权限校验后读取 `stored_files` |

### 8.5 上传完成凭证

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 上传完成凭证 | `POST /pickup-requests/{pickupId}/completion-proof` | `PickupService`、`FileStorageService`、`NotificationService` | 写入完成凭证 `stored_files`，更新 `pickup_requests.completion_proof_file_id`，新增通知 |
| 查看完成凭证 | `GET /pickup-requests/{pickupId}/completion-proof` | `PickupService`、`FileStorageService` | 仅服务参与者可读 |

### 8.6 发布方确认完成

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 发布方确认 | `POST /pickup-requests/{pickupId}/completion-confirmation` | `PickupService`、`PointService`、`NotificationService` | 代取状态变为 `COMPLETED`，写入 `completed_at` |
| 有偿服务结款 | 同上 | `PointService` | 将报酬积分从发布方转入接单方，写入 `point_transactions(type=INCOME_COMPLETE)` |

### 8.7 主动取消与超时取消

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 发布方主动取消待接单 | `POST /pickup-requests/{pickupId}/cancel` | `PickupService`、`PointService` | 代取变为 `CANCELLED`，`cancel_reason=USER_CANCELLED`；有偿服务将扣减的积分退回发布方，写入 `point_transactions(type=REFUND_CANCEL)` |
| 接单截止超时取消 | 后端定时扫描或业务访问前校验 | `PickupService`、`PointService` | 代取变为 `CANCELLED`，`cancel_reason=ACCEPT_DEADLINE_EXPIRED`；有偿服务退回发布方积分 |

### 8.8 双方评价

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 查询评价资格 | `GET /pickup-requests/{pickupId}/evaluation-eligibility` | `EvaluationService`、`PickupService` | 读取代取状态和参与者，不写表 |
| 提交评价 | `POST /pickup-requests/{pickupId}/evaluations` | `EvaluationService`、`NotificationService` | 新增 `evaluations`，通过唯一约束避免重复评价，新增通知 |
| 查看评价与好评率 | `GET /users/{userId}/evaluations`、`GET /users/{userId}/rating-summary` | `EvaluationService` | 基于 `evaluations` 动态聚合 |

### 8.9 站内通知

| 流程节点 | 主要 API | 核心类/服务 | 数据库变化 |
|---|---|---|---|
| 业务事件生成通知 | 认证审核、接单、完成凭证、确认完成、积分、评价等业务接口 | `NotificationService` | 新增 `notifications` |
| 查询通知列表 | `GET /users/me/notifications` | `NotificationService` | 分页读取通知 |
| 查询未读数 | `GET /users/me/notifications/unread-count` | `NotificationService` | 按 `receiver_id` 和 `is_read` 查询 |
| 标记已读 | `POST /users/me/notifications/{notificationId}/read` | `NotificationService` | 更新 `is_read` 和 `read_at` |

## 九、AI 辅助设计与人工修正记录

### 9.1 类设计 AI 审查

| 审查对象 | AI 审查发现 | 人工判断 | 最终处理 |
|---|---|---|---|
| 业务范围 | 初稿容易继续保留失物招领、搭子、二手、问答、私聊等旧模块 | 与当前 P3 范围不一致，类图会失焦 | 删除旧范围类，核心类围绕代取闭环重建 |
| 代取主对象 | 初稿容易设计 `Task + Order` 双主模型 | 会导致状态分裂，不利于 P4 编码 | 使用 `PickupRequest` 统一维护代取主状态 |
| 用户类职责 | 初稿可能让 `User` 直接处理报酬结算和好评率 | 违反单一职责 | `User` 仅处理身份、资料、认证状态和积分余额领域方法；积分流转编排由 `PointService`、评价统计由 `EvaluationService` 完成 |
| 文件关系 | 初稿可能把文件溯源字段画成业务强关系 | 文件表不应承担权限判断 | 文件只保存元数据和上传溯源，业务对象通过文件 ID 引用 |
| 服务边界 | 初稿可能设计大而全平台服务 | 接口过胖且难以测试 | 拆分为认证、用户、审核、文件、代取、积分、评价、通知服务 |

### 9.2 API 设计 AI 审查

| 审查对象 | AI 审查发现 | 人工判断 | 最终处理 |
|---|---|---|---|
| 接口命名和资源层级 | 初稿资源层级可能沿用旧任务和订单命名 | 当前主资源应是代取请求 | 统一使用 `pickup-requests` 作为代取主资源 |
| 旧范围接口 | 初稿可能保留旧范围接口 | 与当前闭环无关 | 取消旧范围接口，不进入 OpenAPI |
| 取消原因 | 初稿只给出 `CANCELLED` 状态 | 无法区分主动取消和接单截止超时 | 补充 `PickupCancelReason`：`USER_CANCELLED`、`ACCEPT_DEADLINE_EXPIRED`、`SYSTEM_CANCELLED` |
| 报酬计价 | 初稿设计接入第三方支付处理有偿代取资金 | 第三方分账/结款在沙箱无法跑通，且超出课程 MVP 合规范围 | 有偿代取改用平台内虚拟积分：发布扣减、完成转移、取消退回，`PointTransactionType` 为 `EARN_VERIFICATION/EARN_CHECK_IN/SPEND_PUBLISH/REFUND_CANCEL/INCOME_COMPLETE` |
| 认证审核 | 初稿只描述用户认证状态，缺少管理员审核接口 | P3 需要覆盖实名认证审核流程 | 补充管理端审核列表、处理和认证材料读取接口 |
| 通知方式 | 初稿可能设计 WebSocket 或私聊推送 | 当前 MVP 不做实时通信 | 使用 HTTP 轮询查询通知列表和未读数量 |
| 文件接口 | 初稿可能提供通用文件读取入口 | 取件凭证、认证材料等存在权限边界 | 文件读取嵌入用户、审核和代取业务接口，不开放通用读取接口 |

### 9.3 数据库设计 AI 审查

| 审查对象 | AI 审查发现 | 人工判断 | 最终处理 |
|---|---|---|---|
| 第三范式 | 表职责基本清晰，但快照和追踪字段容易被误判为冗余 | 快照、追踪、扩展字段有明确业务语义 | 保留实名审核快照、文件溯源、积分流水溯源、评价业务定位字段 |
| 索引合理性 | 高频查询路径需要明确索引支撑 | 登录、认证审核、大厅、我的记录、积分流水、超时扫描、通知未读、评价统计都是核心路径 | 在对应表设计唯一约束和普通索引 |
| 潜在性能瓶颈 | 初稿可能提前引入分表、缓存统计表、消息队列 | 当前课程 MVP 数据量不需要过度设计 | 暂不新增分表、缓存统计表或独立消息队列 |
| 积分关系 | 初稿可能让积分流水表反向保存代取外键并驱动业务状态 | 积分模块不应拥有代取业务状态 | `point_transactions.related_pickup_id` 仅作流水溯源，不反向约束代取；余额存于 `users.point_balance` |
| 文件关系 | 初稿可能让 `stored_files.business_type/business_id` 成为业务强外键 | 文件权限必须由业务模块判断 | 保留溯源字段，不作为权限依据或强外键 |
| 旧范围表 | 初稿可能保留旧校园互助大而全表 | 与当前范围不一致 | 删除旧范围表，只保留七类核心表 |
| 日志和归档 | 初稿可能新增日志表、支付流水表、通知归档表 | 当前 MVP 不需要新增复杂辅助表 | 不新增日志表、通知归档表；积分变动以 `point_transactions` 留痕，关键异常通过应用日志记录 |

## 十、一致性检查结论

类设计、API 设计、数据库设计已经统一收敛到“校园可信代取服务闭环”。旧版大而全校园互助模块已从当前详细设计中删除。

一致性结论：

- `PickupRequest`、`PointTransaction`、`StoredFile`、`Evaluation`、`NotificationRecord` 等核心对象在类、API、数据库中保持一致。
- 代取状态统一为 `WAITING_ACCEPT`、`IN_PROGRESS`、`COMPLETED`、`CANCELLED`。
- 积分流水类型统一为 `EARN_VERIFICATION`、`EARN_CHECK_IN`、`SPEND_PUBLISH`、`REFUND_CANCEL`、`INCOME_COMPLETE`。
- 文件引用、积分流水溯源、评价引用和通知回溯关系已基本对齐。
- 文件模块、积分模块、评价模块、通知模块均作为独立模块与代取主流程协作，不再形成旧版大而全模型。
- 当前详细设计足以支撑 P4 编码开发；P4 应以 `class_design.md`、`api_design.yaml`、`database_design.md` 为实现依据，本文作为整合索引和一致性说明。
