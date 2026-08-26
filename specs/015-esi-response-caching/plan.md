# Implementation Plan: ESI 响应缓存（按失效时间跳过真实调用）

**Branch**: `015-esi-response-caching` | **Date**: 2026-08-20 | **Spec**: [spec.md](../015-esi-response-caching/spec.md)
**Input**: Feature specification from `specs/015-esi-response-caching/spec.md`
**轨道**: 特性轨（P2 设计规划）

---

## Summary

**需求**（FR-001~FR-009）：当前 `EsiApiService` 对人物/军团业务数据查询每次真实请求 ESI，未利用 ESI 响应头声明的缓存失效时间。本特性在 **EsiGateway 端口实现（`EsiApiService`）统一包裹**一层响应缓存：缓存未过期（响应头 `max-age`/`Expires` 窗口内）不真调 ESI、直接复用 Redis 缓存；响应头缺失则不缓存、每次真调；强制刷新受固定时间窗（每键每 60 分钟 5 次）限流；并发未命中以 `setIfAbsent` 合并。

**技术方案总览（运行时动态 TTL + 按用户隔离）**：
1. **TTL 来源**：读取 ESI 响应头 `Cache-Control: max-age`（优先），退回 `Expires`（HTTP-date）；两者皆无或 ≤0 → 不缓存。
2. **缓存键**：`esi:resp:{datasource}:{userId|pub}:{normalized-uri}:{page}`。鉴权数据按 userId 隔离；公开数据（类型/ID→名称）段置 `pub` 全局共享；accessToken 不入键。
3. **载体类型往返**：全局 `Jackson2JsonRedisSerializer<Object>` 反序列化无类型头，`List<Assets>` 会还原为 `LinkedHashMap` 列表。故列表缓存在 `EsiResponseCache` 内**自持有 RedisTemplate + 显式按具体领域类型往返**（JavaType/TypeReference），maxPage 为 Integer 直读。
4. **强制刷新**：`ForceRefreshLimiter` 用固定时间窗哨兵（每键每窗口 5 次额度），超限拒绝并返回缓存。
5. **防并发穿透**：未命中 `setIfAbsent` 原子占位，持锁方真调并回填，其余复用（参照 `doGetAccessToken` 的锁+轮询先例）。
6. **fail-open**：缓存读/写/序列化异常不阻断业务，记日志告警后直接真调或直返。

**范围（12 类查询全部走过 `EsiApiService` 统一包装）**：人物/军团资产（列表+页数）、军团蓝图（页数）、人物/军团工业（列表+页数）、采矿台账（列表+页数）、军团建筑（列表+页数）、人物/军团钱包流水（列表+页数）、人物/军团钱包交易、类型信息、ID→名称。**不含**长期静态接口、market_order。**不改 28 个 API 类。**

## Technical Context

**Language/Version**: Java 17（Spring Boot 3.5.14，技术栈冻结）
**Primary Dependencies**: Spring Data Redis（RedisTemplate+Jackson serializer，已冻结）、Reactor（Flux/Mono）、Hutool（日期解析）、Lombok
**Storage**: Redis（既有 `CacheGateway` 端口 + `RedisCacheGateway` 实现）
**Testing**: JUnit 5 + AssertJ + Mockito + SpringBootTest（@ActiveProfiles("test")）
**Target Platform**: Linux 服务器（Spring Boot 应用）
**Project Type**: Web service（DDD 五层：Interfaces → Application → Domain ← Infrastructure）
**Performance Goals**: 缓存命中读接口 <10ms（本地 Redis），单键并发真调数 = 1；固定窗内强刷 ≤5
**Constraints**: 技术栈冻结不得增删依赖；领域层不得 import 上层；缓存交互不引入对应用层的视角依赖；鉴权数据无 userId 不缓存（安全优先）
**Scale/Scope**: 新增 1 个缓存包（3 组件）+ EsiApiService 12 处查询包裹改造；不改 API 类；一次性缓存键约定

## Constitution Check

*GATE：Phase 0 前必须通过；Phase 1 设计后复核。*

### API Performance Gates
- 读接口 95 分位 <200ms：**满足**。缓存命中零 ESI 调用，退化为本地 Redis 读（<10ms）；未命中仅一次真调。
- 数据库查询优化计划：**不涉及**。本特性不新增 DB 查询，缓存全在 Redis。
- 外部依赖熔断：**满足且增强**。ESI 既有超时/熔断保留；新增缓存层进一步降低 ESI 调用频率（防穿透、强刷限流）。

### Test Coverage Gates
- 单元测试 ≥80%：**满足**。TDD 逐组件覆盖：TTL 提取（max-age/Expires/无头/≤0）、键构造（userId 隔离/公开 pub/不含 token）、列表类型往返（LinkedHashMap→List<Assets>）、强刷限流窗口、setIfAbsent 防穿透、fail-open。
- 集成测试覆盖关键旅程：**满足**。缓存命中不真调（SC-001）、无头不缓存（SC-004）、键用户隔离（IDOR）、强刷窗口、并发不穿透（SC-002）各 ≥1 条。
- 安全测试覆盖认证/授权流：**满足**。缓存键隔离提权测试（跨用户互不命中）必过 `ecc:security-reviewer`。
- 性能 SLA 验证：以 `./mvnw test` 全绿 + 防穿透/命中调用数断言为验收。

