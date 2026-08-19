# Plan: 013 — ESI 人物授权基准与 403 友好返回

**Spec**: [spec.md](spec.md) (G1 已批准)
**Status**: G2 待批准
**Date**: 2026-08-19
**Branch**: feat/013-esi-char-auth-403
**设计安全评审**: `ecc:security-reviewer` 已完成（2026-08-19），H1/H2 已修复，M1/M2/M3 已修订进 plan。

---

## 关键口径（设计决策）

| 口径 | 决策 | 依据 |
|------|------|------|
| 授权基准 | **人物（角色）为唯一令牌来源**；`eve_account` 角色行存 refreshToken/corpId/allianceId，无独立军团/联盟令牌 | spec FR-001 + 现状勘察（EsiApiService.doGetAccessToken(characterId,userId) 唯一取 token 入口） |
| 军团/联盟读取参数 | 端点入参统一为**关联角色ID(characterId)**；服务层 `authorize`/`authorizeInternal` 校验归属并从同角色行解析 `getCorpId()`/`getAllianceId()` 作为 ESI 请求目标。**目标军团ID恒从已授权角色行派生，调用方无法指定任意军团** | spec FR-002/002a/003/004；anti-IDOR（EveAccountMapper 双键过滤已核正确）；消除「同一 ID 兼作角色与军团」双重语义 |
| 403 捕获层 | **ESI API 封装层**（`*.Api` 的 `.onStatus`/`exchangeToMono` 状态分支）区分 `HTTP 403 → EsiException(ESI_AUTH_PERMISSION_LOW)`；其余 4xx 仍 `ESI_AUTHORIZATION_FAILURE` | 现状：WalletApi 已 onStatus 分 4xx/5xx 但 4xx 未再分 403；PageTotalApi.getMaxPage 是军团分页唯一入口且存 bug |
| 错误码放置 | **`infrastructure/external/esi/ResultCode` 枚举**新增 `ESI_AUTH_PERMISSION_LOW("ESI00403", 文案)`，与既有 `ESI_AUTHORIZATION_FAILURE(ESI00400)`/`ESI_SERVER_FAILURE(ESI00500)` 同位 | **H1 修复**：`EsiException` 构造器绑定 infrastructure 版枚举；shared `ResultCode` 无 ESI 码，放 shared 会编译失败/双源 |
| 403 文案 | `GlobalExceptionHandler.resolveHttpStatus` 增 `ESI_AUTH_PERMISSION_LOW → HTTP 403`；**message 用枚举友好文案，绝不回显 ESI 原始 error/errorDescription** | **H2 修复**：现有 `handleEveHelperException` 把 `e.getMessage()` 原样回前端；回显 ESI 原始串违反安全红线（错误不泄露敏感数据）+ 与 US3 目标冲突 |
| 403 识别时序 | **先判 `statusCode()==403` 再解 body**，避免 ESI 错误体为空/非 JSON 时空流导致 403 契约丢失 | **M1 修复** |
| 范围 | **全部军团/联盟同步服务统一校正**（钱包流水/交易/蓝图/工业/采矿/建筑/资产）；已正确者仅核查不改 | spec FR-002a（G1 已明确全部，非先聚焦钱包） |

---

## D1: 错误码与 403 异常识别（基础设施层）

**文件**:
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/ResultCode.java`(改，**非 shared 版**)
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/EsiException.java`(不改，复用继承)

```java
// infrastructure/external/esi/ResultCode 枚举新增：
ESI_AUTH_PERMISSION_LOW("ESI00403",
    "该人物缺少目标军团的 Director/Bookkeeper 等角色、已离开或不在目标军团，请确认角色或重新授权"),
```

> **不改动既有码值**：`ESI_AUTHORIZATION_FAILURE`/`ESI_SERVER_FAILURE` 保持，仅新增 `ESI00403`。新码落 **infrastructure** 枚举（`EsiException` 绑定的类型），不污染 shared 层。

**403 识别工具** `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/EsiStatusUtil.java`（新增，静态无状态，放 ESI 防腐层）：

```java
public final class EsiStatusUtil {
    public static boolean isForbidden(HttpStatusCode s) { return s.value() == 403; }

    // H2:message 用枚举友好文案,绝不携带 ESI 原始 error/errorDescription;
    //     原始串只作结构化日志(不 return 给前端)
    public static EsiException forbiddenAgent(HttpStatusCode statusCode) {
        return new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW);
    }
}
```

