# P4 单元测试与覆盖率说明

## 1. 范围说明

P4 单元测试聚焦于当前 MVP 已实现的核心后端模块，不对未实现的扩展功能编造测试：

- 认证与 JWT：`AuthServiceImpl`、`JwtUtil`
- 用户资料与实名：`UserServiceImpl`、`VerificationReviewServiceImpl`
- 文件上传与访问：`FileStorageServiceImpl`
- 代取主流程：`PickupServiceImpl`
- 平台积分：`PointServiceImpl`
- 评价与好评率：`EvaluationServiceImpl`
- 站内通知：`NotificationServiceImpl`
- 统一响应与错误结构、工具类：`ApiResponse`、`ErrorResponse`、`StudentIdMasker`
- Controller Web 切片（`@WebMvcTest`，不连库）：用户、代取、文件与实名

对应测试代码位于 `backend/src/test/java/com/campushub/` 下，共 16 个测试类：

| 测试类 | 用例数 | 覆盖重点 |
|--------|-------|---------|
| `service/impl/AuthServiceImplTest` | 5 | 注册默认值、密码哈希、重复用户名、登录失败拒签 JWT |
| `service/impl/UserServiceImplTest` | 13 | 资料更新、实名状态回写、积分变动原语（余额条件更新/签到去重/余额不足） |
| `service/impl/VerificationReviewServiceImplTest` | 6 | 提交去重、审核通过/驳回状态回写、认证赠送积分 |
| `service/impl/FileStorageServiceImplTest` | 3 | 图片类型/大小校验、元数据落库、受信读取 |
| `service/impl/PickupServiceImplTest` | 13 | 状态流转、鉴权、有报酬积分扣减/退回/转入、超时惰性取消 |
| `service/impl/PointServiceImplTest` | 9 | 赠送/签到/扣减/退回/完成入账与流水写入 |
| `service/impl/EvaluationServiceImplTest` | 16 | 评价资格、被评价人推导、重复评价拦截、好评率聚合 |
| `service/impl/NotificationServiceImplTest` | 8 | 通知创建、分页、未读数、标记已读幂等/越权 |
| `security/JwtUtilTest` | 4 | 签发/解析 round-trip、篡改/过期/空密钥 |
| `common/ApiResponseTest` | 2 | 成功响应序列化契约 |
| `common/ErrorResponseTest` | 2 | 错误响应序列化契约 |
| `util/StudentIdMaskerTest` | 3 | 学号脱敏边界 |
| `controller/UserModuleWebTest` | 9 | 用户接口路由、鉴权、参数校验、响应字段 |
| `controller/PickupModuleWebTest` | 11 | 代取接口路由、鉴权、参数校验、响应字段 |
| `controller/UserFileAndVerificationWebTest` | 8 | 资料/实名/文件上传接口 Web 切片 |
| `integration/CoreFlowIntegrationTest` | 4 | 见集成测试说明（1 正常流程 + 3 异常流程） |

> 合计 **116** 个测试方法（含集成测试）。此外 `BackendApplicationTests` 作为最小 JWT 引导的上下文加载冒烟测试一并在 `verify` 阶段执行。

## 2. 本阶段补充内容

本轮为 P4 验收补充了以下测试支撑：

- 增加 `backend/src/test/resources/application.yaml`，提供测试环境 JWT 密钥，修复 `BackendApplicationTests` 因缺少 `jwt.secret` 无法启动的问题。
- 在 `backend/pom.xml` 中接入 `jacoco-maven-plugin`，生成覆盖率报告并在 `verify` 阶段校验核心模块行覆盖率不低于 `60%`。

## 3. 关注的核心断言

单元测试不是「只测方法存在」，而是覆盖真实业务规则：

- 注册时默认昵称、默认角色、密码哈希存储
- 重复用户名注册冲突
- 登录密码错误 / 用户不存在时拒绝签发 JWT
- 实名认证重复提交拦截、审核通过/驳回的状态回写、认证通过赠送 100 积分
- 代取发布时 `UNPAID` / `PAID` 分支：无报酬不动积分、有报酬从发布方扣减积分后进入待接单，余额不足抛 `INSUFFICIENT_POINTS`（409）
- 自己不能接自己的单、超时接单自动取消、未上传完成凭证不能确认完成
- 取消时有报酬积分退回发布方、确认完成时积分转入接单方、非参与者读取取件凭证被拒绝
- 每日签到 +5 与当日重复签到去重
- 评价资格判定、被评价人后端推导、重复评价拦截、好评率按角色聚合
- 通知未读数与标记已读的幂等/越权处理
- 文件上传类型校验、统一错误响应结构

## 4. 覆盖率命令

在 `backend/` 目录执行：

```bash
./mvnw verify
```

覆盖率报告输出目录：

```text
backend/target/site/jacoco/index.html
```

## 5. 本地运行结果

本地于 2026-07-02 执行：

```bash
cd backend
./mvnw verify
```

结果：

- 后端全量测试 **120** 项全部通过（Failures: 0，Errors: 0，Skipped: 0；其中 16 个核心测试类共 116 个用例，另含上下文冒烟测试及少量历史遗留编译产物）
- JaCoCo 行覆盖率 **73.03%**（`covered=696`，`missed=257`，`total=953`），分支覆盖率 61.22%，指令覆盖率 72.62%
- `verify` 阶段的 `>= 60%` 覆盖率校验已通过

## 6. 结论

当前仓库的单元测试已覆盖认证、JWT、实名、文件、代取状态流转、平台积分、评价和通知等核心模块，行覆盖率 73.03%，符合 P4「核心模块覆盖率 >= 60%」的验收目标。覆盖率统计以 JaCoCo 报告为准。
