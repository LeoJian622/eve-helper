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

### 合并的"首个胜出"顺序已固定（MINOR-1 已修）

`putIfAbsent` 的胜出者取决于 `charsInCorp` 的顺序，而该顺序来自 `queryAccountList` 的结果集顺序。
该 SQL 原先**没有 `ORDER BY`**，InnoDB 不保证行序（受执行计划、行更新、page split 影响），
同一份数据两次请求可能选中不同人物的 token，线上会出现「同一分账余额时而 100 时而 999」且无从排查。
现已加 `order by character_id`，胜出者固定为 **characterId 最小者**，结果可复现。

## 6. division 合法性防御（018 IMPORTANT-3）

`WalletDivisionBalance.division` 的类型是 `Integer`（可空）。若 ESI 返回 `division` 缺失或为 null 的畸形记录（字段改名、代理错误页被部分反序列化等）：

- `HashMap` **接受 null 作为 key**，不会在插入时报错；
- 随后 `Comparator.comparingInt(WalletDivisionBalance::division)` 接收的是 `ToIntFunction`，`Integer` 被自动拆箱 → **NPE**；
- 该 NPE 位于单角色 `try/catch` **之外**（那个 try 只包住单角色取数），沿 `buildCorporationBalance` → stream → `getUserWalletBalance` 一路上抛；
- 结果：**整个 `/wallet/balance` 返回 500**，连人物余额和其他军团的正常数据一起丢失。

故合并前置校验 `division == null || < 1 || > 7` 即跳过并记 warn。回归测试：`nullDivision_skippedWithoutNpe`、`outOfRangeDivision_skipped`。

## 7. 故障可观测：status 与 partial（018 IMPORTANT-2 已落地）

**这是使用本端点时最需要留意的字段。** 早期实现把人物与军团的取数都包在 `catch (Exception)` 里，
失败一律降级为 `0.0` / 空列表，导致「余额真的是 0」与「我们没查到」在返回结构上**完全同形**。
现已改为显式状态：

| 场景 | 返回 |
|------|------|
| 查询成功 | `balance` 为数值，`status: "OK"` |
| refreshToken 被撤销 / 归属校验失败 | `balance: null`，`status: "UNAUTHORIZED"` |
| ESI 5xx / 超时 / 解析失败 | `balance: null`，`status: "UPSTREAM_ERROR"` |
| ESI 返回 null 余额（调用成功） | `balance: 0.0`，`status: "OK"` —— 这是**可信的**零余额 |

军团侧另有两个字段：

- `partial`：为 `true` 表示该军团下有人物查询失败，分账集合不完整。此时 `totalBalance`
  只是已取到分账之和，**不代表军团总资产**。例如 7 个分账中 4 个失败时会系统性低估。
- `unavailableCharacterIds`：失败的人物ID，便于定位需要重新授权的角色。

> ⚠ 调用方（尤其展示 ISK 资产的前端）**不得**把 `null` 或 `0` 直接当作「账上没钱」，
> 必须先看 `status` / `partial`。`balance` 可为 `null` 属破坏性变更。

### 状态归类的实现约束

区分 `UNAUTHORIZED` 与 `UPSTREAM_ERROR` 是按**错误码字符串**判定的，而非异常类型：
`EsiException` 与其错误码枚举都定义在 infrastructure 层，application 层不得 import
（DDD 分层规则）。判据集合 `REAUTH_REQUIRED_CODES` 含 `AUT00301`（未认证）、
`AUT00208`（归属不符/账号已解绑）、`ESI00400`（ESI 未授权或过期）；其余（含 `ESI00500`
ESI 服务器失败）归为 `UPSTREAM_ERROR`。**若 `ESI00400` 码值变更须同步该常量。**

日志按状态分级：授权类失败是用户可自行修复的常态，只记 warn 不打堆栈以免刷屏；
上游故障打堆栈便于定位。

## 8. 调用开销与已知局限（IMPORTANT-5 部分落地）

设 N = 用户绑定人物数、M = 关联军团数：