## Project Structure

### Documentation（本特性）

```text
specs/015-esi-response-caching/
├── spec.md          # G1 已批
├── plan.md          # 本文件
└── tasks.md         # P3 生成
```

`contracts/`、`quickstart.md`、`research.md`、`data-model.md`：**跳过**。理由——纯后端内部缓存机制，无对外接口契约变更、无新依赖、无用户可运行新入口；缓存键约定在本文档承载，无数据表模型。

### Source Code（repository root，单 Spring Boot 工程）

```text
# 领域层（domain，不得 import 上层）
src/main/java/xyz/foolcat/eve/evehelper/domain/
└── model/vo/                  # 无新增（缓存数据=序列化领域实体，载体见 infra）

# 基础设施层（infrastructure）——本特性主体
src/main/java/xyz/foolcat/eve/evehelper/infrastructure/
├── external/esi/cache/                     # 【新增包】
│   ├── EsiResponseCache.java               # 缓存操作：键构造、原子占位、命中读、类型往返
│   ├── EsiCacheTtlExtractor.java           # 解析响应头 TTL（max-age→Expires→无缓存）
│   ├── EsiResponseCacheProperties.java     # 强制刷新窗口/次数、datasource 等配置
│   └── ForceRefreshLimiter.java            # 固定时间窗计数哨兵（每键每窗口 N 次）
├── external/esi/EsiApiService.java         # 12 处业务查询走缓存包裹（核心改造，见 Phase B）
└── cache/RedisCacheGateway.java            # 复用，不改造（提供 set/setIfAbsent/get/…）

# 配置（技术栈冻结，仅补充 read 属性）
src/main/resources/application.yml           # 可选：esi 响应缓存窗口/次数默认值（或走 EsiClientProperties 前缀）
```

**新增内部类型（irrelevant to DDD 边界，全部 infra 层）**：
- `EsiResponseCache` 不在领域层建端口——缓存是 ESI 防腐层内部优化，不暴露领域契约；它直接依赖 `CacheGateway`（已存在端口）。**守层**：`EsiApiService` 已实现 `EsiGateway`，仅内部注入 cache 组件，端口签名不变，上层零感知。

**Structure Decision**：沿用既有单工程 DDD 五层，不新增容器/模块。缓存组件全部落在 `infrastructure/external/esi/cache/`，与 ESI 防腐层同域；守层靠"EsiApiService 已是端口实现、注入内部组件"保证领域层不被渗透。

## Design Decisions

| 决策 | 结论 | 依据 |
|------|------|------|
| 集成点 | EsiApiService 统一包裹，不改 API 类 | 单点改造、守层、上层零感知；缓存载体=领域实体/Integer |
| TTL 来源 | 运行时响应头 max-age→Expires | 用户选定"动态"；ESI 承诺即 TTL |
| 无响应头 | 不缓存、每次真调 | 用户选定；FR-003，省默认 TTL 兜底 |
| 键隔离 | 鉴权按 userId；公开置 pub | 用户选定；FR-004 防 IDOR |
| accessToken | 不入键 | FR-005；token 20 分钟轮换不驱逐业务缓存 |
| 强制刷新 | 固定时间窗每键每窗口 5 次 | 用户选定"定时间窗"；FR-006/007 |
| 类型往返 | 显式按领域类型反序列化 | 全局 Jackson 无类型头→LinkedHashMap；FR-009 变体 |
| 防穿透 | setIfAbsent 原子占位 + 持锁回填 | FR-008；参照 doGetAccessToken 锁先例 |
| fail-open | 缓存异常不阻断真调 | FR-009；读路径稳健性 |
| 强制刷新额度窗口 | 默认 60 分钟；超限返回缓存 + 提示 | Assumption；可配置 |

## Implementation Steps

> 每步产 TDD 失败测试→实现→通过→评审；步骤 <2h。走 RED→GREEN→REFACTOR。

### Phase A：缓存组件（infra/external/esi/cache/，4 组件）

**A1 `EsiResponseCacheProperties`**（配置载体，KISS）：
```java
@Data
@ConfigurationProperties(prefix = "eve.esi.cache")   // 或并入 EsiClientProperties 前缀，P4 裁定
public class EsiResponseCacheProperties {
    private int forceRefreshWindowMinutes = 60;   // 强刷固定时间窗
    private int forceRefreshLimit = 5;            // 窗内真调额度
    private String datasource = "serenity";
}
```

