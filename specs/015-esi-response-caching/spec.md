# Feature Specification: ESI 响应缓存（按失效时间跳过真实调用）

**Feature Branch**: `015-esi-response-caching`
**Created**: 2026-08-20
**Status**: Draft（待门禁 G1 批准）
**Input**: 用户描述 —— ESI 服务器部分接口存在缓存时间。在调用相关接口时根据缓存时间来判断是否真实去调用。

---

## 背景与问题

当前 `EsiApiService`（`EsiGateway` 端口实现）对人物/军团业务数据查询（资产、蓝图、工业、采矿台账、建筑、钱包流水、钱包交易、类型、ID→名称）每次调用都直接发起真实 HTTP 请求到 ESI，**完全没有利用 ESI 声明在响应头里的缓存失效时间**。只有 accessToken/授权状态两类走 Redis 缓存。

由此造成：大量同步任务（定时任务、用户多次刷新）对同一 ESI 资源反复发起真实调用，加剧单账号对 ESI 的请求压力；而 ESI 在响应头 `Expires` / `Cache-Control: max-age` 中明确承诺了数据有效期（实测同一晨曦接口如 `/characters/{id}/assets/` 为 3600s、`/corporations/{id}/industry/jobs/` 为 300s）。合理做法是：**在 ESI 声明的缓存失效时间内不真实调用，直接复用本地缓存**。

**痛点确认（当前代码路径）**：`AssetsApi.queryCharactersAssets` / `CorporationApi` / `IndustryApi` / `WalletApi` 等 API 层每次 `esiClient.get()....retrieve().bodyToFlux(...)` 直接发请求；分页走 `PageTotalApi.queryMaxPage`。均无业务数据缓存。

## 设计总览

在 `EsiApiService`（EsiGateway 端口实现）统一包裹——不改 28 个 API 类，上层应用零感知。缓存载体为 API 层返回、经 assemble 后落地前的数据（分页 maxPage 为 Integer，列表为领域实体列表 Flux）。

- **缓存时间来源**：运行时 ESI 响应头 `Cache-Control: max-age`，退回 `Expires`（HTTP-date）。
- **响应头缺失时不缓存**：无缓存失效声明的请求每次都真调（不做默认 TTL 兜底）。
- **缓存键隔离**：`esi:resp:{datasource}:{userId|pub}:{URI}:{page}`。鉴权数据按 userId 隔离；公开数据（类型、ID→名称）userId 段置 `pub`，全局共享。accessToken 不入键。
- **TTL 内强制刷新**：提供强制刷新参数；固定时间窗内（每键每 60 分钟）最多允许 5 次真实刷新，超出拒绝（返回现有缓存并提示）。
- **防并发穿透**：未命中以 `setIfAbsent` 合并并发真实调用，避免同一过期键同时被大量请求打穿到 ESI。
- **新增组件**（`infrastructure/external/esi/cache/`）：`EsiResponseCache`（Redis 操作，复用 `CacheGateway` 端口）、`EsiCacheTtlExtractor`（解析响应头 TTL）、强制刷新限流哨兵。

## 范围

**纳入**（`EsiApiService` 当前已暴露的 12 类查询）：人物/军团资产（列表+页数）、军团蓝图（页数）、人物/军团工业（列表+页数）、采矿台账（列表+页数）、军团建筑（列表+页数）、人物/军团钱包流水（列表+页数）、人物/军团钱包交易、类型信息、ID→名称批量解析。

**不纳入**：7 天/长期静态接口（SDE 类），避免过度范围；market_order（公共行情，已有独立处理）。

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 缓存失效时间内不真实调用 ESI（Priority: P1）

用户刷新人物资产列表。首次调用真实请求 ESI；随后在 ESI 声明的缓存失效时间（如 3600s）内再次刷新同一资源，**不发起新的真实 ESI 请求**，直接返回上次结果。

**Why this priority**: 这是本特性核心价值——按 ESI 缓存时间避免重复真实调用，直接缓解对单账号 ESI 的请求压力。

**Independent Test**: 对同一资源连续调用两次；用 Mockito 断言 WebClient 只被触发一次；第二次调用返回与首次一致的领域数据。

**Acceptance Scenarios**:

1. **Given** 缓存键不存在, **When** 首次调用, **Then** 真调 ESI、以响应头 TTL 写入 Redis、返回结果
2. **Given** 缓存命中且未过期（响应头 `max-age` 内）, **When** 再次调用, **Then** 不真调 ESI、直接返回缓存数据
3. **Given** 缓存已过期, **When** 再次调用, **Then** 真调 ESI 并以新 TTL 回填

