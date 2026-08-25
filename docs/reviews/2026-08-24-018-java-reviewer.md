# 018-wallet-balance-by-user — P6 Java 代码评审

**日期**: 2026-08-24
**评审人**: `ecc:java-reviewer`（子代理）
**范围**: commit `a836528` 的 11 个 Java 文件 + `src/SQL/convert/012_rbac_permissions.sql`
**分支**: `018-wallet-balance-by-user`
**结论**: **CONDITIONAL APPROVE** —— 无 CRITICAL，8 个 IMPORTANT，9 个 MINOR

> 按仓库 G6 门禁标准（无 Critical / Important 未修复项），本轮**尚未满足** P6 通过条件。

## 验证证据（评审轮次实测）

| 命令 | 结果 |
|------|------|
| `./mvnw -o test -Dtest=WalletBalanceApplicationServiceTest` | `Tests run: 11, Failures: 0, Errors: 0` / `BUILD SUCCESS` |
| `target/surefire-reports/...WalletBalanceControllerIT.txt` | `Tests run: 3, Failures: 0, Errors: 3`，根因 `com.mysql.cj.jdbc.exceptions...` → ApplicationContext 加载失败（本机 MySQL 不可达，已知环境噪声，非代码回归） |

## IDOR 专项结论：新端点干净 ✅

`GET /wallet/balance` 逐环节核验，**不存在 IDOR**：

- `WalletBalanceController.java:36` —— `userId` 唯一来源 `UserUtil.getUserId()`（读 `SecurityContextHolder`），无 path/query/body 注入入口。
- `AuthorizeUtil.java:37-44` —— `authorize(cid)` **忽略调用方传入的任何 userId**，自行从 SecurityContext 重取，`userId <= 0` 直接抛 `ACCESS_UNAUTHORIZED`（fail-closed），再 `getAccountOne(userId, characterId)`。
- `EveAccountMapper.xml:318-323` —— `where user_id = #{userId}`，参数化按用户过滤。
- 故 plan.md 中被质疑的 `account.getUserId()`（`WalletBalanceApplicationService.java:150,165`）**安全**：该 `account` 由 `authorize()` 用当前登录 userId 过滤查出，`account.getUserId() ≡ 当前登录 userId`，恒等透传，不可污染。
- `EsiApiService.java:271-273` —— token 缓存键 `ESI_ACCESS_TOKEN_KEY + userId + ":" + characterId`，用户 + 角色双隔离，历史缓存键越权漏洞（`f153ed4`）在本路径不可达。
- 军团 `corpId` 全部派生自 `accounts`（当前用户自有行）的 `getCorpId()` 分组（`:74-76`），非外部输入。
- **军团多角色合并无跨用户泄漏**：参与合并的 token 全属同一登录用户自己的人物；union 是规格 US2 明确要求，EVE 侧 division 可读性由 ESI 按各自 token 独立裁决，本代码只做并集。

补充核验（主代理独立完成）：`application.yml:134-141` 的 `whiteUrlList` 仅含 `POST:/user`、`POST:/auth/tokens`，`/wallet/balance` **未被误加白名单**，认证要求生效。

## IMPORTANT（阻塞 G6）

### IMPORTANT-1 — ADMIN/ROOT 查军团总览 `currentBalance` 恒为 0（017 引入的功能回归）

`WalletOverviewApplicationService.java:128-141` + `:241-247`

`:128` 刚用 `authorize(cid)` 拿到"确定属于当前用户、确定在目标军团"的 `cid`，`:129` 取出 `corpId` 后即丢弃 `cid`；`:141` 调 `fetchCorporationBalances(corpId, scope)` 让它**从 scope 重新反查角色**：

```java
// :241-247
int lookupUserId = scope == null ? GlobalConstants.SYSTEM_USER_ID : scope.intValue();
Integer charId = resolveCharIdInCorp(corpId, lookupUserId);
```

`AccessGuard.java:100-104`：ROOT 的 `corporationScope()` 返回 **null** → `lookupUserId = SYSTEM_USER_ID(1)` → 查**系统用户**的角色列表。

