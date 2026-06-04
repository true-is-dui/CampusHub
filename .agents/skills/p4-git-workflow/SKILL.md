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

## 2. Branch Check Before Commit

Before every commit, always check the current branch:

    git branch --show-current
    git status

Rules:

- If the current branch is `main`, do not commit. Tell the user that direct commits to `main` are not allowed.
- If the current branch is `dev`, do not automatically create a new branch. Tell the user that the current branch is `dev` and ask whether to commit directly or switch/create a task branch, unless the user has already clearly requested direct commit on `dev`.
- If the current branch matches `feature/*`, `fix/*`, or `docs/*`, continue using the current branch. Do not create a new branch automatically.
- If the current branch has another name, explain the current branch and ask whether to continue, switch, or create a new branch.

Important: this skill must not create a new branch before every commit. Multiple commits may be made on the same task branch.

## 3. Creating a Branch

Only create a new branch when the user explicitly asks for it, or when the current branch is clearly unsuitable and the user confirms the new branch.

For a feature branch:

    git checkout dev
    git pull
    git checkout -b feature/<task-name>

For a fix branch:

    git checkout dev
    git pull
    git checkout -b fix/<bug-name>

For a documentation branch:

    git checkout dev
    git pull
    git checkout -b docs/<doc-name>

Do not create branches from outdated task branches unless the user explicitly asks.

## 4. Pre-Commit Diff Inspection

Before staging files, always inspect the working tree:

    git status
    git diff --stat
    git diff

If there are staged changes, also inspect:

    git diff --cached --stat
    git diff --cached

Do not commit without understanding what changed.

## 5. Staging Rules

Do not blindly run:

    git add .

Prefer staging explicit files:

    git add <file1> <file2> <file3>

Use `git add .` only if all of the following are true:

1. The user explicitly wants to include all changes.
2. `git status`, `git diff --stat`, and `git diff` have been inspected.
3. There are no unrelated, generated, local-only, or sensitive files.
4. The response clearly states that all current changes will be included.

If the user specified a scope, respect it strictly.

Examples:

- If the user says "只提交后端登录", only stage files related to backend login.
- If the user says "不要提交前端", do not stage frontend files.
- If the user says "只提交文档", do not stage code files.

## 6. Files Not to Commit by Default

Do not commit local, generated, temporary, or sensitive files by default:

    .env
    .env.local
    application-local.yml
    application-dev-local.yml
    .DS_Store
    .idea/
    .vscode/
    node_modules/
    target/
    dist/
    build/
    *.log
    *.tmp
    *.class

Do not commit real passwords, database credentials, JWT secrets, access keys, private tokens, or machine-specific absolute paths.

If configuration examples are needed, commit example files instead:

    .env.example
    application-example.yml
    application-dev.example.yml

## 7. Commit Message Style

Use Conventional Commit style.

Common types:

    feat: new feature
    fix: bug fix
    refactor: code restructuring without behavior change
    test: add or update tests
    chore: build, dependency, config, or maintenance change
    docs: documentation change
    style: formatting-only change

Good examples:

    feat: implement login token validation
    feat: add pickup request publishing flow
    fix: prevent cancelled orders from being paid
    fix: correct file permission check
    refactor: simplify order status handling
    test: add pickup request service tests
    chore: update backend dev configuration
    docs: update P4 deployment notes

Avoid vague messages:

    feat: update code
    fix: fix bug
    chore: modify files
    docs: update

## 8. Commit Message Format

For small changes, use a subject-only commit:

    git commit -m "fix: correct login token validation"

For larger changes, use a subject and body:

    git commit -m "feat: implement pickup request publishing flow" -m "- Add request creation persistence
    - Validate required request fields
    - Return response data used by the frontend"

Rules:

- The subject should be clear and specific.
- The body should describe actual changes visible in the diff.
- Use 2-4 bullet points for non-trivial commits.
- Do not claim that tests passed unless they were actually run.
- Do not mention Codex, ChatGPT, or AI-generated content by default.
- Add AI-related notes only if the user explicitly asks or the course/team convention requires it.

## 9. Commit Plan Before Commit

Before committing, provide a short commit plan unless the user explicitly requested direct commit without review.

The commit plan should include:

    Current branch:
    <branch>

    Files to commit:
    - ...

    Files not included:
    - ...

    Proposed commit message:
    <message>

    Verification:
    - ...

If there are suspicious files, unrelated changes, generated files, or sensitive-looking files, pause and ask before staging them.

## 10. Verification Notes

This skill does not require running all tests before every commit.

However, if the user asks for verification, or if the change is risky, run the most relevant available checks.

Do not invent project commands. Use only commands that exist in the repository.

Possible examples:

    mvn test
    mvn spring-boot:run
    npm run build
    npm test

If no tests or build commands are run, say so honestly in the final report.

## 11. After Commit

After a successful commit, report:

    Committed successfully.

    Branch:
    <branch>

    Commit:
    <hash>

    Commit message:
    <message>

    Committed files:
    - ...

    Verification:
    - ...

    Remaining changes:
    - ...

After committing, check:

    git status
    git log -1 --oneline

If the commit fails, explain the reason and do not claim success.

## 12. Relationship With Documentation Commit Skill

For documentation-only commits during non-coding documentation phases, prefer the existing `doc-commit-convention` skill.

Use this skill when:

- the commit is part of P4 coding-stage work;
- the commit includes code;
- the commit includes configuration, tests, frontend/backend files, or implementation-related documentation;
- the documentation change is directly tied to P4 development, deployment, testing, or coding workflow;
- the user explicitly asks to use the P4 Git workflow.

Documentation-only commits may still use a `docs/*` branch during P4 if the change belongs to P4 development work.

Do not mix unrelated documentation-only changes with code commits unless the user explicitly wants them committed together.

## 13. Default Behavior Summary

Default behavior:

1. Check the current branch before every commit.
2. Do not commit directly on `main`.
3. Do not automatically create a new branch if already on a suitable task branch.
4. Be careful when committing directly on `dev`.
5. Inspect `git status` and `git diff` before staging.
6. Stage explicit files instead of using `git add .`.
7. Exclude local, generated, temporary, and sensitive files.
8. Use clear Conventional Commit messages.
9. Respect the user's requested commit scope.
10. Report branch, commit hash, committed files, verification, and remaining changes after commit.