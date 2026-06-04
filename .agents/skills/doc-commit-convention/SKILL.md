---
name: doc-commit-convention
description: Use this skill when preparing documentation commits, reviewing documentation diffs, selecting files for a documentation-only commit, or generating commit messages for CampusHub phase documents. Trigger it for requests such as "submit the docs", "commit the current documentation changes", "generate a commit message", "commit only P4 docs", or "review the documentation diff before committing". Do not use this skill for normal feature coding or code commits unless the user explicitly asks to apply documentation commit rules.
---

# CampusHub Documentation Commit Convention

This skill defines how Codex should handle Git commits for documentation changes in the CampusHub project.

The project is currently centered on P0-P4 phase documents, requirement documents, architecture documents, detailed design documents, API/database design documents, AI collaboration records, and prompt logs. Therefore, this skill is mainly for documentation-stage commits.

When the project enters a heavier coding stage, code commit rules, test commands, build checks, backend/frontend module boundaries, and implementation-specific conventions should be added separately or handled by another skill.

## 1. Core Principles

Every documentation commit must follow these principles:

1. Inspect the actual changes before generating a commit message.
2. Respect the user's specified commit scope.
3. Do not blindly run `git add .`.
4. Do not commit temporary files, cache files, local environment files, IDE files, build outputs, or unrelated changes.
5. Keep the commit message consistent with the existing style of this repository.
6. Do not mention Codex, ChatGPT, or AI-generated content in the commit message by default.
7. Add `[AI-assisted]` only if the user, course, team, or repository convention explicitly requires it.
8. If there is uncertainty about whether a file should be included, explain the uncertainty and ask the user for confirmation before committing.
9. Do not claim that a commit has been completed unless the commit command actually succeeds.

## 2. Pre-Commit Inspection

Before preparing any commit, run:

```bash
git status
git diff --stat
git diff
```

If the user specifies a file, directory, module, or phase, inspect the relevant diff more specifically. For example:

```bash
git diff -- docs/P4
git diff -- P4-编码开发.md
git diff -- api_design.md database_design.md
```

Use the actual repository paths. Do not assume paths that do not exist.

## 3. User-Specified Scope Has Priority

The user's specified commit scope has the highest priority.

### 3.1 When the User Does Not Specify a Scope

If the user says something general, such as:

```text
帮我提交
按规范提交
commit 一下
提交当前修改
把这次文档修改提交一下
```

Codex should:

1. Inspect all working tree changes.
2. Identify which files belong to the same documentation task.
3. Exclude obviously unrelated or unsafe files.
4. Prepare a proposed commit plan.
5. Show the plan to the user before committing, unless the user explicitly says to commit directly.

The proposed plan should include:

```text
拟提交文件：
- ...

未纳入提交的文件：
- ...

拟定 commit message：
...

说明：
- ...
```

Do not commit immediately unless the user explicitly asks for direct submission.

### 3.2 When the User Specifies a Scope

If the user says something specific, such as:

```text
只提交 P4 文档
只提交 api_design.md 和 database_design.md
提交支付模块相关文档修改
这次只提交通知轮询相关内容
不要提交前端代码
不要提交 P1/P2
```

Codex must:

1. Only include files or changes within the user's requested scope.
2. Never include unrelated files just because they are modified.
3. If the user specifies a module or topic rather than exact files, infer relevant files from the diff, but do not expand the scope to clearly unrelated changes.
4. If a changed file is close to the requested scope but not clearly included, mention it separately.
5. Show the actual files to be committed and the excluded files before committing, unless the user explicitly asks to commit directly.

Example plan:

```text
用户指定范围：支付模块相关文档修改

拟提交文件：
- docs/P3/api_design.md
- docs/P3/database_design.md
- docs/P4/implementation_notes.md

排除文件：
- docs/P1/srs_ieee830.md：本次修改不属于支付模块
- frontend/package-lock.json：与本次文档提交无关

拟定 commit message：
docs: clarify payment tracking fields across design docs

- Mark payment linkage fields as tracking-only metadata
- Sync affected API and database design descriptions
- Keep unrelated frontend changes out of this commit
```

## 4. Documentation Commit Types

Use the following commit types for documentation-stage work.

### 4.1 `docs`

Use `docs:` for normal documentation updates, phase document synchronization, requirement document edits, architecture/design updates, API/database document edits, prompt log updates, and AI collaboration record updates.

Examples:

```text
docs: sync P1/P2 artifacts with current MVP scope
docs: refine P4 implementation notes for notification polling
docs: update phase reference documents
docs: clarify tracking fields in payment and file records
```

### 4.2 `fix`

Use `fix:` when correcting inaccurate, inconsistent, obsolete, or conflicting documentation.

Examples:

```text
fix: correct inconsistent pickup request status references
fix: align payment API paths with REST naming rules
fix: remove outdated review status from requirement docs
```

### 4.3 `refactor`

Use `refactor:` when reorganizing documentation structure, simplifying descriptions, or restructuring design explanations without changing the project scope or functional meaning.

Examples:

```text
refactor: reorganize P3 class design around service boundaries
refactor: simplify trusted pickup workflow descriptions
```

### 4.4 `feat`

For documentation work, prefer `docs:` even when adding new documents.

Use `feat:` only when the newly added documentation represents a substantive expansion of project functionality, delivery scope, or module coverage.

Examples where `docs:` is usually better:

```text
docs: add P4 implementation notes
docs: add API error code reference
docs: add database field reference
```

Use `feat:` only if the change truly adds a new functional scope to the project and the repository history supports that style.

## 5. Commit Message Format

Use a subject and a body for non-trivial documentation commits.

### 5.1 Subject Format

```text
<type>: <summary>
```