**失败场景**：ADMIN 绑定人物 C（属军团 X），调 `GET /wallet/overview/corp/C` → `authorize(C)` 通过 → `corpId=X` → ROOT ⇒ `scope=null` ⇒ 用 SYSTEM_USER_ID(1) 的账号列表找军团 X 的角色 → 系统用户在军团 X 无角色 → `charId=null` → `:246` 返回 `emptyList()` → `balanceByDivision` 空 → `:172-178` `currentBalance = 0.0`、divisions 全零。**管理员看到自己军团余额恒为 0，HTTP 200 无任何错误提示。**

`WalletOverviewControllerIT.java:94-95` 恰好掩盖了此 bug：`lenient().when(eveAccountRepository.getAccountList(1)).thenReturn(List.of(account))` 让 SYSTEM_USER_ID "恰好"拥有该角色，生产环境不成立。

**主代理复核补充**：即使非 ROOT 用户也存在语义偏差 —— `resolveCharIdInCorp` 用 `findFirst()` 挑角色，可能挑到与用户请求的 `cid` **不同**的另一个角色，而不同角色对军团钱包的 ESI 权限并不相同。且 `spec.md:15` 本就把该 `findFirst()` 列为待解决缺陷，018 只在新端点做了多角色合并，**未修 016/017 的军团总览路径** —— 属"spec 已识别却漏做的一半"。

**修复**：`getCorporationOverview` 既已 `authorize(cid)`，应直接把已授权的 `cid` 传给余额查询，删除 `resolveCharIdInCorp` 反查：
```java
List<WalletDivisionBalance> esiBalances = fetchCorporationBalancesByChar(corpId, cid);
```
同时修 IT：移除 `getAccountList(1)` 的 stub，改为断言 `queryCorporationWalletBalances` 使用 `cid` 换来的 token。

### IMPORTANT-2 — fail-open 吞异常：授权失败/上游故障与"余额真为 0"不可区分

`WalletBalanceApplicationService.java:153-156,168-171`

```java
} catch (Exception e) {
    log.warn("人物余额查询失败: cid={}", cid, e);
    return 0.0;
}
```

`catch (Exception)` 覆盖 `EveHelperException(ACCESS_UNAUTHORIZED)`、`EsiException(ESI_AUTHORIZATION_FAILURE)`、`EsiException(ESI_SERVER_FAILURE)`、`ParseException`、所有 `RuntimeException`，一律降级 `0.0`。

- **场景 A（业务误导）**：玩家 refreshToken 在 EVE 官网被撤销 → `doGetAccessToken` 抛 `ESI_AUTHORIZATION_FAILURE` → 捕获 → `balance = 0.0`。客户端渲染"Alice: 0 ISK"，玩家实际持有 100 亿 ISK。用户无法区分"我没钱"与"你没查到"，可能据此做出错误交易决策；无任何字段提示需重新授权。
- **场景 B（军团总额静默缩水）**：军团 X 有 7 分账，唯一有 Accountant 的人物 token 过期 → `mergeCorporationBalances` 得空 map → `:104-106` `totalBalance = 0.0`、`divisions = []`。返回 200，与"军团真的是 0"完全同形。
- **场景 C（部分失败被求和掩盖）**：3 个分账查到、4 个失败 → `totalBalance` 只是 3 个之和，客户端当"军团总资产"，**系统性低估且无告警**。

注意：**不构成越权静默成功** —— 归属校验失败返回 0.0 而非他人数据，方向 fail-closed；问题是**故障不可观测**。

**修复**：① VO 增显式状态位（`status`: `OK`/`UNAUTHORIZED`/`UPSTREAM_ERROR`，非 OK 时 `balance = null` 而非 `0.0`；`CorporationBalanceVO` 增 `partial` + `unavailableCharacterIds`）；② 收窄 catch，与 016 既有做法对齐（`WalletOverviewApplicationService.java:226-232` 分 `ParseException`/`RuntimeException` 两段）；③ 日志分级：授权类 warn 不打堆栈，上游 5xx 打堆栈。

### IMPORTANT-3 — `division = null` → `mergeCorporationBalances` NPE → 整个端点 500

`WalletBalanceApplicationService.java:131,138-141`

