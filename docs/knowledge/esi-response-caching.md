# ESI 响应缓存（按失效时间跳过真实调用）

> 面向开发者：理清 `EsiApiService` 12 类业务查询如何按 ESI 响应头声明的缓存失效时间命中 Redis、跳过真实 ESI 调用，以及强刷限流 / 防并发穿透 / 用户隔离 / 公开数据共享的完整机制。
> 关联代码集中在 `infrastructure/external/esi/cache/`（4 组件）、`infrastructure/external/esi/EsiApiService.java`（统一包裹点）、`infrastructure/external/esi/api/`（含响应头入口）。

## 1. 背景与目标

`EsiApiService`（`EsiGateway` 端口实现）对资产 / 蓝图 / 工业 / 采矿 / 建筑 / 钱包流水 / 钱包交易 / 类型 / ID→名称等 **12 类业务查询**，旧实现每次请求都真实调用 ESI。但 ESI 绝大多数 GET 接口在其响应头 `Cache-Control: max-age`（异常时退回 `Expires`）中声明了可缓存时长，swagger 亦注明 `x-cached-seconds`（实测资产/蓝图/建筑/钱包=3600s、工业=300s）。

- **目标**：响应未过期（`max-age`/`Expires` 窗口内）不真调、直接复用 Redis 缓存；过期才真调一次并按响应头 TTL 回填。
- **集成点**：统一在 `EsiApiService` 包裹，**不改 28 个 API 类**（仅为其补"含响应头返回"入口）。
- **键隔离**：`esi:resp:{datasource}:{userId|pub}:{normalized-uri}:{page}` —— 鉴权数据按 userId 隔离防 IDOR、公开数据置 `pub` 全局共享、accessToken **不入键**。

## 2. 缓存组件（`external/esi/cache/`）

| 组件 | 职责 | 关键行为 |
|------|------|----------|
| `EsiCacheTtlExtractor` | 解析响应头 TTL | `Cache-Control: max-age`（容忍空白/引号，≤0→null）优先，退回 `Expires`（RFC1123，已过期/不可解析→null）；皆无→null（不缓存） |
| `EsiResponseCache` | 键构造 + 命中读 + 防穿透 | `getList(key, JavaType)`/`getInt(key)` 按 **元素类型** 反序列化（防 `LinkedHashMap`）；`tryClaim`/`releaseClaim` = `setIfAbsent` 原子占位；**fail-open**（Redis 异常→null/false 不阻断） |
| `EsiResponseCacheProperties` | 配置 | `forceRefreshWindowMinutes=60`、`forceRefreshLimit=5`、`datasource="serenity"` |
| `ForceRefreshLimiter` | 强刷限流 | 固定时间窗计数哨兵，`tryAcquire` 超限拒；键过期天然归零 |

> ⚠️ **JavaType 参数传元素类型**：全局 Redis 用 `Jackson2JsonRedisSerializer<Object>` 无类型头，反序列化需 `constructType(Assets.class)` 显式往返，**传集合 RawClass 会得到 `ArrayList<ArrayList>`。**
> ⚠️ **tryClaim 锁 TTL 单位=毫秒**：`Duration.toMillis()`；测试 stub 需 `eq(5000L), MILLISECONDS` 而非 `eq(5L), SECONDS`。

## 3. 统一包裹四态（`EsiApiService.doXxx`）

每个查询统一裁为四态（安全优先）：

1. **未认证**（`UserUtil.getUserId()≤0`）→ 不缓存、直接真调（公开类不查 `UserUtil`，用 `pub` 键）。
2. **命中** → 读缓存返回，不真调。
3. **未命中** → 真调 + `EsiCacheTtlExtractor` 提取 TTL 回填（无响应头则不回填）。
4. **防穿透/强刷** 见下。

变体参数（language / observerId / division / fromId / ids / include_completed 等）**必须入键**，否则不同实参会互相命中返回错误数据（跨 true/false、跨语言、跨 division 等都是险）。

## 4. 强制刷新与限流（T013）

