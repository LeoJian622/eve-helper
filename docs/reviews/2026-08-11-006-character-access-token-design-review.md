# 设计评审记录：006 角色 ESI AccessToken 查询接口

**评审阶段**: ③ 计划(设计评审,代码尚未编写)
**评审工具**: `ecc:security-reviewer`
**日期**: 2026-08-11
**Feature**: `006-character-access-token-api`
**结论**: **BLOCK** — 2 项 CRITICAL 必须在进入实现阶段前纳入 tasks

---

## 评审对象

拟新增 `GET /character/{characterId}/access-token`,返回角色访问 ESI 的 accessToken。用途限内部调试/运维排查。

初稿方案:复用既有 `EsiApiService.getAccessToken(Integer characterId, Integer userId)`,不改动其签名与语义。

---

## 一、设计中成立的部分(已复核,勿重复质疑)

| 约束 | 结论 | 依据 |
|------|------|------|
| 拒绝 corpId 维度放行 | ✅ 成立 | `EveAccountMapper.xml:312-316` 仅 `user_id AND character_id`;`EsiApiService.java:177` 用查回行的 `character.getCharacterId()` 而非原始入参,无 TOCTOU |
| ROOT/ADMIN 不豁免 | ✅ 成立 | 绕开 `AccessGuard.requireOwnership`(`AccessGuard.java:44-46` 的 ROOT 短路),归属仅由 SQL 决定 |
| 「属他人」与「不存在」不可区分 | ✅ 成立 | 两者均在 `EsiApiService.java:171-174` 同一 catch 收敛为无 message 的 `EsiException(ESI_AUTHORIZATION_FAILURE)`,经 `GlobalExceptionHandler.java:185-196` 均落 400 / ESI00400 / 同一文案 |
| 归属查库 <100ms | ✅ 成立 | `数据库创建脚本.sql:87-88` 有 `unique (character_id)`,等值查询走该唯一索引 |
| 无跨用户时序侧信道 | ✅ 成立 | 非本人角色全部单次索引查询后立即返回(不触网);快慢之差只暴露攻击者自己的绑定关系 |
| 路由无冲突 | ✅ 成立 | `POST /character/{code}`(`CharacterController.java:35`)与新路径深度不同 |

---

## 二、CRITICAL(阻断)

### C1 — refreshToken 明文经 MyBatis 参数日志进入 stdout(违反 FR-009)

**证据链**:
- cache-miss 分支 `EsiApiService.java:186` → `updateRefreshToken` → `:250` `insertOrUpdate` 全列 upsert,`refresh_token` 作为 SQL 绑定参数
- `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl` 存在于**全部 5 个 profile**:`application.yml:102`、`application-prod.yml:110`、`application-ali.yml:111`、`application-aliw.yml:134`、`application-test.yml:135` — **含 prod**
- `StdOutImpl` 打印 `==> Parameters:` 全部绑定参数
- `application.yml:90` `logging.level.web: debug` 使 HTTP 消息转换器打印响应体前 100 字符,含 `Bearer eyJ...` 片段

**为何属本 feature 责任**:本端点定位为运维反复手工调用,是全部调用方中 cache-miss 率最高的一个(叠加 H3 后近乎必然 miss)。

**独立复核**:已由主 agent 用 `grep -n "log-impl" src/main/resources/application*.yml` 与 DDL 查询确认。

**处置**:本 feature 内修(用户 2026-08-11 决定)。改 `Slf4jImpl` + `logging.level.web: info`。FR-009 的验收标准从「检查代码无 log 语句」改为「检查实际日志输出无 token 明文」。

### C2 — cache-miss 路径无并发锁、无超时

**证据链**:`EsiApiService.java:183` 裸 `.block()`。对比同文件 `determineAuthStatus`:

| 防护 | `getAuthorizationStatus` | `getAccessToken` |
|------|--------------------------|------------------|
| per-character 锁 | `:282-288`(注释明写「refreshToken 一次性使用,并发刷新会使第二个请求用已失效旧 token」) | **无** |
| 5s 超时 | `:447` `.timeout(ESI_REFRESH_TIMEOUT)` | **无** |

