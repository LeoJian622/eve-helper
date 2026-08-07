# Tasks: 用户账户 ESI 授权状态返回

**Input**: Design documents from `/specs/003-esi-auth-status/`
**Prerequisites**: [plan.md](plan.md)(必需)、[spec.md](spec.md)(必需)、[research.md](research.md)、[data-model.md](data-model.md)、[contracts/api-contract.md](contracts/api-contract.md)、[quickstart.md](quickstart.md)

**Tests**: 宪法强制 TDD(Red-Green-Refactor),测试任务必须先写并确认失败,再实现。

**Organization**: 按用户故事分组,便于独立实现与测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件,无未完成依赖)
- **[Story]**: 归属用户故事(US1/US2)
- 描述含确切文件路径

---

## Phase 1: Setup

**Purpose**: 确认分支基线可构建

- [X] T001 验证基线构建通过:`mvn clean package -DskipTests`(分支 `003-esi-auth-status`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 所有用户故事依赖的枚举基础

**⚠️ CRITICAL**: US1/US2 均依赖此阶段完成

- [X] T002 [P] 创建 `EsiAuthStatus` 枚举于 `src/main/java/xyz/foolcat/eve/evehelper/shared/kernel/enums/EsiAuthStatus.java`:四值 `AUTHORIZED("AUTHORIZED","授权正常")` / `EXPIRED("EXPIRED","授权过期")` / `NOT_AUTHORIZED("NOT_AUTHORIZED","未授权")` / `UNKNOWN("UNKNOWN","无法判定")`;`code`+`description` 双字段构造,`getCode()`/`getDescription()`;风格对齐既有 `CorporationActivityEnum`

**Checkpoint**: 枚举就绪,可进入用户故事

---

## Phase 3: User Story 1 - 返回绑定角色 ESI 授权状态 (Priority: P1) 🎯 MVP

**Goal**: `addUser` 接口每个返回条目附带 `authStatus`,覆盖 AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN 四态
**Independent Test**: 调用 `GET /user/{userId}`,断言每条目含 `authStatus` 且取值合法;单元测试 Mock ESI 覆盖四态

### Tests for User Story 1(先写,确认失败)

- [X] T003 [P] [US1] 单元测试 `EsiApiServiceTest` 于 `src/test/java/xyz/foolcat/eve/evehelper/domain/service/esi/EsiApiServiceTest.java`:refreshToken 为空 -> `NOT_AUTHORIZED`
- [X] T004 [P] [US1] 单元测试(同上文件):Mock `authorizeOAuth.updateAccessToken` 返回 `AuthTokenResponse` -> `AUTHORIZED`,并验证回写新 refreshToken 与缓存 accessToken
- [X] T005 [P] [US1] 单元测试(同上文件):Mock 抛 `EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE)` -> `EXPIRED`
- [X] T006 [P] [US1] 单元测试(同上文件):状态缓存命中(Redis 有 `esi_auth_status:{characterId}`)-> 直接返回缓存态,不调用 `authorizeOAuth`

### Implementation for User Story 1

- [X] T007 [P] [US1] 在 `src/main/java/xyz/foolcat/eve/evehelper/application/dto/UserAccountDTO.java` 新增字段 `private EsiAuthStatus authStatus;`(含 `@Schema` 注解)
- [X] T008 [US1] 在 `src/main/java/xyz/foolcat/eve/evehelper/domain/service/esi/EsiApiService.java` 实现 `EsiAuthStatus getAuthorizationStatus(EveAccount account)`:①refreshToken 空返回 `NOT_AUTHORIZED`;②查 Redis 状态缓存(`esi_auth_status:{characterId}`),命中即返回;③否则 `authorizeOAuth.updateAccessToken(REFRESH_TOKEN, token).timeout(Duration.ofSeconds(5)).block()`,成功->解析 JWT 取 characterId、缓存 accessToken 19min、回写新 refreshToken(`eveAccountService.insertOrUpdate`)、返回 `AUTHORIZED`;④`EsiException(ESI_AUTHORIZATION_FAILURE)`->`EXPIRED`;⑤其余异常(含 `ESI_SERVER_FAILURE`/网络/超时)->`UNKNOWN`;⑥缓存状态(AUTHORIZED/EXPIRED 5min,UNKNOWN 30s)。依赖 T002
- [X] T009 [US1] 在 `src/main/java/xyz/foolcat/eve/evehelper/application/service/UserApplicationService.java` 新增 `List<UserAccountDTO> queryAccountListWithAuthStatus(Integer userId)`:标注 `@Transactional(propagation = NOT_SUPPORTED)`;调用 `eveAccountService.getAccountList(userId)`->`eveAccountAssembler.domain2UserAccountTO`->为每条 `EveAccount` 调 `esiApiService.getAuthorizationStatus` 并 set 到 DTO;注入 `EsiApiService` 与 `EveAccountAssembler`。依赖 T007、T008
- [X] T010 [US1] 修改 `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/UserController.java` 的 `addUser(@PathVariable Integer userId)`:改为 `return Result.success(userApplicationService.queryAccountListWithAuthStatus(userId));`(移除控制器内的 assembler 调用)。依赖 T009

**Checkpoint**: US1 功能完整,可独立验证四态

---

## Phase 4: User Story 2 - 状态判定失败的优雅降级 (Priority: P2)

**Goal**: 多角色并行判定,单角色异常不影响其他角色;空列表正常返回
**Independent Test**: 多角色中 1 个 ESI 异常,其余状态正确,接口仍成功;无角色时返回空列表

### Tests for User Story 2(先写,确认失败)

- [X] T011 [P] [US2] 单元测试 `EsiApiServiceTest`:Mock 抛 `EsiException(ResultCode.ESI_SERVER_FAILURE)` 与 `TimeoutException` -> `UNKNOWN`
- [X] T012 [P] [US2] 单元测试 `UserApplicationServiceTest` 于 `src/test/java/xyz/foolcat/eve/evehelper/application/service/UserApplicationServiceTest.java`:3 个角色,其中 1 个 `getAuthorizationStatus` 抛异常被隔离为 `UNKNOWN`,另 2 个状态正确;验证并行执行(总耗时约等于最慢单调用,非累加)
- [ ] T013 [P] [US2] 集成测试 `UserControllerAuthStatusIT` 于 `src/test/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/UserControllerAuthStatusIT.java`:用户无绑定角色 -> `data=[]` 且成功
- [ ] T014 [P] [US2] 集成测试(同上文件):多角色混合状态(AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN)端到端验证

### Implementation for User Story 2

- [X] T015 [US2] 在 `queryAccountListWithAuthStatus` 中实现并行冷路径:以 `CompletableFuture.supplyAsync` 提交到有界线程池(复用 `AsyncConfiguration` 执行器或新增专用有界池),`CompletableFuture.allOf` 等待;每角色 `getAuthorizationStatus` 内部 try-catch 兜底(任何未预期异常->`UNKNOWN`),确保单角色异常不传播。依赖 T009
- [X] T016 [US2] 验证 `getAuthorizationStatus` 的 catch 链覆盖所有异常路径(`EsiException` 按 ResultCode 分流 + 兜底 `Exception`->`UNKNOWN`),无异常外泄。依赖 T008

**Checkpoint**: US1 与 US2 均独立可用

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: 质量门禁与验证

- [ ] T017 运行 `mvn test`,确认全绿且新增代码覆盖率 ≥80%
- [X] T018 [P] `ecc:java-reviewer` 评审,修复 CRITICAL/HIGH
- [X] T019 [P] `ecc:security-reviewer` 评审(涉及认证/外部 API),修复 CRITICAL/HIGH
- [ ] T020 执行 [quickstart.md](quickstart.md) 验证矩阵,手动确认 `authStatus` 字段

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: 无依赖,立即开始
- **Foundational (Phase 2)**: 依赖 Phase 1;阻塞所有用户故事
- **US1 (Phase 3)**: 依赖 Phase 2(T002 枚举)
- **US2 (Phase 4)**: 依赖 US1 的 `getAuthorizationStatus` 与 `queryAccountListWithAuthStatus`(T008/T009)
- **Polish (Phase 5)**: 依赖 US1+US2 完成

### Within Each User Story
- 测试先写并确认失败 -> 实现 -> 测试转绿
- 枚举(T002)-> DTO(T007)-> 域服务方法(T008)-> 应用服务(T009)-> 控制器(T010)

### Parallel Opportunities
- T003–T006(US1 测试)同文件,按顺序写但可一次性生成
- T007(DTO)与 T008(域服务)不同文件,可并行
- T011–T014(US2 测试)不同文件,可并行
- T018/T019(评审)可并行

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. T001 基线构建 -> T002 枚举 -> T003–T006 测试(失败)-> T007–T010 实现 -> 测试转绿
2. **STOP and VALIDATE**: `GET /user/{userId}` 返回含 `authStatus` 的列表,四态正确

### Incremental Delivery
1. US1 完成 -> 四态可判定(MVP)
2. US2 完成 -> 并行 + 隔离 + 空列表(韧性)
3. Polish -> 评审 + 覆盖率 + 验证

---

## Testing & Quality Gates

- 单元覆盖 ≥80%(宪法门禁,阻塞合并)
- 集成测试覆盖四态 + 空列表 + 多角色隔离
- 安全测试:复用既有 JWT/RBAC,验证接口仍受保护
- 性能:状态缓存命中路径 <200ms;单角色 ESI 刷新 5s 超时

## Notes
- 测试先写并确认失败再实现(宪法 TDD)
- 每个任务或逻辑分组后提交
- [P] 任务 = 不同文件、无未完成依赖
- 避免跨故事依赖破坏独立性
