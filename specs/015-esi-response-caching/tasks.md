---

description: "Task list for ESI 响应缓存特性 (015)"

---

# Tasks: ESI 响应缓存（按失效时间跳过真实调用）

**Input**: Design documents from `/specs/015-esi-response-caching/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)
**Tests**: 按 spec SC-001~SC-004 关键旅程集成测试 + 各组件 TDD 单元测试（本特性为后端缓存机制，测试是 TDD 铁律要求，非可选）

**Organization**: 按 plan Phase A→B→C 组织；每个组件独立 TDD（RED→GREEN→REFACTOR）

## 格式说明
- **[P]**: 可并行（不同文件、无依赖）
- **[USn]**: 归属 user story（映射 spec US1–US5）

---

## Phase 1: 缓存组件层（基础设施 `infra/external/esi/cache/` 新建，4 组件）

**Purpose**: 构建缓存设施核心，US1/2/3 的依赖基础。

- [ ] T001 `[US1]` TDD `EsiCacheTtlExtractor` — 新建 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/cache/EsiCacheTtlExtractor.java`，解析响应头 TTL：`Cache-Control: max-age`（容忍空白/引号，≤0→null）优先，退回 `Expires`（RFC1123，已过期/不可解析→null），皆无→null。
- [ ] T002 `[P] [US1]` TDD `EsiResponseCacheProperties` — 新建 `.../cache/EsiResponseCacheProperties.java`，`@ConfigurationProperties(prefix="eve.esi.cache")`：`forceRefreshWindowMinutes=60`、`forceRefreshLimit=5`、`datasource="serenity"`。
- [ ] T003 `[P] [US2]` TDD `EsiResponseCache`（键构造+命中读）— 新建 `.../cache/EsiResponseCache.java`：键 `esi:resp:{datasource}:{userId|pub}:{normalized-uri}:{page}`；`getList(key, JavaType)` / `getInt(key)`；列表按参数化 JavaType 反序列化（防 LinkedHashMap）；fail-open（异常→null 不阻断）。
- [ ] T004 `[P] [US4]` TDD `ForceRefreshLimiter` — 新建 `.../cache/ForceRefreshLimiter.java`：固定时间窗计数哨兵，`tryAcquire(key, window, limit)`（窗口 setIfAbsent 起 1，increment 超限拒；键过期天然归零）。
- [ ] T005 `[US1]` `EsiResponseCache` 原子占位防穿透 — `touch(key)` 用 `CacheGateway.setIfAbsent` 原子占位，未命中并发合并（补 T003 缺失的防穿透逻辑 + 并发测试）。

**Checkpoint**: 4 组件可用，各自单测全绿。此后 US1–US5 包裹可开始。

---

## Phase 2: EsiApiService 查询包裹（核心改造）

**Purpose**: 12 类业务查询接入缓存判读。每个查询统一模式：命中不真调 / 未命中真调+回填 / 强刷限流 / 公开 pub。

- [ ] T006 `[US1]` 列表通路响应头改造 — 使列表查询可取响应头 TTL：在涉及的 API 类新增"含响应头"入口（`exchangeToMono`+`toEntity` 取 `ResponseEntity`，参照 `PageTotalApi` 先例），原 `Flux<T>` 方法保留。**P4 裁定具体类**（优先资产/工业/钱包/建筑，分步）。
- [ ] T007 `[US1]` EsiApiService 注入缓存组件 — 注入 `EsiResponseCache`、`EsiCacheTtlExtractor`、`ForceRefreshLimiter`、`EsiResponseCacheProperties`，端口签名不变。
- [ ] T008 `[US1]` 资产类包裹 — `queryCharactersAssets` / `queryCorporationsAssets`（列表）与 `*MaxPage`（Integer）。命中不真调、未命中真调+回填、无响应头每次真调。
- [ ] T009 `[US1]` 工业/蓝图类包裹 — `queryCharacterIndustryJobs` / `queryCorporationIndustryJobs` / `queryCorporationBlueprintsMaxPage` / `queryCorporationIndustryJobsMaxPage`。
- [ ] T010 `[US1]` 采矿/建筑类包裹 — `queryCorporationMiningObserver` / `*MaxPage` / `queryCorporationStructures` / `*MaxPage`。
- [ ] T011 `[US1]` 钱包流水/交易类包裹 — `queryCharacter/CorporationWalletJournal`(+MaxPage×2)、`queryCharacter/CorporationWalletTransactions`。
- [ ] T012 `[US5]` 公开数据包裹（pub 共享） — `queryUniverseType` / `queryUniverseNames`：userId 段置 `pub`，二次调用免真调（全局共享）。
- [ ] T013 `[US4]` 强刷限流接入 — 强刷参数据 `ForceRefreshLimiter.tryAcquire`，超限返回现有缓存；公开类不强刷限流。

