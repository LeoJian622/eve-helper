# Implementation Plan: 017-corp-overview-character-entry

## 概述

将军团钱包总览端点入口从"军团ID"改为"人物ID"，由服务端通过 `AuthorizeUtil.authorize(cid)` 反查军团ID，消除 IDOR 风险并简化客户端调用。

## 实施顺序

### 任务 1：服务层方法签名与逻辑重构

**文件**：`src/main/java/.../application/service/WalletOverviewApplicationService.java`

**变更**：
- 首参 `Integer corpId` → `Integer cid`
- 在参数校验后、`corporationScope` 调用前插入：
  ```java
  EveAccount account = authorizeUtil.authorize(cid);
  Integer corpId = account.getCorpId();
  ```
- 更新 Javadoc：说明人物维度入口 + 归属校验 + 反查军团ID
- 更新日志 warn：`corpId=` → `cid=`

**依赖**：无

### 任务 2：控制器端点 URL 与 Swagger 注解

**文件**：`src/main/java/.../interfaces/web/controller/WalletOverviewController.java`

**变更**：
- `@GetMapping("/corp/{corpId}")` → `@GetMapping("/corp/{cid}")`
- `@PathVariable Integer corpId` → `@PathVariable Integer cid`
- `@Parameter(name = "corpId", description = "军团ID")` → `@Parameter(name = "cid", description = "人物ID(归属校验后反查军团ID)")`
- `@Operation(summary = ...)` 描述更新

**依赖**：任务 1（方法签名已变）

### 任务 3：单元测试同步

**文件**：`src/test/java/.../application/service/WalletOverviewApplicationServiceTest.java`

**变更**：
- 5 个军团测试方法首参 `CORP` → `CID`
- `nullCorpId_rejectedBeforeOwnership` → 重命名 `nullCid_rejectedBeforeOwnership`
- `validCorpInput_callsCorporationScope` → 重命名 `validCidInput_authorizesAndCallsCorporationScope` + 增加 `verify(authorizeUtil).authorize(CID)`

**依赖**：任务 1

### 任务 4：集成测试同步

**文件**：`src/test/java/.../interfaces/web/controller/WalletOverviewControllerIT.java`

**变更**：
- 所有 `get("/wallet/overview/corp/" + CORP)` → `get("/wallet/overview/corp/" + CID)`
- `corpOverview_foreignCorp_returnsZeroed` → 重命名 `corpOverview_nonRootUserNoJournals_returnsZeroed`
- 新增 `jdbcTemplate.update("delete from wallet_journal where owner_id = ?", (long) CORP)` 清理残留数据

**依赖**：任务 2

### 任务 5：SQL 脚本注释与 url_perm 同步

**文件**：
- `src/SQL/convert/012_rbac_permissions.sql`
- `src/SQL/convert/012_wallet_overview_index.sql`

**变更**：
- 所有注释中 `GET:/wallet/overview/corp/{corpId}` → `GET:/wallet/overview/corp/{cid}`
- INSERT/DELETE/SELECT 中 `url_perm` 字面量同步更新
- 提供一条增量 UPDATE 语句供生产数据库迁移（`UPDATE sys_permission SET url_perm = 'GET:/wallet/overview/corp/{cid}' WHERE url_perm = 'GET:/wallet/overview/corp/{corpId}'`）

**依赖**：任务 2

### 任务 6：编译验证

运行 `mvn compile` 确认无编译错误。

**依赖**：任务 1-5

### 任务 7：测试验证

运行 `mvn test` 确认全部测试通过，无回归。

**依赖**：任务 6

## 关键文件清单

| 文件 | 类型 |
|------|------|
| `WalletOverviewApplicationService.java` | 生产代码 |
| `WalletOverviewController.java` | 生产代码 |
| `WalletOverviewApplicationServiceTest.java` | 单元测试 |
| `WalletOverviewControllerIT.java` | 集成测试 |
| `012_rbac_permissions.sql` | 数据库脚本 |
| `012_wallet_overview_index.sql` | 数据库脚本 |

## 风险评估

- **低风险**：方法签名变更，所有调用方已同步修改
- **中风险**：生产数据库 `sys_permission.url_perm` 需执行 UPDATE，否则 RBAC 匹配失败（提供迁移 SQL）
- **缓解**：部署时先执行迁移 SQL，再部署新代码；或先部署新代码（旧 url_perm 仍能匹配旧路径），再执行迁移 SQL
