# Feature Specification: 角色 ESI AccessToken 查询接口

**Feature Branch**: `006-character-access-token-api`
**Created**: 2026-08-11
**Status**: Draft
**Input**: User description: "增加一个角色接口，用角色ID,获取角色访问ESI的accesstoken。要做角色所有权的校验。避免用户访问到非他所有的角色"

## 澄清结论(阶段① 已与用户确认)

| 决策点 | 结论 | 理由 |
|--------|------|------|
| 接口用途 | **仅内部调试/运维排查** | access token 是可直接冒用的 bearer 凭证,不面向普通业务前端开放 |
| 所有权校验维度 | **仅本人名下 characterId 精确匹配** | 拒绝 corpId 维度放行,杜绝「小号入团窃取团内他人 token」 |
| ROOT/ADMIN 豁免 | **不豁免** | 最小权限优先于与其它接口的一致性;ADMIN 令牌泄露不得等于全站角色接管 |
| 返回体 | **token + 角色ID + 过期时间** | 调用方可预判过期;characterId 回显在校验通过后不构成额外泄露 |
| 生产可用性 | **配置开关控制,默认关闭** + `@Hidden` 不进公开 API 文档 | 「仅内部用途」必须由机制保证,不能只写在文档里 |
| 限流 | 本 feature **不加**限流,仅加越权审计日志 | 限流另行评估 |
| 范围 | **含修复设计评审发现的 C1/C2/H1/H3** | 本端点会把既有内部路径缺陷提升为外部可触发攻击面 |

## 设计评审修正(阶段③ security-reviewer BLOCK 后修订)

设计评审(`docs/reviews/2026-08-11-006-character-access-token-design-review.md`)推翻了本 spec 初稿的 3 处事实性错误,已在下文更正:

| 初稿错误 | 实际情况 | 依据 |
|----------|----------|------|
| 「ESI 刷新沿用既有 5 秒超时」 | `getAccessToken` **无超时**,裸 `.block()`;5s 超时只存在于 `getAuthorizationStatus` | `EsiApiService.java:183` vs `:447` |
| 「并发请求各自刷新,不出现串号」 | ESI refreshToken 一次性轮换,并发刷新会使一方失败并可能回写已作废 token,导致**绑定永久失效** | `EsiApiService.java:183`(无锁)vs `:282-288`(有锁) |
| 「refreshToken 加密存储」 | **明文存储**,`varchar(64)` 且无任何加解密代码 | `数据库创建脚本.sql:77` |

此外发现既有缺陷 H3:`queryAccountList` 漏 select `user_id`,导致 `EveAccount.userId` 恒为 null,accessToken 被写入 `esi_access_token:null:{characterId}` 孤儿键 —— 本 spec 依赖的「缓存键按 userId 隔离」前提当前**不成立**,须在本 feature 内修复。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 获取本人角色的 ESI AccessToken (Priority: P1)

作为已登录用户(运维/开发者),我想用我名下某个角色的角色ID换取该角色当前可用的 ESI accessToken,以便直接对 ESI 发起请求排查数据同步问题。

**Why this priority**: 这是本 feature 的唯一核心能力,无此能力则 feature 不存在。

**Independent Test**: 给定用户 U 名下已授权角色 C,以 U 身份请求 `GET /character/{characterId}/access-token`,返回带 Bearer 前缀的 accessToken、characterId 与剩余有效秒数。

**Acceptance Scenarios**:

1. **Given** 用户 U 名下存在角色 C 且 refreshToken 有效, **When** U 请求 C 的 accessToken, **Then** 返回 200 与 `{accessToken, characterId, expiresIn}`,accessToken 带 `Bearer ` 前缀
2. **Given** Redis 中已缓存 C 的 accessToken, **When** U 再次请求, **Then** 直接返回缓存值,不触发 ESI 刷新调用
3. **Given** 缓存未命中, **When** U 请求, **Then** 用 refreshToken 向 ESI 换取新 token 并返回

---

### User Story 2 - 拒绝访问非本人所有的角色 (Priority: P1)

