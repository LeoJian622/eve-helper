# Implementation Plan: 角色 ESI AccessToken 查询接口

**Feature Branch**: `006-character-access-token-api`
**Created**: 2026-08-11
**Spec**: [spec.md](./spec.md)
**设计评审**: [docs/reviews/2026-08-11-006-character-access-token-design-review.md](../../docs/reviews/2026-08-11-006-character-access-token-design-review.md) — 结论 BLOCK,已修订 spec 并将 C1/C2/H1/H3 纳入范围

## 1. 目标与范围

### 做

| 项 | 对应需求 |
|----|----------|
| 新增 `GET /character/{characterId}/access-token` 端点 | FR-001、FR-007 |
| 精确归属校验(仅 `(userId, characterId)` 命中) | FR-002~FR-006 |
| 应用层显式 `userId <= 0` 门禁 | FR-012(评审 H1) |
| `getAccessToken` 补 per-(userId,characterId) 锁 + 5s 超时 | FR-013(评审 C2) |
| MyBatis / web 日志配置去除 token 明文 | FR-014(评审 C1) |
| `queryAccountList` 补 `user_id` 列 | FR-015(评审 H3) |
| 越权拒绝与成功审计日志 | FR-016(评审 H4-部分) |
| 配置开关(默认 false)+ `@Hidden` | FR-017(评审 M4) |
| `expiresIn` 取自 `AuthTokenResponse` / 归一化 TTL | FR-018(评审 M1) |
| 替换 `assert` 为显式空值检查 | FR-019(评审 M3) |
| ESI 上游错误文案归一化 | FR-020(评审 M2) |
| `@Positive` 参数校验 | FR-011(评审 M5) |

### 不做(记入遗留待办)

- 限流(用户决定另行评估;评审 H4 的限流部分)
- `refresh_token` 加密存储与 `varchar(64)` 加宽 DDL(评审 H5)
- `characterId` Integer → Long(既有全局债务)
- `ACCESS_UNAUTHORIZED` → 403 的状态码语义修正(评审 L2,既有约定)
- RBAC 既有 pattern 审计(评审 H2,属上线前运维动作,非代码任务)

## 2. 架构设计(DDD 分层)

```
interfaces/web/controller/CharacterController
    ↓ UserUtil.getUserId() 取当前用户,@Positive 校验 characterId
application/service/CharacterApplicationService.queryAccessToken(characterId, userId)
    ↓ 显式 userId<=0 门禁(FR-012) + 审计日志(FR-016)
domain/port/esi/EsiGateway.getAccessTokenWithExpiry(characterId, userId)   ← 新增端口方法
    ↓
infrastructure/external/esi/EsiApiService  ← 实现:归属校验 + 锁 + 超时 + 缓存键构造
    ↓
domain/service/system/EveAccountService.getAccountOne(userId, characterId)  ← 既有精确查询
```

**关键设计决策**

| 决策 | 理由 |
|------|------|
| **不用** `AccessGuard.requireOwnership` | 它走 `ResourceOwnershipPolicy.isOwnedBy` 会允许 corpId 命中放行,且对 ROOT 豁免 —— 与 FR-003、FR-004 直接冲突 |
| 归属校验复用 `getAccountOne(userId, characterId)` | 走 SQL `where user_id=? and character_id=?`,天然满足精确匹配;满足 SC-006「不新增手写校验实现」 |
| 新增 `getAccessTokenWithExpiry` 而非改 `getAccessToken` 签名 | 既有 6 个调用方(资产/工业/月矿/建筑/钱包/授权状态)不受影响;缓存键构造留在 infra 层(FR-018 要求键格式不出 infra) |
| 锁与超时加在 `getAccessToken` 内部 | 既有调用方同样受益;与 `determineAuthStatus:447,282-288` 同构,复用 `ESI_REFRESH_TIMEOUT` 与 `setIfAbsent` 模式 |
| 领域读模型用 `record` | 与既有 `TokenResult`(`domain/model/vo/TokenResult.java`)风格一致 |
| **`queryAccessToken` 非事务** | `EveAccountService` 类级 `@Transactional`;本方法含 ESI 网络调用,须 `@Transactional(propagation = NOT_SUPPORTED)` 或不标注并确保不被事务传播包裹(评审 M6,沿用 `specs/003-esi-auth-status/research.md:47` 结论) |

## 3. 新增/修改文件清单

### 新增

| 文件 | 说明 |
|------|------|
| `domain/model/vo/CharacterAccessTokenResult.java` | record(accessToken, characterId, expiresIn) |
| `src/test/java/.../application/service/CharacterApplicationServiceTest.java` | US1/US2 单测 |
| `src/test/java/.../infrastructure/external/esi/EsiApiServiceAccessTokenTest.java` | 锁/超时/缓存键/expiresIn 单测 |