`WalletDivisionBalance.java:9` 的 `division` 是 **`Integer`**（可空）。`:131` `merged.putIfAbsent(b.division(), ...)` —— `HashMap` 接受 null key，不报错。随后：

```java
// :138-141（在 try/catch 之外）
return merged.entrySet().stream()
        .map(e -> new WalletDivisionBalance(e.getKey(), e.getValue()))
        .sorted(Comparator.comparingInt(WalletDivisionBalance::division))   // ← Integer 拆箱
        .toList();
```

`Comparator.comparingInt` 接收 `ToIntFunction`，`WalletDivisionBalance::division` 返回 `Integer` 被自动拆箱 → **null 时 NPE**。

**失败场景**：ESI `/corporations/{id}/wallets/` 某条记录 `division` 缺失或为 null（畸形响应、字段改名、代理错误页被部分反序列化）→ `EsiApiService.java:1322` 构造 `new WalletDivisionBalance(null, x)` → `:131` 存入 null key → `:140` NPE。**该 NPE 在 try/catch 之外**（`:126-136` 的 try 只包住单角色取数），沿 `buildCorporationBalance` → `:78-80` stream → `getUserWalletBalance` 上抛 → **整个 `/wallet/balance` 返回 500，人物余额与其他军团的正常数据一并丢失**。

**修复**：循环内前置过滤非法 division：
```java
for (WalletDivisionBalance b : balances) {
    if (b.division() == null || b.division() < 1 || b.division() > 7) {
        log.warn("军团余额 ESI 返回非法 division: corpId={}, charId={}, division={}",
                corpId, account.getCharacterId(), b.division());
        continue;
    }
    merged.putIfAbsent(b.division(), b.balance() == null ? 0.0 : b.balance());
}
```
并补单测：`ESI 返回 division=null → 跳过该条且不影响其他分账`。

### IMPORTANT-4 — `getUserWalletBalance(Integer userId)` 把信任边界外移，`getAccountList` 无归属断言

`WalletBalanceApplicationService.java:55-63`

服务签名接收 `userId` 作为**普通参数**，`:57-60` 只校验 `> 0`，`:63` 直接 `getAccountList(userId)`，**无任何断言 `userId == 当前登录主体`**。当前唯一调用方（`WalletBalanceController.java:36`）传 `UserUtil.getUserId()`，故今天安全 —— 但保护完全依赖调用点自觉。

**失败场景**：将来有人加"管理员查看指定用户余额"的重载/端点，传入请求参数的 `targetUserId`：
- `:63` `getAccountList(targetUserId)` → 返回**他人**全部人物（`characterId` + `characterName` + `corpId` + `corpName`）；
- `:69-71` 每个人物走 `fetchCharacterBalance` → `authorize(cid)` 用**调用者自己**的 userId 校验 → `getAccountOne` 查不到 → `USER_ACCOUNT_NOT_EXIST` → 被 `:153` 吞掉 → `balance = 0.0`；
- 结果 HTTP 200，**泄漏他人全部人物 ID / 人物名 / 军团 ID / 军团名**，余额全 0，日志仅 warn 无越权告警。

此为本仓库 IDOR 历史缺陷的同一形态（数据列表无归属过滤 + 下游 fail-open 吞异常掩盖）。

**修复**（推荐 ①）：① 服务内自取身份，与 plan.md Step 2 原始设计一致 —— 无参 `getUserWalletBalance()`，内部 `UserUtil.getUserId()`；② 或保留参数但加运行时不变量：
```java
Integer current = UserUtil.getUserId();
if (current == null || current <= 0 || !current.equals(userId)) {
    log.warn("钱包余额聚合:声明身份与当前主体不一致 declared={}, current={}", userId, current);
    throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
}
```

### IMPORTANT-5 — ESI/DB 调用放大：无去重、无提前终止、无超时、未接入 015 缓存

`WalletBalanceApplicationService.java:69-80,147-157,162-172`（N = 人物数）

| 环节 | 次数 | 说明 |
|---|---|---|
| `authorize(cid)` → `getAccountOne` → **DB** | **2N** | `:149` 人物路径 + `:164` 军团路径各 1 次 |
| `esiGateway.getAccessToken` | 2N | Redis 命中则廉价；未命中触发 refreshToken 轮换 + 分布式锁 |
| ESI 人物余额 | N | `:151` |
| ESI 军团余额 | N | `:166`，**按人物数重复，不按军团数** |

