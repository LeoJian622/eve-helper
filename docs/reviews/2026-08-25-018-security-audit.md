# 018-wallet-balance-by-user — P6 安全审计

**日期**: 2026-08-25
**审计对象**: 分支 `018-wallet-balance-by-user` HEAD `dd365d9`（修复后的现状，非原始提交）
**审计人**: 安全审计子代理（独立复核，被要求不采信前一轮 Java 评审结论）
**结论**: **CONDITIONAL APPROVE** —— 0 CRITICAL，1 HIGH，3 MEDIUM，2 LOW

> 说明：`ecc:security-reviewer` 类型在本环境两次派遣均零产出卡死（输出文件 0 字节），
> 已停止并改用通用代理执行本次审计。

## 实测证据（审计轮次）

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0   （WalletBalanceApplicationServiceTest）
BUILD SUCCESS
```
IT 未跑（需远程 MySQL/Redis，该轮未验证运行态；主代理已另行验证 018 IT 3/3、017 IT 12/12 通过）。

---

## HIGH-1 — 无限流/无超时/未接缓存的 ESI 阻塞扇出：任意登录用户可全站 DoS，并可耗尽 ESI 全应用级 error limit

**位置**
- `WalletBalanceApplicationService.java:116-127`（每请求 N 人物 + 每军团组最多 N 次 ESI）
- `EsiApiService.java:1315-1317, 1320-1324`（`.block()` 无 `.timeout(...)`，且是全项目唯一绕过 `esiResponseCache` 的两个 ESI 查询，对比 `:1300-1309`）
- `EsiClientConfig.java:41-51`（`esiClient` 未配 `HttpClient`/`responseTimeout`）
- `application.yml:1-12`（Tomcat max 200 / accept-count 100）

### 场景 A（已验证）— Tomcat 线程池耗尽，全站不可用

攻击者：**任意持 `USER` 角色的普通登录账号**（无需管理员、无需越权）。
请求：`GET /wallet/balance`，自己的合法 JWT，并发 25 路循环。

单请求阻塞链：N 次 `queryCharacterWalletBalance` + 最多 N 次 `queryCorporationWalletBalances`，全部在 Tomcat 工作线程上 `.block()`，**两端都无超时**。8 个绑定人物 ⇒ 最多 16 次串行 HTTPS 往返；ESI P95 300–800ms ⇒ 单请求持有线程 5–12 秒。25 路并发即长期占用约 200 线程 ⇒ accept 队列填满 ⇒ **整个应用对所有用户不可用**。若 ESI 侧 TCP 半开/响应停滞，线程被**无界**持有，恢复需重启。

`application.yml:5-7` 的注释本身就写明了这条判据：「任何在工作线程上阻塞的逻辑都会把它们变成全站可用性上限」。018 正是引入了这样的逻辑，倍数为 2N。

### 场景 B（已验证）— ESI error limit 连坐，跨用户破坏

攻击者：同上普通 USER。前置：其 6 个人物同属一军团且**均无** Accountant/Junior_Accountant 角色（**正常玩家的常态，无需刻意构造**）。

每次请求对 `/corporations/{id}/wallets/` 发起 6 次调用 ⇒ 6 × HTTP 403。
**关键**：本轮新增的提前终止 `merged.size() >= MAX_DIVISION`（`:266-268`）在此场景**永不触发** —— 一个 division 都没取到，`merged` 恒为空 ⇒ 6 个人物全部被遍历。
2 req/s 轮询 ⇒ 720 次 ESI 4xx/分钟，全部来自本应用出口 IP。ESI error limit 为 **100 errors / 60s / IP**，超限后所有路由返回 420 ⇒ **本部署所有用户的所有 ESI 功能（资产、蓝图、工业、月矿、流水、总览）同时不可用**。
`grep -rn "X-ESI-Error-Limit" src/main/java` 零命中：应用对 error limit 完全无感知，无法自我熔断。
一个普通前端仪表盘的定时刷新就能无意触发同一后果。

### 与前一轮的分歧（审计方明确记录）

前一轮将此列为 IMPORTANT-5「调用放大」并归入「可协商延期」，交付的修复是 authorize 请求内去重 + `size()>=7` 提前终止。审计方**不同意**该定级与处置：

1. 这不是性能问题而是**安全（可用性）问题**：任意最低权限已认证用户即可触发，场景 B 的影响**跨越用户边界**。
2. 已交付的两项修复对上述场景**均无效**：authorize 去重只省 DB 单行查询（ESI 调用次数不变，`getAccessToken` 内部 `EsiApiService.java:265` 的 `getAccountOne` 仍每次一查）；提前终止在最坏的 403 场景**可证明永不生效**。
3. 真正有效的两项（`.block()` 超时、接入 015 响应缓存）恰是被延期的那两项。
4. 本仓库对同类风险已有既成对策却未复用：`WalletJournalApplicationService.java:36-40,58-59,146-154` 与 `WalletTransactionApplicationService.java:36,58` 都为「防 ESI 限流滥用」加了 60s Redis 冷却，`/wallet/balance` 没有。

### 修复方案（按性价比）

1. 复用 `requireSyncCooldown` 模式加 per-user 冷却（ESI 对 `/wallet/` 声明 120s 缓存，冷却 60s 不损新鲜度）。
2. 两个余额查询接入 `esiResponseCache`，TTL 由 `esiCacheTtlExtractor` 从响应头提取。
3. `EsiApiService.java:1316,1323` 加 `.timeout(...)`；`EsiClientConfig` 的 `esiClient` 配 `HttpClient.responseTimeout(...)`。
4. 对返回 403 的 (charId, corpId) 做短期负缓存，避免每次请求重复撞 403。
5. 读取 `X-ESI-Error-Limit-Remain`，低于阈值时熔断。

---

## MEDIUM-1 — `classify()` 把 ESI 403（`ESI00403`）误判为 `UPSTREAM_ERROR`，直接助长 HIGH-1

**位置** `WalletBalanceApplicationService.java:67-71, 194-200`；`infrastructure/external/esi/ResultCode.java:33`

`REAUTH_REQUIRED_CODES` 只含 `AUT00301` / `AUT00208` / `"ESI00400"`。而 `ESI_AUTH_PERMISSION_LOW("ESI00403")` 的语义是「该人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团」——**需要用户去改角色或重新授权的持久性权限缺失**，却被归为 `UPSTREAM_ERROR`，而 `BalanceQueryStatus` 明确把它定义为「与授权无关，重试可能恢复」。

安全后果不是数据泄漏，而是**误导客户端进入重试循环**：`status` 是调用方唯一判据，前端看到「重试可能恢复」就会重试，而 403 每次都计入 ESI error limit —— 正是 HIGH-1 场景 B 的放大源。二者是同一根因链上的两环。

**修复**：把 `ESI_AUTH_PERMISSION_LOW.getCode()` 纳入判据，或新增 `PERMISSION_DENIED` 状态位（语义更准，前端可提示「该角色需要 Accountant 权限」）。
**附带脆弱性**：`"ESI00400"` 硬编码为字符串字面量，靠注释维持一致性，无编译期或测试期约束。建议加架构测试断言其码值。

---

## MEDIUM-2 — RBAC 绑定无自动化验证；`addAll` 累加语义使未来的宽泛 pattern 可静默继承到本端点

**位置** `012_rbac_permissions.sql:110-140`（Step 5）、`:142-152`（校验 SELECT 被注释）、`WalletBalanceControllerIT.java:54-55, 76-77`、`RbacAuthorizationManager.java:99-118`

**已核验通过**：
- `url_perm` 格式 `GET:/wallet/balance` 与既有端点一致；本端点无 path 变量，`pathMatcher.match` 精确命中。
- `/wallet/balance` **未**出现在任何白名单：`application.yml:134-141`、`application-test.yml:143-147`、`application-prod.yml.example:165-170` 均只含 `POST:/user`、`POST:/auth/tokens`。
  ⚠ 但 `application-{ali,aliw,prod}.yml` 在 `.gitignore` 且本机不存在，**这三个 profile 的白名单无法验证**，部署前须人工核对（List 属性整体覆盖）。
- 「角色不存在时 `INSERT...SELECT` 静默少插」方向安全：`RbacAuthorizationManager.java:121-133` fail-closed，缺行 ⇒ 403，不会误放行。
- `InitPermissionRolesCache.java:18-25` 是 `CommandLineRunner`，启动即 `refreshPermRolesRules()`，故「SQL 后忘刷 Redis」在重启后自愈 —— 比前一轮评估的运维风险低。

**新增发现**：`RbacAuthorizationManager.java:99-118` 对**所有** Ant-匹配 `GET:/wallet/balance` 的 `url_perm` 行执行 `authorizedRoles.addAll(roles)`。若将来有人加入 `GET:/wallet/**` 并绑定更宽角色集，本端点会**静默继承**该授权，而 IT 把 `RbacAuthorizationManager` 整体 mock 成无条件放行，**无任何测试能发现**。加上 Step 5 校验 SELECT 处于注释状态，该路径完全依赖人工。

**修复**：① 增加**不 mock** `RbacAuthorizationManager` 的用例 —— 前一轮以「需写远程共享 Redis、有污染风险」暂缓，但可改为 `@MockitoBean RedisTemplate` 返回受控规则 hash，**完全绕开共享 Redis**；② 取消注释校验 SELECT 或写入部署清单必跑项。

**spec 残留不一致**：`spec.md` US1 场景 4 仍写「返回 401 Unauthorized」，实际为 403 + `AUT00301`。技术设计表已改为「白名单式」，但 US1 未同步。

---

## MEDIUM-3 — 测试未锚定 `getAccessToken` 的 `userId` 实参：历史越权参数无回归保护

**位置** `WalletBalanceApplicationServiceTest.java:93`（`getAccessToken(eq(cid), anyInt())`）、多处军团查询第二参一律 `any()`

18 个用例中**没有任何一个** `verify(esiGateway).getAccessToken(CID_1, USER_ID)`，也未断言 CORP_A 的查询确实使用了 `"Bearer token-" + CID_1`。而 `userId` 恰是本仓库历史 ESI token 缓存键越权漏洞（`f153ed4`）所在参数，`EsiApiService.java:326-328` 的缓存键 `esi_access_token:<userId>:<characterId>` 也建立在它之上。

**可利用性**：今天不可利用。但一个把 `auth.account().getUserId()` 换成常量、`SYSTEM_USER_ID` 或另一个 account 的 userId 的重构 —— **正是 017 IMPORTANT-1 真实发生过的失败模式** —— 会通过全部 18 个测试。

**修复**：`stubAccessToken` 已返回 `"Bearer token-" + cid`，变成真断言只需两行 verify。

**IT 覆盖边界的表述偏差**：`WalletBalanceControllerIT.java:70-71` 注释称归属校验「由 AuthorizeUtil 单测与 CharacterPrivacyIT 覆盖」，但实测 `CharacterPrivacyIT`/`CorporationPrivacyIT` **均未触达** `/wallet/balance`（只覆盖 `/assets/*`）。在本端点上可接受（端点零入参，无可跨租户探测的目标），但该注释比事实宽。

---

## LOW-1 — `authCache` 为普通 `HashMap`，正确性依赖两条 stream 保持串行

**位置** `WalletBalanceApplicationService.java:113, 116-118, 125-127, 172-185`

本轮重点审查项，**核验结论：当前实现安全，无跨请求/跨用户泄漏，失败态缓存不会放行**。逐条验证：

- **方法局部**：`:113` 在 `getUserWalletBalance()` 方法体内声明；服务三个字段 `:73-75` 全为 `private final`，无 `static`、无实例级可变状态 ⇒ 每次请求独立 Map，结构上不可能跨请求泄漏。
- **失败态不放行**：`computeIfAbsent` 失败时存 `AuthResult(null, status)`；`auth.account()` 的两处解引用**均**前置 `status == OK` 守卫（`:141-143` 人物路径、`:303-305` 军团路径）⇒ 缓存失败态只能导致**继续拒绝**，不存在「先失败后放行」。方向 fail-closed。
- **并发**：两处 stream 均串行，全文无 `parallel()`；mapping function 不递归回 `authorizeOnce`，无 `ConcurrentModificationException` 风险。同一用户并发请求各持独立 Map。
- **键混淆**：`computeIfAbsent(cid, id -> authorize(id))` 用形参 `id` 而非外层捕获变量，不存在「A 的结果被 B 复用」。

**残留风险（故列 LOW）**：一旦将来有人为解决 HIGH-1 的延迟问题加 `.parallel()`，`HashMap` 上会出现数据竞争。爆炸半径仍限于同一用户（Map 内所有 cid 都出自 `getAccountList(currentUserId)`，且 `getAccessToken` 内部会用 `(userId, characterId)` 二次校验），不构成越权。
**修复**：改 `ConcurrentHashMap`，或显式注明串行约束并加架构测试。

---

## LOW-2 — `EveAccount` 仍未排除 `refreshToken` 的 `toString()`

**位置** `EveAccount.java:14-16`（`@Data`）、`:46-48`；`EveAccountMapper.xml:318-320`

**本轮核验：018 未泄漏**，逐条确认：
- 服务内全部 9 条日志只记标量（`userId`/`cid`/`corpId`/`charId`/`division`/`code`），无 account、无 token。
- 本轮新增的 `logFailure`：UNAUTHORIZED 分支只打 `resultCode.getCode()`（不打堆栈也不打 message）；UPSTREAM_ERROR 分支打 `e`，而 `EsiStatusUtil.dataError()` 明确**不读取** ESI 响应体、message 一律取枚举友好文案（H2 安全红线），故异常链不携带外部内容；`ParseException`（Nimbus）message 不含 token 明文。
- 三个 VO 只承载 id/name/balance/status/partial/unavailableCharacterIds，序列化不可能包含 account。
- 已有启动期防线：`SecurityBaselineValidator` 全扫 `logging.level.*`，任一 logger 为 debug/trace 即拒启（注释明确理由是 `reactor.netty: debug` 会打 Bearer token、mapper 包 debug 会让 `refresh_token` 明文入日志）。

**残留风险**：将来一句 `log.debug("accounts={}", accounts)` 即整批落盘（虽被 baseline validator 在生产拦住 debug，但 `log.warn("accounts={}", accounts)` 不受约束）。
**修复**：加 `@ToString(exclude = "refreshToken")`。

---

## 通过项（含验证路径）

### 1. IDOR / 水平越权 —— 通过 ✅

| 环节 | 位置 | 依据 |
|---|---|---|
| 身份入口 | `WalletBalanceController.java:33-38` | 端点无 path variable / query param / body；控制器**不传递任何身份参数** |
| 身份自取 | `WalletBalanceApplicationService.java:92-98` | `UserUtil.getUserId()` 读 SecurityContextHolder；`null` 或 `<= 0` 立即抛 `ACCESS_UNAUTHORIZED`，不触达仓储（测试用 `verifyNoInteractions` 固化） |
| 主体来源 | `UserUtil.java:27-43` + `JwtAuthorizationTokenFilter.java:78-112` | principal 来自 RSA 验签通过的 JWT 的 `userId` claim；验签/过期/黑名单任一失败均走 `rejectOrPass`；claim 缺失 ⇒ -1 ⇒ fail-closed |
| 列表过滤 | `EveAccountMapper.xml:318-330` | `where user_id = #{userId}` 参数化 |
| 归属复校 | `AuthorizeUtil.java:37-44` | `authorize(cid)` **忽略任何传入 userId**，自行从 SecurityContext 重取；`getAccountOne` 双谓词过滤，查无即抛 `USER_ACCOUNT_NOT_EXIST` |
| 军团 ID 来源 | `:121-123` | `corpId` 全部派生自 `where user_id` 结果的 `getCorpId()` 分组，**非外部输入** |

**审计方补充的更根本依据**（前一轮未引用）：`数据库创建脚本.sql:72,88` —— `eve_account` 有 `unique (character_id)`，即**一个人物在全表只能绑定到一个用户**。加上写入 `eve_account` 的路径经 grep 确认只有 `EsiApiService.java:503`（`updateRefreshToken`）与 `:1408`，二者都必须持有有效的 EVE SSO refresh token，**不存在任何控制器可让用户自助注入任意 character_id**。因此「用户 A 的账号列表里出现他人的人物」在**数据模型层面即不可达**，而非仅靠 `where user_id` 一道谓词。这也是判定军团并集安全的前提。

### 2. fail-open 鉴权 —— 通过 ✅（独立复核）

逐个检查每个 catch 出口，确认**没有任何路径在鉴权/归属失败时放行或混入他人数据**：
- `authorizeOnce`（`:172-185`）：两类异常均返回 `account` 为 null 的 `AuthResult`，**没有任何分支返回可用 account**。
- 消费侧两处守卫齐备（`:141-143`、`:303-305`），非 OK 一律提前返回，不进入 token 换取。
- `buildCharacterBalance`（`:150-162`）与 `fetchSingleCharCorpBalances`（`:310-319`）三段捕获，全部返回 null balance / null 列表 + 非 OK 状态，**不返回 0.0，不返回他人数据**。
- **纵深防御**：即使应用层守卫被绕过，`EsiApiService.doGetAccessToken`（`:262-277`）在**读缓存之前**先做 `getAccountOne(userId, characterId)`，失败即抛。该顺序（DB 校验先于 Redis 读）是关键 —— 意味着即使 Redis 里存在 `esi_access_token:<attacker>:<victimChar>` 这样的键，也永远读不到。

**与前一轮的分歧**：同意「方向 fail-closed」，但**不同意「IMPORTANT-2 修复已完成」** —— 状态位落地了，可分类逻辑本身是错的（MEDIUM-1），且错误方向恰好指向「鼓励重试」，与 HIGH-1 构成同一根因链。应视为「部分修复」。

### 3. ESI token 缓存键越权 —— 通过 ✅

`:145`、`:307` 的 `auth.account().getUserId()`：`auth` 由 `authorizeOnce(cid)` 产出，其 account 来自 `where user_id = <当前登录> and character_id = cid` ⇒ **恒等于**当前登录 userId，是恒等透传而非可污染的自由变量。缓存键双维隔离。要为他人 cid 换 token，需 `getAccountOne(myUserId, theirCid)` 返回行 —— 在 `unique (character_id)` + 双谓词下不可达。`f153ed4` 在本路径不可复现。

### 4. 军团分账并集 —— 通过 ✅（无越权）

参与并集的每个 token 都属于**当前登录用户自己的人物**，每次 ESI 调用由 CCP 按该人物的军团角色独立裁决，返回的每条 division 都是 ESI 授权给「用户本人控制的人物」的数据。应用只做并集，不构造、不猜测、不越过 ESI 判定。US2 明确要求此行为。

「凑满 1..7 提前终止」的安全影响：**无**。仅减少调用；被跳过的人物既不参与合并也不进入 `failedCharacterIds`，此时 7 个分账已全部取到，`partial=false` 正确。（其唯一副作用是 403 场景永不生效，已计入 HIGH-1。）

纯信息性（非缺陷）：军团 Director 若刻意给人物 A 只授 division 1、给人物 B 只授 division 4 以实现职责分离，且不知二者是同一玩家，则并集在业务语义上突破了其意图。但 CCP 权限模型无法表达「跨角色不得聚合」，且玩家手动登录两个角色一样能看到全部 —— 不属于本应用的授权缺陷。

### 5. 新增响应字段的信息泄漏 / 侧信道 —— 通过 ✅

- `unavailableCharacterIds`：每个 ID 都来自调用者自己的 `getAccountList` 结果，且**同一响应的 `characters[]` 里已全部列出** ⇒ 零增量信息。
- `BalanceQueryStatus` 的状态区分**不构成账号存在性 oracle**：构成 oracle 需要「攻击者可控的探测参数」，而本端点 path/query/body **全为空**，攻击者无法指定任何目标；返回状态全部关于其本人的人物。（相比之下 `/wallet/overview/corp/{cid}` 才有可探测入参，其存在性口径属既有 MINOR-7。）
- `USER_ACCOUNT_NOT_EXIST` → UNAUTHORIZED 的映射只会在「查询期间人物被并发解绑」时出现，不构成信息通道。

### 6. 其他已核验

- **SQL 注入**：相关查询全 `#{}` 参数化，无 `${}` 拼接。
- **响应缓存头**：`SecurityConfig.java:43-61` 未关闭默认头 ⇒ Spring Security 注入 `Cache-Control: no-cache, no-store, must-revalidate`，财务数据不被中间代理缓存。
- **CSRF disabled** 对本端点无影响：GET + Bearer，不依赖 cookie，无 ambient credential。
- **NPE / 500 面**：IMPORTANT-3 的 `division=null` 已前置过滤（1..7 白名单），测试覆盖 null 与越界 0/8，`comparingInt` 拆箱 NPE 不再可达。
- **017 IMPORTANT-1 真修**：grep 确认 `resolveCharIdInCorp` 已删除，`:139` 改为 `fetchCorporationBalances(corpId, account)`，不再从 scope 反查、不再退化到 `SYSTEM_USER_ID`。
- **IMPORTANT-4 真修**：`getUserWalletBalance()` 已无参、身份自取，两个边界测试固化「不触达仓储」。
- 无硬编码密钥、无空 catch、无 `Optional.get()`、测试无 `Thread.sleep()`。

---

## 处置建议（审计方原文）

**阻塞合并**
- HIGH-1：至少落地 `.timeout(...)` + `esiClient.responseTimeout` + per-user 冷却三项。均为局部改动、零 API 变更，不需新一轮 spec 修订。

**建议本轮顺手修（合计 < 15 行）**
- MEDIUM-1：`ESI00403` 纳入判据（或新增状态位）—— 同时削弱 HIGH-1 场景 B。
- MEDIUM-3：两行 verify 断言，为历史越权参数补回归保护。
- LOW-2：`EveAccount` 加 `@ToString(exclude = "refreshToken")`。

**登记 follow-up**
- MEDIUM-2：不 mock RBAC 的 IT（用 `@MockitoBean RedisTemplate` 绕开共享 Redis 污染顾虑）；取消注释 Step 5 校验 SELECT；同步 `spec.md` US1 场景 4 的 401→403。
- LOW-1：`ConcurrentHashMap` 或串行约束显式化。
- 部署前人工核对 `application-{ali,aliw,prod}.yml` 的 `whiteUrlList`（本机不存在，审计无法覆盖）。