作为系统,我必须拒绝任何用户用他人角色ID换取 accessToken,且不因错误码差异泄露「该角色是否存在」或「该角色属于谁」。

**Why this priority**: 与 US1 同等优先。token 越权等于角色接管,是本 feature 的核心安全约束;若此项不成立,US1 不得上线。

**Independent Test**: 以用户 U 身份请求不属于 U 的角色ID(分别取「存在但属他人」与「完全不存在」两种),两者返回**完全相同**的错误码与消息。

**Acceptance Scenarios**:

1. **Given** 角色 C 属于用户 V(≠U), **When** U 请求 C 的 accessToken, **Then** 拒绝访问,不返回任何 token 数据
2. **Given** 角色ID 999999999 在系统中不存在, **When** U 请求, **Then** 返回与场景 1 **完全一致**的错误码与消息(消除账户存在性 oracle)
3. **Given** 用户 U 名下角色 C 与用户 V 名下角色 D 属于同一军团, **When** U 请求 D 的 accessToken, **Then** **拒绝**(corpId 同团不构成 token 访问授权)
4. **Given** 请求方持有 ROOT/ADMIN 角色但角色 C 不在其名下, **When** 请求 C 的 accessToken, **Then** **拒绝**(ROOT 不豁免)
5. **Given** 请求未携带有效 JWT, **When** 请求任意角色的 accessToken, **Then** 拒绝(fail-closed)

---

### User Story 3 - token 不被中间环节留存 (Priority: P2)

作为系统,我必须保证返回的 accessToken 不被浏览器、代理或访问日志缓存留存,降低凭证泄露面。

**Why this priority**: 属纵深防御。US1/US2 成立后接口已可用且不越权,但凭证在传输链路的留存仍是独立风险面。

**Independent Test**: 请求成功响应中包含 `Cache-Control: no-store`;检查代码中无任何位置以明文打印 accessToken。

**Acceptance Scenarios**:

1. **Given** 请求成功, **When** 检查响应头, **Then** 含 `Cache-Control: no-store`
2. **Given** 任意成功或失败路径, **When** 检查日志输出, **Then** 日志中不含 accessToken 明文(仅记录 userId/characterId/结果)

### Edge Cases

- **角色ID 为 null 或非数字**:路径变量类型不匹配,由框架返回参数错误,不进入业务逻辑
- **角色ID 为 0 或负数**:视为参数非法,拒绝且不查库
- **角色ID 超出 Integer 范围**(EVE 角色ID 已近 int 上限 2147483647):框架类型转换失败返回参数错误;此为既有全局约束(`EveAccount.characterId` 为 Integer),本 feature 不扩大该债务
- **角色存在于本人名下但 refreshToken 为空**(从未授权/已解绑):无法换取 token,返回 ESI 授权失败,提示重新授权
- **refreshToken 已被 CCP 失效**:ESI 返回 4xx,转为 ESI 授权失败错误码
- **ESI 服务不可用/超时**:返回 ESI 服务异常,不返回半成品 token
- **同一用户并发请求同一角色**:ESI refreshToken 一次性轮换,**必须**由 per-(userId,characterId) 锁串行化;抢不到锁的请求不刷新,返回 ESI 授权失败或重读缓存。**不得**允许双方各自刷新(会导致回写已作废 token → 绑定永久失效)
- **ESI /token 挂起**:必须有 5 秒超时兜底,否则 servlet 线程被无限占用 → 线程池耗尽(可远程触发)
- **端点开关关闭时被请求**:返回 404,不暴露端点存在性

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 提供以角色ID为路径变量的只读端点,返回该角色的 ESI accessToken
- **FR-002**: 系统 MUST 仅在 `(当前登录用户ID, 请求的角色ID)` 在角色绑定表中精确命中一行时才返回 token
- **FR-003**: 系统 MUST NOT 以 corpId(军团)维度授予 token 访问权
- **FR-004**: 系统 MUST NOT 为 ROOT/ADMIN 角色豁免 FR-002 的归属校验
- **FR-005**: 系统 MUST 对「角色属他人」与「角色不存在」返回完全相同的错误码与消息
- **FR-006**: 系统 MUST 在未认证/主体无法识别时拒绝(fail-closed),不得回退为放行
- **FR-007**: 返回体 MUST 包含 accessToken(带 Bearer 前缀)、characterId、expiresIn(剩余有效秒数)
- **FR-008**: 系统 MUST NOT 在返回体中包含 refreshToken
- **FR-009**: 系统 MUST NOT 以任何日志级别输出 accessToken 或 refreshToken 明文
- **FR-010**: 成功响应 MUST 携带 `Cache-Control: no-store`
- **FR-011**: 角色ID 为 null / ≤0 时 MUST 在查库前拒绝

