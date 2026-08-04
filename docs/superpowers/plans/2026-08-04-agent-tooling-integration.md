# Spec-Kit + Superpowers + ECC 工具链集成与清理 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重写过期的 CLAUDE.md、建立三套工具链的分级协作规则、修订 Spec-Kit 宪法、清理过期文件并消除 .env 密码泄露隐患。

**Architecture:** 文档/配置变更,零业务代码改动。精简 CLAUDE.md(~150 行)+ `@docs/AI_WORKFLOW.md` 导入详细工作流 + 单行 AGENTS.md 指针;宪法按 Spec-Kit 惯例修订(含 Sync Impact Report)。

**Tech Stack:** 纯 Markdown/JSON/git 操作。Windows + Git Bash(POSIX 语法)。

**不使用 worktree 的原因:** Task 1 需要在用户真实 git 索引上解除 .env 暂存,settings.local.json 是本地配置,必须在主工作区执行。

## Global Constraints

- **技术栈不可变更**: 禁止修改 `pom.xml` 和 `src/` 下任何业务代码。
- **禁止裸 `git commit`**: 用户有进行中工作已暂存(`src/main/resources/application-*.yml`),必须保持暂存但**不得提交**。所有提交一律路径限定: `git commit -m "..." -- <path1> <path2>`。
- **`.env.*` 永不提交**: 含明文密码。
- 保持中文文档惯例(所有新建/重写文档用中文)。
- GateGuard hook: 会话首条 Bash、新建文件前可能被拦截要求陈述事实,按提示陈述后重试同一操作即可。
- 文件行尾为 CRLF(git 自动处理,Write 工具写 LF 无碍)。
- 设计规格(事实来源): `docs/superpowers/specs/2026-08-04-agent-tooling-integration-design.md`

---

### Task 1: 解除 .env 密码文件的 git 暂存(安全优先)

**Files:**
- Modify (index only): `.env.dev`, `.env.aliw`, `.env.prd` — 从索引移除,本地文件保留

**Interfaces:**
- Consumes: 无
- Produces: 后续所有提交不再携带 .env 文件;`.gitignore` 规则开始生效

- [ ] **Step 1: 确认当前暂存状态**

Run: `git diff --cached --name-only`
Expected: 列表中包含 `.env.aliw` `.env.dev` `.env.prd` 和 `src/main/resources/application-*.yml`

- [ ] **Step 2: 从索引移除 .env 文件(本地保留)**

```bash
git rm --cached .env.dev .env.aliw .env.prd
```

- [ ] **Step 3: 验证移除成功且 ignore 生效**

```bash
git status --short | grep '\.env' || echo "OK: .env 不在 git 视野中"
git check-ignore -v .env.dev
```
Expected: 第一条输出 `OK: .env 不在 git 视野中`;第二条输出 `.gitignore:行号:...env...  .env.dev`(证明 ignore 规则接管)

- [ ] **Step 4: 确认用户 WIP 未受影响**

Run: `git diff --cached --name-only`
Expected: 只剩 `src/main/resources/application-ali.yml` `application-aliw.yml` `application-pro.yml`(无 .env)。本任务**不提交**——文件从未入库,移出索引即完成。

---

### Task 2: 删除过期文件并迁移 SKILL.md

**Files:**
- Delete: `DDD_ARCHITECTURE_MIGRATION.md`、`DDD_MIGRATION_PLAN.md`、`eve-helper230926120017005Config.json`、`eve-helpereveConfig.json`、`eve-helpersystemConfig.json`
- Rename: `SKILL.md` → `.claude/skills/eve-helper-patterns/SKILL.md`

**Interfaces:**
- Consumes: Task 1 完成(避免提交时误带文件——虽然本任务用路径限定提交)
- Produces: 根目录只剩 CLAUDE.md/Readme.md 等有效文档;`.claude/skills/eve-helper-patterns/` 成为可用项目技能

- [ ] **Step 1: 删除五个过期文件**