**Checkpoint**: 12 类查询全部走缓存判读。

---

## Phase 3: 关键旅程集成测试（SC-001~SC-004）

**Purpose**: 端到端验证缓存语义与隔离，满足 P5 验证门禁。

- [ ] T014 `[US1]` 缓存命中不真调 — Mockito 断言同一资源 TTL 内二次调用 API 调用次数 0（SC-001）。
- [ ] T015 `[US2]` 无响应头每次真调 — 无缓存头接口二次调用仍真调、缓存键不存在（SC-004）。
- [ ] T016 `[US3]` 键用户隔离（IDOR） — 两 userId 同一 characterId 互不命中、各自真调；公开 pub 共享。
- [ ] T017 `[US4]` 强刷窗口 — 连续 6 次强刷第 6 次拒；窗口过后归零（SC-003）。
- [ ] T018 `[US5]` 并发不穿透 — 10 并发同一已过期键，真实调用次数 = 1（SC-002）。

---

## Phase 4: 收尾与评审

**Purpose**: 全量验证、代码评审、文档登记。

- [ ] T019 `[P]` `docs/knowledge/` 流程文档 — 按 project 约定生成 ESI 响应缓存流程文档并登记 `docs/INDEX.md`（参照既有 knowledge-doc-convention 记忆）。
- [ ] T020 全量 `./mvnw test` 全绿（证据铁律，P5）。
- [ ] T021 `ecc:java-reviewer` 评审（Java 变更必须） + `ecc:security-reviewer`（缓存键隔离/授权数据必过）→ 修 P6 遗留。

---

## Dependencies & Execution Order

### Phase Dependencies
- **Phase 1**：无依赖，立即开始；T001 与 T002/T003/T004 可并行（不同文件），T005 依赖 T003。
- **Phase 2**：依赖 Phase 1 完成；T006（响应头改造）先行，T007 注入后各包裹任务 T008–T013 可按依赖逐步推进。
- **Phase 3**：依赖 Phase 2 完成。
- **Phase 4**：依赖 Phase 3 完成。

### 并行机会
- Phase 1：T002/T003/T004 可并行（不同缓存组件文件）。
- Phase 2：T008/T009/T010/T011 各自独立文件（不同 API 类包裹），可并行。
- Phase 3：T014/T015/T016/T017/T018 为不同旅程测试，可并行。

### TDD 铁律
每个实现任务先写失败测试（RED）→ 亲眼看失败 → 最小实现（GREEN）→ 亲眼看通过 → REFACTOR。Java 命令 `./mvnw test -Dtest=<Class>#<method>`。

---

## Testing & Quality Gates

### Test Coverage（MUST）
- 新增代码单元测试 ≥80%（A 组 4 组件逐组件 TDD 覆盖）。
- 集成测试覆盖关键旅程 SC-001~SC-004（T014–T018）。
- 安全测试：键用户隔离 IDOR 防护（T016）必过。
- 性能：缓存命中不真调 + 并发不穿透调用数断言（SC-001/002）。

### 完成定义
- 所有任务勾选 + 每个实现任务展示 RED→GREEN 输出。
- Phase 4 T020 全绿 + T021 评审无 Critical/Important。

## Notes

- `exchangeToMono` 响应头改造（T006）是本特性最复杂风险点——需在不破坏既有 `Flux<T>` 语义前提下取得响应头；P4 首步做，先以资产类试点，若方案阻塞回退到"分页用 PageTotalApi、列表独立取头"。
- mark P4 执行模式：`superpowers:subagent-driven-development`（默认）。