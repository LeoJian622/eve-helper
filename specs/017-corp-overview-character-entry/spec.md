# Feature Specification: Corporation Overview Character Entry（军团总览人物入口）

**Feature Branch**: `017-corp-overview-character-entry`
**Created**: 2026-08-21
**Status**: Implemented (补工件)
**Input**: User description: "该方法应该接收人物ID，根据人物iD获得军团ID 再进行业务"

## 背景与动机

012-wallet-overview 交付的军团钱包总览端点 `GET /wallet/overview/corp/{corpId}` 直接以军团ID为入口。存在两个问题：

1. **IDOR 风险**：客户端可直接传入任意 `corpId`，归属校验仅靠 `AccessGuard.corporationScope` 按同步者 userId 过滤——若当前用户不是该军团同步者，结果静默为空而非显式拒绝，错误归因困难。
2. **客户端负担**：客户端须先知道军团ID才能调用，而实际场景中客户端总是从"已登录人物"出发，需要额外的"人物 → 军团"查询。

本特性将军团总览入口改为人物维度：客户端传入人物ID（`cid`），服务端通过 `AuthorizeUtil.authorize(cid)` 完成归属校验后反查 `EveAccount.corpId`，再执行原有军团总览业务。

## User Story — 以人物入口查看军团钱包总览 (Priority: P1)

作为 EVE 玩家，我希望用已登录的人物作为入口查看其所属军团的钱包总览，**以便**无需单独查询军团ID即可获取军团钱包摘要。

**Acceptance Scenarios**:

1. **Given** 当前用户拥有一个人物（`characterId=CID`）属于军团 `CORP`，**When** 调用 `GET /wallet/overview/corp/{CID}`，**Then** 服务端通过 `authorize(CID)` 反查 `corpId=CORP` 后返回军团总览数据（余额/收支/分账分布）。
2. **Given** 客户端传入一个不属于当前用户的 `cid`，**When** 调用军团总览，**Then** `authorize(cid)` 抛 `ACCESS_UNAUTHORIZED`，请求被拒绝（显式 403）。
3. **Given** 客户端传入 `cid=null`，**When** 调用军团总览，**Then** 抛 `PARAM_ERROR`（400），不触达归属校验。
4. **Given** 客户端传入 `division=0` 或 `division=8`（越界），**When** 调用军团总览，**Then** 抛 `PARAM_ERROR`（400），不触达归属校验。
5. **Given** 人物已授权但其 `EveAccount.corpId=null`（罕见异常数据），**When** 调用军团总览，**Then** 后续仓储查询以 `owner_id=null` 执行，返回零值而非 NPE。

## 技术设计要点

### 端点签名变更

| 项目 | 变更前 | 变更后 |
|------|--------|--------|
| URL | `/wallet/overview/corp/{corpId}` | `/wallet/overview/corp/{cid}` |
| Path variable 含义 | 军团ID | 人物ID |
| 归属校验 | `AccessGuard.corporationScope`（按同步者 userId 过滤，静默为空） | `AuthorizeUtil.authorize(cid)`（显式 403）+ `corporationScope`（保留） |
| corpId 来源 | 客户端传入 | 服务端 `authorize(cid).getCorpId()` 反查 |

### 服务方法签名

```java
// 变更前
public WalletOverviewVO getCorporationOverview(Integer corpId, Integer division, String range, Date start, Date end)

// 变更后
public WalletOverviewVO getCorporationOverview(Integer cid, Integer division, String range, Date start, Date end)
```

### 执行流程

```
cid 入参
  ↓ 参数校验（cid 非空 + division 范围 + range 合法）
  ↓ resolveRange
  ↓ authorizeUtil.authorize(cid)  // 归属校验 + 反查 EveAccount
  ↓ account.getCorpId()  // 得到军团ID
  ↓ accessGuard.corporationScope(...)  // 保留，同步者过滤
  ↓ fetchCorporationBalances(corpId, scope)  // ESI 直查余额
  ↓ walletJournalRepository.selectOverview*  // 原有聚合
  ↓ 组装 WalletOverviewVO 返回
```

### 安全性

- 入参边界校验**先于**归属校验（IDOR 防御纵深）
- `authorize(cid)` 使用 `eveAccountService.getAccountOne(userId, characterId)`，按 (userId, characterId) 联合查询 + 归属断言，fail-closed
- `corporationScope` 仍保留，提供同步者私有读过滤（与 014 强关联）

## 影响范围

### 必须同步修改

| 文件 | 变更类型 |
|------|---------|
| `WalletOverviewApplicationService.java` | 方法签名 + 内部逻辑（authorize 反查） |
| `WalletOverviewController.java` | URL `/corp/{corpId}` → `/corp/{cid}`，Swagger 注解 |
| `WalletOverviewApplicationServiceTest.java` | 5 个军团测试方法首参 `CORP` → `CID`，新增 `verify(authorizeUtil).authorize(CID)` |
| `WalletOverviewControllerIT.java` | 集成测试 URL `/corp/{CORP}` → `/corp/{CID}`，"越权"测试重命名 + 清理残留数据 |
| `012_rbac_permissions.sql` | 注释 + INSERT/DELETE 中 url_perm 值同步（`corp/{corpId}` → `corp/{cid}`） |
| `012_wallet_overview_index.sql` | 注释中端点路径同步 |

### 不影响

- `WalletTransactionController`、`WalletJournalController` 的 `/corp/{corpId}` 端点（其他特性，不在此次范围）
- `fetchCorporationBalances` 内部逻辑（仍按 corpId + scope 工作，无需改动）
- `resolveCharIdInCorp` 辅助方法（保留，供 `fetchCorporationBalances` 使用）

## RBAC 权限迁移注意事项

数据库中 `sys_permission.url_perm` 已有的 `GET:/wallet/overview/corp/{corpId}` 行需要更新为 `GET:/wallet/overview/corp/{cid}`，否则 RBAC 匹配失败会导致 403。本次交付 SQL 脚本（复用 `012_rbac_permissions.sql` 的幂等 upsert 逻辑），部署后执行即可。