---

### User Story 2 - 响应头缺失时不缓存（Priority: P1）

对未返回 `Cache-Control`/`Expires` 的 ESI 接口，不写入缓存，每次调用都真实请求。

**Why this priority**: 避免为无法确定 TTL 的数据缓存"猜一个默认时长"，防止陈旧数据在未知窗口中滞留。这是数据新鲜度正确性的底线。

**Independent Test**: Mockito 模拟 ESI 响应无缓存头；断言调用后缓存键不存在、下次调用仍真调。

**Acceptance Scenarios**:

1. **Given** ESI 响应无 `max-age` 亦无 `Expires`, **When** 调用, **Then** 不写缓存、结果直接返回
2. **Given** 随后再次调用同一资源, **When** 调用, **Then** 仍真调 ESI（缓存从未生效）

---

### User Story 3 - 缓存键按用户隔离（Priority: P1）

不同用户查询同一人物的资产等鉴权数据，互不共享缓存，杜绝跨用户读到他人数据（IDOR）。

**Why this priority**: 与 014 特性根治的越权问题同源。缓存若跨用户命中，会把一个用户视角的数据暴露给另一用户，属于安全红线。

**Independent Test**: 用两个不同 userId 对同一 characterId 调用；断言两键独立、各自真调一次、互不覆盖。

**Acceptance Scenarios**:

1. **Given** 用户 A 已缓存 characterId=X 的资产, **When** 用户 B 请求同一 X, **Then** 不命中 A 的缓存、独立真调
2. **Given** 公开数据（类型/ID→名称）, **When** 任意用户调用, **Then** 共享同一 pub 缓存键、二次调用免真调

---

### User Story 4 - 固定时间窗内强制刷新限 5 次（Priority: P2）

用户主动强制刷新（跳过缓存）可立即拿到最新数据；但每隔固定时间窗（每键 60 分钟）最多允许 5 次真实刷新，超出次数拒绝并返回现有缓存。

**Why this priority**: 满足"想立即拿最新"的人工诉求，同时防止反复强刷打爆单账号 ESI 配额。P2 因为强刷是可选增强，且核心缓存路径已由 US1–3 覆盖。

**Independent Test**: 对同一键连续强刷 6 次；断言前 5 次真调、第 6 次拒绝且返回缓存；窗口届满后恢复额度。

**Acceptance Scenarios**:

1. **Given** 当前强制刷新计数 < 5, **When** 强制刷新, **Then** 忽略缓存真调 ESI、结果回填
2. **Given** 当前计数 ≥ 5 且窗口未过, **When** 再强刷, **Then** 拒绝、返回现有缓存（响应提示已达上限）
3. **Given** 固定窗口（60 分钟）届满, **When** 强刷, **Then** 计数归零、再次允许真调

---

### User Story 5 - 未命中并发不穿透（Priority: P3）

同一缓存键同时大量请求（如定时任务与手动刷新并发），只允许一次真实 ESI 调用，其余请求复用该次结果。

**Why this priority**: 防止缓存刚过期瞬间被并发打穿到 ESI，是对单账号配额的最后一道保护。属稳健性增强，故 P3。

**Independent Test**: 并发 10 请求同一已过期键；断言 WebClient 真实调用次数 = 1。

**Acceptance Scenarios**:

1. **Given** 缓存过期, **When** 10 个并发请求同一键, **Then** 仅 1 次真调 ESI、其余复用结果

---

### Edge Cases