### Functional Requirements(设计评审后新增)

- **FR-012** (H1): 应用服务入口 MUST 显式拒绝 `userId == null || userId <= 0`,不得依赖「库中无 user_id≤0 的行」这一数据巧合实现 fail-closed
- **FR-013** (C2): ESI refreshToken 刷新 MUST 由 per-(userId, characterId) 锁串行化,并 MUST 有 5 秒超时
- **FR-014** (C1): 生产环境 MUST NOT 通过 MyBatis 参数日志或 HTTP 消息转换器 DEBUG 日志输出 refreshToken / accessToken 明文
- **FR-015** (H3): `queryAccountList` MUST 返回 `user_id` 列,使 `EveAccount.userId` 非空,保证 accessToken 缓存键按用户隔离
- **FR-016** (H4-部分): 授权拒绝与成功两条路径 MUST 记录审计日志(userId + characterId + 结果,不含 token)
- **FR-017** (M4): 该端点 MUST 由配置开关控制,生产默认关闭,关闭时返回 404;MUST NOT 出现在公开 API 文档
- **FR-018** (M1): `expiresIn` MUST NOT 由 Redis TTL 哨兵值(-1/-2)直接透出;缓存未命中时用 `AuthTokenResponse.expiresIn`,命中时归一化 TTL
- **FR-019** (M3): MUST NOT 用 `assert` 做生产环境空值校验(默认 JVM 不带 `-ea`,assert 为空语句)
- **FR-020** (M2): 上游 ESI 原始错误文本 MUST NOT 回显给客户端,须归一化为固定文案,细节仅进服务端日志

### Key Entities

- **EveAccount(角色绑定)**: 已存在实体。关键字段 `userId`(系统用户ID)、`characterId`(EVE 角色ID)、`refreshToken`(**明文存储**,`varchar(64)`,见评审 H5)。`(userId, characterId)` 构成本 feature 的授权判定依据
- **CharacterAccessTokenResult(领域读模型)**: 新增。承载 `accessToken`、`characterId`、`expiresIn`,由领域/应用层生成,接口层直接作为响应载荷

## Success Criteria *(mandatory)*

### Performance Criteria

- **SC-001**: 缓存命中路径 P95 < 200ms;缓存未命中(需调 ESI)受 ESI 5s 超时约束
- **SC-002**: 归属校验查库 < 100ms(`(user_id, character_id)` 等值查询)
- **SC-003**: ESI 刷新调用 MUST 有 5 秒超时(本 feature 新增,`getAccessToken` 原无超时)

### Code Quality Criteria

- **SC-004**: 新增代码单元测试行覆盖率 ≥80%
- **SC-005**: US2 的 5 个越权场景 100% 由自动化测试覆盖
- **SC-006**: 归属校验逻辑不新增手写实现,复用既有校验设施(避免多份实现漂移)

### Measurable Outcomes

- **SC-007**: 任意跨用户 token 获取尝试 100% 被拒绝且响应不可区分
- **SC-008**: 代码库中 accessToken 明文日志输出点为 0 处

## Assumptions