### 修改

| 文件 | 修改内容 |
|------|----------|
| `domain/port/esi/EsiGateway.java` | 新增 `getAccessTokenWithExpiry` |
| `infrastructure/external/esi/EsiApiService.java` | 实现新方法;`getAccessToken` 补锁+超时;替换 3 处 `assert` |
| `application/service/CharacterApplicationService.java` | 新增 `queryAccessToken`,含 userId 门禁与审计日志 |
| `interfaces/web/controller/CharacterController.java` | 新增端点,`@Hidden` + `@Positive` + `@ConditionalOnProperty` |
| `src/main/resources/mappers/system/EveAccountMapper.xml` | `queryAccountList` 补 `user_id` |
| `src/main/resources/application.yml` | `log-impl` 改 `Slf4jImpl`;`logging.level.web` 改 `info`;新增开关默认 false |
| `src/main/resources/application-{prod,ali,aliw}.yml` | `log-impl` 改 `Slf4jImpl` |
| `infrastructure/external/esi/auth/AuthorizeOAuth.java` | 上游错误文案归一化(FR-020) |
| `.env.example` | 文档化新增开关 |

## 4. 契约

见 [contracts/api-contract.md](./contracts/api-contract.md)

## 5. 测试策略

**测试类型**:纯 Mockito 单测为主(本地无法连 MySQL,远程库连接超时 —— 见记忆「环境注记」)。

| 测试点 | 用例 |
|--------|------|
| US1 正常路径 | 缓存命中返回缓存值不调 ESI;缓存未命中刷新并返回;expiresIn 来源正确 |
| US2 越权(核心) | ①角色属他人 → 拒绝 ②角色不存在 → **与①完全同码同文案** ③同军团他人角色 → 拒绝 ④ROOT 非本人角色 → 拒绝 ⑤userId=-1/0/null → 拒绝 |
| FR-013 锁 | 抢不到锁时不调 `updateAccessToken`;超时参数被传入 |
| FR-018 expiresIn | 缓存命中时 TTL=-2/-1 被归一化,不透出哨兵值 |
| FR-015 | `queryAccountList` 返回的 `EveAccount.userId` 非空(可用 `XMLMapperBuilder` 离线解析验证 SQL 含 `user_id`,参考 `BlueprintsMapperSqlTest`) |
| FR-014 | 实测:启动后检查日志输出无 token 明文(非仅检查代码) |
| FR-011 | characterId=0/-1 在查库前被拒 |

**回归基线**:全量 `mvn test` 有 ~267 个既有 errors(需 Spring 上下文/DB 的集成测试,属环境问题)。判断回归须与 `git stash` 基线对比 errors 数,不能只看是否有失败。新增测试类须能独立跑通。

**命令**:`./mvnw`(项目无 `mvn` 命令)。

## 6. 风险

| 风险 | 缓解 |
|------|------|
| 给 `getAccessToken` 加锁可能影响既有 6 个调用方(如批量同步中大量角色串行化) | 锁按 (userId, characterId) 粒度,不同角色互不阻塞;锁 TTL 参照 `AUTH_STATUS_LOCK_TTL_SECONDS`(10s)覆盖 5s 超时 + DB 回写 |
| 改 `log-impl` 为 `Slf4jImpl` 后开发期 SQL 调试不便 | dev/test profile 可保留 StdOutImpl(测试库无真实 token);prod/ali/aliw 必须改 |
| `queryAccountList` 补列后可能有代码依赖 `userId` 为 null 的行为 | 需 grep 确认无此依赖;`persistRefreshedToken` 加 `userId == null` 时拒写缓存的防御 |
| 本地无法跑集成测试,C1 的日志实测受限 | 至少验证配置项已改;实测项标注为上线前人工验证 |

## 7. 合宪性检查

| 宪法条款 | 符合性 |
|----------|--------|
| I. REST API First + Test-First | ✅ 走 Result<T> 信封;DDD 分层依赖方向正确;每任务 RED 先于 GREEN |
| I. API 性能 200ms P95 | ✅ 缓存命中路径满足;未命中受 ESI 5s 超时约束(SC-001 已声明) |
| I. 外部调用 5s 超时 | ✅ FR-013 正是修复此项缺失 |
| II. 可观测性与安全 | ✅ FR-016 审计日志;FR-014 敏感数据不入日志 |
| III. YAGNI + 覆盖率 ≥80% | ✅ 不做限流/加密(明确排除);SC-004 要求 ≥80% |
| IV. 技术栈冻结 | ✅ 无新增依赖 |
| V. 统一 Spec-First | ✅ ①澄清→②规格→③计划+安全评审(本文档)→④拆解→⑤TDD→⑥验证→⑦评审→⑧提交 |
