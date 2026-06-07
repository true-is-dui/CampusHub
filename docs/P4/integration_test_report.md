# P4 集成测试说明

## 1. 目标

P4 集成测试要求覆盖核心正常流程和异常流程。考虑学校 GitLab Runner 不稳定、且当前仓库以前后端分离为主，本项目采用后端 `MockMvc + SpringBootTest` 的 HTTP 集成方式，验证：

- 路由是否符合 API 契约
- JWT 鉴权链路是否生效
- 参数校验和统一错误响应是否正确
- 跨控制器的核心流程是否能串联

## 2. 测试代码

新增测试文件：

- `backend/src/test/java/com/campushub/integration/CoreFlowIntegrationTest.java`

该测试加载完整 Spring Boot Web 上下文，使用真实 `JwtUtil` 生成和解析 Token，通过替换 Service Bean 避免数据库依赖，重点验证 HTTP 层契约和主链路编排。

## 3. 覆盖的正常流程

`normalFlow_registerLoginPublishAcceptUploadProofAndComplete`

覆盖链路：

1. 用户注册
2. 用户登录
3. 获取当前用户资料
4. 发布代取需求
5. 另一用户登录并接单
6. 接单方上传完成凭证
7. 发布方确认完成

说明：

- 当前仓库尚未提供评价与通知控制器，因此本轮集成测试严格对齐已实现接口，不伪造未落地 API。
- 文档中已明确这一点，避免出现“为了凑验收而编造接口”的不一致问题。

## 4. 覆盖的异常流程

本轮至少覆盖 3 类异常：

- `abnormalFlow_publishWithoutLogin_returns401`
  说明：未登录访问受保护发布接口，返回 `40101`

- `abnormalFlow_publishWithInvalidRewardPayload_returns40001`
  说明：`rewardType=PAID` 但未传 `rewardAmount`，返回 `40001`

- `abnormalFlow_duplicateAccept_returns40901`
  说明：重复接单或接单冲突时，返回 `40901`

## 5. 本地执行命令

```bash
cd backend
./mvnw.cmd test
```

## 6. 本地执行结果

2026-06-07 本地执行后：

- `CoreFlowIntegrationTest` 中 1 条正常流程和 3 条异常流程全部通过
- 后端全量测试合计 `65` 项全部通过
- 鉴权拦截、参数校验、统一错误响应均按预期返回

## 7. 结论

当前集成测试代码已经覆盖 P4 要求中的核心正常流程与异常流程，且严格限定在仓库现有 MVP 实现范围内，没有扩展未确认的业务接口。