- 沿用既有 JWT 认证与 `UserUtil.getUserId()` 取当前用户,不新增认证机制
- 沿用既有 `eve_account` 表与 `queryOneUserIdAndCharacterId` 精确查询,不新增表或 DDL
- 沿用既有 Redis accessToken 缓存(键 `esi_access_token:{userId}:{characterId}`,TTL 19 分钟)。**注**:该键的用户隔离当前被 H3 破坏(存在 `null:{cid}` 孤儿键),须由 FR-015 修复后此假设才成立
- 该端点不在安全白名单内,必须经 JWT 认证。**RBAC**:上线前须审计 Redis 权限表中所有可匹配 `GET:/character/*/access-token` 的既有 Ant pattern(如 `GET:/character/**`),确认无越权继承,再新增专用权限行(评审 H2)
- 接口定位为内部调试用途,不承诺对外契约稳定性
- **改动** `EsiGateway`:新增带过期信息的取 token 方法(FR-018),并为既有 `getAccessToken` 补锁与超时(FR-013)。既有调用方(资产/工业/月矿/建筑/钱包)签名不变,但同样受益于锁与超时
- 本 feature **不做**:限流(用户决定另行评估)、refreshToken 加密存储、`refresh_token` 列加宽 DDL、`characterId` 改 Long。这些记入遗留待办

## 遗留待办(T019 登记,本 feature 明确排除)

按用户决策与 YAGNI 原则排除,但风险已确认存在,须另开 feature 处理:

