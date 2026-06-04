---
name: p4-git-workflow
description: Use this skill when working on CampusHub P4 coding-stage Git operations, including checking the current branch before committing, preparing code-stage commits, deciding what files to stage, generating commit messages, or confirming whether a commit should be made on the current branch. Trigger it for requests such as "提交这次代码", "按 P4 规范提交", "检查这次 diff", "帮我 commit", "看看现在能不能提交", or "确认当前分支是否合适". Do not use this skill for documentation-only commits during non-coding documentation phases when the existing doc-commit-convention skill is more appropriate.
---

# CampusHub P4 Git Workflow

This skill defines Git branch and commit rules for the CampusHub P4 coding stage.

It only controls Git workflow and commit behavior. It does not define coding style, architecture rules, API design rules, database design rules, or business implementation rules.

## 1. Branch Roles

Use the following branch model:

- `main`: stable branch for phase submission and demonstration.
- `dev`: integration branch for P4 development.
- `feature/*`: task branch for new features.
- `fix/*`: task branch for bug fixes.
- `docs/*`: task branch for documentation changes.

Do not develop or commit directly on `main`.

`dev` is the base branch for daily development. Feature, fix, and documentation branches should normally be created from the latest `dev`.

Examples:

```text
feature/auth-login
feature/pickup-request
feature/order-flow
feature/file-upload
feature/notification
feature/review-credit

fix/login-token
fix/order-status-transition
fix/file-permission

docs/p4-readme
docs/deployment-guide
docs/test-report