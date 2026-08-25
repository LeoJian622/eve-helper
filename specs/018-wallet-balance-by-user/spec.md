# Feature Specification: Wallet Balance By User（按用户聚合钱包余额）

**Feature Branch**: `018-wallet-balance-by-user`
**Created**: 2026-08-24
**Status**: Draft
**Input**: User description: "将人物，军团余额接口单独拆分出来。不与流水和交易明细使用相同接口。按用户ID获取绑定的所有人物和军团的详细余额。不同人物对军团的钱包的可阅读权限不一样，按可获取的数据最多的结果来返回。"

## 背景与动机

现有 `/wallet/overview/{cid}` 和 `/wallet/overview/corp/{cid}` 端点以**单个人物ID**为入口，返回该人物/军团的完整总览（余额 + 收支 + 类目 + 趋势）。

**存在的问题**：

1. **缺少用户维度聚合端点**：客户端需先遍历所有人物才能获取"我的所有余额"，多次往返。
2. **军团余额权限覆盖不全**：现有 `resolveCharIdInCorp` 使用 `findFirst()` 只取一个角色，若该角色缺少某些 division 的 `Account_Take_N` 权限，则无法获取完整数据。
3. **职责混合**：总览端点混合了"余额查询"与"流水聚合"（收支/类目/趋势），对于只需余额的场景过重。

本特性新增独立端点，专注于**按用户聚合的余额查询**，并将军团余额获取改为多角色权限合并。

## User Stories

### US1: 按用户获取所有人物和军团余额 (Priority: P1)

作为 EVE 玩家，我希望通过一个端点获取我所有绑定人物和军团的余额，**以便** 无需多次调用即可看到资产总览。

**Acceptance Scenarios**:

1. **Given** 当前用户绑定了 2 个人物（分别属于不同军团），**When** 调用 `GET /wallet/balance`，**Then** 返回 `{ characters: [...], corporations: [...] }`，其中 characters 包含 2 条记录（每人物一条），corporations 包含 2 条记录（每军团一条）。
2. **Given** 当前用户有 3 个人物属于同一军团，**When** 调用 `GET /wallet/balance`，**Then** corporations 中该军团只出现一次，divisions 是 3 个人物权限合并后的结果。
3. **Given** 当前用户没有绑定任何人物，**When** 调用 `GET /wallet/balance`，**Then** 返回 `{ characters: [], corporations: [] }`。
4. **Given** 当前用户未认证（无 JWT），**When** 调用 `GET /wallet/balance`，**Then** 返回 401 Unauthorized。

### US2: 军团余额按多角色权限合并 (Priority: P1)

作为拥有多个军团角色的用户，我希望军团余额能显示所有我能访问的 division，**以便** 不因单个角色权限不足而遗漏数据。

**Acceptance Scenarios**:

1. **Given** 用户在军团 A 有角色 X（可访问 division 1-3）和角色 Y（可访问 division 4-7），**When** 获取军团余额，**Then** 返回 division 1-7 的完整数据（合并两个角色的结果）。
2. **Given** 用户在军团 A 只有角色 X，角色 X 只能访问 division 1-3，**When** 获取军团余额，**Then** 只返回 division 1-3 的数据，division 4-7 显示为 0 或省略。
3. **Given** 用户在军团 A 有多个角色，所有角色的权限完全重叠，**When** 获取军团余额，**Then** 每个 division 取第一个获取到的值（去重）。

## 技术设计

### 端点签名

| 项目 | 值 |
|------|-----|
| URL | `GET /wallet/balance` |
| Path variable | 无（从 JWT 解析 userId） |
| Query params | 无 |
| 归属校验 | 无需（直接查当前用户的数据） |
| 权限 | 需登录，且角色为 `ADMIN` 或 `USER`（白名单式，见「RBAC 权限」） |

### 返回结构

```json
{
  "code": "0000000",
  "message": "ok",
  "data": {
    "characters": [
      {
        "characterId": 123,
        "characterName": "Alice",
        "balance": 1000000.00
      },
      {
        "characterId": 456,
        "characterName": "Bob",
        "balance": 500000.00
      }
    ],
    "corporations": [
      {
        "corpId": 789,
        "corpName": "Test Corp",
        "totalBalance": 5000000.00,
        "divisions": [
          { "division": 1, "balance": 3000000.00 },
          { "division": 2, "balance": 2000000.00 }
        ]
      }
    ]
  }
}
```

