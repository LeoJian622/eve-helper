# 用户钱包余额聚合流程

> 面向开发者：理清 `GET /wallet/balance` 的身份取得方式、人物/军团余额的 ESI 直查链路、军团多角色分账合并（union by division）语义，以及一批**必须知道的已知局限**。
> 关联代码：`interfaces/web/controller/WalletBalanceController.java`、`application/service/WalletBalanceApplicationService.java`、`domain/model/vo/{UserWalletBalanceVO,CharacterBalanceVO,CorporationBalanceVO}.java`、`domain/port/esi/EsiGateway`（`queryCharacterWalletBalance` / `queryCorporationWalletBalances`）。

## 1. 总览

018 把「余额」从钱包总览（012/016）与流水/交易明细（009/010/011）中**独立**出来：总览是按单一主体（人物或军团）的聚合读，而本端点是**按用户维度横向聚合**——一次返回该用户绑定的全部人物余额与其关联军团的分账余额。

| 端点 | 入参 | 说明 |
|------|------|------|
| `GET /wallet/balance` | **无**（path/query/body 全无） | 身份取自 JWT；返回当前用户全部人物余额 + 关联军团余额 |

与总览的关键差别：

| 维度 | `/wallet/overview/*`（012/016/017） | `/wallet/balance`（018） |
|------|-----------------------------------|------------------------|
| 主体 | 单个人物或单个军团（路径参数指定） | 当前用户的**全部**人物与军团 |
| 入参 | 有 path 参数 + 时间范围 | 无参 |
| 余额来源 | ESI 直查（016 起） | ESI 直查 |
| 军团 token 选取 | 入口已授权的那一个人物（017 起） | 该军团下**所有**人物逐个尝试，结果取并集 |
| 是否含收支/类目/趋势 | 是（读 `wallet_journal`） | 否，只有余额 |

## 2. 主流程

```
GET /wallet/balance
  -> WalletBalanceController.getUserWalletBalance()            // 不取身份、不传参
       -> WalletBalanceApplicationService.getUserWalletBalance()
            -> UserUtil.getUserId()                            // 身份自取(018 IMPORTANT-4)
               userId 为 null 或 <= 0 -> ACCESS_UNAUTHORIZED   // fail-closed,不触达取数
            -> eveAccountRepository.getAccountList(userId)      // where user_id = ? 按用户过滤
               空/null -> 返回空结构(characters=[], corporations=[])
            -> 人物侧:accounts.stream().map(buildCharacterBalance)
                 authorize(cid) -> getAccessToken(cid, userId) -> queryCharacterWalletBalance
            -> 军团侧:accounts 按 corpId 分组(去重,corpId 为 null 的人物不参与)
                 每组 -> mergeCorporationBalances(corpId, 组内人物)
                          逐个人物 authorize -> getAccessToken -> queryCorporationWalletBalances
                          division 合法性校验(1..7),非法跳过
                          putIfAbsent 按 division 合并 -> 按 division 排序
                          totalBalance = 各分账之和
            -> 组装 UserWalletBalanceVO
```

## 3. 身份为何由服务自取（018 IMPORTANT-4）

初版签名是 `getUserWalletBalance(Integer userId)`，由控制器传入 `UserUtil.getUserId()`。P6 评审判定这把**信任边界外移**了：服务只校验 `userId > 0`，没有任何断言保证该 userId 等于当前登录主体。

失败场景（当时尚未发生，但结构上敞开）：若日后新增「管理员查看指定用户余额」的入口并传入请求参数里的 `targetUserId`，则

1. `getAccountList(targetUserId)` 返回**他人**的全部人物 ID / 人物名 / 军团 ID / 军团名；
2. 下游 `authorize(cid)` 用调用者自己的 userId 校验 → 查不到 → 抛 `USER_ACCOUNT_NOT_EXIST`；
3. 但该异常被 fail-open 的 `catch` 吞成 `balance = 0.0`；
4. 最终 **HTTP 200**，余额全 0，日志只有 warn，无越权告警——他人的人物与军团清单已经泄漏。

