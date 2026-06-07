# P4 CI/CD 配置与运行记录

## 1. 说明

根据课程最新要求，学校 GitLab Runner 不稳定，因此 P4 对 CI/CD 的验收调整为：

- 仓库内存在可执行的 CI/CD 配置
- 能体现依赖安装、测试、构建等阶段
- 保留最近一次运行或等效本地执行记录
- 不再强制要求学校流水线一定成功跑完

## 2. 配置文件

本项目当前 CI/CD 配置文件：

- 根流水线：`.gitlab-ci.yml`
- 后端子流水线：`.gitlab/backend.yml`
- 前端子流水线：`.gitlab/frontend.yml`

### 2.1 根流水线职责

根流水线根据变更目录触发前后端子流水线：

- `backend/**/*` 变更时触发后端流水线
- `frontend/**/*` 变更时触发前端流水线

### 2.2 后端流水线职责

后端流水线包含：

- `backend-test`
  运行 `mvn test jacoco:report`
- `backend-build`
  运行 `mvn -DskipTests package`

产物：

- `backend/target/surefire-reports/`
- `backend/target/site/jacoco/`
- `backend/target/*.jar`

### 2.3 前端流水线职责

前端流水线包含：

- `frontend-build`
  运行 `npm ci`
  运行 `npm run build`

产物：

- `frontend/dist/`

## 3. 最近一次本地等效运行记录

由于学校 Runner 调度不稳定，本阶段采用本地等效命令作为运行记录补充。

### 后端

执行时间：2026-06-07

```bash
cd backend
./mvnw.cmd verify
```

结果：

- 共执行 `65` 项测试，全部通过
- JaCoCo 行覆盖率 `60.70%`
- 后端可完成 `verify` 与打包

### 前端

执行时间：2026-06-07

```bash
cd frontend
npm ci
npm run build
```

结果：

- 依赖可安装
- 前端可成功构建 `dist/`
- 构建时存在 Vite 大包体积 warning，但不影响本次 P4 验收

## 4. 风险说明

- GitLab Runner 可能出现排队、无可用执行器、镜像拉取失败等环境问题
- 因此若学校平台显示流水线未启动或失败，不能直接认定配置缺失
- 本项目已在仓库层面提供完整可执行配置，并保留本地等效运行记录作为验收依据

## 5. 结论

P4 所需的 CI/CD 配置已补齐，并覆盖依赖安装、自动测试、覆盖率报告和构建产物要求，符合课程对“配置存在即可”的最新说明。