- **冗余 1（N+1）**：`:63` 的 `getAccountList(userId)` 已把全部 `EveAccount`（含 `refreshToken`、`userId`）载入内存，`:149`/`:164` 却又为每个 cid 各查一次库拿同一行。8 人物 = **16 次多余单行查询**。
  **失败场景**：8 人物分属 2 军团 → 16 次 DB 单行查询 + 16 次 ESI 串行阻塞（`EsiApiService.java:1316` `.block()`）。ESI 单次 P95 约 300–800ms → **端点 P95 约 5–12 秒**，Tomcat 线程被长时间占用；前端仪表盘轮询会很快打满线程池。
- **冗余 2（无提前终止）**：`:125-137` 无条件遍历该军团下**所有**人物。ESI 一次返回该 token 可见的全部分账；若第一个人物已返回 1..7 全部 division，后续调用**零贡献**。建议 `if (merged.size() >= 7) break;` 并按"最可能有权限"排序候选。
- **冗余 3（无超时）**：`WalletApi.java:99-106` / `EsiApiService.java:1316,1321-1323` 的 `.block()` **无 `.timeout(...)`**，`EsiClientConfig.java:41-51` 的 `esiClient` 也**未配 `responseTimeout`/`HttpClient`**。对比 `EsiApiService.java:305` 的 refreshToken 路径明确带 `.timeout(ESI_REFRESH_TIMEOUT)`。ESI 卡住时请求线程无界等待。
- **冗余 4（未接入 015 缓存）**：`EsiApiService.java:1315-1324` 的两个余额方法是全项目**唯一**绕过 `esiResponseCache` 的 ESI 查询（对比 `:1300-1309` 流水查询走 `doListWithClaim` + `esiCacheTtlExtractor`）。016 为"实时通路"刻意如此，可接受；但 018 将其放大 2N 倍。ESI 对 `/characters/{id}/wallet/` 声明 120s 缓存，**同分钟内重复调用只消耗自己的 error limit（100/min per IP），拿不到更新数据**。
  **失败场景（error limit 连坐）**：某用户 3 个人物 token 失效 → 每次请求 3 次 ESI 4xx；前端 5 秒轮询 → 每分钟 36 次错误。多用户叠加即触发 ESI **全应用级** error limit 封禁，**所有用户的所有 ESI 功能一起不可用**。

**修复（优先级序）**：① 消除 2N 次 DB 查询 —— `getAccountList` 结果建 `Map<Integer, EveAccount>`，`fetch*` 改为接收已授权的 `EveAccount`，不再调 `authorize`（归属已由 `getAccountList(userId)` 的 `where user_id` 保证）；② 加 `merged.size() >= 7` 提前 break；③ 接入 015 `esiResponseCache`，TTL 用 `esiCacheTtlExtractor` 从 `Expires` 头提取（与 ESI 自身 120s 对齐，不损新鲜度）；④ 给 `.block()` 加 `.timeout(...)`，N 个人物考虑用现有线程池并发。

### IMPORTANT-6 — RBAC 门禁无自动化覆盖，且 spec 承诺与实现不符

`012_rbac_permissions.sql:111-132`、`WalletBalanceControllerIT.java:52-53,64-65`

`RbacAuthorizationManager.java:99-131` 是 **fail-closed**：URL 无匹配 `sys_permission` 行 → `authorizedRoles` 空 → `hasPermission = false` → 403（仅 `ROOT_ROLE_CODE`/ADMIN 直通）。但 IT 把它整体 mock 成无条件放行：

```java
// WalletBalanceControllerIT.java:52-53,64-65
@MockitoBean RbacAuthorizationManager rbacAuthorizationManager;
lenient().when(rbacAuthorizationManager.check(any(), any()))
        .thenReturn(new AuthorizationDecision(true));
```

**失败场景**：Step 5 SQL 未执行（或执行后未刷新 `URL_PERM_ROLES_KEY` Redis 缓存 —— 脚本头部 `:55-56` 提到需 `refreshPermRolesRules`，Step 5 注释**未重复该提醒**）→ 所有 USER 角色账号访问 `/wallet/balance` 得 403，仅 ADMIN 可用。**无任何测试能发现**，因 IT 已将 RBAC 短路。