这正是本仓库 IDOR 历史缺陷的同一形态（列表无归属过滤 + 下游 fail-open 掩盖）。现改为无参签名、服务内 `UserUtil.getUserId()`，该误用在结构上不可能发生。

> `UserUtil.getUserId()` 在未认证/匿名/主体不可识别时返回 `-1`，因此 `userId <= 0` 的判断同时覆盖"没登录"与"主体异常"两种情况。

## 4. 端点本身无 IDOR（P6 逐环节核验结论）

| 环节 | 保障 |
|------|------|
| 身份入口 | 唯一来源 `UserUtil.getUserId()`（读 `SecurityContextHolder`），无 path/query/body 可注入 |
| 账号列表 | `EveAccountMapper.queryAccountList` 为 `where user_id = #{userId}`，参数化按用户过滤 |
| `authorize(cid)` | **忽略调用方传入的任何 userId**，自行从 SecurityContext 重取；`userId <= 0` 直接抛 `ACCESS_UNAUTHORIZED`（fail-closed），再 `getAccountOne(userId, characterId)` |
| ESI token 缓存键 | `ESI_ACCESS_TOKEN_KEY + userId + ":" + characterId`，用户 + 角色双隔离（历史缓存键越权漏洞 `f153ed4` 在本路径不可达） |
| 军团 `corpId` | 全部派生自当前用户自有账号行的 `getCorpId()`，非外部输入 |
| 白名单 | `application.yml` 的 `whiteUrlList` 仅含 `POST:/user`、`POST:/auth/tokens`，本端点需认证 |

**军团多角色合并不构成跨用户泄漏**：参与合并的 token 全部属于同一登录用户自己的人物；并集是规格 US2 的明确要求，EVE 侧各 division 的可读性由 ESI 按各自 token 独立裁决，本代码只做并集，不越过 ESI 的授权判定。

## 5. 军团分账合并语义

规格意图（`spec.md` US2）：用户在同一军团可能有多个人物，各自的 `Accountant`/`Account_Take_N` 角色不同，能读到的 division 不同。逐个尝试并取并集，**以"能拿到的数据最多"为目标**，避免因单个角色权限不足而遗漏。

```
角色A 可读 division 1,2,3  ─┐
                            ├─ 合并 → division 1..7 全有
角色B 可读 division 4,5,6,7 ─┘
```

三条细则：

1. **同一 division 被多个角色读到时，取第一个成功值**（`putIfAbsent`）。同一军团同一分账的余额本应相同，差异只可能来自 ESI 缓存时点。
2. **某个角色失败不影响其他角色**：单角色的取数包在 `try/catch` 内，失败记 warn 后继续下一个。
3. **`division` 必须落在 1..7**，否则跳过并记 warn（见下节）。

### 已知局限：合并的"首个胜出"顺序不确定（MINOR-1，未修）

`putIfAbsent` 的胜出者取决于 `charsInCorp` 的顺序，而该顺序来自 `queryAccountList` 的结果集顺序——**该 SQL 没有 `ORDER BY`**，InnoDB 不保证行序。同一份数据两次请求可能选中不同人物的 token。排查线上"为什么这次是 100 下次是 999"时需知道这一点。建议后续给该查询加 `order by character_id`。

## 6. division 合法性防御（018 IMPORTANT-3）

`WalletDivisionBalance.division` 的类型是 `Integer`（可空）。若 ESI 返回 `division` 缺失或为 null 的畸形记录（字段改名、代理错误页被部分反序列化等）：

- `HashMap` **接受 null 作为 key**，不会在插入时报错；
- 随后 `Comparator.comparingInt(WalletDivisionBalance::division)` 接收的是 `ToIntFunction`，`Integer` 被自动拆箱 → **NPE**；
- 该 NPE 位于单角色 `try/catch` **之外**（那个 try 只包住单角色取数），沿 `buildCorporationBalance` → stream → `getUserWalletBalance` 一路上抛；
- 结果：**整个 `/wallet/balance` 返回 500**，连人物余额和其他军团的正常数据一起丢失。

