---

description: "T3 DDD 深度架构重构任务清单"
---

# Tasks: T3 DDD 深度架构重构——消除五层依赖违规根因

**Input**: Design documents from `/specs/004-ddd-layering-purity/`
**Prerequisites**: plan.md (required), spec.md (required)

**Organization**: Tasks grouped by 5 user stories(对应 5 阶段重构),每阶段以编译+测试+grep 验证为完成门槛,行为等价。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件、无依赖)
- **[Story]**: US1(ESI下沉) / US2(Redis端口) / US3(PO映射下沉) / US4(仓储契约) / US5(SysUser+异步)

## Path Conventions

- 单 Maven 项目:`src/main/java/xyz/foolcat/eve/evehelper/` 下分层 domain/application/infrastructure/interfaces/shared

---

## Phase 1: Setup(共享基础设施)

**Purpose**: 建立领域端口承载结构

- [X] T001 [P] 创建 `domain/port/esi/EsiGateway.java` 与 `domain/port/cache/CacheGateway.java` 端口接口骨架(空接口,后续填充)
- [X] T002 [P] 创建 `infrastructure/cache/RedisCacheGateway.java` 适配器骨架

---

## Phase 2: Foundational(阻塞性前置——定义端口契约)

**⚠️ CRITICAL**: 端口契约必须先定义,所有用户故事才能开始

- [X] T003 定义 `EsiGateway` 接口方法签名(从 `domain/service/esi/EsiApiService.java` 现有被调用的能力提取:ESI 鉴权/字符/资产/市场/工业等,避免过度抽象)
- [X] T004 定义 `CacheGateway` 接口方法签名(从 `TokenService`/`LoginRateLimiterService`/`TokenBlacklistService`/`SysPermissionService` 现有 RedisTemplate 用法提取:get/set/setIfAbsent/delete/hasKey/increment/expire/opsForHash/convertAndSend)
- [X] T005 在 `infrastructure/cache/RedisCacheGateway.java` 实现 `CacheGateway`(注入 RedisTemplate,封装 Redis 操作)

**Checkpoint**: 端口契约就绪,可开始各用户故事

---

## Phase 3: User Story 1 - ESI 外部访问下沉防腐层(P1)🎯 MVP

**Goal**: `EsiApiService` 迁入 infrastructure,9 个领域服务改依赖 `EsiGateway`,domain 不再 import `infrastructure.external.esi`
**Independent Test**: `grep "infrastructure.external.esi" domain/` 无命中;既有 ESI 功能行为等价

- [X] T006 [US1] 将 `domain/service/esi/EsiApiService.java` 迁移至 `infrastructure/external/esi/EsiApiService.java`,实现 `EsiGateway`
- [X] T007 [P] [US1] 修改 `domain/service/system/AssetsService.java` 依赖 `EsiGateway`)
- [X] T008 [P] [US1] 修改 `domain/service/system/BlueprintsService.java` 依赖 `EsiGateway`
- [X] T009 [P] [US1] 修改 `domain/service/system/IndustryJobService.java` 依赖 `EsiGateway`
- [X] T010 [P] [US1] 修改 `domain/service/system/MiningDetailService.java` 依赖 `EsiGateway`
- [X] T011 [P] [US1] 修改 `domain/service/system/StructureService.java` 依赖 `EsiGateway`
- [X] T012 [P] [US1] 修改 `domain/service/system/UniverseNameService.java` 依赖 `EsiGateway`
- [X] T013 [P] [US1] 修改 `domain/service/system/WalletJournalService.java` 依赖 `EsiGateway`
- [X] T014 [P] [US1] 修改 `domain/service/system/InvTypesService.java` 依赖 `EsiGateway`
- [X] T015 [US1] 全量 `./mvnw compile` + `test-compile` 通过;grep 验证 domain 无 `infrastructure.external.esi` import

**Checkpoint**: US1 完成,domain→infrastructure ESI 依赖消除

---

## Phase 4: User Story 2 - Redis 经缓存端口抽象(P2)

**Goal**: 5 个领域服务改注 `CacheGateway`,domain 不再 import `RedisTemplate`
**Independent Test**: `grep "RedisTemplate" domain/` 无命中;缓存 key/TTL 语义不变

- [X] T016 [P] [US2] 修改 `domain/service/security/TokenService.java` 注入 `CacheGateway` 替代 RedisTemplate
- [X] T017 [P] [US2] 修改 `domain/service/security/TokenBlacklistService.java` 注入 `CacheGateway`
- [X] T018 [P] [US2] 修改 `domain/service/security/LoginRateLimiterService.java` 注入 `CacheGateway`
- [X] T019 [P] [US2] 修改 `domain/service/system/SysPermissionService.java` 注入 `CacheGateway`
- [X] T020 [US2] 迁移后的 `infrastructure/external/esi/EsiApiService.java` 注入 `CacheGateway`
- [X] T021 [US2] 全量编译+测试通过;grep 验证 domain 无 `RedisTemplate` import

**Checkpoint**: US2 完成,domain→Redis 依赖消除

---

## Phase 5: User Story 3 - PO↔Domain 映射下沉 infrastructure(P2)

**Goal**: 26 个 application assembler 不再 import `*PO`,PO 映射收归仓储实现
**Independent Test**: `grep "infrastructure.persistence.entity.*PO" application/` 无命中;application↔infrastructure 循环消解