> 说明：`forbiddenAgent` 不再接收/组装 `ErrorResponse`——ESI error/errorDescription 仅在调用点 `log.warn`（结构化、window 不落地）。若 handler 语义需具体原因核对，也只进日志不进响应。

---

## D2: ESI API 层 403 单识改造（关键任务）

对军团/联盟数据读取所经 API 层的 4xx 分支，把 `403` 从 `ESI_AUTHORIZATION_FAILURE` 中分出。

**模式（WalletApi 为例）**——**先判 403 后解 body（M1）+ message 不回显原始串（H2）**：

```java
.retrieve()
.onStatus(HttpStatusCode::is4xxClientError, response -> {
    if (EsiStatusUtil.isForbidden(response.statusCode())) {
        log.warn("ESI 4xx 403 权限不足: {}", response.statusCode()); // 结构化,含原始信息仅在此
        return Mono.error(EsiStatusUtil.forbiddenAgent(response.statusCode()));
    }
    return response.bodyToMono(ErrorResponse.class).flatMap(res ->
            Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE)));
})
.onStatus(HttpStatusCode::is5xxServerError, response ->
        response.bodyToMono(ErrorResponse.class).then(
                Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE))))
```

**覆盖入口**（范围=全部，FR-002a）：

| 入口 | 方法 | 需处理 |
|------|------|--------|
| 军团分页 | `PageTotalApi.queryMaxPage` | **修 bug**：4xx/5xx 分支 `.map().flatMap()` 未 return 实际不生效；修复为正确 return Mono.error + 补 403 单识 + **去 assert 空态兜底 + 响应链加 5s 超时（L1）** |
| 军团钱包流水/余额 | `WalletApi.queryCorporationWalletJournal*`/`queryCorporationWallet` | +403 分支 +5s 超时 |
| 军团钱包交易 | `WalletApi.queryCorporationWalletTransactions*` | +403 分支 |
| 军团工业 | `IndustryApi.queryCorporationIndustryJobs*` | 核对后补 403 |
| 军团结构/蓝图 | `CorporationApi.queryCorporationStructures*`/`queryCorporationBlueprints*` | 核对后补 403 |
| 人物资产 | `AssetsApi.queryCharactersAssets*` | 补 403 |

> 静态核查（M3）：本 task 内**核查全部 ESI 军团数据入口**，确认【客户端直传 corporationId 进 ESI API 的分支为零增长】——corpId 一律来自角色行派生。这是 FR-004 的 enforce-by-construction 证据。

---

## D3: 全服务签名统一（传角色ID → 解析军团ID）

**目标**：军团/联盟同步服务入参统一为角色ID，内部从 eve_account 解析目标军团ID，消除双重语义。

**现状**：
- **已正确**（传入角色→`eveAccount.getCorpId()`）：`AssetsService`、`BlueprintsService`、`IndustryJobService`、`MiningDetailService`、`StructureService` → **仅核查（C6）**，不改签名，纳入 D2 静态核查证据。
- **错位**（参数兼作角色ID与军团ID）：`WalletJournalService.syncCorporationJournal`、`WalletTransactionService.syncCorporationTransactions` → 改造。

**改造 `WalletJournalService.syncCorporationJournal`**（签名语义明确为 characterId）：
```java
public void syncCorporationJournals(Integer characterId, Integer userId) throws ParseException {
    // 请求路径:authorize(characterId);内部路径:authorizeInternal(userId, characterId)
    EveAccount eveAccount = /* 见下 */;
    Integer corpId = eveAccount.getCorpId();   // ← 从角色行解析,不与入参复用
    if (corpId == null) throw new EsiException(ResultCode.ESI_AUTH_PERMISSION_LOW);
    String accessToken = esiApiService.getAccessToken(characterId, eveAccount.getUserId());
    // 逐 division pull,queryCorporation*(corpId,...) 用解析值
}
```
**内部路径归属对账（M2）**：`authorizeInternal(SYSTEM_USER_ID, characterId)` 依赖库中存在对应角色行。内部路径**不得**按 characterId 单键取行绕过 owner 过滤——归属校验不因系统身份旁路。实现时确认系统内是否有 `SYSTEM_USER_ID` 绑定行；若无，内部路径改为「先按 characterId 找到真实拥有者所在行，仍走 `getCorpId()` 派生并校验从属」。**补一条内部路径契约测试**（FR-007）。