**spec 与实现不符**：`spec.md:53` 写"权限 | 需登录（任何已认证用户可访问）"、`:158` "映射到普通用户角色（所有登录用户可访问）"。实现是**白名单式**：`:132` `AND sr.code IN ('ADMIN', 'USER')` —— 仅 code 恰为 `ADMIN`/`USER` 的角色可访问，任何自定义角色（`GUEST`、`CORP_MEMBER`）的已认证用户 → 403。

另注：`src/SQL/convert/` 下无 `sys_role` 种子数据（脚本 `:43` 自承"admin 角色可能为运维外部预置"）。若目标库 `code='USER'` 行不存在，`:128-132` 的 `INSERT ... SELECT` 只插 ADMIN 一行，**静默少插无报错**。

**修复**：① 统一 spec 与实现口径，修 `spec.md:53,158`；② Step 5 增加执行后校验与"刷新 Redis 权限缓存"步骤：
```sql
SELECT sp.url_perm, sr.code FROM sys_permission sp
  JOIN sys_role_permission srp ON srp.permission_id = sp.id
  JOIN sys_role sr ON sr.id = srp.role_id
WHERE sp.url_perm = 'GET:/wallet/balance';   -- 期望 2 行(ADMIN/USER)
```
③ IT 增加**不 mock** `RbacAuthorizationManager` 的用例：向 Redis 写 `GET:/wallet/balance → [USER]`，断言 USER 200、无规则时 403。

### IMPORTANT-7 — 018 集成测试从未通过，提交信息的覆盖声明缺乏证据

`WalletBalanceControllerIT.java:80-133`

实测：`Tests run: 3, Failures: 0, Errors: 3`，全部 `Failed to load ApplicationContext`，根因 MySQL 不可达。提交信息声明"新增集成测试覆盖认证校验和正常查询"，但**该覆盖在任何可核验运行记录中都不存在**（`target/surefire-reports` 下 7 个 Wallet* IT 全部 100% Errors，属既有环境问题，非本次引入）。按"证据铁律"，该覆盖声明不成立。

两处设计有效性疑问（本环境无法实测，需在可用环境验证后定级）：
- `:74-78` `mockAuthenticated()` 直写 `SecurityContextHolder`。Spring Security 6 的 `SecurityContextHolderFilter` 会在链入口用 `securityContextRepository` 的（空）上下文 `setDeferredContext(...)` 覆盖 holder。若覆盖生效，`normalQuery_returnsCorrectStructure` 会拿到匿名主体 → `UserUtil.getUserId()` 返回 -1 → `:57-60` 抛 `ACCESS_UNAUTHORIZED` → 403，断言 200 失败。**该测试的通过与否从未被观测。**
- `:82-87` `unauthenticated_returns401` 仅断言状态码 401。由于 `JwtAuthorizationTokenFilter.java:67-71` 对"无 Authorization 头"直接放行进链、由后续 `rejectOrPass`/entry point 决定，该断言对任何原因的 401 都成立，**无法区分"认证生效"与"配置错误导致一律 401"**。

**修复**：① 用 `@WithMockUser` / `SecurityMockMvcRequestPostProcessors.authentication(...)` 替代直写 `SecurityContextHolder`；② 401 用例同时断言错误码 `AUT00210`；③ 项目级：`pom.xml:198-220` 无 `maven-failsafe-plugin`、surefire 无 `<includes>`，`*IT` 执行完全依赖本地 DB。建议引入 Testcontainers（MySQL + Redis）或至少把 IT 拆到独立 profile + failsafe，让 CI 能给出可核验 IT 证据；当前状态下应把这 3 个用例登记进 `docs/reviews/disabled-tests-inventory.md`。

### IMPORTANT-8 — 017 的破坏性 API 变更混入 018 提交，无版本化/弃用过渡

`WalletOverviewController.java:59-68`、`WalletOverviewApplicationService.java:116`

URL **形状不变**、**语义反转**：`GET /wallet/overview/corp/{x}` 的 `{x}` 由**军团ID**改为**人物ID**。