- **max-age 含单位/大小写变体**（`max-age=3600`、`max-age = 3600`）：解析需容忍空白。
- **max-age 为 0 / 负值**：表示不缓存 → 按"响应头缺失"处理，不缓存、每次都真调。
- **Expires 早于当前时间**：视为已过期，真调并回填。
- **Expires 与 max-age 同时存在**：max-age 优先（HTTP 规范）。
- **缓存序列化失败**（领域实体含复杂类型）：记日志告警、本次不做缓存、下次再试，不阻断业务。
- **强制刷新窗口计时**：以首次真调写入缓存的时间为窗口起点；窗口过期后计数归零。
- **userId 为空**（如定时任务无 SecurityContext）：鉴权数据键无法按用户隔离 → 记 warn、跳过缓存（安全优先，不缓存未知归属数据）。
- **公开数据键**：固定 `pub`，不含 userId，全局共享。

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 在调用 ESI 业务数据前检查本地缓存，缓存未过期（响应头 `max-age`/`Expires` 窗口内）时不发起真实 ESI 调用，直接返回缓存结果。
- **FR-002**: 系统 MUST 从 ESI 响应头解析缓存失效时间：优先 `Cache-Control: max-age`，退回 `Expires`（HTTP-date 解析）。
- **FR-003**: 当 ESI 响应既无 `max-age` 亦无 `Expires`（或 max-age ≤ 0）时，系统 MUST NOT 写入缓存；每次调用均真实请求 ESI。
- **FR-004**: 缓存键 MUST 对鉴权数据按 userId 隔离（`esi:resp:{datasource}:{userId}:{URI}:{page}`）；公开数据（类型、ID→名称）userId 段置 `pub` 全局共享。
- **FR-005**: 缓存键 MUST NOT 包含 accessToken（accessToken 20 分钟轮换，不应驱逐业务缓存）。
- **FR-006**: 系统 MUST 提供强制刷新语义：调用方显式请求时忽略缓存、立即真实调用 ESI 并以新 TTL 回填缓存。
- **FR-007**: 强制刷新 MUST 受固定时间窗限流：每缓存键每 60 分钟窗口内最多允许 5 次真实刷新；超限拒绝真调并返回现有缓存，响应携带已达上限提示。
- **FR-008**: 缓存未命中 / 并发时，系统 MUST 用 `setIfAbsent` 串行化单次真实 ESI 调用，其余并发请求复用该次结果（防并发穿透）。
- **FR-009**: 缓存写入口 MUST NOT 因序列化/Redis 异常阻断业务调用 —— 缓存失败记日志告警、本次不缓存，业务结果仍正常返回（fail-open，读路径不受缓存故障影响）。

### Key Entities

- **EsiResponseCache**: infrastructure 层缓存操作类，复用 `CacheGateway` 端口；负责键构造、`setIfAbsent` 原子写入、命中读取、TTL 管理。
- **EsiCacheTtlExtractor**: 从响应头解析缓存失效时长；对无效/缺失值返回"不缓存"语义。
- **ForceRefreshLimiter**: 强制刷新固定时间窗计数哨兵（每键每窗口 5 次额度）。
- **缓存键（Redis）**: `esi:resp:{datasource}:{userId|pub}:{URI}:{page}`，值 = 序列化后的业务数据（maxPage 为 Integer，列表为领域实体列表 JSON）。

---

## Success Criteria *(mandatory)*

### Performance Criteria
- **SC-001**: 缓存命中时读取接口零 ESI 调用，p95 由网络往返主导的毫秒级降为本地 Redis 读取量级（<10ms）。
- **SC-002**: 缓存未命中时，单键并发真实 ESI 调用数 = 1（防穿透生效）。
- **SC-003**: 固定时间窗（60 分钟）内单键强制刷新真实调用数 ≤ 5。

### Code Quality Criteria
- **SC-004**: 新增代码单元测试覆盖率 ≥80%。
- **SC-005**: TDD——每个组件先有失败测试再实现（RED→GREEN→REFACTOR）。
- **SC-006**: 鉴权数据与公开数据隔离、IDOR 防护有显式测试。

### Measurable Outcomes
- **SC-001**: 同一资源在 TTL 内二次读取，WebClient 真实调用次数从 2 降为 1（可由测试断言）。
- **SC-002**: 某键过期瞬间 10 并发，真实调用次数 = 1。
- **SC-003**: 连续 6 次强刷同一键，第 6 次被拒。
- **SC-004**: 无响应头接口第二次调用仍真调（缓存从未写入）。

---

## Assumptions

- **缓存设施就绪**：复用既有 `CacheGateway` 端口与其 Redis 实现，不新增依赖（技术栈冻结）。
- **ESI 响应头可访问**：API 层已具备读取响应头能力（`PageTotalApi` 以 `exchangeToMono`+`toEntity` 读 `X-Pages` 为先例）；业务数据读取处可同样取得 `Cache-Control`/`Expires`。
- **数据新鲜度取 ESI 承诺**：以 ESI 响应头 max-age 为 TTL（保守下限），TTL 内不主动旁路刷新；用户可显式强刷。
- **强制刷新窗口默认 60 分钟/5 次**，可通过配置调整；本次固定默认值。
- **范围边界**：仅 EsiGateway 已暴露的 12 类业务查询；长期静态接口、market_order 不在本次。
- **鉴权数据无 userId 归属**（定时任务场景）→ 不缓存，安全优先。