```bash
git rm DDD_ARCHITECTURE_MIGRATION.md DDD_MIGRATION_PLAN.md \
  eve-helper230926120017005Config.json eve-helpereveConfig.json eve-helpersystemConfig.json
```

- [ ] **Step 2: 迁移 SKILL.md 到技能目录**

```bash
mkdir -p .claude/skills/eve-helper-patterns
git mv SKILL.md .claude/skills/eve-helper-patterns/SKILL.md
```

- [ ] **Step 3: 验证技能文件 frontmatter 完整**

Run: `head -8 .claude/skills/eve-helper-patterns/SKILL.md`
Expected: 包含 `name: eve-helper-patterns` 和 `description:` 的 YAML frontmatter

- [ ] **Step 4: 路径限定提交**

```bash
git commit -m "chore: 清理过期文档与临时配置,迁移项目编码模式技能

- 删除已完成的 DDD 迁移规划文档(git 历史可查)
- 删除 IDEA MyBatisX 生成器临时配置(含失效绝对路径)
- SKILL.md 迁移至 .claude/skills/eve-helper-patterns/ 成为项目技能

Co-Authored-By: Claude <noreply@anthropic.com>" -- \
  DDD_ARCHITECTURE_MIGRATION.md DDD_MIGRATION_PLAN.md \
  eve-helper230926120017005Config.json eve-helpereveConfig.json \
  eve-helpersystemConfig.json SKILL.md .claude/skills/eve-helper-patterns/SKILL.md
```

- [ ] **Step 5: 验证提交范围**

Run: `git show --stat --oneline HEAD | head -15 && git diff --cached --name-only`
Expected: HEAD 提交只含上述 6 个路径变更;`application-*.yml` 仍在暂存区未被提交

---

### Task 3: 精简 .claude/settings.local.json

**Files:**
- Modify: `.claude/settings.local.json`(整体替换)

**Interfaces:**
- Consumes: 无
- Produces: 干净的权限配置,后续任务提交此文件

- [ ] **Step 1: 用以下内容整体替换 `.claude/settings.local.json`**

```json
{
  "permissions": {
    "allow": [
      "Bash(mvn test:*)",
      "Bash(mvn -T 4 test)",
      "Bash(mvn -T 4 clean verify -DskipTests)",
      "Bash(git add:*)",
      "Bash(git restore:*)",
      "Bash(git mv:*)",
      "Bash(git push:*)",
      "Bash(tree:*)"
    ]
  }
}
```

(删除的是 DDD 迁移期遗留的一次性 sed/for 循环/git-mv 权限和巨型 commit 消息权限;保留通用 mvn/git 权限。)

- [ ] **Step 2: 验证 JSON 合法**

Run: `python -c "import json;json.load(open('.claude/settings.local.json'))" 2>/dev/null && echo OK || node -e "JSON.parse(require('fs').readFileSync('.claude/settings.local.json'))" && echo OK`
Expected: `OK`(两个解释器任一可用即可;若都不可用,人工检查括号配对)

- [ ] **Step 3: 路径限定提交**

```bash
git commit -m "chore: 清理 DDD 迁移期遗留的一次性工具权限

Co-Authored-By: Claude <noreply@anthropic.com>" -- .claude/settings.local.json
```

---

### Task 4: 新建 docs/AI_WORKFLOW.md(工具链协作规则)

**Files:**
- Create: `docs/AI_WORKFLOW.md`

**Interfaces:**
- Consumes: 无
- Produces: CLAUDE.md(Task 5)将 `@docs/AI_WORKFLOW.md` 导入此文件;INDEX.md(Task 7)引用此文件

- [ ] **Step 1: 创建 `docs/AI_WORKFLOW.md`,内容如下(全文)**

````markdown
# AI 开发工作流(Spec-Kit + Superpowers + ECC)

本文档定义三套工具链在 eve-helper 项目中的协作规则,由 `CLAUDE.md` 通过 `@docs/AI_WORKFLOW.md` 导入,对所有 AI agent 与人类开发者生效。