**失败场景**：现有客户端继续传 `corpId=98454654` → `authorize(98454654)` → `getAccountOne(userId, 98454654)` 查无此行 → `USER_ACCOUNT_NOT_EXIST` → `GlobalExceptionHandler.java:179` 映射为 **HTTP 400 + `AUT00208「账号不存在」`**。客户端得到与真实原因（参数语义变了）完全无关的错误码，排障成本极高；且路径形状相同，**没有 404 提示"端点变了"**。

**流程问题**：本提交同时包含 `specs/017-*` 的 spec/plan/tasks 与 017 的生产代码变更，落在 `018-wallet-balance-by-user` 分支上，违反"单一事实源"与"特性轨必须建独立特性分支 `<NNN>-<feature>`"（CLAUDE.md / DEVELOPMENT-WORKFLOW.md P0 门禁，宪法 V.1）。017 因此没有独立的 G1/G2/G6 门禁记录。

**修复**：① 新增 `GET /wallet/overview/corp/by-character/{cid}` 承载新语义，旧 `/corp/{corpId}` 标 `@Deprecated` 保留过渡期（或至少在 `corpId` 形态无法解析为自有人物时返回明确提示的错误码，而非复用 `AUT00208`）；② 017 代码变更从 018 提交拆出，走独立分支与门禁；`docs/knowledge/钱包总览-wallet-overview.md` 同步更新入口参数语义。

## MINOR

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| 1 | `WalletBalanceApplicationService.java:131` + `EveAccountMapper.xml:318-323` | `putIfAbsent` 的"首个胜出"依赖 `charsInCorp` 顺序，而 `queryAccountList` **无 `ORDER BY`** —— InnoDB 不保证行序。同数据两次请求可能选中不同人物 token；`WalletBalanceApplicationServiceTest.java:267-269` 断言的 `100.0` 胜出因此并非契约 | `queryAccountList` 加 `order by character_id`，并注明"取 characterId 最小者" |
| 2 | `WalletBalanceApplicationService.java:147-157` vs `WalletOverviewApplicationService.java:220-233` | DRY：`fetchCharacterBalance` 与 016 一字不差（仅 catch 粒度不同）。这是**安全敏感路径**（授权 + token 换取），重复意味未来加固须改两处，漏一处即行为分叉 | 抽 `WalletBalanceFetcher`（application 协作者）或提升至 domain service，两服务共用 |
| 3 | `:153`、`:168` | `catch (Exception)` 相比 016 的分段捕获（`WalletOverviewApplicationService.java:226-232`）是纪律退化，会吞掉 `IllegalStateException`/`ClassCastException` 等**编程错误**，使 bug 表现为"余额 0"而非快速失败 | 与 016 对齐分段捕获 |
| 4 | `012_rbac_permissions.sql:97,100-107,111-132` | Step 4/5 在 `COMMIT;` **之后**，脱离事务。Step 5 先删后插，autocommit 下若 DELETE 成功、INSERT 失败（连接中断、`sys_role` 缺 `USER` 行）会留下"权限行不存在"中间态。RBAC fail-closed 故后果是 403（方向安全），但与脚本 `:45-53` 自述的"事务内先删后插"幂等承诺不一致 | Step 4/5 各自包进 `START TRANSACTION; ... COMMIT;` |
| 5 | `CharacterBalanceVO.java:14`、`CorporationBalanceVO.java:19`、`:104-106` | 金额用 `Double`。ISK 量级 1e9–1e12，`double` 有效数字 15–16 位，1e12 带 2 位小数已近精度边界；7 分账连加引入尾数漂移（前端显示 `5000000000.000001`）。与 016 现状一致故列 MINOR | 新特性起用 `BigDecimal`，或至少序列化层统一 scale=2 舍入，并在 knowledge 登记口径 |
| 6 | `WalletBalanceApplicationServiceTest.java:204-214,239-242,267-269` | 单测是"mock 回放"而非行为断言：三处合并用例用 `queryCorporationWalletBalances(eq(CORP_A), any())` + 连续 `thenReturn`，第二参为 `any()` → **无法区分是哪个人物的 token 发起调用**。故无法证明 `authorize(CID_1)`/`getAccessToken(CID_1,...)` 真被用于首次军团查询；无法证明失败人物后确实**换了另一人物 token** 重试；无 `verify` 断言 `getAccessToken` 的 `userId` 实参 —— 而这正是本仓库历史越权缺陷所在参数 | token stub 按人物区分（`stubAccessToken` 已返回 `"Bearer token-" + cid`），断言 `verify(esiGateway).queryCorporationWalletBalances(CORP_A, "Bearer token-" + CID_1)`、`verify(esiGateway).getAccessToken(CID_1, USER_ID)`。缺失用例：`division=null`、`corpName` 为 null、`getAccountList` 含 null 元素、`balance` 为负、同 corpId 下所有人物全失败 |
| 7 | `GlobalExceptionHandler.java:179,204` | `USER_ACCOUNT_NOT_EXIST` → HTTP 400 语义偏差。"访问不属于自己的人物"语义上是 403（或统一 404 防存在性 oracle），400 会让客户端当"参数格式错误"重试。属既有行为，本次被 `WalletOverviewControllerIT` 新用例 `corpOverview_foreignCharacter_badRequest` 固化为断言 | 单独立项统一归属失败状态码口径；至少在 017/018 knowledge 记录当前口径 |
| 8 | `docs/knowledge/`、`docs/INDEX.md` | 缺 017/018 的交付文档与 INDEX 登记；017 已改变 `钱包总览-wallet-overview.md` 中的端点入参语义，该文档现已过期 | 新增 `用户钱包余额聚合-wallet-balance-by-user.md`，更新 `钱包总览-wallet-overview.md` 军团入口说明，登记 INDEX |
| 9 | `EveAccount.java:14-15,49` | `@Data` 生成的 `toString()` 含 `refreshToken`，而 `queryAccountList`（`EveAccountMapper.xml:319`）**确实 select 了 `refresh_token`** → `:63` 持有的每个 `EveAccount` 都带（加密的）refreshToken。本次新增代码**未泄漏**（`:134`/`:154`/`:169` 只记标量，VO 只承载 id/name/balance，已核验），但将来一句 `log.debug("accounts={}", accounts)` 就会整批落盘 | `EveAccount` 加 `@ToString(exclude = "refreshToken")`，从根上消除此类误用 |

