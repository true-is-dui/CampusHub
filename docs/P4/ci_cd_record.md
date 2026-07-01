# P4 CI/CD 配置与运行记录

## 1. 说明

根据课程最新要求，学校 GitLab Runner 不稳定，因此 P4 对 CI/CD 的验收调整为：

- 仓库内存在可执行的 CI/CD 配置
- 能体现依赖安装、测试、构建等阶段
- 保留最近一次运行或等效本地执行记录
- 不再强制要求学校流水线一定成功跑完

本项目在仓库内同时保留了 **GitLab CI/CD** 和 **GitHub Actions** 两套等价配置：GitLab 配置对齐课程「建议使用 GitLab CI/CD」的要求；GitHub Actions 配置用于实际的镜像构建与服务器部署（团队部署走 GitHub + GHCR）。两套流水线覆盖的阶段一致：依赖安装 → 测试 → 构建 → 镜像打包推送 → 部署。

## 2. 配置文件

### 2.1 GitLab CI/CD

- 根流水线：`.gitlab-ci.yml`
- 后端子流水线：`.gitlab/backend.yml`
- 前端子流水线：`.gitlab/frontend.yml`
- 部署子流水线：`.gitlab/deploy.yml`

**根流水线**按变更目录触发子流水线：`backend/**/*` 变更触发后端、`frontend/**/*` 变更触发前端；`deploy` 阶段仅在 `main` 分支且相关目录变更时触发。

**后端子流水线**（`maven:3.9.9-eclipse-temurin-17` 镜像，缓存 `.m2/repository`）：

- `backend-test` — `mvn verify`；产物 `backend/target/surefire-reports/`、`backend/target/site/jacoco/`
- `backend-build` — `mvn -DskipTests package`；产物 `backend/target/*.jar`
- `backend-docker-build`（仅 `main`）— 用 `backend/Dockerfile-prod` 构建镜像并 `docker push` 到 GHCR

**前端子流水线**（`node:20` 镜像，缓存 `frontend/node_modules/`）：

- `frontend-build` — `npm ci` + `npm run build`；产物 `frontend/dist/`
- `frontend-docker-build`（仅 `main`）— 用 `frontend/Dockerfile-prod` 构建镜像并 `docker push` 到 GHCR

**部署子流水线**（`alpine:3.20` + `openssh-client`）：SSH 登录服务器，`docker compose pull` 拉取最新镜像并 `up -d`，再 `docker image prune -f` 清理。

### 2.2 GitHub Actions

- 工作流：`.github/workflows/CICD.yml`，触发条件为 push 到 `dev` 分支。

作业（jobs）：

- `backend-test` — JDK 17（temurin，maven 缓存）+ `mvn verify`
- `backend-build-and-push`（依赖 `backend-test`）— `mvn -DskipTests package` → 用 `backend/Dockerfile-prod` 构建镜像 → 登录 GHCR → push（镜像名取仓库变量 `BACKEND_IMAGE`）
- `frontend-build-and-push` — `npm ci` + `npm run build` → 用 `frontend/Dockerfile-prod` 构建镜像 → 登录 GHCR → push（镜像名取仓库变量 `FRONTEND_IMAGE`）
- `deploy`（依赖两个 build-and-push）— 通过 `appleboy/ssh-action` SSH 到服务器，`docker compose pull` + `up -d` + `docker image prune -f`

所需密钥与变量（在平台侧配置，不入库）：

- Secrets：`SERVER_HOST`、`SERVER_USER`、`SERVER_SSH_KEY`、`SERVER_PROJECT_DIR`（GitLab 侧另有 `GHCR_TOKEN`/`GHCR_USERNAME`/`GHCR_NAMESPACE`）
- Vars：`BACKEND_IMAGE`、`FRONTEND_IMAGE`

### 2.3 镜像构建产物

- 后端：`backend/Dockerfile-prod`（`eclipse-temurin:17-jre-alpine`，运行 `campushub.jar`，暴露 8080）
- 前端：`frontend/Dockerfile-prod`（`nginx:1.27-alpine`，托管 `dist` 静态资源，暴露 80）

> 说明：课程任务 4.4 建议 CI 包含「静态检查 / 代码格式检查」。当前两套流水线覆盖依赖安装、测试、构建、打包和部署，**尚未加入独立的静态检查/格式检查步骤**，后续可在测试阶段前补充（如后端 Checkstyle/Spotless、前端 ESLint），本阶段未纳入。

## 3. 最近一次本地等效运行记录

由于学校 Runner 调度不稳定，本阶段采用本地等效命令作为运行记录补充。

### 后端

执行时间：2026-07-02

```bash
cd backend
./mvnw verify
```

结果：

- 共执行 **120** 项测试，全部通过（Failures: 0，Errors: 0，Skipped: 0；19 个测试类）
- JaCoCo 行覆盖率 **73.03%**（`covered=696`，`missed=257`，`total=953`），分支覆盖率 61.22%，指令覆盖率 72.62%
- `mvn verify` 通过，并成功打包 `backend/target/backend-0.0.1-SNAPSHOT.jar`
- 覆盖率达到课程「核心模块 ≥ 60%」要求

### 前端

执行时间：2026-07-02

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
- 本项目已在仓库层面提供完整可执行配置（GitLab + GitHub Actions 两套），并保留本地等效运行记录作为验收依据

## 5. 结论

P4 所需的 CI/CD 配置已补齐：覆盖依赖安装、自动测试、覆盖率报告、构建产物导出，以及镜像构建推送和服务器部署，符合课程对「配置存在即可」的最新说明。最近一次本地等效运行 120 项测试全部通过，行覆盖率 73.03%，满足覆盖率阈值要求。
