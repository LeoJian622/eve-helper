<!-- Sync Impact Report: Constitution v1.6.0
- Version change: 1.5.0 -> 1.6.0 (MINOR: Added mandatory feature-branch isolation + development-trunk protection)
- Added principles: V.1 特性轨必须创建独立特性分支;发展主干(springboot3.0 / main)禁止直接提交特性实现
- Modified principles: V. Uniform Spec-First Process (added feature-branch mandate as sub-clause)
- Templates requiring updates:
  ✅ .specify/templates/plan-template.md (no change needed; plan already carries Branch header)
  ✅ .specify/templates/spec-template.md (checked, no principle-name references)
  ✅ .specify/templates/tasks-template.md (checked, no principle-name references)
  ✅ .specify/templates/constitution-template.md (generic Spec-Kit examples only, unchanged)
- Documents synced: constitution.md (this file), CLAUDE.md (workflow引用), docs/workflow/DEVELOPMENT-WORKFLOW.md (轨道表补充分支要求)
- Follow-up TODOs: none
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

### V. Uniform Spec-First Process (NON-NEGOTIABLE)
All development operations follow one uniform process regardless of change scope — routing work into lighter tiers by size is not permitted: clarify (when ambiguous) -> `/speckit-specify` -> `/speckit-plan` (with `ecc:security-reviewer` design review) -> `/speckit-tasks` (atomic tasks < 2h, each with explicit AC) -> `/speckit-implement` under strict TDD (RED -> GREEN -> REFACTOR)

### V.1 特性轨必须创建独立特性分支 (NON-NEGOTIABLE)
Every feature track (特性轨) MUST be developed on a dedicated feature branch, named per the feature (`<NNN>-<feature>`, e.g. `015-esi-response-caching`, as declared in the plan.md `Branch` header). Implementation commits MUST NOT be made directly on the development trunk (`springboot3.0` / `main` / `origin/...`): trunk branches are reserved for integration only, updated exclusively via merge/PR at the P7 收尾阶段. Rationale: trunk is a long-lived shared reference; direct feature commits pollute its history, break per-feature review/rollback, and leak half-finished work. Violation is corrected by moving the feature commits onto the feature branch and rewinding the trunk to its upstream.
Mandatory post-implementation review gate: `ecc:java-reviewer` required on Java changes; `ecc:security-reviewer` added for auth/user-input/external-API/crypto changes; `ecc:java-build-resolver` on build failure; review records archived under `docs/reviews/`
No change may bypass spec/plan/tasks to write code directly; implementation claims require test/build evidence (`superpowers:verification-before-completion`)
Urgent fixes receive NO exemption and must complete the full spec/plan/tasks sequence before any implementation code is touched: `superpowers:systematic-debugging` establishes root cause as input to `/speckit-specify` (recording reproduction steps, root cause, blast radius, and fix boundary), then plan/tasks (including a failing regression test task), then TDD implementation. Retroactive documentation is prohibited
Diagnosis is not implementation: reading code, adding logs, running tests, and debugger-based reproduction are permitted during root-cause analysis; modifying business code before tasks are approved is not. Production incidents separate mitigation from repair — rollback/degrade/rate-limit/disable-entrypoint are operational actions, while the code fix still follows the full process

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
CONSTITUTION_VERSION: 1.6.0 | RATIFICATION_DATE: 2026-05-12 | LAST_AMENDED_DATE: 2026-08-20
<!-- Version: 1.6.0 | Ratified: 2026-05-12 | Last Amended: 2026-08-20 -->
