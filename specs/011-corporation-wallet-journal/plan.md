# Plan: 011 — 军团钱包流水（Corporation Wallet Journal）

**Spec**: [spec.md](spec.md) (G1 已批准)
**Status**: G2 待批准
**Date**: 2026-08-18

---

## D1: DDL 迁移

**文件**: `src/SQL/convert/011_wallet_journal_division.sql`

```sql
ALTER TABLE wallet_journal ADD COLUMN division INT;
UPDATE wallet_journal SET division = 1 WHERE division IS NULL;
CREATE INDEX idx_owner_div ON wallet_journal(owner_id, division);
```

脚本可重复执行。

---

## D2: 实体/PO/转换器变更

- `WalletJournal.java` — 加 `private Integer division`
- `WalletJournalPO.java` — 加 `private Integer division`
- `WalletJournalPoConverter.java` — MapStruct 自动映射
- `WalletJournalMapper.xml` — BaseResultMap/Base_Column_List/INSERT/UPDATE 加 division
- `WalletJournalVO.java` — 加 `private Integer division`
- `WalletJournalAssembler.java` — 自动映射

---

## D3: 仓储层变更

**接口** `WalletJournalRepository`:
- 新增 `IPage<WalletJournal> selectPageByOwnerAndDivision(IPage, Long ownerId, Integer division)`

**实现** `WalletJournalRepositoryImpl`:
- 实现 `selectPageByOwnerAndDivision`
- `saveOrUpdateBatch` 改造为 M4 batch 模式（500 条/批）

**Mapper** `WalletJournalMapper` + XML:
- 新增 `selectPageByOwnerAndDivision(IPage, ownerId, division)` — WHERE owner_id + division, ORDER BY date DESC
- 新增 `insertOrUpdateBatch(@Param("list") List<WalletJournalPO>)` — foreach 批量 INSERT ON DUPLICATE KEY UPDATE

---

## D4: 领域服务变更

**WalletJournalService**:

### D4a: 角色同步回填 division=0
- `syncCharacterJournal` 拉取后回填 `division=0`，再 saveOrUpdateBatch

### D4b: 军团同步（新增 `syncCorporationJournal`）
- Division 1-7 循环，每个 division:
  1. authorize + getAccessToken
  2. queryCorporationWalletJournalMaxPage 获取总页数
  3. Stream.iterate 逐页拉取
  4. 回填 ownerId=corpId, division=currentDivision
  5. saveOrUpdateBatch（M4 batch）
- try/catch per division 失败隔离，汇总失败抛 EveHelperException

---

## D5: 应用服务变更

**WalletJournalApplicationService**:

### D5a: 角色同步增加冷却限流
- `requireSyncCooldown("wallet:journal:sync:char:" + cid)`

### D5b: 军团同步（新增）
- `syncCorporationJournal(Integer corpId)`:
  1. accessGuard.requireOwnership
  2. requireSyncCooldown
  3. 委托领域服务
  4. catch ParseException → EveHelperException

### D5c: 军团分页查询（新增）
- `queryCorporationPage(Integer corpId, Integer division, int current, int size)`:
  1. 入参校验（division 1-7, current≥1, size 1-1000）
  2. accessGuard.requireOwnership
  3. 仓储查询 + VO 映射

### D5d: 冷却限流
- 复用 CacheGateway + @Value TTL 可配（测试 profile 禁用）

---

## D6: 控制器

**WalletJournalController** 新增:
- `POST /corp/{corpId}/sync` — 军团流水同步
- `GET /corp/{corpId}?division=N&current=1&size=20` — 军团流水分页

---

## D7: RBAC

**文件**: `src/SQL/convert/011_rbac_permissions.sql`
- 2 新端点登记 sys_permission + 绑定 ADMIN

---

## 测试策略

**单元测试**: 同步逻辑（division 循环/失败隔离/回填）、入参校验、冷却限流
**集成测试**: 越权拒绝、同步+分页、幂等性、冷却限流

## 不改动

- ESI 层（已有完整方法）
- 人物侧流水查询逻辑
