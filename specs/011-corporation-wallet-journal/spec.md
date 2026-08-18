# Spec: 011 — 军团钱包流水（Corporation Wallet Journal）

**Feature**: 军团钱包流水 division 1-7 全分账同步 + 分页查询
**Track**: 特性轨（P0–P7）
**Status**: G1 待批准
**Date**: 2026-08-18

---

## 1. 概述

为军团钱包流水（journal）补齐 division 1-7 全分账同步与分页查询能力。当前仅有角色流水同步（009 交付）和旧版军团 division=1 同步（`batchInsertOrUpdateFromEsi`），缺少完整的军团多分账支持。

### 与现有功能的关系

| 功能 | 特性 | 翻页 | division |
|------|------|------|----------|
| 角色钱包流水 | 009 ✅ | page-based | 0 |
| 军团钱包流水 | **011** | page-based | 1-7 |
| 角色钱包交易 | 010 ✅ | cursor (from_id) | 0 |
| 军团钱包交易 | 010 ✅ | cursor (from_id) | 1-7 |

---

## 2. 用户故事

### US1: 军团钱包流水同步与分页查询

**作为**管理员，**我希望**能同步军团的钱包流水并按 division 分页查询，**以便**了解军团各分账的收支明细。

**验收标准**:
- `POST /wallet/journal/corp/{corpId}/sync` 触发军团 division 1-7 全分账流水同步
- `GET /wallet/journal/corp/{corpId}?division=N` 按 division 分页查询流水（时间倒序）
- 单 division 同步失败不影响其它已成功 division 的数据（失败隔离）
- 重复同步幂等（PRIMARY(id) 保证）
- 越权访问被拒绝（accessGuard.requireOwnership）
- 同步端点有冷却限流（复用 010 M1 模式）

---

## 3. 功能需求

### FR-001: wallet_journal 表加 division 列

- `ALTER TABLE wallet_journal ADD COLUMN division INT`（不设默认值）
- `UPDATE wallet_journal SET division = 1`（现有数据全是军团 division=1 流水）
- 加索引 `idx_owner_div(owner_id, division)` 优化分页查询

### FR-002: 角色流水同步回填 division=0

- 现有 `syncCharacterJournal` 方法增加 division=0 回填逻辑
- 新同步的角色流水记录 division=0

### FR-003: 军团流水同步（division 1-7）

- 新增 `syncCorporationJournal(Integer corpId)` 方法
- Division 1-7 逐个循环，每个 division 独立「拉取+回填+保存」
- 单 division 失败 try/catch 隔离，不影响其它 division
- 回填 `ownerId=corpId, division=currentDivision`
- 翻页方式：page-based（maxPage 循环），沿用现有 `batchInsertOrUpdateFromEsi` 模式
- 幂等：PRIMARY(id) + INSERT ON DUPLICATE KEY UPDATE
- 批量写入：复用 M4 优化（500 条/批 batch INSERT ON DUPLICATE KEY UPDATE）

### FR-004: 军团流水分页查询

- 新增 `queryCorporationPage(Integer corpId, Integer division, int current, int size)` 方法
- 入参校验：division 1-7、current≥1、size 1-1000
- 入参校验先于归属校验（防越权探测）
- 按 `owner_id + division` 过滤，时间 date 倒序

### FR-005: 控制器端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/wallet/journal/corp/{corpId}/sync` | 军团流水全分账同步 |
| GET | `/wallet/journal/corp/{corpId}?division=N&current=1&size=20` | 军团流水按分账分页查询 |

### FR-006: RBAC 权限登记

- 2 个新端点登记 `sys_permission`
- 绑定 ADMIN 角色

### FR-007: 同步冷却限流

- 复用 010 M1 的 Redis 冷却模式（`wallet:journal:sync:corp:{corpId}`，60s TTL）
- 测试 profile 禁用

---

## 4. 非功能需求

### NFR-001: 数据迁移幂等

- DDL 脚本可重复执行（`ADD COLUMN IF NOT EXISTS` 或先检查）
- UPDATE 回填幂等（`WHERE division IS NULL` 条件过滤已回填行）

### NFR-002: 测试覆盖

- 单元测试：同步逻辑（division 循环、失败隔离、division 回填）、分页参数校验
- 集成测试：越权拒绝、同步+分页查询、幂等性、冷却限流

### NFR-003: 兼容性

- 现有角色流水同步需增加 division=0 回填（行为变更，需测试验证）
- 现有 `batchInsertOrUpdateFromEsi` 保留（定时任务可能使用），标注 deprecated

---

## 5. 数据模型变更

### wallet_journal 表

```sql
ALTER TABLE wallet_journal ADD COLUMN division INT;
UPDATE wallet_journal SET division = 1 WHERE division IS NULL;
CREATE INDEX idx_owner_div ON wallet_journal(owner_id, division);
```

### WalletJournal 实体新增字段

```java
private Integer division;
```

---

## 6. 技术约束

- 翻页方式：page-based（maxPage），journal ESI 端点不支持 from_id 游标
- 幂等键：PRIMARY(id)，ESI journal id 全局唯一
- 批量写入：M4 batch INSERT ON DUPLICATE KEY UPDATE（500 条/批）
- division 约定：角色=0，军团=1-7
