# P4 单元测试与覆盖率说明

## 1. 范围说明

P4 单元测试聚焦于当前 MVP 已实现的核心后端模块，不对未实现的扩展功能编造测试：

- 认证与 JWT：`AuthServiceImpl`、`JwtUtil`
- 用户资料与实名：`UserServiceImpl`、`VerificationReviewServiceImpl`
- 文件上传与访问：`FileStorageServiceImpl`
- 代取主流程：`PickupServiceImpl`
- 统一响应与错误结构：`ApiResponse`、`ErrorResponse`

对应测试代码位于 `backend/src/test/java/com/campushub/` 下，主要文件如下：

- `service/impl/AuthServiceImplTest.java`
- `service/impl/UserServiceImplTest.java`
- `service/impl/VerificationReviewServiceImplTest.java`
- `service/impl/FileStorageServiceImplTest.java`
- `service/impl/PickupServiceImplTest.java`
- `security/JwtUtilTest.java`
- `common/ApiResponseTest.java`
- `common/ErrorResponseTest.java`

## 2. 本阶段补充内容

本轮为 P4 验收补充了以下测试支撑：

- 增加 `backend/src/test/resources/application.yaml`，提供测试环境 JWT 密钥，修复 `BackendApplicationTests` 因缺少 `jwt.secret` 无法启动的问题。
- 在 `backend/pom.xml` 中接入 `jacoco-maven-plugin`，生成覆盖率报告并在 `verify` 阶段校验核心模块行覆盖率不低于 `60%`。

## 3. 关注的核心断言

单元测试不是“只测方法存在”，而是覆盖真实业务规则：

- 注册时默认昵称、默认角色、密码哈希存储
- 重复用户名注册冲突
- 登录密码错误 / 用户不存在时拒绝签发 JWT
- 实名认证重复提交拦截、审核通过/驳回的状态回写
- 代取发布时 `UNPAID` / `PAID` 分支及预支付分支
- 自己不能接自己的单、超时接单自动取消、未上传完成凭证不能确认完成
- 取消时支付关闭分支、非参与者读取取件凭证被拒绝
- 文件上传类型校验、统一错误响应结构

## 4. 覆盖率命令

在 `backend/` 目录执行：

```bash
./mvnw.cmd test jacoco:report
./mvnw.cmd verify
```

覆盖率报告输出目录：

```text
backend/target/site/jacoco/index.html
```

## 5. 本地运行结果

本地于 2026-06-07 执行：

```bash
./mvnw.cmd verify
```

结果：

- 共执行 `65` 个测试，全部通过
- `BackendApplicationTests` 已改为稳定的最小 JWT 引导测试并通过
- JaCoCo 总行覆盖率为 `60.62%`（`covered=431`，`missed=280`）
- `verify` 阶段的 `>= 60%` 覆盖率校验已通过

## 6. 结论

当前仓库的单元测试已覆盖认证、JWT、实名、文件、代取状态流转等核心模块，符合 P4“核心模块覆盖率 >= 60%”的验收目标。覆盖率统计以 JaCoCo 报告为准。