| 编号 | 项 | 来源 | 风险 | 优先级建议 |
|------|----|------|------|-----------|
| L-1 | **端点限流** | 评审 H4 | 凭证铸造端点无限流。1 次 cache-miss 请求 = 1 次 ESI `/token` + `queryCharacter` + `queryUniverseNames` + DB upsert + refreshToken 轮换;characterId 连续整数,枚举成本极低。当前靠「默认关闭 + 归属校验」缓解 | 中(端点默认关闭后风险大降) |
| L-2 | **refreshToken 加密存储** | 评审 H5 | `eve_account.refresh_token` 明文存储,全仓无加解密代码。而 `CLAUDE.md` 安全红线与本 spec 初稿均**误称**「加密存储」—— 文档已更正,代码待改。泄露即角色被长期接管 | **高** |
| L-3 | **`refresh_token varchar(64)` 加宽** | 评审 H5 | ESI refreshToken 通常 76~88 字符,`varchar(64)` 在非严格 `sql_mode` 下静默截断 → 落库损坏 → 绑定失效。需先核实生产 `sql_mode` 再定 DDL | **高**(可能已在静默丢数据) |
| L-4 | **`characterId` Integer → Long** | 既有债务 | EVE 角色 ID 已近 int 上限(2112832425 距 2147483647 仅 3400 万),溢出会使归属校验对合法用户误拒 | 中(有时间窗口) |
| L-5 | **`ESI00400` 仍映射为 400 而非 403** | 评审 L2 | 归属失败走 `ESI00400`→400,WAF 按状态码统计越权探测会漏掉本端点的拒绝。改动会影响全部 ESI 错误路径,故本 feature 不改 | 低 |
| L-6 | **RBAC 既有 pattern 审计** | 评审 H2 | RBAC 是 Redis 数据驱动的 Ant 通配匹配,既有宽泛规则(如 `GET:/character/**`)会**自动**把授权继承给新路径。属**上线前运维动作**,非代码任务 | **上线前必做** |
| L-7 | **清理 `esi_access_token:null:*` 脏键** | 评审 H3 | H3 修复前已写入的孤儿键仍在 Redis 中。**严重度经阶段⑦复审上调**:这些键**不含 userId,故被多个用户共享**,构成跨用户 token 复用;键内是有效 accessToken 明文,TTL 19 分钟内可被任何能读 Redis 的人取用。修复后新代码不再读写(读路径 `accessTokenKey(userId, cid)` 永不生成 null 变体),故为孤儿键。执行 `SCAN MATCH esi_access_token:null:* + DEL` | **上线前必做**(原记「低」,低估) |
| L-8 | **`MODE_INHERITABLETHREADLOCAL` 线程池上下文泄漏** | IDOR 审计 MEDIUM-3 | `SecurityConfig` 设 `MODE_INHERITABLETHREADLOCAL`,`AsyncConfiguration` 线程池线程会长期携带创建者的 SecurityContext。本 feature 的守卫用传入 userId 不依赖 SecurityContext,故不受影响,但风险仍潜伏 | 中 |
| L-9 | **环境配置文件不入库,C1 修复无法经代码审查保证** | 本次实现发现 | `application-{prod,ali,aliw,test}.yml` 在 `.gitignore` 中。对 prod/ali/aliw 的 `log-impl` 修改仅在**本地**生效,不会随提交分发。且 **profile 属性优先级高于 `application.yml`,故根配置里的 `Slf4jImpl` 不是兜底**。阶段⑦已补 `application-prod.yml.example` 入库模板作缓解,但模板只保证「新部署有正确起点」,不阻止既有环境写错 → 根治见 L-10 | **上线前必做**(已部分缓解) |
| L-10 | **启动时安全基线 fail-fast 断言** | 阶段⑦复审 | 加 `ApplicationRunner`/`@PostConstruct`:若 `spring.profiles.active` 属生产集合(prod/ali/aliw)且 `mybatis-plus.configuration.log-impl` 含 `StdOutImpl`、或 `logging.level.web` 为 debug/trace、或 `access-token-endpoint.enabled` 为 true,则**拒绝启动**并输出明确原因。把「配置是否正确」从人的纪律变成机器的不变量 —— **这是让 C1 从「部分修复」升为「已修复」的唯一途径**,也是跨越 `.gitignore` 边界的唯一手段。<br>✅ **已由 007 实现**(2026-08-12,`007-jwt-key-rotation` T025):`infrastructure/config/security/SecurityBaselineValidator`(`ApplicationRunner`),校验上述 3 项 + `security.keystore.location`(共 4 项)。**判定方向按 fail-closed 反转**:不再枚举生产 profile,而是「仅当 active profile 明确且仅为 `test` 时豁免」—— profile 缺失或未知一律按生产校验,避免漏掉新增的生产 profile 名。测试见 `SecurityBaselineValidatorTest`(11 例) | ~~**高**(L-9 的根治方案)~~ → **已解决** |
| L-11 | **Druid `slf4j` filter 的 SQL 参数泄露通道** | 阶段⑦复审 | `filters: stat,wall,slf4j` 下 Druid `Slf4jLogFilter` 默认 `statement-log-enabled=true`,经 logger `druid.sql.Statement` 输出 SQL 参数。root=info 时关闭,但**该通道不受 `log-impl` 修复保护** —— 若有人为排查打开 `druid.sql` 的 debug,refreshToken 会经此泄露。模板已加注释警示 | 中 |
| L-12 | **补完上游错误文本回显与 `assert`** | 评审 M2/M3 | M2:`CharacterApi.java:54,56` 与 `UniverseApi.queryUniverseNames` 仍 `new EsiException(code, res.getError() + ":" + ...)`,经 `GlobalExceptionHandler:177` 原样下发上游文本 —— 本端点的「message 不可区分」通道未完全关闭,只是从 `/token` 分支移到 `queryCharacter` 分支。M3:`UniverseNameService:57`、`PageTotalApi:56`、`BotDispatcher:144` 仍有 `assert`(JVM 默认不带 `-ea`,为空语句) | 中 |
| L-13 | **清理误导性日志配置 + 测试 SecurityContext 还原** | 阶段⑦复审 | `logging.level.com.bolingcavalry.druidtwosource.mapper` 的包名与项目 `xyz.foolcat.*` 不匹配(疑从模板复制),不会打开项目 mapper 的 debug,属无害遗留但易误导。另 `EsiApiServiceAccessTokenTest` 用 `getContext()` 保存上下文,而该方法在无上下文时会**惰性创建并注册**空 context,恢复后留下空 context 而非 unset 状态(对断言无影响);宜改 `clearContext()` 或引入 `spring-security-test` | 低 |