`WalletTransactionService.syncCorporationTransactions`：同构改造。

---

## D4: 控制器契约与 RBAC

**文件**:
- `WalletJournalController.java`(改) / `WalletTransactionController.java`(改)
- `src/SQL/convert/013_rbac_permissions.sql`(按需)

军团同步端点参数 `corpId` → `characterId`，Swagger 注解同步。**URL 决策（C4）**：保留既有 URL 路径，仅参数语义改 `characterId`（减少前端契约冲击；实现时如 URL 需含军团则再议，plan 默认保留）。

**RBAC（L3/C5）**：调用方 `RbacAuthorizationManager`（`method+path` AntPathMatcher）匹配 URI 占位符名称无关（`{corpId}`→`{characterId}` 不影响 `.../{x}` 匹配）。**若 URL 不变则既有路径模板键不变，不新增 SQL**；仅当 URL 路径真变化才登记 `013_rbac_permissions.sql`（幂等先删后插，参照 010/011/012）。

---

## D5: 测试策略

**单元(Mockito)**：
- `EsiStatusUtil`: `isForbidden(403)=true`、403→ESI_AUTH_PERMISSION_LOW、其它 4xx→ESI_AUTHORIZATION_FAILURE、5xx→ESI_SERVER_FAILURE；`forbiddenAgent` message 不含 ESI 原始串
- `WalletJournalService`/`WalletTransactionService`: 传角色ID→解析 corpId→调 ESI 用解析值；corpId 空→403 码；归属不符→拒绝不调 ESI；**内部路径授权归属不旁路（M2 契约测试）**
- `GlobalExceptionHandler`: ESI_AUTH_PERMISSION_LOW→HTTP 403 且 message=友好文案（非 ESI 原串）
- `PageTotalApi`: 4xx/5xx 修复后正确抛 ESI 对应异常（含 403）

**集成(Spring Boot + test profile,需 MySQL)**：成功路径、越权 401/403、模拟 ESI 403（含 body 空/非 JSON 的 M1 情形）→前端收 403+专属文案、5xx/超时与 403 区分。

**覆盖率**: ≥80%(`ecc:test-coverage`)。

---

## 涉及文件清单

| 变更 | 文件 |
|------|------|
| 改 | `infrastructure/external/esi/ResultCode.java`(枚举增 ESI_AUTH_PERMISSION_LOW=ESI00403) |
| 新增 | `infrastructure/external/esi/EsiStatusUtil.java` |
| 改 | `PageTotalApi.java`(修 bug+403+去assert+5s超时) / `WalletApi.java` / `IndustryApi.java` / `CorporationApi.java` / `AssetsApi.java` |
| 改 | `WalletJournalService.java` / `WalletTransactionService.java`(签名统一+内部归属对账) |
| 改 | `WalletJournalController.java` / `WalletTransactionController.java`(参数改 characterId) |
| 改 | `GlobalExceptionHandler.java`(resolveHttpStatus 增 403 映射) |
| 新增 | `src/SQL/convert/013_rbac_permissions.sql`(仅 URL 路径真变化时) |
| 测试 | `EsiStatusUtilTest`、`WalletJournalServiceSyncTest`、`WalletTransactionServiceSyncTest`、`PageTotalApiTest`、`GlobalExceptionHandler403IT` |
| 归档 | `docs/reviews/013-plan-security-review.md`(本 security-reviewer 评审记录) |

## 不改动

- ESI 授权/刷新锁/缓存逻辑、token 刷新 4xx EXPIRED 路径（L2 仅在 plan 留协议注释：数据 401 与 refresh 失效驻码不可辨，位置区分）。
- 资产/蓝图/工业/采矿/建筑签名（已正确，仅核查）。
- 数据库表结构（无 DDL）。

---

## Polish 检查点（T0xx：仅标记，交由协调方评审）

- **P5 验证报告**：待协调方执行（`./mvnw test` 全量绿 + `ecc:verification-loop` + `ecc:security-scan`），本任务未执行。
- **P6 评审标记**：待协调方派遣 `ecc:java-reviewer` / `ecc:security-reviewer`（本特性涉及认证/外部 API/输入 → security-reviewer 必须）执行 G6 门禁，本任务未执行。
- **设计安全评审**：已由 `ecc:security-reviewer` 完成（本任务审查并修复 H1/H2，修订 M1/M2/M3），记录待归档 `docs/reviews/`。