### 执行流程

```
JWT token
  ↓ 解析 userId
  ↓ eveAccountRepository.getAccountList(userId)  → 获取所有人物
  ↓ 按 corpId 分组（去重，同一军团只处理一次）
  ↓ 对每个人物: fetchCharacterBalance(cid) → ESI 直查余额
  ↓ 对每个军团:
      ↓ 获取该军团下的所有角色（用户在该军团的）
      ↓ 对每个角色: fetchCorporationBalances(corpId, charId) → ESI 直查
      ↓ 合并所有角色的结果（union by division）
  ↓ 组装 UserWalletBalanceVO 返回
```

### 军团余额多角色合并逻辑

```java
/**
 * 合并用户在军团中的所有角色的 division 余额。
 * 每个 division 取第一个非空值（去重）。
 */
private List<WalletDivisionBalance> mergeCorporationBalances(
        Integer corpId, List<EveAccount> charsInCorp) {
    Map<Integer, Double> merged = new HashMap<>();
    for (EveAccount account : charsInCorp) {
        try {
            List<WalletDivisionBalance> balances = fetchSingleCharCorpBalances(
                    corpId, account.getCharacterId());
            for (WalletDivisionBalance b : balances) {
                // 每个 division 只取第一个有数据的
                merged.putIfAbsent(b.division(), b.balance());
            }
        } catch (Exception e) {
            log.warn("军团余额查询失败: corpId={}, charId={}", corpId, account.getCharacterId(), e);
            // 继续尝试下一个角色
        }
    }
    return merged.entrySet().stream()
            .map(e -> new WalletDivisionBalance(e.getKey(), e.getValue()))
            .toList();
}
```

### 安全性

- 端点无需 path 参数，不存在 IDOR 风险
- 从 JWT 解析 userId，只返回当前用户的数据
- ROOT 用户也只能看到自己绑定的数据（无特殊权限）

### 影响范围

| 文件 | 变更类型 |
|------|---------|
| `UserWalletBalanceVO.java`（新增） | 返回结构 |
| `CharacterBalanceVO.java`（新增） | 人物余额子结构 |
| `CorporationBalanceVO.java`（新增） | 军团余额子结构 |
| `WalletBalanceController.java`（新增） | 新端点 |
| `WalletBalanceApplicationService.java`（新增） | 业务逻辑 |
| `EsiGateway.java` | 新增 `queryCorporationWalletBalancesByChar(corpId, charId, token)` 方法（可选） |

### 不影响

- 现有 `/wallet/overview/{cid}` 和 `/wallet/overview/corp/{cid}` 端点保持不变
- 现有流水/交易明细端点保持不变
- `WalletOverviewApplicationService` 保持不变

## RBAC 权限

新增权限项：`GET:/wallet/balance`，绑定到 `ADMIN` 与 `USER` 两个角色
（`src/SQL/convert/012_rbac_permissions.sql` Step 5）。

⚠ 这是**白名单式**授权，不等于"所有登录用户可访问"：`RbacAuthorizationManager`
为 fail-closed，自定义角色（如 `GUEST`、`CORP_MEMBER`）的已认证用户若未列入，
访问该端点会得到 403。若后续需要放开给全部登录用户，应改为对 `sys_role` 全表授权
或纳入白名单机制，而不是逐个追加角色 code。

## 部署注意事项

无数据库迁移（纯新增端点），但 RBAC 权限须手工登记：

1. 执行 `src/SQL/convert/012_rbac_permissions.sql` 的 Step 5。
2. 运行该脚本的「Step 5 执行后校验」查询，确认返回 **2 行**（ADMIN 与 USER 各一）。
   少于 2 行说明目标库缺对应 `sys_role.code` —— `INSERT ... SELECT` 在角色不存在时
   **静默少插且不报错**，须人工补齐。
3. 调用 `refreshPermRolesRules`（或对应管理入口）刷新 Redis 权限缓存，
   否则新权限不会生效。