- 端口 `EsiGateway` 16 个鉴权方法新增 `boolean forceRefresh` **尾参重载**（原方法保留委托 `false`，调用方零破坏）；公开 2 方法不强刷。
- 语义：`forceRefresh=true` + 有额度 → **忽略缓存读**、直接真调回填；`forceRefresh=true` + 超限 → 落回缓存判读（命中→缓存，未命中→真调回填，该真调不额外占 force 计数）；默认 `false` → 命中不真调。
- 限流：每键每 `forceRefreshWindowMinutes`(60) 次最多 `forceRefreshLimit`(5) 次强制刷新。

## 5. 防并发穿透（FR-008，T013b）

未命中真调路径存在缓存击穿风险（并发涌进同一过期键）。解决：`EsiResponseCache.tryClaim` 用 `setIfAbsent(key+":lock", …)` 原子占位：

- **持锁方**（tryClaim=true）承担真调并回填，流终止（`doFinally`，列表）或完成后（`try/finally`，maxPage 同步）`releaseClaim` 释放锁。
- **未抢锁方**（tryClaim=false）有界轮询读缓存复用持锁方结果，命中则不重复真调；轮询窗口 **50×100ms≈5s**（与 `ESI_DATA_TIMEOUT` 对齐，覆盖真实真调全程），**超时 fail-open** 直接真调不阻塞业务。
- 强刷有额度但未抢锁：也直接真调、**不读陈旧缓存**（保 force 新鲜语义）。

> 该机制使 **SC-002（10 并发同键→1 真调）** 成立——是 Phase3 T018 的验收基础。

## 6. 序列流程（资产列表示例）

```
请求A(带keyK, miss)          Redis                          ESI
  tryClaim(K:lock)=true ───▶ setIfAbsent=1 ────────────────▶ 真调
                                                          ◀── response + max-age
  回填 K (TTL内) ──────────▶ set(K, payload, ttl)
  releaseClaim(K) ─────────▶ delete(K:lock) ───────────────┘
请求B(带keyK, miss)          
  tryClaim(K:lock)=false ───▶ (已被A占位)
  awaitCached: 轮询get(K) ─▶ (读到A回填) ── 返回缓存, 不真调 ✅
```

## 7. 已验收旅程（Phase3 SC-001~004）

| 旅程 | 验收 | 测试 |
|------|------|------|
| SC-001 命中不真调 | 同 key TTL 内二次调用真调次数=0 | `assets_sameKey_firstMissBackfill_secondHit_noRealCall` |
| SC-002 并发不穿透 | 10 并发同已过期键真调次数=1 | `assets_tenConcurrentSameExpiredKey_onlyOneRealCall` |
| SC-003 强刷窗口 | 连续 6 次强刷第 6 次拒、窗口后归零 | `forceRefresh_windowExpiry_requota_reallowsFresh` |
| SC-004 无头每次真调 | 无缓存头接口二次仍真调、键不存在 | `characterAssets_noHeader_realCallEveryTime` |
| SC-001 IDOR | 两 userId 同 characterId 键互不命中、各自真调 | `assets_twoUsers_sameCharacter_isolatedKeys_eachRealCalls` |

## 8. 已知权衡（收尾遗留）

- `EsiApiServiceTest` 全量约 163s：大头是 access-token 超时类用例真实等待（非本特性引入）；部分 cache-miss 用例因 `tryClaim` 未 stub→false 走 5s 轮询，亦占时间。T020 全量评估，不影响正确性。
- 待收尾 MEDIUM/LOW：T008 资产 maxPage 仍与列表 page=1 同键（待统一 `:maxPage` 口径）；T009 工业 `include_completed` 未入键（人物侧跨 true/false 互命中风险）；T012 type 空列表命中判据 `!isEmpty()` 使空缓存永不生效（可用 `isPresent` 区分空结果与缺失）；force 超限未命中回填子例未测。

## 9. 相关文档

- 特性工件：`specs/015-esi-response-caching/`（spec/plan/tasks）
- 授权绑定：`esi-authorization-binding.md` | 数据归属：`esi-data-user-binding.md`