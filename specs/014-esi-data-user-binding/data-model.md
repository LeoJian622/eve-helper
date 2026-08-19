# Data Model: ESI 数据 × 系统用户强关联

**阶段**: P2 Phase 1 | **说明**: 8 张 ESI 业务表新增 `user_id` 列；隶属关系与列语义对照。

## 新增列（统一）

```
user_id BIGINT NULL  COMMENT '同步者系统用户ID'
```

- 类型对齐 `eve_account.user_id`（Long）。NULL 表示存量/未归属（迁移前）；迁移后人物行归角色属主、军团行归管理域。
- 无 FK 约束（BIGINT 可空、存量 NULL，MySQL 不强制引用）——软引 `eve_account.user_id`。

## 各表归属性列对照

| 表 | 现有归属性列 | 维度 | user_id 语义 |
|----|------------|------|-------------|
| assets | `owner_id`(=character_id) | 人物 | 同步者 |
| blueprints | `owner_id`(=character_id) | 人物 | 同步者（写休眠） |
| mining_detail | `character_id` | 人物 | 同步者 |
| wallet_journal | `owner_id` + `division`（0/NULL/1=人物;1-7=军团） | 人物/军团 | 同步者（行级） |
| wallet_transaction | `owner_type`+`owner_id`+`division` | 人物/军团 | 同步者（行级） |
| industry_job | `corporation_id` | 军团 | 同步者 |
| observer | `corporation_id` | 军团 | 同步者（写休眠） |
| structure | `corporation_id` | 军团 | 同步者 |

注:表名以实际 DDL 为准（`industry_job`/`observer` 等可能与 PO 映射的既有表名存在拼写差异，见实现时核对，如 observer 的 `crorporation_id` 拼写疑点）。

## 实体字段对齐（domain 实体 + PO + converter + mapper insert 列）

统一在 `xyz.foolcat.eve.evehelper.domain.model.entity.system.*` 与 `infrastructure.persistence.entity.system.*PO` 增加 `Long userId`（`@TableField("user_id")`）。MapStruct `*PoConverter` 同名字段自动映射，无需手写；仅核对无 `@Mapping` 冲突。

## 读过滤约定

- 人物维度读：`owner_id = :characterId` + 守卫 `requireOwnership(charId)`；**不**加 user_id 谓词（角色属主共享）。
- 军团维度读：`<归属列> = :corpId` + `AND user_id = :scope`（`scope` 非 null，即非 ROOT）；ROOT scope=null 不加谓词。
- 军团读无行 → 统一空 200（防 oracle）。

## 迁移目标

- 人物行 → join `eve_account` 按 `character_id` 取 `user_id`。
- 军团行 → ROOT admin `user_id`（部署占位）。
- 孤儿行 → 管理域兜底；0 丢失校验。