- [X] T022 [P] [US3] 将各 `application/assembler/system/*Assembler.java` 中 `domain2Po`/`po2Domain` 方法下沉至对应 `infrastructure/persistence/repository/system/*RepositoryImpl.java`(或独立 infra assembler)
- [X] T023 [P] [US3] 将 `application/assembler/eve/{IndustryActivityMaterials,IndustryActivityProducts,IndustryBlueprints}Assembler.java` 的 PO 映射下沉至对应 eve RepositoryImpl
- [X] T024 [US3] 精简 application assembler 仅保留 Domain↔DTO/VO 映射;编译+测试通过;grep 验证 application 无 `*PO` import

**Checkpoint**: US3 完成,application↔infrastructure 循环消除

---

## Phase 6: User Story 4 - 仓储接口契约净化(P2)

**Goal**: 领域仓储接口返回领域实体/读模型,不再 import application DTO / interfaces VO
**Independent Test**: `grep "application.dto\|interfaces.web.vo" domain/repository domain/service` 无命中

- [X] T025 [US4] 修改 `domain/repository/system/BlueprintsRepository.java` 返回类型(移除 `application.dto.BlueprintsDTO`/`PageQuery` 依赖)—— 新增 `domain/model/query/BlueprintsPageCriteria` 承载查询条件;`BlueprintsVO` 迁至 `application/dto/response`
- [X] T026 [US4] 修改 `domain/repository/system/BlueprintsDataRepository.java` 返回类型(移除 `BlueprintCostDTO`/`BlueprintFormulaDTO`)
- [X] T027 [US4] 修改 `domain/repository/system/MarketOrderRepository.java` 返回类型(移除 `MarketOrderDTO`)
- [X] T028 [US4] 修改 `domain/repository/system/InvTypesRepository.java` 与 `MarketGroupsRepository.java`(移除 interfaces VO)
- [X] T029 [US4] 将 `infrastructure/persistence/mapper/system/InvTypesMapper.xml` 中 `interfaces.web.vo.InvTypesVO` resultType 改为 PO/读模型
- [X] T030 [US4] application 层组装器承接 实体→DTO/VO 转换;编译+测试通过;grep 验证 domain 无上层类型 import

**Checkpoint**: US4 完成,domain 契约净化

---

## Phase 7: User Story 5 - 安全适配器解耦 + 异步任务归位(P3)

**Goal**: `SysUser` 移除 `UserDetails`;`domain/service/thread` 迁出 domain
**Independent Test**: domain 实体无 `UserDetails` import;`domain/service/thread` 为空

- [ ] T031 [US5] 修改 `domain/model/entity/system/SysUser.java` 移除 `implements UserDetails`,新增 `domain/port/UserDetailsPort` 或 infra 适配器提供 Spring Security 契约
- [ ] T032 [US5] 将 `domain/service/thread/MarketOrderAsyncService.java` 迁至 application 或 infrastructure
- [ ] T033 [US5] 编译+测试通过;grep 验证 domain 实体无 `UserDetails`、`domain/thread` 为空

**Checkpoint**: US5 完成

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 全量依赖扫描验证与文档

- [ ] T034 [P] 全项目 grep 无禁止跨层 import(domain→infrastructure 客户端/PO、domain→application 类型、application→infrastructure PO、仓储返回上层 DTO/VO)
- [ ] T035 [P] 更新记忆/文档登记 T3 完成状态
- [ ] T036 全量 `./mvnw compile` + `test-compile` 通过,确认零功能回归

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup(T1-2)**: 无依赖,可立即开始
- **Foundational(T3-5)**: 依赖 Setup,阻塞所有用户故事
- **US1(T6-15)**: 依赖 Foundational,无跨故事依赖
- **US2(T16-21)**: 依赖 Foundational + US1 完成(EsiApiService 迁移后)
- **US3(T22-24)**: 依赖 Foundational,可与 US1/US2 并行
- **US4(T25-30)**: 依赖 Foundational + US3(PO 映射下沉后)
- **US5(T31-33)**: 依赖 Foundational,可与 US1-4 并行
- **Polish(T34-36)**: 依赖所有用户故事完成

### 关键依赖链

- US1 → US2(TokenService 等依赖 EsiApiService 注入 CacheGateway)
- US3 → US4(仓储实现吸收 PO 映射后,接口契约才可净化)

### 并行机会

- T001/T002、T006-T014、T016-T020、T022-T023、T031-T032 均可并行
- 在 Foundational 完成后,US1/US3/US5 可并行启动

---

## Implementation Strategy

### MVP First(US1 优先)

1. Phase1 Setup → Phase2 Foundational(端口契约)
2. Phase3 US1(ESI 下沉)——最大依赖违规项,先攻
3. 验证 US1 独立可测后继续

### Incremental Delivery

1. 完成 Setup+Foundational → 端口就绪
2. US1 → 验证 → 继续
3. US2 → ... → US5
4. 每阶段独立编译+测试+grep 验证,独立提交

---

## Testing & Quality Gates

- 每阶段完成门槛:`./mvnw compile` + `./mvnw test-compile` 通过
- 行为等价:既有 ESI/缓存/鉴权/限流功能无回归
- 依赖合规:grep 无禁止跨层 import
- 新增端口/适配器逻辑:单测 ≥80%

## Notes

- 行为等价为硬约束,不新增功能、不改 REST 契约、不引入新依赖(技术栈冻结)
- 测试代码需同步更新被迁移类型的 import
- 每阶段独立提交,避免大改难回滚
- 本任务为内部重构,无用户故事面向终端用户;"[P]"标注基于文件无冲突原则