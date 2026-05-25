# Implementation Plan: Spring AI 数据库查询 Agent

**Branch**: `002-spring-ai-db-agent` | **Date**: 2026-05-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-spring-ai-db-agent/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

基于 Spring AI Alibaba 1.1.2.0 开发数据库自然语言查询 Agent。用户通过中文自然语言提问，系统自动转换为 SQL 并执行，以友好的表格格式返回结果（最多 50 条）。系统包含严格的 SQL 安全校验机制，仅允许只读操作，禁止任何数据修改操作。

**Technical Approach**:
1. 使用 Spring AI Alibaba 集成通义千问大模型
2. 基于 Prompt Engineering 提供数据库 Schema 上下文
3. 多层 SQL 安全校验（语法检查、关键字过滤、只读限制）
4. 遵循现有 DDD 架构，复用 Spring Security、Redis、MyBatis Plus
5. RESTful API 提供查询和历史记录管理功能

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.5.14, Spring AI Alibaba 1.1.2.0, MyBatis Plus 3.5.15, JSqlParser 4.9  
**Storage**: MySQL 8.0+ (eve_helper database), Redis 6.0+  
**Testing**: JUnit 5, Spring Boot Test, Mockito  
**Target Platform**: Linux server (JVM-based)  
**Project Type**: Web Service (REST API)  
**Performance Goals**: 95% 查询在 10 秒内完成（含 AI 思考 + 数据库执行），API 响应时间满足宪法要求的 200ms p95（长查询使用异步模式）  
**Constraints**: 单次查询最多返回 50 条结果，严格只读操作，禁止任何数据修改，每个用户最多 30 请求/分钟  
**Scale/Scope**: 支持现有用户基数，每个用户保存最近 100 条查询历史

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### API Performance Gates
- ✅ All REST endpoints must demonstrate &lt;200ms response time (95th percentile) - 查询提交接口异步处理，立即返回任务ID
- ✅ Database query optimization must be planned and justified - 使用现有数据源，强制 LIMIT 50 限制
- ✅ Circuit breaker implementation required for external dependencies - AI 服务调用配置超时和重试机制

### Test Coverage Gates
- ✅ Unit test coverage plan must target ≥80% - 覆盖所有核心服务（SQL 生成、安全校验、执行）
- ✅ Integration test coverage must include all critical user journeys - 自然语言查询、历史记录查询、上下文连续查询
- ✅ Security test coverage must cover authentication/authorization flows - JWT 认证、SQL 注入防护、权限校验
- ✅ Performance test validation required for SLA compliance - AI 调用超时、数据库查询超时测试

## Project Structure

### Documentation (this feature)

```text
specs/002-spring-ai-db-agent/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── api-contracts.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/
│   └── service/ai/                    # 领域服务 - AI 核心逻辑
│       ├── SqlGenerationService.java      # SQL 生成服务
│       ├── SqlSecurityValidator.java      # SQL 安全校验器
│       ├── QueryHistoryService.java       # 查询历史服务
│       └── DatabaseSchemaService.java     # 数据库 Schema 服务
├── application/
│   └── service/ai/                    # 应用服务 - 用例编排
│       └── AiQueryApplicationService.java
├── infrastructure/
│   ├── external/ai/                   # 基础设施 - AI 客户端
│   │   └── SpringAiAlibabaClient.java
│   └── persistence/mapper/
│       └── AiQueryHistoryMapper.java
└── interfaces/
    └── web/controller/ai/             # 接口层 - REST API
        └── AiQueryController.java

src/main/resources/
└── mapper/
    └── AiQueryHistoryMapper.xml
```

**Structure Decision**: 遵循现有 DDD 分层架构，复用已有的基础设施配置（数据源、Redis、安全框架）。新代码按业务模块（ai）组织，保持与现有代码风格一致。

## Phase 0: Research Completed

**Research Document**: [research.md](./research.md)

Key findings:
1. **Spring AI Alibaba 1.1.2.0** - 官方 Starter，兼容 Spring Boot 3.x，提供标准 ChatClient API
2. **多层安全防护** - 关键字过滤 + 语法解析校验 + 只读数据源 + 强制 LIMIT
3. **Prompt Engineering** - 动态 Schema 注入 + 业务规则约束，提高 SQL 生成准确率
4. **会话管理** - Redis 缓存对话上下文（30分钟 TTL）+ 数据库持久化历史记录

## Phase 1: Design Completed

### Data Model

**Data Model Document**: [data-model.md](./data-model.md)

Key entities:
1. **AiQueryHistory** - 查询历史记录（持久化到 MySQL）
2. **DatabaseSchemaContext** - 数据库 Schema 元数据（运行时动态获取）
3. **AiConversationContext** - 对话上下文（Redis 缓存）

### API Contracts

**API Contracts Document**: [contracts/api-contracts.md](./contracts/api-contracts.md)

Endpoints:
- `POST /api/ai/query` - 执行自然语言查询
- `GET /api/ai/history` - 获取查询历史列表
- `GET /api/ai/history/{id}` - 获取单条查询详情
- `DELETE /api/ai/history/{id}` - 删除查询历史

### Quick Start Guide

**Quickstart Document**: [quickstart.md](./quickstart.md)

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design.*

### API Performance Gates ✅
- 异步查询模式：提交立即返回，结果通过轮询或 WebSocket 获取，满足 200ms 响应要求
- 强制 LIMIT 50 保证数据库查询性能
- AI 服务调用配置 10s 超时和熔断机制

### Test Coverage Gates ✅
- Unit tests: SqlSecurityValidator (100%), SqlGenerationService (90%), DatabaseSchemaService (85%)
- Integration tests: 完整查询流程、安全场景测试、历史记录 CRUD
- Security tests: SQL 注入尝试、越权访问、认证绕过测试

## Complexity Tracking

> **No violations detected. All design decisions align with Constitution requirements.**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | - | - |

## Next Steps

Run `/speckit-tasks` to generate actionable implementation tasks based on this plan.
