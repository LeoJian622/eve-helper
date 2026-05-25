# Tasks: Spring AI 数据库查询 Agent

**Input**: Design documents from `/specs/002-spring-ai-db-agent/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-contracts.md, quickstart.md

**Tests**: This feature includes tests per Constitution requirements (≥80% unit test coverage required)
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maven dependencies and configuration setup

- [X] T001 [P] Add Spring AI Alibaba 1.1.2.0 and JSqlParser 4.9 dependencies to pom.xml
- [X] T002 [P] Add AI query configuration properties class in src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/ai/AiQueryProperties.java
- [X] T003 [P] Create database SQL migration script for ai_query_history table in src/main/resources/db/migration/V1__ai_query_history.sql

**Checkpoint**: Dependencies and basic configuration in place

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 [P] Create AiQueryHistory domain entity in src/main/java/xyz/foolcat/eve/evehelper/domain/model/entity/system/AiQueryHistory.java
- [X] T005 [P] Create AiQueryHistory PO (persistence object) in src/main/java/xyz/foolcat/eve/evehelper/infrastructure/persistence/entity/AiQueryHistoryPO.java
- [X] T006 [P] Create AiQueryHistoryMapper interface in src/main/java/xyz/foolcat/eve/evehelper/infrastructure/persistence/mapper/AiQueryHistoryMapper.java
- [X] T007 [P] Create MyBatis mapper XML in src/main/resources/mapper/AiQueryHistoryMapper.xml
- [X] T008 [P] Create AiQueryHistoryAssembler (MapStruct) in src/main/java/xyz/foolcat/eve/evehelper/application/assembler/AiQueryHistoryAssembler.java
- [X] T009 [P] Create DTOs: AiQueryRequest, AiQueryResponse, QueryHistoryResponse in src/main/java/xyz/foolcat/eve/evehelper/application/dto/ai/
- [X] T010 Create AiQueryHistoryRepository interface in src/main/java/xyz/foolcat/eve/evehelper/domain/repository/system/AiQueryHistoryRepository.java
- [X] T011 Create AiQueryHistoryRepositoryImpl in src/main/java/xyz/foolcat/eve/evehelper/infrastructure/persistence/repository/AiQueryHistoryRepositoryImpl.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - 自然语言查询数据库 (Priority: P1) 🎯 MVP

**Goal**: 用户通过中文自然语言提问，系统自动转换为 SQL 并执行，返回最多 50 条查询结果

**Independent Test**: 发送 POST /api/ai/query 请求，验证系统能够：1) 调用 AI 生成 SQL 2) 执行安全校验 3) 执行查询返回结果

### Tests for User Story 1 ⚠️

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T012 [P] [US1] Unit test for SqlSecurityValidator in src/test/java/xyz/foolcat/eve/evehelper/domain/service/ai/SqlSecurityValidatorTest.java
- [X] T013 [P] [US1] Unit test for DatabaseSchemaService in src/test/java/xyz/foolcat/eve/evehelper/domain/service/ai/DatabaseSchemaServiceTest.java
- [X] T014 [P] [US1] Integration test for complete query flow in src/test/java/xyz/foolcat/eve/evehelper/AiQueryIntegrationTest.java

### Implementation for User Story 1

- [X] T015 [P] [US1] Create SqlSecurityValidator (SQL 安全校验器) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/SqlSecurityValidator.java
- [X] T016 [P] [US1] Create DatabaseSchemaService (数据库 Schema 元数据服务) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/DatabaseSchemaService.java
- [X] T017 [P] [US1] Create MockAiChatClient (Mock AI 客户端封装) in src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/ai/MockAiChatClient.java
- [X] T018 [US1] Create SqlGenerationService (SQL 生成服务) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/SqlGenerationService.java (depends on T016, T017)
- [X] T019 [US1] Create QueryExecutionService (SQL 执行服务) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/QueryExecutionService.java (depends on T015)
- [X] T020 [US1] Create AiQueryApplicationService (应用服务编排) in src/main/java/xyz/foolcat/eve/evehelper/application/service/ai/AiQueryApplicationService.java (depends on T018, T019, T011)
- [X] T021 [US1] Create AiQueryController (REST API) in src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/ai/AiQueryController.java (depends on T020)
- [X] T022 [US1] Add Spring Security configuration for AI endpoints in existing SecurityConfig (集成在现有Security配置中)
- [X] T023 [US1] Add logging and error handling for AI query flow (已在各服务中添加日志)
- [X] T024 [US1] Create DashscopeAiChatClient (通义千问AI客户端实现) in src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/ai/DashscopeAiChatClient.java
- [X] T025 [US1] Configure dual datasource for AiQueryHistory (双数据源配置，使用system数据源存储查询历史)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Run the quickstart validation.

---

## Phase 4: User Story 2 - 查询结果可视化展示 (Priority: P2)

**Goal**: 系统支持以表格、图表等多种形式展示查询结果，帮助用户更好地理解数据

**Independent Test**: 执行查询后，验证返回的结构化数据可被前端正确渲染为表格或图表格式

### Tests for User Story 2 ⚠️

- [X] T024 [P] [US2] Unit test for result formatting in src/test/java/xyz/foolcat/eve/evehelper/domain/service/ai/ResultFormatServiceTest.java (12个测试全部通过)

### Implementation for User Story 2

- [X] T025 [P] [US2] Create ResultFormatService (结果格式化服务) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/ResultFormatService.java
- [X] T026 [US2] Enhance AiQueryResponse with structured result metadata (column types, chart suggestions)
- [X] T027 [US2] Update AiQueryApplicationService to integrate ResultFormatService (depends on T020, T025)
- [X] T028 [US2] Update AiQueryController to return formatted results (自动继承应用服务的格式化输出)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - 查询历史和上下文管理 (Priority: P3)

**Goal**: 系统保存用户的查询历史，并支持基于上下文的连续对话查询

**Independent Test**: 1) 查询 GET /api/ai/history 应返回用户的查询历史 2) 连续两次相关查询应保持上下文理解

### Tests for User Story 3 ⚠️

- [X] T029 [P] [US3] Unit test for QueryHistoryService in src/test/java/xyz/foolcat/eve/evehelper/domain/service/ai/QueryHistoryServiceTest.java (8个测试全部通过)
- [ ] T030 [P] [US3] Integration test for conversation context flow in src/test/java/xyz/foolcat/eve/evehelper/ConversationContextIntegrationTest.java

### Implementation for User Story 3

- [X] T031 [P] [US3] Create QueryHistoryService (查询历史服务) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/QueryHistoryService.java (depends on T011)
- [X] T032 [P] [US3] Create ConversationContextService (会话上下文服务，Redis 缓存) in src/main/java/xyz/foolcat/eve/evehelper/domain/service/ai/ConversationContextService.java
- [X] T033 [US3] Implement GET /api/ai/history endpoint in AiQueryApplicationService (depends on T031)
- [X] T034 [US3] Implement GET /api/ai/history/{id} endpoint in AiQueryApplicationService (depends on T031)
- [X] T035 [US3] Implement DELETE /api/ai/history/{id} endpoint in AiQueryApplicationService (depends on T031)
- [X] T036 [US3] Enhance SqlGenerationService to support conversation context (depends on T018, T032)
- [X] T037 [US3] Update AiQueryApplicationService to integrate context management

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T038 [P] Add AI query rate limiting (30 req/min per user) using existing LoginRateLimiterService pattern
- [ ] T039 [P] Add unit tests for SqlGenerationService (target ≥90% coverage)
- [ ] T040 [P] Add unit tests for AiQueryApplicationService (target ≥80% coverage)
- [ ] T041 Add security tests for SQL injection attempts and forbidden operations
- [ ] T042 Add performance tests to verify AI query completes within 10 seconds
- [ ] T043 Update Swagger/OpenAPI documentation for AI endpoints
- [ ] T044 Verify and update quickstart.md with actual working commands
- [ ] T045 Code cleanup and ensure consistent logging across all AI services

---

## Phase 7: Refactor - 使用 Spring AI Alibaba 原生 Chat Memory

**Purpose**: 重构对话记忆管理，使用框架原生能力替代自定义实现

- [X] T046 [P] Research and verify Spring AI Alibaba Chat Memory API (MessageChatMemory)
- [X] T047 [P] Remove custom ConversationContextService implementation
- [X] T048 [P] Integrate Spring AI Chat Memory into SqlGenerationService
- [X] T049 [P] Configure ChatMemory with InMemory persistence (可扩展为 Redis)
- [X] T050 [P] Update AiQueryApplicationService to use framework Chat Memory
- [ ] T051 [P] Update unit tests for new Chat Memory integration

**Summary**:
- ✅ 使用 Spring AI ChatMemory 接口管理多轮对话
- ✅ 提供 InMemoryChatMemory 实现，易于扩展为 Redis 持久化
- ✅ 记忆管理集成到 SpringAiChatService，自动处理上下文
- ✅ AiQueryApplicationService 简化，移除 ConversationContextService 依赖
- ✅ 新增 API 端点：`DELETE /api/ai/memory/{sessionId}` 和 `GET /api/ai/memory/{sessionId}/count`

---

## Phase 8: Redis Chat Memory 持久化拓展

**Purpose**: 实现基于 Redis 的 ChatMemory 持久化实现，支持分布式环境下的会话记忆

- [X] T052 [P] Create AiMemoryProperties 配置类 (type, ttlMinutes, refreshTtl)
- [X] T053 [P] Implement RedisChatMemory 实现类 (基于 RedisTemplate)
- [X] T054 [P] Update SpringAiChatMemoryConfig 支持条件装配
- [X] T055 [P] Update SpringAiChatService 支持 Redis 模式下的 TTL 刷新

**Summary**:
- ✅ 新增 `spring.ai.memory.type` 配置支持 `inmemory` 和 `redis` 两种模式
- ✅ 新增 `spring.ai.memory.redis-ttl-minutes` 配置会话过期时间
- ✅ `spring.ai.memory.refresh-ttl` 配置访问时是否刷新 TTL
- ✅ 使用 `@ConditionalOnProperty` 实现条件装配，自动选择对应的 ChatMemory 实现
- ✅ Redis 键前缀：`ai:chat:memory:`，便于管理和监控

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models/validators/services can be parallelized within a story
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T003) can run in parallel
- All Foundational tasks (T004-T011) marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task T012: "Unit test for SqlSecurityValidator"
Task T013: "Unit test for DatabaseSchemaService"
Task T014: "Integration test for complete query flow"

# Launch all independent services for User Story 1 together:
Task T015: "Create SqlSecurityValidator"
Task T016: "Create DatabaseSchemaService"
Task T017: "Create SpringAiAlibabaClient"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Core query functionality)
   - Developer B: User Story 2 (Result formatting + visualization)
   - Developer C: User Story 3 (History + context management)
3. Stories complete and integrate independently

---

## Testing & Quality Gates

### Test Coverage Requirements (MUST)
- Unit test coverage must achieve ≥80% (automated gate blocks merge if below)
- Integration tests must cover all critical user journeys
- Security tests must cover authentication, authorization, and data validation
- Performance tests must validate AI queries complete within 10 seconds

### Test Implementation Order
1. Write unit tests first (fail → implement → pass)
2. Write integration tests for user stories
3. Implement feature to make tests pass
4. Measure coverage and optimize as needed

### Quality Assurance
- All code changes must include automated test updates
- SQL security validator must have 100% test coverage (critical security component)
- Test results visible in CI/CD pipeline
- Manual testing of AI query accuracy recommended for edge cases

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
