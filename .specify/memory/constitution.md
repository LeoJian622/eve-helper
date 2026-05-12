<!-- Sync Impact Report: Constitution v1.2.0
- Version change: 1.1.0 → 1.2.0 (MINOR: Added performance and test coverage requirements)
- Modified principles: 
  I. CLI Interface, Test-First (NON-NEGOTIABLE), Integration Testing → I. CLI Interface, Test-First (NON-NEGOTIABLE), Integration Testing, API Performance
  III. Code Quality, Testing Standards, User Experience Consistency → III. Code Quality, Testing Standards, User Experience Consistency, Coverage Metrics
- Added sections: API Performance Requirements, Coverage Metrics
- Removed sections: None
- Templates requiring updates: 
  ✅ .specify/templates/plan-template.md
  ✅ .specify/templates/spec-template.md
  ✅ .specify/templates/tasks-template.md
  ⚠ .specify/templates/commands/*.md (Manual review needed)
- Follow-up TODOs: None

-->

# EVE Helper Constitution

## Core Principles

### I. CLI Interface, Test-First (NON-NEGOTIABLE), Integration Testing, API Performance
Every feature exposes functionality via CLI; Text in/out protocol: stdin/args → stdout, errors → stderr; Support JSON + human-readable formats
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
CONSTITUTION_VERSION: 1.2.0 | RATIFICATION_DATE: 2026-05-12 | LAST_AMENDED_DATE: 2026-05-12
<!-- Version: 1.1.0 | Ratified: 2026-05-12 | Last Amended: 2026-05-12 -->
