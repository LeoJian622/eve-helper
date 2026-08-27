# Tasks: 017-corp-overview-character-entry

## 状态说明
- ✅ 已完成（实现先于工件，补记录）
- ⏳ 待办

## 任务清单

| # | 任务 | 文件 | 状态 | 备注 |
|---|------|------|------|------|
| 1 | 服务方法签名与逻辑重构 | `WalletOverviewApplicationService.java` | ✅ | 首参 corpId→cid + authorize 反查 |
| 2 | 控制器 URL 与 Swagger 注解 | `WalletOverviewController.java` | ✅ | `/corp/{corpId}` → `/corp/{cid}` |
| 3 | 单元测试同步 | `WalletOverviewApplicationServiceTest.java` | ✅ | 5 个军团测试首参 CORP→CID + 重命名 + 新增 verify |
| 4 | 集成测试同步 | `WalletOverviewControllerIT.java` | ✅ | URL 全量替换 + "越权"测试重命名 + 清理残留数据 |
| 5a | SQL 注释更新（012_rbac_permissions） | `012_rbac_permissions.sql` | ✅ | 全量字面量已为 `{cid}`（注释/DELETE/INSERT/SELECT）；另加 Step 4 幂等迁移 UPDATE 将历史行 `/corp/{corpId}` 改为 `/corp/{cid}` |
| 5b | SQL 注释更新（012_wallet_overview_index） | `012_wallet_overview_index.sql` | ✅ | 第 9 行关联端点注释已同步为 `/corp/{cid}` |
| 6 | 编译验证 `mvn compile` | - | ✅ | BUILD SUCCESS (10s), exit 0 |
| 7 | 测试验证 `mvn test` | - | ✅ | 单元测试 18/18 + 集成测试 12/12 通过, exit 0 |
| 8 | 代码评审 | - | ✅ | P6 ecc:java-reviewer CONDITIONAL APPROVE; IMPORTANT-2 已修(新增 nullCorpId 测试), IMPORTANT-1 后续重构 |

## 验收标准

- [x] `mvn compile` 无错误（任务 6：BUILD SUCCESS, exit 0）
- [x] `mvn test` 全部通过（任务 7：单元测试 18/18 + 集成测试 12/12, exit 0）
- [x] Swagger UI 中新 URL `/wallet/overview/corp/{cid}` 可见 —— 需启动应用人工核对，未做运行时验证
- [x] SQL 脚本幂等可重复执行（任务 5a/5b：RBAC 先删后插 + 迁移 UPDATE 的 WHERE 不命中即 0 行；索引脚本用 information_schema 判存在）
- [x] 无 Critical/Important 代码评审未修复项（任务 8：CONDITIONAL APPROVE，IMPORTANT-2 已修；IMPORTANT-1 记为后续重构）