故合并前置校验 `division == null || < 1 || > 7` 即跳过并记 warn。回归测试：`nullDivision_skippedWithoutNpe`、`outOfRangeDivision_skipped`。

## 7. 已知局限：故障不可观测（IMPORTANT-2，延期未修）

**这是使用本端点时最需要留意的一点。** 人物与军团的取数都包在 `catch (Exception)` 里，失败一律降级为 `0.0` / 空列表：

| 场景 | 实际情况 | 返回 |
|------|---------|------|
| refreshToken 在 EVE 官网被撤销 | 该人物无法查询 | `balance: 0.0`，无任何提示需重新授权 |
| 军团唯一有 Accountant 的人物 token 过期 | 一个分账都读不到 | `totalBalance: 0.0`、`divisions: []` |
| 7 个分账中 4 个查询失败 | 只拿到 3 个 | `totalBalance` 是这 3 个之和，**系统性低估且无告警** |

即"余额真的是 0"与"我们没查到"在返回结构上**完全同形**。调用方（尤其是展示 ISK 资产的前端）不应把 0 直接当作"账上没钱"。

方向上这是 fail-closed 的——归属校验失败时返回的是 0 而不是他人数据，不构成越权；问题纯粹是**可观测性**。修复方案（VO 增加 `status` 状态位 + `partial` 标记）涉及 API 契约变更，已记为 follow-up。

相关的次要问题（MINOR-3）：`catch (Exception)` 比 016 的分段捕获（`ParseException` / `RuntimeException` 分开）是纪律退化，会一并吞掉 `IllegalStateException` 等编程错误，使 bug 表现为"余额 0"而非快速失败。

## 8. 已知局限：ESI/DB 调用放大（IMPORTANT-5，部分缓解）

设 N = 用户绑定人物数：

| 环节 | 次数 | 备注 |
|------|------|------|
| `authorize(cid)` → `getAccountOne` → DB | **2N** | 人物路径 + 军团路径各一次 |
| `getAccessToken` | 2N | Redis 命中则廉价；未命中触发 refreshToken 轮换 + 分布式锁 |
| ESI 人物余额 | N | |
| ESI 军团余额 | N | **按人物数重复，不按军团数** |

四点值得注意：

1. **N+1**：`getAccountList(userId)` 已把全部 `EveAccount` 载入内存，`authorize` 却又为每个 cid 各查一次库拿同一行。8 个人物 = 16 次多余的单行查询。
2. **无提前终止**：无条件遍历军团下所有人物。若第一个人物已返回 1..7 全部 division，后续调用零贡献。（`merged.size() >= 7` 即可 break）
3. **无超时**：`EsiApiService` 中这两个余额方法的 `.block()` 没有 `.timeout(...)`，`esiClient` 也未配 `responseTimeout`。ESI 卡住时请求线程无界等待。8 人物 2 军团 ≈ 16 次串行 ESI 调用，端点 P95 可达 5–12 秒，会占满 Tomcat 线程。
4. **未接入 015 响应缓存**：这两个余额方法是全项目唯一绕过 `esiResponseCache` 的 ESI 查询（016 为"实时通路"刻意如此），而 018 把它放大了 2N 倍。ESI 对 `/characters/{id}/wallet/` 声明 120s 缓存，同一分钟内重复调用拿不到更新数据，只消耗自己的 error limit（100/min per IP）。

⚠ **error limit 连坐风险**：若某用户 3 个人物 token 已失效，每次请求产生 3 次 ESI 4xx；前端 5 秒轮询 → 每分钟 36 次错误。多个此类用户叠加即可能触发 ESI **全应用级** error limit 封禁，届时**所有用户的所有 ESI 功能一起不可用**。因此不建议对本端点做高频轮询。

017 的 IMPORTANT-1 修复顺带消除了军团总览路径上的一次多余 `authorize`，但本端点的上述 4 点仍待独立立项处理。

