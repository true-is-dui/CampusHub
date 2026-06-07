# P4 Bug 修复日志

## Bug 1：后端测试上下文无法启动

### 问题现象

执行 `./mvnw.cmd test` 时，`BackendApplicationTests` 报错，测试中断。

### 根因分析

`JwtUtil` 在应用启动时强依赖 `jwt.secret`。  
生产配置通过环境变量或本地私有配置提供该值，但测试环境没有注入，导致 Spring 上下文初始化失败。

### 修复方案

- 新增 `backend/src/test/resources/application.yaml`
- 为测试环境提供固定的 `jwt.secret`

### 验证结果

- 再次执行 `./mvnw.cmd test`
- `BackendApplicationTests` 可以正常通过

---

## Bug 2：确认完成流程缺少完成凭证约束

### 问题现象

检查代取完成流程时发现，若未上传完成凭证，发布方仍可能继续确认完成。

### 根因分析

服务逻辑关注了“当前状态是否为 `IN_PROGRESS`”，但遗漏了“完成凭证必须先上传”这一业务前置条件。

### 修复方案

- 在确认完成逻辑中增加完成凭证存在性判断
- 通过单元测试覆盖该异常分支

### 验证结果

- `PickupServiceImplTest.confirmComplete_rejectsWhenNoProof` 能稳定复现并验证该规则
- 正常完成分支测试仍通过

---

## Bug 3：P4 缺少统一覆盖率统计与阈值校验

### 问题现象

仓库已有多组单元测试，但没有统一覆盖率输出，也无法直接证明“核心模块覆盖率 >= 60%”。

### 根因分析

`backend/pom.xml` 之前未集成 JaCoCo，缺少报告生成与校验步骤。

### 修复方案

- 在 `backend/pom.xml` 中接入 `jacoco-maven-plugin`
- 生成覆盖率报告
- 在 `verify` 阶段校验核心模块行覆盖率不低于 `60%`

### 验证结果

- 执行 `./mvnw.cmd test jacoco:report`
- 生成 `backend/target/site/jacoco/index.html`
- `verify` 阶段可进行阈值检查

---

## Bug 4：跨字段校验错误字段名与契约不一致

### 问题现象

`rewardType=PAID` 但未传 `rewardAmount` 时，接口返回的错误 key 为 `rewardAmountConsistent`，不利于前端按契约展示字段级错误。

### 根因分析

DTO 使用 `@AssertTrue` 做跨字段校验，Spring 默认把方法派生属性名 `rewardAmountConsistent` 暴露到校验错误中。

### 修复方案

- 在 `GlobalExceptionHandler` 中对字段名做对外统一映射
- 将 `rewardAmountConsistent` 规范化为 `rewardAmount`

### 验证结果

- 集成测试 `abnormalFlow_publishWithInvalidRewardPayload_returns40001` 通过
- 返回结构中的错误 key 已对齐为 `rewardAmount`

## 小结

本阶段 Bug 修复集中在三类问题：

- 测试环境配置缺失
- 核心业务规则遗漏
- 验收证据链不完整

修复后，测试、覆盖率和验收材料已能形成闭环。
