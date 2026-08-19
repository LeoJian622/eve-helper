# Research: ESI 数据 × 系统用户强关联

**阶段**: P2 Phase 0 研究 | **产出**: 设计决策集中记录（并入 plan.md Design Decisions）

## 一、代码事实测绘（Explore 子代理）

- **8 表均无 `user_id` 列/字段**；归属性列语义：assets/blueprints/wallet_journal/wallet_transaction 用 `ownerId`(Long)、industry_job/observer/structure 用 `corporationId`(Long)、mining_detail 用 `characterId`(Integer)。所列均为 EVE ID，非系统用户 ID。
- **写路径已持系统 userId**：6 条活跃链路均在 `authorizeUtil.authorize(cid)`→`eveAccount.getUserId()` 处取到 userId，但**从不落库**。仓储 save 为纯数据签名 `(List<域实体>)`，无 userId 参数 → 需在域实体回填 userId。
- **`eve_account.user_id` 为 Long**；`UserUtil.getUserId()` 返回 `Integer`（AuthenticatedPrincipal Integer / JWT claim）。存在边界类型阻抗。
- **休眠表**：observer 无生产写入入口（ObserverService 仅转发，无 ESI 同步/controller）；blueprints 写方法 `saveAndUpdateBlueprints` 为未完成桩（无落库，controller 只读）。
- `AccessGuard` 4 方法；`ResourceOwnershipPolicy.isOwnedBy` 现为 `characterId OR corpId` 字符串匹配（成员共享漏洞源）；ROOT=`ADMIN` 角色（GlobalConstants.ROOT_ROLE_CODE）。

## 二、决策与依据

见 plan.md「Design Decisions」。重点：

1. **user_id 用 Long**，对齐 `eve_account.user_id`，写路径原样 `eveAccount.getUserId()`，避免截断。
2. **军团读 = 查询级 `user_id` 过滤**（同步者私有），ROOT→null→不过滤；**统一空 200 不做硬 403**（防存在性 oracle，FR-007）。
3. **人物读 = 保留 `requireOwnership(charId)`**（角色属主共享 FR-004），不按 user_id 过滤；user_id 仅存档（FR-001）。
4. **联盟机制**：`isOwnedBy` 增 `allianceId` 匹配分支（FR-005，机制预留），不影响既有 char/corp 匹配。
5. **休眠表只做 schema+PO**，不发生成行为（YAGNI）。
6. **存量迁移**：人物行→角色属主（join eve_account）；军团行→管理域（ROOT admin）；孤儿行归管理域兜底；0 丢失。
7. 类型转换点：读写边界 Integer↔Long 仅在 `corporationScope()` 一处，避免散落。

**备选考虑**：军团读硬 403（供甲非同步者明确拒绝）——弃用（存在性 oracle）；新增复合索引（owner_id,user_id）——暂不加（owner_id 索引已够，等值谓词非过滤主导）。