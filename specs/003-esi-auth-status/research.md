# Research: 用户账户 ESI 授权状态返回

**Date**: 2026-08-04 | **Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

> Phase 0 产物:记录关键设计决策、依据与备选方案。所有 NEEDS CLARIFICATION 已在 spec 阶段解决(FR-007 的"无法判定"态)。

## D1. 四态到 ESI 调用结果的映射

**Decision**: 状态由"是否存在 refreshToken"与"refreshToken 能否换取 accessToken"两步派生,异常类型决定 EXPIRED 与 UNKNOWN 的区分。

| 条件 | 状态 | 依据 |
|------|------|------|
| `EveAccount.refreshToken` 为空 | `NOT_AUTHORIZED` | 未授权(无凭证) |
| `updateAccessToken(REFRESH_TOKEN, token)` 成功 | `AUTHORIZED` | 可正常返回 accessToken |
| 抛 `EsiException(ESI_AUTHORIZATION_FAILURE)`(4xx) | `EXPIRED` | `AuthorizeOAuth` 在 4xx(invalid_grant)时抛此码,代表 refreshToken 失效/吊销 |
| 抛 `EsiException(ESI_SERVER_FAILURE)`(5xx)/ 其他异常(网络/超时) | `UNKNOWN` | ESI 服务端问题或不可达,非授权失效 |

**Rationale**: `AuthorizeOAuth.updateAccessToken` 已对 4xx/5xx 分别抛 `ESI_AUTHORIZATION_FAILURE`/`ESI_SERVER_FAILURE`(见 `infrastructure/external/esi/auth/AuthorizeOAuth.java:87-90`),天然区分"授权过期"与"服务不可用",无需新增错误分类逻辑。

**Alternatives**: 把所有失败统一为 EXPIRED--被否,因 ESI 5xx 或网络抖动不应误报为"授权过期",否则用户会被误导去重新授权。

## D2. 状态缓存策略(满足 <200ms p95)

**Decision**: 在 Redis 缓存判定结果,键 `esi_auth_status:{characterId}`;成功态(AUTHORIZED/EXPIRED)TTL 5min,UNKNOWN 态 TTL 30s。请求路径先查缓存,命中即返回(纯 Redis,~1ms/角色)。

**Rationale**:
- 宪法要求 <200ms p95 与外部调用 5s 超时。若每次请求都对每角色同步刷新 ESI,N 个角色串行可达 N×5s,p95 必然超限。
- 状态缓存使常态(缓存命中)路径仅做 N 次 Redis GET,远低于 200ms;冷路径(未命中)才触发 ESI 刷新,且为低频。
- UNKNOWN 用更短 TTL(30s),使 ESI 恢复后能较快重试,避免长时间停留在"无法判定"。

**Alternatives**:
- (a) 复用既有 access token 缓存(19min)作为 AUTHORIZED 信号--否,缓存命中时未真正使用 refreshToken(违背用户"使用 refreshToken 认证"的明确意图),且会掩盖已吊销的 refreshToken。
- (b) 后台定时任务异步刷新状态、接口只读缓存--否,引入调度任务与状态一致性问题,违反 YAGNI(首版实现保持简单)。

## D3. refreshToken 轮换处理

**Decision**: ESI refreshToken 一次性使用,刷新成功时响应携带新 refreshToken。`getAuthorizationStatus` 在 AUTHORIZED 分支必须回写新 refreshToken(并按既有流程缓存 accessToken 19min),否则旧 token 已失效、下次刷新会失败。

**Rationale**: ESI OAuth 规范:每次 refresh 作废旧 refreshToken、签发新 refreshToken。不回写会导致角色授权在下次判定时被误判为 EXPIRED。

**Implementation note**: 不复用 `EsiApiService.updateRefreshToken`(它额外调用 `characterApi.queryCharacter` 与 `universeApi.queryUniverseNames` 刷新角色信息,2 次额外 ESI 调用,状态判定不需要)。新增轻量回写:仅解析 accessToken 的 JWT 取 characterId(用于缓存键),回写 `EveAccount.refreshToken` 并 `eveAccountService.insertOrUpdate`。角色名/军团等信息沿用查询到的既有值,不变更。

**Spec 一致性**: spec FR-006"不得主动删除或修改存储的 refreshToken"指不因判定而清除 token;刷新成功后的轮换回写属"既有 ESI 刷新流程"的自然行为(spec 假设已允许),不构成违反。

## D4. 事务边界

**Decision**: `UserApplicationService.queryAccountListWithAuthStatus(userId)` 标注 `@Transactional(propagation = NOT_SUPPORTED)`(或方法级覆盖类默认 readOnly 事务),确保 ESI 网络调用不在数据库事务内。DB 读取依赖 `EveAccountService.getAccountList` 自身的事务;刷新成功回写依赖 `EveAccountService.insertOrUpdate` 自身的事务。

**Rationale**: 在 `@Transactional` 方法内做网络调用会长时间持有 DB 连接,浪费连接池资源并增加死锁/超时风险。将 ESI 调用置于事务外,各 DB 操作各用自己的短事务。

**Alternatives**: 保留类默认 readOnly 事务包裹全方法--否,ESI 调用期间持锁不可接受。

## D5. 熔断器缺口与缓解

**Decision**: 不引入独立熔断库(冻结技术栈不含,引入需宪法修订)。以"UNKNOWN 降级 + 5s 超时 + 30s 短 TTL 缓存"作为等价容错:ESI 持续失败时返回 UNKNOWN 并短缓存,后续 30s 内同角色直接返回 UNKNOWN,避免持续冲击失败依赖。

**Rationale**: 完整熔断器(half-open 探测、失败率统计等)需 Resilience4j 等,超出本功能收益;且既有 ESI 调用均未配熔断,保持一致。列入 plan.md Complexity Tracking 作为已知缺口。

**Alternatives**: 自研简单失败率计数熔断(Redis 滑窗)--否,增加复杂度且与既有代码不一致,YAGNI。

## D6. 并行冷路径计算

**Decision**: 冷路径(缓存未命中)对多角色的刷新调用以 `CompletableFuture` 并行提交到有界线程池,单调用 5s 超时,整体以 `CompletableFuture.allOf` 等待(可加整体上限)。线程池复用既有 `AsyncConfiguration` 中的执行器或新增专用有界池(实现阶段定)。

**Rationale**: 串行冷路径 N×5s 体验不可接受;并行把冷路径限制在约 5s。常态命中缓存不触发并行,无额外开销。

**Alternatives**: 串行 + 仅靠缓存--否,冷路径体验过差(10 角色可达 50s)。

## D7. 5s 超时实现

**Decision**: 在 `authorizeOAuth.updateAccessToken(...).timeout(Duration.ofSeconds(5))` 上加 Reactor 超时;超时抛 `TimeoutException`,归入 UNKNOWN。

**Rationale**: 宪法"外部调用 5s 超时";Reactor `Mono.timeout` 是既有 WebFlux 客户端的自然超时机制。注意:超时后底层 HTTP 连接可能仍在进行,但 `block()` 会及时返回,满足接口响应时限。