## 工具链职责

| 工具链 | 角色 | 主要入口 |
|--------|------|----------|
| **Spec-Kit** | 规格驱动开发:需求 → 澄清 → 计划 → 任务 → 实现 | `/speckit-specify` `/speckit-clarify` `/speckit-plan` `/speckit-tasks` `/speckit-implement` `/speckit-analyze` `/speckit-checklist` |
| **Superpowers** | 过程技能:头脑风暴、TDD、系统化调试、完成前验证 | `superpowers:brainstorming` `superpowers:test-driven-development` `superpowers:systematic-debugging` `superpowers:verification-before-completion` |
| **ECC** | 质量门禁:代码评审、安全评审、构建修复 | `ecc:java-reviewer` `ecc:security-reviewer` `ecc:java-build-resolver` `/ecc:build-fix` `/ecc:code-review` |

## 分级流程

### L1 — 中大型功能(新功能、跨层变更、架构调整)

1. 需求模糊时先用 `superpowers:brainstorming` 澄清
2. `/speckit-specify` 生成 spec.md(自动创建顺序编号的 feature 分支)
3. `/speckit-clarify` 澄清规格中的模糊点(最多 5 个问题)
4. `/speckit-plan` 生成实现计划等设计产物(必须符合宪法)
5. `/speckit-tasks` 生成按依赖排序的 tasks.md
6. 可选:`/speckit-analyze` 做跨产物一致性检查
7. `/speckit-implement` 按任务执行

### L2 — 小改动(单文件、局部逻辑、不影响架构)

Superpowers TDD:
1. 写失败测试(RED),运行确认失败
2. 最小实现(GREEN),运行确认通过
3. 重构(IMPROVE),保持测试全绿
4. 覆盖率目标 ≥80%

### L3 — 紧急修复

1. `superpowers:systematic-debugging` 定位根因(禁止盲目修改)
2. 最小修复
3. 补复现该 bug 的回归测试
4. 运行全量测试确认无回归

## 实现后评审门禁(所有级别强制)

1. **Java 代码变更**: `ecc:java-reviewer` 必审,CRITICAL/HIGH 问题必须修复后方可合并
2. **涉及安全**(认证、授权、用户输入、外部 API、加密): 追加 `ecc:security-reviewer`
3. **构建失败**: 使用 `ecc:java-build-resolver` 或 `/ecc:build-fix`,最小改动恢复构建
4. **完成前验证**: `superpowers:verification-before-completion` — 有测试/构建证据才能声称完成

## Git 规范

- 提交格式: Conventional Commits(`feat/fix/docs/chore/test/refactor/perf/ci: 描述`),描述可用中文
- L1 feature 分支由 Spec-Kit 自动创建(顺序编号)
- 提交前 `mvn test` 必须通过
- **永不提交**: `.env.*` 实际配置、`eve-jwt.jks`、任何 token 或密码

## 禁止事项

- ❌ 升级或替换冻结技术栈的任何核心依赖(宪法第四条,须走宪法修订程序)
- ❌ L1 变更跳过 spec/plan/tasks 直接写代码
- ❌ 未运行测试就声称完成
- ❌ 手工修改 `.specify/scripts/` 与 `.specify/workflows/`(由 Spec-Kit 维护)
````

- [ ] **Step 2: 验证文件创建**

Run: `grep -c "分级流程" docs/AI_WORKFLOW.md && wc -l docs/AI_WORKFLOW.md`
Expected: 第一条 ≥1;行数约 60 行

(不单独提交——与 Task 5 一起提交,保证 CLAUDE.md 的 @导入不出现悬空引用窗口。)

---

### Task 5: 重写 CLAUDE.md 并新建 AGENTS.md

**Files:**
- Modify: `CLAUDE.md`(整体重写)
- Create: `AGENTS.md`

