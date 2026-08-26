# Tasks: 020 ESI 客户端重复代码消除（Phase 1）

**Input**: Design documents from `/specs/020-esi-client-dedup/`
**Prerequisites**: plan.md ✅, spec.md ✅（G1 + G2 已通过）

**Tests**: 包含测试任务（TDD 铁律要求）

**Organization**: 重构任务按实施阶段组织（非 User Story），依赖排序：T1 → T2 → T3 → T4 → T5 → T6

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions

---

## Phase 1: Setup (N/A)

**Purpose**: 无（重构任务无需项目初始化）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 提取公共常量和工具方法，为后续 API 类改造奠定基础

**⚠️ CRITICAL**: 本阶段完成前不得开始 API 类改造

- [ ] T001 新建 EsiClientConstants 常量类 in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/EsiClientConstants.java`
  - 提取 `public static final Duration ESI_DATA_TIMEOUT = Duration.ofSeconds(5);`
  - 私有构造器禁止实例化
  - Javadoc 说明常量用途（宪法：外部调用 5s 超时）

**Checkpoint**: 常量类就绪，可被 API 类引用

---

## Phase 3: 工具方法增强 + TDD 测试

**Purpose**: 增强 EsiStatusUtil，提供 `applyErrorHandlers` 和 `handleResponseWithHeaders` 工具方法，并编写单元测试（TDD 铁律：先写测试再改造）

**Independent Test**: `mvn test -Dtest=EsiStatusUtilTest` 全部通过

### Tests (MUST WRITE FIRST - TDD) ⚠️

- [ ] T002 编写 EsiStatusUtil 工具方法单元测试 in `src/test/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/EsiStatusUtilTest.java`
  - 测试 `applyErrorHandlers(ResponseSpec)`:
    - 2xx 响应正常通过
    - 4xx 通用错误 → EsiException(ESI_AUTHORIZATION_FAILURE)
    - 403 特判 → EsiException(ESI_AUTH_PERMISSION_LOW)
    - 5xx 服务端错误 → EsiException(ESI_SERVER_FAILURE)
  - 测试 `handleResponseWithHeaders(ClientResponse, ParameterizedTypeReference, String)`:
    - 2xx 响应 → Mono<ResponseEntity<T>> 正常返回
    - 4xx/403/5xx 分支同上
    - 验证日志格式 `"ESI {apiName} 权限不足(403)"` 和 `"ESI {apiName} 服务端错误(5xx): status={}"`
  - 使用 Mockito mock WebClient/ClientResponse/ResponseSpec

**Checkpoint**: 测试已编写并确认失败（RED 阶段）

### Implementation

- [ ] T003 实现 EsiStatusUtil 工具方法 in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/EsiStatusUtil.java`
  - 新增 `public static ResponseSpec applyErrorHandlers(ResponseSpec spec)`
  - 新增 `public static <T> Mono<ResponseEntity<T>> handleResponseWithHeaders(ClientResponse response, ParameterizedTypeReference<T> typeRef, String apiName)`
  - 运行 `mvn test -Dtest=EsiStatusUtilTest` 确认测试通过（GREEN 阶段）
  - 重构优化（REFACTOR 阶段）

**Checkpoint**: 工具方法就绪，单元测试通过，可被 API 类调用

---

## Phase 4: API 类改造（分批执行）

**Purpose**: 改造 33 个 API 类，替换重复的 onStatus 和 exchangeToMono 模式

**Independent Test**: 每改造 5-10 个类后运行 `mvn test` 验证行为等价

### Batch 1: AlliancesApi ~ CharacterApi (6 个类) [P]

- [ ] T004 [P] 改造 AlliancesApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/AlliancesApi.java`
  - 替换 `.onStatus().onStatus()` → `.as(EsiStatusUtil::applyErrorHandlers)`
  - 替换 `exchangeToMono` 块 → `EsiStatusUtil.handleResponseWithHeaders(...)`
  - 替换 `private static final Duration ESI_DATA_TIMEOUT` → `import static EsiClientConstants.ESI_DATA_TIMEOUT`

- [ ] T005 [P] 改造 AssetsApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/AssetsApi.java`
  - 同上改造规则

- [ ] T006 [P] 改造 BookmarksApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/BookmarksApi.java`
  - 同上改造规则

- [ ] T007 [P] 改造 CalendarApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/CalendarApi.java`
  - 同上改造规则

- [ ] T008 [P] 改造 CharacterApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/CharacterApi.java`
  - 同上改造规则

- [ ] T009 运行 Batch 1 验证: `mvn test` in repository root
  - 确认 0 failures, 0 errors
  - 如有失败，回退并修复

### Batch 2: CorporationApi ~ MarketApi (5 个类) [P]

- [ ] T010 [P] 改造 CorporationApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/CorporationApi.java`
- [ ] T011 [P] 改造 IndustryApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/IndustryApi.java`
- [ ] T012 [P] 改造 MarketApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/MarketApi.java`
- [ ] T013 [P] 改造 SovereigntyApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/SovereigntyApi.java`
- [ ] T014 [P] 改造 UniverseApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/UniverseApi.java`
- [ ] T015 运行 Batch 2 验证: `mvn test`

### Batch 3: WalletApi ~ WarApi + PageTotalApi (4 个类) [P]

- [ ] T016 [P] 改造 WalletApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/WalletApi.java`
- [ ] T017 [P] 改造 WarApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/WarApi.java`
- [ ] T018 [P] 改造 PageTotalApi in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/PageTotalApi.java`
  - 特殊处理：含 `.block()` 场景，确保 `handleResponseWithHeaders` 兼容
- [ ] T019 [P] 改造其余 API 类（RouteApi, SearchApi, SkillApi, StatusApi 等）in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/*.java`
  - 使用 `ls api/*.java | wc -l` 确认总数为 33
