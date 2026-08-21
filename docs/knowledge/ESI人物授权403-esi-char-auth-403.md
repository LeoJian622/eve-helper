# ESI 人物授权基准与 403 友好返回

> 面向开发者：正确理解「人物授权唯一基准」（FR-001）、军团/联盟数据同步统一以关联角色ID驱动、以及 ESI 数据接口 403 的友好映射。这是之后所有军团/联盟数据功能必须遵守的架构不变量，不是单点修复。
> 关联代码：`EsiGateway`/`EsiApiService`（取 token）、五个军团服务（Wallet/Blueprints/Industry/Structure/Mining）、`EsiStatusUtil`/`EsiException`/`infrastructure ResultCode`、`GlobalExceptionHandler`。

## 1. 核心不变量：人物授权是唯一基准（FR-001）

**任何 ESI 授权调用都以「人物（eve_account 角色行）」的 refreshToken 为唯一 token 来源**。不存在独立「军团 token」或「联盟 token」——军团/联盟数据是「某人物持有的角色身份」的派生视图，用户无需（也无法）对军团单独授权。

落到代码的硬约束：

- `EsiGateway.getAccessToken(Integer characterId, Integer userId)`（`EsiApiService.doGetAccessToken`）——**唯一 token 入口**，第一参数是人物角色ID，换 token 只用 `eveAccount.getRefreshToken()`（`:260`）
- `eve_account` 表：`refreshToken` **仅存角色行**；`corpId`/`allianceId` 是**从属字段**（角色身份的派生属性），不作为 token 维度
- 缓存键 `accessTokenKey(userId, ownedCharacterId)` / 刷新锁 `refreshLockKey(ownedCharacterId)` 均按 **characterId** 隔离
- **不存在** `getAccessToken(corpId)` / `getCorpAccessToken` / `getAllianceAccessToken` 等 corp/alliance 维度入口（`EsiGateway` 无此类重载）

**核查证据**：见 `docs/reviews/013-fr001-auth-sole-basis-audit.md`（14 处调用点全部人物维度）。

## 2. 军团/联盟数据读取：统一「传入角色ID → 解析军团ID → 用角色 token 调 ESI」

所有军团/联盟数据读取端点与同步服务的**唯一签名约定**：

```
入参 = 角色ID(characterId)
  → 归属校验 requireOwnership(characterId)          // 防 IDOR：该角色须属于当前用户
  → authorize(characterId) / authorizeInternal(SYSTEM_USER_ID, characterId)
      // 请求上下文 vs 无安全上下文(定时任务)两种路径，均 fail-closed
  → corpId = eveAccount.getCorpId()                  // 目标军团ID从角色行派生
  → accessToken = getAccessToken(characterId, getUserId())   // token 用角色行
  → ESI 军团数据端点用解析出的 corpId（而非入参角色ID）
  → 落库回填 ownerId=corpId, ownerType=corporation, division=1..7
```

**关键点**：角色ID(characterId) 与军团ID(corpId) 是**两个不同维度**，绝不复用同一个值。调用方无法指定任意军团——目标军团恒由该角色 eve_account 行派生（`corpId==null` 时抛 `EsiException(ESI_AUTH_PERMISSION_LOW)` 映射 HTTP 403）。

### 已统一的服务（013 US1/US4）

| 服务 | 军团同步方法 | 入参 | 目标军团来源 |
|------|------------|------|------------|
| WalletJournalService | `syncCorporationJournal(characterId)` | characterId | `eveAccount.getCorpId()` |
| WalletTransactionService | `syncCorporationTransactions(characterId)` | characterId | `eveAccount.getCorpId()` |
| WalletApplicationService/Controller | `POST /wallet/journal/corp/{characterId}/sync`、`POST /wallet/transaction/corp/{characterId}/sync` | characterId | 同上（URL 保留 `/corp/`） |

> 其余军团服务（Blueprints/IndustryJob/Structure/MiningDetail/Assets）在 013 前就已遵守「token 用角色行 + corpId 由 getCorpId() 派生」的既有正确模式，013 经静态核查确认为零直传残留，**未改签名**。

