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