- [ ] T020 运行 Batch 3 验证: `mvn test`

**Checkpoint**: 所有 33 个 API 类改造完成，测试通过

---

## Phase 5: EsiApiService 改造

**Purpose**: 替换 EsiApiService 中的 ESI_DATA_TIMEOUT 引用（如有）

- [ ] T021 改造 EsiApiService in `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/gateway/esi/EsiApiService.java`
  - 检查是否有 `private static final Duration ESI_DATA_TIMEOUT` 声明
  - 如有，替换为 `import static EsiClientConstants.ESI_DATA_TIMEOUT`
  - 运行 `mvn test` 确认无回归

**Checkpoint**: EsiApiService 改造完成

---

## Phase 6: 全量验证 (Evidence Gate)

**Purpose**: 证据铁律执行区，运行完整验证并产出报告

- [ ] T022 构建验证: `mvn clean package -DskipTests` → exit 0
- [ ] T023 测试验证: `mvn test` → 0 failures, 0 errors
- [ ] T024 遗漏检查: `grep -r ".onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())" src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/` → 无匹配
- [ ] T025 遗漏检查: `grep -r "private static final Duration ESI_DATA_TIMEOUT" src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/` → 无匹配
- [ ] T026 Diff 审查: `git diff --stat` 确认无计划外变更
- [ ] T027 产出 VERIFICATION REPORT（格式见 `ecc:verification-loop`）

**Checkpoint**: VERIFICATION REPORT = READY，可进入 P6 评审

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: 代码清理与文档更新

- [ ] T028 代码清理: 删除未使用的 import、优化 Javadoc
- [ ] T029 更新 knowledge 文档 in `docs/knowledge/`（如有需要）
- [ ] T030 提交所有变更: `git add -A && git commit -m "refactor(020): ESI 客户端重复代码消除 (Phase 1)"`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 2 (Foundational)**: 无依赖，立即开始
- **Phase 3 (工具方法 + TDD)**: 依赖 Phase 2（T001 完成）
- **Phase 4 (API 类改造)**: 依赖 Phase 3（T002 + T003 完成）
- **Phase 5 (EsiApiService)**: 依赖 Phase 2（T001 完成），可与 Phase 4 并行
- **Phase 6 (全量验证)**: 依赖 Phase 4 + Phase 5 完成
- **Final Phase (Polish)**: 依赖 Phase 6 完成

### Task Dependencies

```
T001 (常量) → T002 (测试) → T003 (工具方法实现) → T004-T020 (API 类改造，分批)
                                                ↘
                                                  T021 (EsiApiService) → T022-T027 (验证) → T028-T030 (Polish)
```

### Parallel Opportunities

- **Batch 1 (T004-T008)**: 6 个 API 类可并行改造（不同文件）
- **Batch 2 (T010-T014)**: 5 个 API 类可并行改造
- **Batch 3 (T016-T019)**: 4 个 API 类可并行改造
- **Phase 5 (T021)**: 可与 Phase 4 并行（依赖 T001 即可）

---

## Implementation Strategy

### MVP First (Phase 2 + 3 Only)

1. Complete Phase 2: T001（常量类）
2. Complete Phase 3: T002 + T003（工具方法 + 测试）
3. **STOP and VALIDATE**: 运行 `mvn test -Dtest=EsiStatusUtilTest` 确认通过
4. 此时可暂停，工具方法已就绪，API 类改造可延后

### Incremental Delivery

1. Phase 2 + 3 → 工具方法就绪（MVP!）
2. Phase 4 Batch 1 → 改造 6 个类 → 测试验证
3. Phase 4 Batch 2 → 改造 5 个类 → 测试验证
4. Phase 4 Batch 3 → 改造剩余类 → 测试验证
5. Phase 5 + 6 → 全量验证 → VERIFICATION REPORT

### Parallel Team Strategy

With multiple developers:

1. Team completes Phase 2 + 3 together（工具方法 + TDD）
2. Once Phase 3 is done:
   - Developer A: Batch 1 (T004-T008)
   - Developer B: Batch 2 (T010-T014)
   - Developer C: Batch 3 (T016-T019) + Phase 5 (T021)
3. Phase 6 全量验证由 Team 共同执行

---

## Testing & Quality Gates

### Test Coverage Requirements (MUST)
- `EsiStatusUtilTest` 覆盖率 ≥90%（工具方法是核心，需高覆盖）
- 现有 `EsiApiServiceTest`、`EsiApiServiceUnitTest` 保持不变（集成测试验证行为等价）
- 整体覆盖率 ≥80%（宪法要求）

### Test Implementation Order
1. T002: 编写 EsiStatusUtil 单元测试（RED）
2. T003: 实现工具方法（GREEN）
3. T009/T015/T020: 每批次 API 类改造后运行 `mvn test`（回归验证）
4. T023: 全量测试（最终验证）

### Quality Assurance
- 所有代码变更必须通过 `mvn test`
- T024-T025 grep 验证无遗漏
- T027 产出 VERIFICATION REPORT
- P6 评审：`ecc:java-reviewer` 必须通过

---

## Notes

- [P] tasks = different files, no dependencies
- 重构任务无 User Story 标签，按实施阶段组织
- TDD 铁律：T002 必须先于 T003（先写测试再实现）
- 每批次改造后必须运行 `mvn test` 验证
- Commit after each batch or logical group
- Stop at any checkpoint to validate independently