**A2 `EsiCacheTtlExtractor`**（TTL 解析，Hutool 日期 + 正则）：
```java
@Component
public class EsiCacheTtlExtractor {
    // 输入 responseHeaders(HttpHeaders)，输出 Duration 或 null(null=不缓存)
    public Duration extract(HttpHeaders headers) {
        // 1. Cache-Control: 正则取 max-age(容忍空白/引号)；<=0 → null
        // 2. 退回 Expires(HTTP-date, RFC1123)；已过期/不可解析 → null
        // 3. 两者皆无 → null(不缓存, FR-003)
    }
}
```
**TDD 断言**：`max-age=3600`→PT3600S；`max-age = 300`（空白）；`max-age=0`→null；仅 `Expires: 未来时间`→对应 Duration；Expires 过去→null；空头→null；max-age 与 Expires 并存→max-age 优先。

**A3 `ForceRefreshLimiter`**（固定时间窗计数哨兵）：
```java
@Component
public class ForceRefreshLimiter {
    // 键:复刻缓存键 + ":frwindow";用 CacheGateway.increment + expire 实现滑动固定窗
    public boolean tryAcquire(String cacheKey, Duration window, int limit) {
        // 窗口不存在→setIfAbsent(1, window)；存在→increment，>limit 拒
    }
    public void reset… // 窗口过期天然清零(键过期)
}
```
**TDD 断言**：窗口内 5 次通过、第 6 次拒；窗口过期后计数归零重新允许；increment 单测。

**A4 `EsiResponseCache`**（核心，Redis 操作）：
```java
@Component
public class EsiResponseCache {
    private final CacheGateway cacheGateway;       // 复用端口
    private final ObjectMapper objectMapper;       // 显式类型序列化
    // 键: esi:resp:{datasource}:{userId|pub}:{uri}:{page}
    // 未命中:touch(key) 用 setIfAbsent 原子占位防穿透;命中:get
    // 写:set(key, value, ttl);列表按参数化 JavaType(如 List<Assets>) 反序列化
    public <T> List<T> getList(String key, JavaType type)  // LinkedHashMap→List<T>
    public Integer getInt(String key)
    // fail-open:任何异常 catch 记日志，返回 null 由上游真调
}
```
**TDD 断言**：键构造按 userId/pub 分界；同键二次 get 命中；列表 JSON 往返还原 `List<Assets>`（非 LinkedHashMap）；Integer 往返；序列化异常→null 不抛（fail-open）。

### Phase B：EsiApiService 12 处查询包裹

在每个查询方法外层加缓存判读（统一模式）：
```java
// 示例:queryCharactersAssets
String key = cache.cacheKey(datasource, userId, "/characters/{id}/assets/", page, isPublic=false);
if (!forceRefresh) {
    var cached = cache.getList(key, ListType.assets());   // 命中→直接返回
    if (cached != null) return Flux.fromIterable(cached);
}
// 未命中/强刷:先限流判读
if (forceRefresh && !forceRefreshLimiter.tryAcquire(key, window, limit)) {
    return Flux.fromIterable(cache.getList(key, …));  // 超限返回现有缓存
}
// 真调 ESI(经既有 API 类)→ 取响应头 TTL → cache.put 回填 → 返回
```
- maxPage 类（`*MaxPage`）：走 similler 但类型为 Integer 的 getInt/put。
- 公开类（`queryUniverseType`、`queryUniverseNames`）：userId 段置 pub，不做强刷限流（或不限）。
- 列表响应头 TTL 需从**原始响应**读取 —— 通路改造：API 层的 `bodyToFlux` 目前丢弃了 headers。**关键子步骤**：对列表查询改用 `exchangeToMono`+`toEntity(ResponseBody)` 以同时取得响应头与 body，再在 EsiApiService 解析 TTL、缓存、并派发实体。

**技术要点**：
- 列表通路需要原响应头 → 在 API 类新增返回带 headers 的入口（如返回体或 `exchangeToMono` 取 `ResponseEntity`），或 EsiApiService 内用 WebClient 直取。**P4 裁定**：优先改造 `exchangeToMono` 取 `ResponseEntity`，避免动 `Flux<T>` 语义；每类新增一个"含头"方法，原方法保留（其它调用方不受影响）。
- 分页 maxPage 已走 `PageTotalApi`（`queryMaxPage` 持 `ResponseEntity<String>`），天然持头 → 直接在其外层加缓存。

**TDD 断言**（每查询）：未命中真调+回填；命中不真调（Mockito 调 API 次数 0）；强刷 5 次内真调第 6 拒；公开类型共享 pub 键；无响应头每次真调；userId 隔离。

### Phase C 测试与评审（贯穿，TDD 铁律）

- 关键旅程集成测试：缓存命中不真调、无头每真调、键用户隔离（IDOR）、强刷窗口、并发不穿透（SC-001~SC-004 各 ≥1）。
- 全绿 `./mvnw test`；`ecc:java-reviewer`（必须）+ `ecc:security-reviewer`（缓存键隔离，授权数据，必过）评审；补 `docs/knowledge/` 流程文档登记。

## Phase 2（tasks）预告

P3 按 A→C 原子化为 tasks.md：每个 <2h + 显式 AC + RED→GREEN→REFACTOR。A 组（4 组件）可并行标注其中独立组件。

## Complexity Tracking

> Constitution 无强制违背项（技术栈冻结不动、单一工程、无新增依赖，仅补充内部读配置）。留空。