**Interfaces:**
- Consumes: Task 4 产出的 `docs/AI_WORKFLOW.md`(被 @导入)
- Produces: agent 入口文档;Task 7 的 INDEX.md 引用它们

- [ ] **Step 1: 用以下内容整体重写 `CLAUDE.md`(全文)**

````markdown
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**开始任何开发任务前先阅读**: @docs/AI_WORKFLOW.md (分级开发流程与工具链规则)

## 项目概述

EVE Helper 是一个基于 Java Spring Boot 的应用程序,用于 EVE Online 玩家读取和分析角色/军团的订单、资产和市场数据。项目集成了 EVE Online ESI (EVE Swagger Interface) API,采用领域驱动设计 (DDD) 架构。

**版本**: 0.0.2-SNAPSHOT
**Java**: 17
**Spring Boot**: 3.5.14

## 技术栈(已冻结)

> ⚠️ **硬约束**: 技术栈已冻结(宪法第四条)。未经宪法修订程序,不得升级或替换任何核心依赖。

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 语言 |
| Spring Boot | 3.5.14 | 核心框架 (Web/Security/Validation/Cache/Data Redis/WebSocket/Actuator) |
| Spring WebFlux + Reactor Netty | Boot 托管 | ESI 响应式 HTTP 客户端 |
| MyBatis Plus | 3.5.15 (spring-boot3-starter) | ORM 与分页 |
| Druid | 1.2.24 | 连接池,多数据源 |
| MySQL Connector/J | 9.6.0 | 数据库驱动 |
| Redis | Boot 托管 | 缓存与会话存储 |
| Lombok | 1.18.40 | 减少样板代码 |
| MapStruct | 1.6.3 | 对象映射(编译期注解处理) |
| Hutool | 5.8.44 | 工具库 |
| SpringDoc OpenAPI | 2.8.16 | API 文档 |
| Nimbus JOSE JWT | 10.0.2 | JWT 处理 |
| Prometheus metrics | 1.0.0 | 指标暴露 |

## 构建和开发命令

```bash
# 构建项目
mvn clean package

# 运行应用 (默认 dev profile)
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 运行所有测试 / 单个测试类 / 单个方法
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName

# 跳过测试构建
mvn package -DskipTests
```

### 访问端点
- **应用**: http://localhost:9999
- **Swagger UI**: http://localhost:9999/swagger-ui.html
- **API 文档**: http://localhost:9999/v3/api-docs

## DDD 架构概览

五层结构,依赖规则: `Interfaces → Application → Domain ← Infrastructure`。领域层是核心,不依赖任何其他层;基础设施层实现领域层定义的接口。