## 3. ESI 数据接口 403 友好映射

ESI 数据端点可能因权限不足返回 HTTP 403（如该人物缺少目标军团的 Director/Bookkeeper 角色、已离开）。前端必须收到**可理解、可行动的专属提示**，而非笼统「外部服务异常」。

### 错误码与 HTTP 映射（`GlobalExceptionHandler.resolveHttpStatus`）

| infrastructure ResultCode | code | HTTP | 语义 |
|---------------------------|------|------|------|
| `ESI_AUTH_PERMISSION_LOW` | `ESI00403` | **403** | token 有效但该角色无权访问目标军团数据（缺 Director/Bookkeeper、已离开等） |
| `ESI_AUTHORIZATION_FAILURE` | `ESI00400` | 400 | refreshToken 失效 / 授权过期，需重新授权 |
| `ESI_SERVER_FAILURE` | `ESI00500` | 400 | ESI 服务器故障/超时 |

**三路可区分**（FR-006）：数据权限 403 ≠ 授权失效 ≠ 服务故障，各自独立错误码，前端可据此分流。

### 识别工具（`EsiStatusUtil`）

- `isForbidden(HttpStatusCode)`：`value()==403`
- `forbiddenAgent(HttpStatusCode)`：返回 `EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW)`
- **时序铁律**：先判 statusCode==403，再解 body——body 空/非 JSON 仍走 403 分支

### 安全红线（H2）

**message 恒用块友好文案，绝不回显 ESI 原始 error/errorDescription**。`EsiException` 的 message 来自枚举 `ResultCode.msg`，防泄露上游细节。

## 4. 关键实现细节（勿当 bug 修）

- **`EsiException extends EveHelperException`**：为让 infra 层错误经统一业务异常处理器转译为 Result，这是 **plan 批准的 domain→infrastructure 知情偏离**（domain 服务捕获/抛 `EsiException`），属首个批准决定先例，勿回滚。
- **division 级失败隔离 vs 403 透传（013 US1 修复）**：Wallet journal/transaction 军团同步按 division 1..7 独立提交，**瞬时 `RuntimeException`（网络抖动）**记录失败分账继续其余 division（已成功落库不回滚）；但 **`EsiException`（403/5xx/授权失效）透传**交由处理器映射 HTTP 状态，**不被折叠成 400**（FR-005）。两者由 `catch (EsiException)` / `catch (RuntimeException)` 分叉。
- **军团分页查询端点保持 `corpId` 入参**：`queryCorporationPage(corpId, division, ...)` 只是**只读库查询**（不触发 ESI），不在同步签名统一范围，未改动。
- **RBAC URL 未变**：controller 参数名 `{corpId}`→`{characterId}` 不改变 AntPathMatcher 匹配（忽略占位符名），`method:path` 键不变，无需新增权限 SQL（013 T020 记录）。

## 5. 关键代码文件索引

| 环节 | 文件 |
|------|------|
| token 获取唯一入口 | `domain/port/esi/EsiGateway.java`、`infrastructure/external/esi/EsiApiService.java`（doGetAccessToken） |
| 403 识别工具 | `infrastructure/external/esi/EsiStatusUtil.java` |
| 错误码 | `infrastructure/external/esi/ResultCode.java` |
| 错误异常 | `infrastructure/external/esi/EsiException.java` |
| HTTP 状态映射 | `interfaces/web/advice/GlobalExceptionHandler.java`（handleEveHelperException/resolveHttpStatus） |
| 军团同步（US4） | `domain/service/system/WalletJournalService.java`、`WalletTransactionService.java` |
| 军团同步端点（US1） | `interfaces/web/controller/WalletJournalController.java`、`WalletTransactionController.java` |
| 归属校验 | `application/security/AccessGuard.java`、`domain/util/AuthorizeUtil.java` |
| FR-001 审计证据 | `docs/reviews/013-fr001-auth-sole-basis-audit.md` |