**后果**:
- **完整性**:并发两请求 → 两条 `updateRefreshToken` 竞态回写 `eve_account.refresh_token`(`:250` 非 selective 全列 upsert),落库可能是已被 CCP 作废的值 → **角色绑定永久失效,须重新授权**。任何登录用户反复刷本接口即可自伤。
- **可用性**:ESI /token 挂起 → servlet 线程无限期占用 → 线程池耗尽。旧设计中该方法只被定时任务调用,节流天然存在;变为外部端点后成为可远程触发的线程饥饿。

**处置**:本 feature 内补与 `determineAuthStatus` 同构的锁 + 超时。推翻 spec 初稿「不改 `getAccessToken`」的假设。

---

## 三、HIGH

| 编号 | 问题 | 处置 |
|------|------|------|
| H1 | `userId=-1` 的 fail-closed 是**数据巧合**而非显式门禁。`getAccountOne(-1, cid)` 当前确实拒绝,但仅因库中无 `user_id≤0` 的行;DDL(`数据库创建脚本.sql:84`)无 `CHECK (user_id>0)`,`UserUtil.java:39-41` 对 Number 主体原样 `intValue()` 不校验正数。对比 `AccessGuard.java:48-51,79-82` 均有显式 `<=0` 拒绝 | 修(FR-012)+ RED 测试。注:spec US2 场景 5 目前只能靠数据巧合通过,测试会假绿 |
| H2 | RBAC 是 Redis 数据驱动的 Ant 通配匹配(`RbacAuthorizationManager.java:92-118`),既有宽泛规则(如 `GET:/character/**`)会**自动**把授权继承给新路径,无需显式配置。另 `:128` ADMIN 在路由层直接放行 → 上线首日该端点可能只对 ADMIN 可达 | 上线前审计现有 pattern(记入 spec Assumption),非代码任务 |
| H3 | `EveAccountMapper.xml:318-322` `queryAccountList` **漏 select `user_id`**,而 `BaseResultMap:18` 映射了它 → `EveAccount.userId` 恒 null → `UserApplicationService.java:112,119` 调用链最终在 `EsiApiService.java:486` 写入 **`esi_access_token:null:{characterId}`** 孤儿键。三重后果:①spec 依赖的「缓存键按 userId 隔离」前提不成立;②真实键永不填充 → 本接口近乎必然 miss → 放大 C1/C2;③refreshToken 轮换开销已付、accessToken 扔进垃圾键 | 修(FR-015)。已由主 agent 独立复核 mapper XML 与 BaseResultMap 确认 |
| H4 | 凭证铸造端点无限流、拒绝路径**零审计**(`EsiApiService.java:172-174` 静默吞异常,对比 `AccessGuard.java:53` 有 `log.warn`)。放大比:1 次 miss 请求 = 1 次 `/token` + `queryCharacter` + `queryUniverseNames` + DB upsert + refreshToken 轮换;characterId 连续整数,枚举成本极低 | **部分修**:加审计日志(FR-016)。限流经用户决定另行评估 |
| H5 | spec 初稿称 refreshToken「加密存储」,实际**明文** `varchar(64)`(`数据库创建脚本.sql:77`),全仓无加解密代码。且 ESI refreshToken 通常 76~88 字符,`varchar(64)` 非严格模式下静默截断 → 落库损坏 → 绑定失效;C2 使本接口成为最高频轮换触发者,显著提高该 bug 暴露率 | spec 已更正事实描述。加密与 DDL 加宽记入遗留待办 |

---

## 四、MEDIUM