- **domain/**: 实体(`model/entity/eve|system`,均继承 `BaseEntity`)、仓储接口(`repository/`)、领域服务(`service/esi|eve|system|security|thread`)、Specification 模式
- **application/**: 应用服务、CommandBus/QueryBus(泛型反射分发)、DTO、MapStruct 组装器(26+)、命令/查询处理器(CQRS)
- **infrastructure/**: 持久化(PO + MyBatis mapper + 仓储实现)、外部集成(`external/esi` 30+ API 类,OAuth2 PKCE;`external/onebot`)、配置(多数据源、Spring Security/JWT/RBAC)
- **interfaces/**: REST 控制器、过滤器、全局异常处理、VO
- **shared/**: BaseEntity/PageResult、枚举、EveHelperException、常量、注解、Result<T>/ResultCode、工具类

### 添加新功能
1. 从领域模型开始(实体/值对象/聚合) → 2. 领域层定义仓储接口 → 3. 应用服务协调用例 → 4. 基础设施层实现仓储 → 5. MapStruct 组装器 → 6. 接口层控制器 → 7. MyBatis mapper XML

### 关键技术细节
- **多数据源**: `eve`(游戏静态数据,只读)与 `eve_helper`(运行时数据),独立 MyBatis Plus 配置
- **JWT**: RSA-256(`eve-jwt.jks`);access 900s / refresh 604800s;refresh token 存 Redis;登出黑名单;登录限流;`JwtAuthorizationTokenFilter`
- **ESI OAuth**: Authorization Code + PKCE;refresh token 加密存库;access token 缓存 Redis 19 分钟;支持晨曦(Serenity)/宁静(Tranquility)服务器
- **RBAC**: `RbacAuthorizationManager`;URL-权限-角色映射缓存 Redis;权限格式 `METHOD:PATH`;白名单在 application-*.yml
- **缓存**: Redis 主缓存,默认 TTL 3000 秒
- **异步**: @EnableScheduling + AsyncConfiguration,市场订单线程池

## 分级开发流程(速查)

| 级别 | 适用场景 | 流程 |
|------|----------|------|
| **L1** | 中大型功能、跨层变更 | Spec-Kit: `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement` |
| **L2** | 小改动(单文件/局部逻辑) | Superpowers TDD: RED → GREEN → IMPROVE |
| **L3** | 紧急修复 | `superpowers:systematic-debugging` → 最小修复 → 回归测试 |

所有级别实现后强制评审: `ecc:java-reviewer` 必审;涉及认证/用户输入/外部 API/加密时追加 `ecc:security-reviewer`;构建失败用 `ecc:java-build-resolver`。详见 @docs/AI_WORKFLOW.md。

## 安全红线

- **永不提交**: `.env.*` 实际配置、`eve-jwt.jks`、任何 token 或密码;敏感配置走环境变量(参考 `.env.example`)
- ESI tokens 敏感 — 加密存储在数据库
- 除白名单外所有端点需 JWT 认证;RBAC 由 `RbacAuthorizationManager` 执行
- 参数化查询(MyBatis);校验所有外部输入;错误消息不泄露敏感数据

## 重要文件

- `pom.xml`: Maven 依赖与构建配置(冻结技术栈的版本以此为准)
- `src/main/resources/application.yml`: profile 选择 (active: dev)
- `src/main/resources/application-{dev,ali,aliw,pro}.yml`: 环境配置
- `.env.example`: 环境变量模板
- `.specify/memory/constitution.md`: 项目宪法(含技术栈冻结条款)
- `docs/INDEX.md`: 文档索引;`docs/AI_WORKFLOW.md`: AI 开发工作流

## 注意事项

- MapStruct 组装器必须在编译期由注解处理器生成
- Redis 必须运行;两个 MySQL 数据库(eve 和 eve_helper)必须可访问
- 中文注释和文档是有意的(目标受众)
- 业务代码变更须走分级开发流程;技术栈本身不可变更
````

- [ ] **Step 2: 创建 `AGENTS.md`(单行指针)**

```markdown
# AGENTS.md

Read and follow [CLAUDE.md](./CLAUDE.md) — this project's single source of truth for architecture, constraints (frozen tech stack), and the tiered development workflow.
```

- [ ] **Step 3: 验证 CLAUDE.md 版本与 pom.xml 一致**

Run: `grep -E "3\.5\.14|3\.5\.15|1\.2\.24|1\.6\.3|5\.8\.44|2\.8\.16|9\.6\.0|10\.0\.2" CLAUDE.md | wc -l`
Expected: ≥8(每个关键版本至少出现一次);且 `grep -c "2\.7\.18\|Java 11" CLAUDE.md` 输出 0

- [ ] **Step 4: 路径限定提交**

```bash
git add docs/AI_WORKFLOW.md AGENTS.md
git commit -m "docs: 重构 agent 文档体系

- CLAUDE.md 修正技术栈至实际版本(Java 17 / Spring Boot 3.5.14),声明技术栈冻结
- 新增 docs/AI_WORKFLOW.md: Spec-Kit + Superpowers + ECC 分级开发流程
- 新增 AGENTS.md 跨工具入口指针

Co-Authored-By: Claude <noreply@anthropic.com>" -- CLAUDE.md docs/AI_WORKFLOW.md AGENTS.md
```

---

### Task 6: 修订 Spec-Kit 宪法至 v1.3.0

**Files:**
- Modify: `.specify/memory/constitution.md`(整体重写)

**Interfaces:**
- Consumes: 无(Template 已核查: 仅 constitution-template.md 的通用 Example 注释含 CLI 字样,属 Spec-Kit 样板,无需改动)
- Produces: 宪法 v1.3.0,REST API First + 技术栈冻结

- [ ] **Step 1: 用以下内容整体重写 `.specify/memory/constitution.md`(全文)**

````markdown
<!-- Sync Impact Report: Constitution v1.3.0
- Version change: 1.2.0 → 1.3.0 (MINOR: Aligned principles with project reality; added technology stack freeze)
- Modified principles:
  I. CLI Interface, Test-First (NON-NEGOTIABLE), Integration Testing, API Performance → I. REST API First, Test-First (NON-NEGOTIABLE), Integration Testing, API Performance
- Added principles: IV. Technology Stack Freeze (NON-NEGOTIABLE)
- Removed sections: None
- Templates requiring updates:
  ✅ .specify/templates/plan-template.md (checked, no principle-name references)
  ✅ .specify/templates/spec-template.md (checked, no principle-name references)
  ✅ .specify/templates/tasks-template.md (checked, no principle-name references)
  ✅ .specify/templates/constitution-template.md (generic Spec-Kit examples only, unchanged)
- Follow-up TODOs: None

-->

# EVE Helper Constitution

## Core Principles

### I. REST API First, Test-First (NON-NEGOTIABLE), Integration Testing, API Performance
Every feature exposes functionality via REST APIs following the DDD layering rule (Interfaces → Application → Domain ← Infrastructure); responses use the unified Result<T> envelope with ResultCode; the domain layer carries no outward dependencies
Red-Green-Refactor cycle mandatory: Tests written → User approved → Tests fail → Then implement; Test coverage gates PR merges
Focus areas requiring integration tests: New library contract tests, Contract changes, Inter-service communication, Shared schemas
API performance: All REST endpoints must respond within 200ms (95th percentile), with explicit timeouts and failure monitoring

### II. Observability, Security Requirements
Structured logging required; MAJOR.MINOR.BUILD format; Code quality metrics enforced
Encrypted JWT storage; Sensitivity classification for secrets; Rate limiting applied to auth endpoints; RBAC audited quarterly
Security monitoring: Real-time intrusion detection, automated vulnerability scanning, secure credential rotation

### III. Code Quality, Testing Standards, User Experience Consistency, Coverage Metrics
YAGNI enforced; Complexity thresholds enforced; First implementations simple; Refactor only when duplicate code identified
Test coverage ≥80% with automated gate: Unit tests must achieve 80% minimum coverage; Integration tests must cover all critical paths
Mandatory code review for all changes, with automated quality metrics enforcement; Automated dependency vulnerability scanning

### IV. Technology Stack Freeze (NON-NEGOTIABLE)
The core technology stack is frozen: Java 17, Spring Boot 3.5.x, MyBatis Plus, Druid, MySQL, Redis, Spring Security, Spring WebFlux, MapStruct, Lombok, Hutool, SpringDoc OpenAPI, Nimbus JOSE JWT
Upgrading, replacing, or introducing core dependencies requires the constitutional amendment process (version bump + Sync Impact Report + migration plan)
Experimental libraries may only be used in prototypes and must not enter the main codebase without amendment

## API Performance Requirements
All REST API endpoints must:
- Respond within 200ms (95th percentile) for standard requests
- Implement explicit 5-second timeout for all external service calls
- Monitor and log response time metrics with automatic alerting for degradation
- Optimize database queries to avoid N+1 problems
- Use connection pooling for all external services
- Implement circuit breakers for unreliable dependencies
- Perform regular load testing to validate performance targets

## Coverage Metrics
- Code coverage measurement must be automated and integrated into CI/CD pipeline
- Minimum coverage requirements: 80% line coverage for unit tests, 100% for critical paths
- Coverage reports must be visible in pull requests and block merge if requirements not met
- Integration tests must cover all critical user journeys and error scenarios
- Security tests must cover authentication, authorization, and data validation
- Performance tests must validate SLAs under expected load

## Governance
Constitution supersedes all other practices; Amendments require documentation, approval, migration plan; Versioned stored in Git history; Compliance reviewed quarterly
All changes must include performance impact assessment and test coverage analysis

## Memory Files
CONSTITUTION_VERSION: 1.3.0 | RATIFICATION_DATE: 2026-05-12 | LAST_AMENDED_DATE: 2026-08-04
<!-- Version: 1.2.0 | Ratified: 2026-05-12 | Last Amended: 2026-05-12 -->
````

- [ ] **Step 2: 验证 CLI 条款已移除、冻结条款已加入**

```bash
grep -c "Every feature exposes functionality via CLI" .specify/memory/constitution.md   # 期望 0
grep -c "Technology Stack Freeze" .specify/memory/constitution.md                      # 期望 ≥1
grep -c "CONSTITUTION_VERSION: 1.3.0" .specify/memory/constitution.md                  # 期望 1
```

- [ ] **Step 3: 路径限定提交**

```bash
git commit -m "docs: 修订宪法 v1.3.0 — REST API First 与技术栈冻结

移除模板遗留的 CLI Interface 条款(与 REST API 项目不符),
新增第四条技术栈冻结原则(宪法修订程序方可变更核心依赖)。

Co-Authored-By: Claude <noreply@anthropic.com>" -- .specify/memory/constitution.md
```

---

### Task 7: 更新 docs/INDEX.md

**Files:**
- Modify: `docs/INDEX.md`(多处定点编辑)

**Interfaces:**
- Consumes: Task 2(文件已删/迁移)、Task 4/5(新文件已就位)
- Produces: 无失效链接的文档索引

- [ ] **Step 1: 移除架构文档表中的 DDD 迁移文档两行**

Edit `docs/INDEX.md`,old_string:
```
| [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md) | DDD架构迁移说明 | 2025-01-29 |
| [DDD迁移计划](../DDD_MIGRATION_PLAN.md) | DDD迁移详细计划 | 2025-01-29 |
```
new_string: (空,整行删除)

- [ ] **Step 2: 更新工具文档表**

Edit `docs/INDEX.md`,old_string:
```
| [CLAUDE.md](../CLAUDE.md) | Claude AI使用指南 | 2026-02-01 |
| [SKILL.md](../SKILL.md) | 技能系统文档 | 2026-02-01 |
```
new_string:
```
| [CLAUDE.md](../CLAUDE.md) | Claude Code 项目指南(架构/约束/流程速查) | 2026-08-04 |
| [AGENTS.md](../AGENTS.md) | 跨 AI 工具入口指针 | 2026-08-04 |
| [AI开发工作流](./AI_WORKFLOW.md) | Spec-Kit + Superpowers + ECC 分级流程 | 2026-08-04 |
| [项目编码模式技能](../.claude/skills/eve-helper-patterns/SKILL.md) | Git 历史提炼的编码模式 | 2026-08-04 |
```

- [ ] **Step 3: 修正五处 DDD 迁移文档链接**

Edit 3a — old: `- **DDD架构**: [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)`
new: `- **DDD架构**: [开发指南 - DDD架构说明](./DEVELOPMENT.md#ddd架构说明)`

Edit 3b — old: `- [DDD架构](../DDD_ARCHITECTURE_MIGRATION.md)`
new: `- [DDD架构](./DEVELOPMENT.md#ddd架构说明)`

Edit 3c — old: `- ✅ [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)`
new: `- ✅ [AI开发工作流](./AI_WORKFLOW.md)`

Edit 3d — old: `- ✅ [DDD迁移计划](../DDD_MIGRATION_PLAN.md)`
new: `- ✅ [CLAUDE.md](../CLAUDE.md)`

Edit 3e — old: `| 什么是DDD架构? | [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md) |`
new: `| 什么是DDD架构? | [开发指南 - DDD架构说明](./DEVELOPMENT.md#ddd架构说明) |`

- [ ] **Step 4: 修正外部资源版本链接**

Edit 4a — old: `- [Spring Boot 2.7.x](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)`
new: `- [Spring Boot 3.5.x](https://docs.spring.io/spring-boot/reference/)`

Edit 4b — old: `- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)`
new: `- [MySQL](https://dev.mysql.com/doc/)`

- [ ] **Step 5: 更新统计与页脚日期**

Edit 5a — old:
```
- **总文档数**: 15个
- **核心文档**: 5个
- **架构文档**: 7个
- **工具文档**: 2个
- **最近更新**: 2026-02-01
- **文档覆盖率**: 95%
```
new:
```
- **总文档数**: 14个
- **核心文档**: 5个
- **架构文档**: 5个
- **工具文档**: 4个
- **最近更新**: 2026-08-04
- **文档覆盖率**: 95%
```

Edit 5b — old: `**最后更新**: 2026-02-01`
new: `**最后更新**: 2026-08-04`

- [ ] **Step 6: 验证无失效引用**

Run: `grep -n "DDD_ARCHITECTURE_MIGRATION\|DDD_MIGRATION_PLAN\|\.\./SKILL\.md\|2\.7\.x" docs/INDEX.md || echo "OK: 无失效引用"`
Expected: `OK: 无失效引用`

- [ ] **Step 7: 路径限定提交**

```bash
git commit -m "docs: 更新文档索引,移除失效引用并登记新文档

Co-Authored-By: Claude <noreply@anthropic.com>" -- docs/INDEX.md
```

---

### Task 8: 最终验证(对照设计规格)

**Files:** 无变更,纯验证

- [ ] **Step 1: 逐条验证设计规格的验收标准**

```bash
# 1. CLAUDE.md 无过期版本
grep -cE "2\.7\.18|Java 11" CLAUDE.md                               # 期望 0
# 2. .env 不在索引且被 ignore
git diff --cached --name-only | grep '\.env' || echo "OK-env"       # 期望 OK-env
git check-ignore -v .env.dev | head -1                              # 期望有输出
# 3. 技能文件完整
ls .claude/skills/                                                  # 期望 eve-helper-patterns + speckit-* 各项
# 4. 宪法无 CLI 条款、有冻结条款
grep -c "Every feature exposes functionality via CLI" .specify/memory/constitution.md  # 期望 0
grep -c "Technology Stack Freeze" .specify/memory/constitution.md                      # 期望 ≥1
# 5. INDEX.md 无失效链接
grep -c "DDD_ARCHITECTURE_MIGRATION\|DDD_MIGRATION_PLAN" docs/INDEX.md                 # 期望 0
# 6. 用户 WIP 完整保留在暂存区
git diff --cached --name-only                                       # 期望恰好 3 个 application-*.yml
# 7. 业务代码零改动: 本次所有提交不涉及 src/ 与 pom.xml
git log --oneline 53e8893..HEAD --name-only | grep -E "^src/|^pom\.xml" || echo "OK-no-src-changes"
```
Expected: 全部符合注释中的期望值

- [ ] **Step 2: 若有遗漏的未提交变更,按归属路径限定提交;若工作区干净则跳过**

Run: `git status --short`
Expected: 仅剩用户 WIP(暂存的 yml + application-dev.yml 的未暂存修改);.env 文件被 ignore 不显示

- [ ] **Step 3: 向用户汇报**

汇报内容: 新文档体系入口(CLAUDE.md → AI_WORKFLOW.md)、宪法 v1.3.0 要点、清理清单执行结果、.env 安全处理结果、暂存区中用户 WIP 保持原样未提交。