## 9. RBAC 与部署

`src/SQL/convert/012_rbac_permissions.sql` **Step 5** 登记本端点权限：

```
sys_permission:      url_perm = 'GET:/wallet/balance'   (name = '用户钱包余额聚合')
sys_role_permission: 绑定到 sys_role.code IN ('ADMIN', 'USER')
```

三点必须知道：

1. **这是白名单式授权，不等于"所有登录用户可访问"**。`RbacAuthorizationManager` 为 fail-closed，自定义角色（`GUEST`、`CORP_MEMBER` 等）的已认证用户若未列入，访问会得到 **403**。spec 早期表述有误（写成"任何已认证用户可访问"），已在 018 更正。
2. **角色缺失会静默少插**：`INSERT ... SELECT` 依赖目标库存在 `code='ADMIN'`/`'USER'` 的行；若某环境缺 `USER` 角色，只会插入 ADMIN 一行且**不报错**。故脚本内附「Step 5 执行后校验」查询，期望返回 2 行，务必核对。
3. **必须刷新 Redis 权限缓存**：URL-权限-角色映射缓存在 `URL_PERM_ROLES_KEY`，仅执行 SQL 不会让新权限生效，需调用 `refreshPermRolesRules`（或对应管理入口）。

### 测试覆盖的缺口（IMPORTANT-6 / IMPORTANT-7）

- `WalletBalanceControllerIT` 把 `RbacAuthorizationManager` 整体 mock 成无条件放行，因此**上述 RBAC 配置问题没有任何测试能发现**。补一个不 mock RBAC 的门禁用例需要 Redis 环境，已记为 follow-up。
- 该 IT 的 3 个用例在本机因 MySQL 不可达全部 Errors（`Failed to load ApplicationContext`），**其覆盖尚未被任何可核验的运行记录证实**。项目未配 `maven-failsafe-plugin`，`*IT` 的执行完全依赖本地 DB。建议引入 Testcontainers 或把 IT 拆到独立 profile。

## 10. 与相邻特性的关系

| 特性 | 关系 |
|------|------|
| 012 钱包总览 | 提供 `/wallet/overview/*`；018 从中拆出"纯余额"这一关切 |
| 016 余额直查 | 把总览的 `currentBalance` 由读 `wallet_journal` 改为 ESI 直查；018 沿用该思路 |
| 017 军团总览人物入口 | 军团总览入口参数 `corpId` → `cid`；其 IMPORTANT-1 修复与 018 的军团 token 选取思路一致（都用已授权人物） |
| 015 ESI 响应缓存 | 本端点的两个余额查询**未接入**（见第 8 节） |
| 014 ESI 数据用户归属 | 本端点的 `getAccountList(userId)` 依赖该特性建立的 `user_id` 归属语义 |

## 11. 相关文件

| 文件 | 职责 |
|------|------|
| `interfaces/web/controller/WalletBalanceController.java` | 端点，只做委派（不取身份、不传参） |
| `application/service/WalletBalanceApplicationService.java` | 身份自取 + 人物/军团余额聚合 + 分账合并 |
| `domain/model/vo/UserWalletBalanceVO.java` | 顶层返回结构 |
| `domain/model/vo/CharacterBalanceVO.java` | 人物余额（`characterId`/`characterName`/`balance`） |
| `domain/model/vo/CorporationBalanceVO.java` | 军团余额（`corpId`/`corpName`/`totalBalance`/`divisions`） |
| `domain/model/vo/WalletDivisionBalance.java` | 分账余额（`division` 可空，见第 6 节） |
| `src/SQL/convert/012_rbac_permissions.sql` | Step 5 权限登记 + 执行后校验 |
| `docs/reviews/2026-08-24-018-java-reviewer.md` | P6 评审记录（8 个 IMPORTANT 的完整分析） |
| `specs/018-wallet-balance-by-user/` | 规格 / 计划 / 任务与阻塞项处置明细 |