## 通过项（已核验）

- **DDD 分层纯度合规**：三个 VO 仅 import `java.util.List`，无 application/infrastructure/interfaces 依赖；`WalletBalanceApplicationService` 的 import 全指向 `domain.*` + `shared.*`，无 PO 泄漏；controller 只做身份取值 + 委派。
- **[SPRING] 依赖注入**：`@RequiredArgsConstructor` 构造器注入，无 `@Autowired` 字段。
- **[SPRING] 无可变单例状态**：三字段均 `private final`；`HashMap merged`（`:124`）为方法局部变量，无竞态。
- **`@Transactional` 层次正确**：查询路径无事务标注。
- **SQL 注入**：`queryAccountList` / `queryOneUserIdAndCharacterId` 全 `#{}` 参数化，无拼接。
- 无硬编码密钥、无 `Optional.get()`、无空 catch、测试无 `Thread.sleep()`。
- **规模**：service 173 行 / 最长方法 29 行（`getUserWalletBalance`），controller 39 行，远低于 800/50 阈值；无 >4 层嵌套。
- **测试规范**：`should`-style 英文方法名 + 中文 `@DisplayName`，`@Nested` 分组清晰；单测用 `@ExtendWith(MockitoExtension.class)` 纯 Mockito，未滥用 `@SpringBootTest`。
- **`@AfterEach` 清理 SecurityContext**（`WalletBalanceControllerIT.java:68-72`）已按前次意见落地。

## 处置建议

**必须修复（阻塞合并）**：IMPORTANT-1、IMPORTANT-3、IMPORTANT-4、IMPORTANT-6、IMPORTANT-8