| 编号 | 问题 | 处置 |
|------|------|------|
| M1 | `expiresIn` 由 Redis TTL 推导有三重问题:①`CacheGateway.java:69-70` 约定键不存在返回 **-2**、无 TTL 返回 **-1**,哨兵值会直接进响应体;②TTL 是缓存 TTL(19 分)而非 token 有效期(20 分),契约语义错位;③应用层须复制 `EsiApiService.java:177` 的键格式 → 跨层污染(H3 已证明该格式在仓内漂移出 null 变体) | 修(FR-018):`EsiGateway` 新增带过期信息的方法,miss 用 `AuthTokenResponse.expiresIn`(`AuthTokenResponse.java:21-22` 已有),hit 在 infra 内读 TTL 并归一化 -1/-2。键构造不出 infra 层 |
| M2 | `AuthorizeOAuth.java:88` 把上游原始错误文本(`invalid_grant:Invalid refresh token...`)经 `EsiException(code, message)` → `GlobalExceptionHandler.java:177` 原样下发。虽不破坏「他人 vs 不存在」不可区分(那两例走无 message 构造器),但使「本人角色但 token 已废」可被 message 区分,并泄露 ESI 内部细节 | 修(FR-020) |
| M3 | `EsiApiService.java:184,232,234` 三处 `assert`。JVM 默认不带 `-ea` → 空语句 → NPE → `GlobalExceptionHandler.java:219-224` 返回 **500** | 修(FR-019) |
| M4 | `SecurityConfig.java:52-55` `/v3/api-docs/**` 为 `permitAll`,`application.yml:153-156` springdoc 扫描 `/**` → 一个「仅内部调试」的凭证接口,其路径与响应结构可被**未认证**者完整读到 | 修(FR-017):`@Hidden` + 配置开关默认 false |
| M5 | FR-011(characterId≤0 查库前拒绝)在复用路径下不成立 —— `getAccessToken` 无入参校验,`characterId=0` 直达 SQL | 修:controller 加 `@Validated` + `@Positive`。注:随之的 `ConstraintViolationException` 走 `GlobalExceptionHandler.java:68-77` 返回 PARAM_ERROR,与 ESI00400 不同,但不违反不可区分约束(0 非合法 characterId) |
| M6 | `EveAccountService` 类级 `@Transactional`(`:16`)。若新方法继承事务,ESI 网络调用会跑在 DB 事务内。`specs/003-esi-auth-status/research.md:47` 已为同类问题结论过用 `NOT_SUPPORTED` | plan 中显式声明本方法非事务 |

---

## 五、LOW

| 编号 | 问题 | 处置 |
|------|------|------|
| L1 | `Cache-Control: no-store` 可能已由 Spring Security 默认 `CacheControlHeadersWriter` 全局提供(`SecurityConfig.java:51-69` 未 disable headers)。应实测验证而非盲目新增代码;并确认错误响应也带该头 | 实测 |
| L2 | `GlobalExceptionHandler.java:190-196` 把授权拒绝映射为 **400**(ESI00400 既非 RESOURCE_NOT_FOUND 亦非 ACCESS_UNAUTHORIZED),语义上 403 更合适;`ESI_SERVER_FAILURE` 落 400 而非 502/504 | 属既有约定,在契约中写明,不改 |
| L3 | `CharacterApplicationService.java:34` 既有 `log.error("角色授权失败: code={}", code, e)` 把一次性 OAuth code 写进日志。新方法不得沿用此模式 | 新代码规避 |

---

## 六、门禁动作

- [x] C1、C2 纳入 tasks,进入实现阶段前推翻 spec 初稿的「不改 `getAccessToken`」假设
- [x] spec 修正 3 处事实错误(5s 超时不存在、并发会互相打断、refreshToken 未加密)
- [x] spec 新增 FR-012~FR-020 承接评审结论
- [ ] 阶段⑦ 对实现再跑一次 `security-reviewer`,重点复验 C1 的日志**实测**与 C2 的并发测试
- [ ] 阶段⑦ 追加 `ecc:java-reviewer`

## 七、用户决策记录(2026-08-11)

| 决策点 | 用户选择 |
|--------|----------|
| feature 范围 | 完整修:接口 + C2/H3 + 全部 HIGH |
| 端点开关 | 默认关闭,需显式开启;加 `@Hidden` |
| 限流与审计 | **只加审计日志**,限流另行评估 |
| C1 日志配置 | 本 feature 内修 |