Requirements:

1. Use a clear type, such as `docs`, `fix`, `refactor`, or rarely `feat`.
2. Keep the summary concise and specific.
3. Use lowercase type.
4. Prefer imperative or concise descriptive style.
5. Match the existing repository style when there is a clear precedent.
6. Do not add `[AI-assisted]` unless explicitly required by the user, course, team, or repository convention.

Avoid vague subjects:

```text
docs: update files
docs: update docs
docs: Updated phase reference documents
fix: fix problems
```

Prefer specific subjects:

```text
docs: sync P1/P2/P3 artifacts with current MVP scope
docs: refine P4 implementation docs for notification polling
fix: correct inconsistent payment tracking descriptions
refactor: simplify pickup workflow descriptions across design docs
```

### 5.2 Body Format

For meaningful documentation changes, include a body with 2-4 bullet points.

Each bullet should describe an actual change visible in the diff. Do not exaggerate or invent changes.

Format:

```text
- ...
- ...
- ...
```

Example:

```text
docs: refine P4 implementation docs for notification polling

- Clarify HTTP polling strategy for in-site business notifications
- Align notification records with pickup request status changes
- Update affected implementation notes without changing earlier phase scope
```

For very small documentation changes, a subject-only commit is acceptable if that matches repository history.

## 6. Common Documentation Commit Templates

### 6.1 Phase Document Synchronization

```text
docs: sync P1/P2/P3 artifacts with current MVP scope

- Update requirement artifacts to match the latest trusted pickup workflow
- Align architecture records with narrowed module boundaries
- Refresh detailed design references for affected service and data models
```

### 6.2 P4 Documentation Update

```text
docs: update P4 implementation docs for current design scope

- Align coding notes with the latest P1/P2/P3 design decisions
- Clarify implementation constraints for selected MVP modules
- Preserve earlier phase decisions without introducing new requirements
```

### 6.3 API Documentation Correction

```text
fix: align API documentation with REST naming rules

- Rename affected pickup request endpoints to resource-oriented paths
- Update related request and response examples
- Keep unchanged business rules out of this commit
```

### 6.4 Database or Field Description Correction

```text
docs: clarify tracking fields in payment and file records

- Mark payment and file linkage fields as tracking-only metadata
- Sync affected database and API descriptions
- Avoid introducing new settlement or storage requirements
```

### 6.5 Notification Polling Documentation

```text
docs: refine notification polling documentation

- Clarify HTTP polling as the MVP notification delivery strategy
- Require unread notification records for business status changes
- Document timeout and polling behavior consistently across affected files
```

### 6.6 Removing Outdated Design References

```text
fix: remove outdated review status references from documents

- Delete obsolete pending-review state descriptions from requirement artifacts
- Align use cases and status definitions with the current workflow
- Keep historical AI reflection records unchanged unless directly affected
```

## 7. File Selection Rules

### 7.1 Files Usually Safe to Commit for Documentation Tasks

The following file types are often acceptable for documentation commits, depending on the actual task and user scope:

```text
*.md
*.yaml
*.yml
*.puml
*.drawio
*.pdf
*.docx
```

Still, only commit them if they are relevant to the current task.

### 7.2 Files That Require Caution

Be cautious with:

```text
*.xlsx
*.png
*.jpg
*.jpeg
*.zip
*.jar
*.db
```

Only include these files if they are clearly part of course deliverables, diagrams, survey data, design assets, or the user explicitly asks to include them.

### 7.3 Files That Must Not Be Committed by Default

Do not include the following files by default:

```text
.DS_Store
node_modules/
target/
dist/
build/
.idea/
.vscode/
*.log
*.tmp
*.class
.env
.env.local
application-local.yml
application-dev-local.yml
```

If these files appear in `git status`, warn the user and keep them out of the commit unless the user gives a clear reason to include them.

## 8. Commit Execution Rules

Unless the user explicitly says to commit directly, first show the proposed commit plan and wait for confirmation.

After confirmation, run:

```bash
git add <confirmed files only>
git commit -m "<subject>" -m "<body>"
```

Do not use:

```bash
git add .
```

unless all of the following conditions are true:

1. The user explicitly asked to commit all changes.
2. `git status`, `git diff --stat`, and `git diff` have been inspected.
3. There are no temporary files, cache files, local configuration files, IDE files, build outputs, or unrelated modifications.
4. The proposed commit plan has made clear that all changes are being included.

When staging files, prefer explicit file paths:

```bash
git add docs/P4/implementation_notes.md docs/P3/api_design.md
```

If only part of a file should be committed, use an appropriate partial staging workflow such as:

```bash
git add -p <file>
```

Do not partially stage changes unless the resulting commit can still be clearly explained and the user has approved the scope.

## 9. Post-Commit Feedback Format

After a successful commit, report:

```text
已完成提交。

commit hash:
<hash>

commit message:
<full commit message>

本次提交文件：
- ...

当前 git status:
<summary>

说明：
- ...
```

If there are remaining uncommitted changes, list them clearly.

If the commit fails, explain the reason and do not claim success.

## 10. Default Behavior Summary

Default rules:

1. If no scope is specified, inspect all diffs and propose a commit plan first.
2. If a scope is specified, only include changes within that scope.
3. Do not commit directly unless the user explicitly asks for direct submission.
4. Do not use `git add .` by default.
5. Do not add `[AI-assisted]` by default.
6. Keep commit messages consistent with the repository's existing style.
7. Prefer `docs:` for documentation-stage work.
8. Use `fix:` for documentation corrections.
9. Use `refactor:` for documentation restructuring without scope changes.
10. Use `feat:` only when a documentation change reflects a real functional or delivery-scope expansion.