**可协商延期（须登记 follow-up）**：
- IMPORTANT-2（返回结构增故障状态位）—— 涉 API 契约变更，建议与 spec 修订一并进行
- IMPORTANT-5 —— 建议至少先落地"消除 2N 次 DB 查询"与"`merged.size() >= 7` break"两项零风险改动，缓存接入独立立项
- IMPORTANT-7 —— 环境限制所致，需在 MySQL/Redis 可达环境重跑并附输出，同时登记 `disabled-tests-inventory.md`

**建议本轮顺手修**：MINOR-1/2/3/5/6/9（改动量均 10 行内；其中 MINOR-6 的 `verify(esiGateway).getAccessToken(CID_1, USER_ID)` 恰为本仓库历史高频的 userId 实参越权问题提供回归保护）
**列入 follow-up**：MINOR-4/7/8

---

## 补记（2026-08-25）：IMPORTANT-7 闭合，两项疑虑被实测推翻

评审时测试主机不可达，IT 只能以 `Failed to load ApplicationContext` 集体报错，故评审只能判定
「该覆盖从未被观测过」。次日该主机（`192.168.12.249` MySQL:3307 / Redis:6379）恢复可达，实跑结果：

**首次运行 3 个用例全部失败** —— 评审的判断成立，且问题比预想更多。三处均为**测试断言错误**，
生产代码无需改动：

| # | 现象 | 根因 | 处置 |
|---|------|------|------|
| 1 | `$.code` expected `0000000` but was `200` | `ResultCode.SUCCESS` 的 code 是 `"200"`；`spec.md:59` 的响应示例同样写错 | 断言改用 `ResultCode.SUCCESS.getCode()`，spec 示例同步修正 |
| 2 | Status expected `401` but was `403` | `GlobalExceptionHandler:204-205` 把 `ACCESS_UNAUTHORIZED` 映射为 403（全项目既有约定） | 改断言 403 + 错误码 `AUT00301`，并注明该用例实际验证的是应用服务身份兜底 |
| 3 | `characters[0].balance` expected `1000000.0` but was `0.0` | IT 未 mock `AuthorizeUtil`，真实实现按 `(userId, characterId)` 查 `eve_account`；测试库无该组合 → 抛「账号不存在」→ **被 fail-open 的 catch 吞成 0.0** | 增加 `@MockitoBean AuthorizeUtil` 并 stub（测试库为远程共享主机，不写入数据） |

修正后：**018 IT 3/3 通过、017 IT 12/12 通过**（failed 0，exitCode 0）。

### 两项疑虑被推翻

评审在 IMPORTANT-7 中提出两点「无法在本环境实测，需验证后再定级」的怀疑，现给出实测结论：

1. **`mockAuthenticated()` 直写 `SecurityContextHolder` 会被 `SecurityContextHolderFilter` 覆盖？**
   → **不成立**。两个已认证用例的 `status().isOk()` 均通过，说明主体在过滤器链内可见。
   评审建议改用 `@WithMockUser` / `SecurityMockMvcRequestPostProcessors.authentication(...)` 仍是更稳妥的写法，
   但当前写法并未失效，故降级为风格建议。

2. **401 断言「对任何原因的 401 都成立」，无法区分认证生效与配置错误？**
   → **比评审预想的更糟**：该响应根本不是 401 而是 403，断言从一开始就不可能通过。
   评审建议的「补断言错误码」已采纳（断言 `AUT00301`）。

### 仍未闭合

- **IMPORTANT-6 的 RBAC 门禁用例**：IT 仍将 `RbacAuthorizationManager` mock 成无条件放行，
  故 SQL 未执行 / 缓存未刷新 / 角色缺失这三类配置问题依旧没有测试能发现。补该用例需向
  **远程共享 Redis** 写权限规则，有污染他人环境风险，暂缓。
- **IMPORTANT-7 的项目级建议**：`pom.xml` 仍无 `maven-failsafe-plugin`，`*IT` 执行完全依赖测试主机可达。
  引入 Testcontainers 或拆独立 profile 的建议保留。
- 第 3 项根因是 **IMPORTANT-2 的真实实例证据**：一次本应暴露测试配置缺陷的归属校验失败，
  被静默降级为「余额 0」，排查时唯一线索是断言数字不对，异常本身不出现在响应中。
  这为「故障不可观测」的实际代价提供了具体例证，建议据此提升 IMPORTANT-2 的处理优先级。