| 环节 | 当前次数 | 说明 |
|------|---------|------|
| `authorize(cid)` → DB | **N** | 已按 characterId 请求内缓存（原为 2N） |
| `getAccessToken` | ≤2N | Redis 命中则廉价；未命中触发 refreshToken 轮换 + 分布式锁 |
| ESI 人物余额 | N | |
| ESI 军团余额 | ≤N | 分账凑满 1..7 即提前终止（原为无条件遍历） |

### 已落地的两项

1. **authorize 请求内去重**：同一人物原先被 authorize 两次（人物余额路径 + 军团余额路径），
   各查一次库；现按 `characterId` 缓存，单次请求内只校验一次。
   > 这里**没有**采用"直接用 `getAccountList` 的结果替代 `authorize`"这个更省的方案，原因有两条：
   > ① `authorize` 除查库外还从 SecurityContext 取 userId 做归属校验，替代等于把双重校验降为单重
   > （`getAccountList` 的 `where user_id` 成为唯一防线）——对财务数据不值得；
   > ② 两个查询的 SELECT 字段集并不一致：`queryAccountList` 只取 8 列，
   > `getAccountOne` 取 12 列（多 `gmt_create`/`gmt_modified`/`type`/`qq`）。
   > 其中 `type`（注释 `CH:0 EU:1`，即晨曦/欧服）目前**全项目零使用**，故当下无碍；
   > 但若将来用它做多服务器分流，替代方案会静默拿到 `null`。
2. **分账凑满提前终止**：ESI 单次调用即返回该 token 可见的全部分账；若前一个人物已覆盖 1..7，
   后续人物的调用零贡献。注意"跳过"不等于"失败"，不会置 `partial`。

### 仍未处理

- **无超时**：`EsiApiService` 中这两个余额方法的 `.block()` 没有 `.timeout(...)`，
  `esiClient` 也未配 `responseTimeout`。ESI 卡住时请求线程无界等待。
- **未接入 015 响应缓存**：这两个余额方法是全项目唯一绕过 `esiResponseCache` 的 ESI 查询
  （016 为"实时通路"刻意如此）。ESI 对 `/characters/{id}/wallet/` 声明 120s 缓存，
  同一分钟内重复调用拿不到更新数据，只消耗自己的 error limit（100/min per IP）。

⚠ **error limit 连坐风险**：若某用户 3 个人物 token 已失效，每次请求产生 3 次 ESI 4xx；
前端 5 秒轮询 → 每分钟 36 次错误。多个此类用户叠加即可能触发 ESI **全应用级** error limit 封禁，
届时**所有用户的所有 ESI 功能一起不可用**。因此不建议对本端点做高频轮询。

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

### 测试覆盖与状态码口径

- `WalletBalanceControllerIT` 3 个用例已在 MySQL/Redis 可达环境实跑通过（3/3）。首次真实运行时
  3 个全部失败，暴露出三处**断言错误**（成功码应为 `200` 而非 `0000000`；未认证是 **403** 而非 401；
  未 mock `AuthorizeUtil` 导致真实 DB 查询失败被 fail-open 吞成余额 0）。详见
  `specs/018-wallet-balance-by-user/tasks.md` T9。
- **状态码口径**：本项目 `ACCESS_UNAUTHORIZED` 由 `GlobalExceptionHandler` 映射为 **HTTP 403**
  （而非语义上更贴切的 401），该映射对全部端点生效，属既有约定（相关讨论见评审 MINOR-7）。
  所以"未带 JWT 访问本端点"得到的是 403 + `AUT00301`，不是 401。
- **RBAC 门禁仍无自动化覆盖**：IT 把 `RbacAuthorizationManager` 整体 mock 成无条件放行，
  因此上述 RBAC 配置问题（SQL 未执行、缓存未刷新、角色缺失）**没有任何测试能发现**。
  补"不 mock RBAC"的用例需要向测试 Redis 写权限规则，而测试环境是**远程共享主机**
  （`192.168.12.249`），写入有污染他人环境的风险，故暂缓。
- 项目未配 `maven-failsafe-plugin`，`*IT` 的执行完全依赖测试主机可达。若该主机不可达，
  IT 会以 `Failed to load ApplicationContext` 形式集体报错——那是环境问题，不是代码回归。
  建议后续引入 Testcontainers 或把 IT 拆到独立 profile，使 CI 能稳定给出证据。

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
