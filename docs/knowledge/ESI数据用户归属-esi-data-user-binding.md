# ESI 数据 × 系统用户强关联（双锁模型）

> 面向开发者：理清「ESI 同步的每一份业务数据落 `user_id`（系统用户归属）」的**双锁模型**——人物维读=角色属主共享、军团维读=同步者私有、系统管理域（ROOT/ADMIN）豁免看全量；以及 `corporationScope` 军团读过滤语义、联盟维度归属判定机制、8 表 `user_id` schema 与写路径、存量迁移脚本用法与安全要点。这是后续所有军团/人物数据功能的授权基线，不是单点修复。
> 关联代码：`application/security/AccessGuard.java`（`requireOwnership` / `corporationScope` / `isCurrentUserRoot`）、`domain/service/security/ResourceOwnershipPolicy.java`（`isOwnedBy` 联盟分支）、军团/人物读的应用服务与仓储、8 张 ESI 业务表实体/PO/converter/mapper、`src/main/resources/db/migration/014_esi_user_binding.sql`（迁移部署产物）。

## 1. 总览：为什么要落 user_id

特性轨 014（ESI 数据 × 系统用户强关联）给 ESI 同步的**每一份业务数据记录**落一个 `user_id`（系统用户归属），从根源阻断 "知道 角色 ID/军团 ID 即可越权读取"（IDOR）。核心语义变更：**推翻军团成员共享**——军团维度数据从 "同团成员可读全团" 收紧为 "只对实际同步它的用户可见"；人物维度维持 "该角色的全部属主共享"；系统管理域（ROOT）豁免看全量。

技术方案是**双锁模型**：写路径统一从 `EveAccount.getUserId()`（Long，对齐 `eve_account.user_id`）取值落 `user_id`；读路径按维度分流——人物维靠 `requireOwnership(charId)` 校验、军团维靠 `corporationScope()` 注入 `user_id = :scope` 谓词。

| 维度 | 读锁 | user_id 过滤 | 管理域 | 依据 |
|------|------|-------------|--------|------|
| 人物（角色） | `requireOwnership(charId)`（角色属主共享） | **不**按 user_id 过滤 | ROOT 豁免归属校验 | FR-004 |
| 军团 | `corporationScope()` → `user_id = :scope`（同步者私有） | `scope` 非 null 时过滤 | ROOT → scope null → 看全量 | FR-003 / FR-006 |
| 联盟 | `isOwnedBy` 加 `allianceId` 匹配分支 | 机制预留，无数据消耗 | — | FR-005 |

**设计目标**：所有读均有统一守卫收敛，避免各应用服务各自手写而漏掉某入口；越权统一空 200（非硬 403），杜绝数据存在性 oracle（FR-007）。

## 2. 双锁模型：人物维 vs 军团维

读路径的两把锁，语义根本不同：

- **人物维锁 = 角色属主共享（`requireOwnership`）**。只要当前用户持有一个角色，就能读该角色的全部数据（包括其他持有同一角色的用户同步的数据）。**不引入 user_id 谓词**。背后的理由：人物是用户个人身份维，多属主共享是既有正确语义（FR-004）。已有 11 个人物读/写调用点**零改动**。

- **军团维锁 = 同步者私有（`corporationScope` → user_id 谓词）**。军团数据**不再按 corpId 成员匹配**，而是只返回**当前用户实际同步过**的那部分（FR-003）。同一军团的甲乙两人可见性不同：甲同步过则甲能看到，乙从未同步则乙拿到空。读过滤统一经 `user_id = :scope` 注入仓储查询，覆盖军团钱包流水、军团钱包交易、军团钱包总览、建筑 6 处军团读共 9 个调用点。

```
// 军团维（示例：军团钱包流水）——改造前 vs 改造后
// 改造前：requireOwnership(corpId, "军团钱包流水")  → 同团成员可读全团
// 改造后：
Long scope = accessGuard.corporationScope("军团钱包流水");   // ROOT→null；非 ROOT→当前 userId
walletJournalRepository.queryCorporationPage(corpId, division, scope, ...);

// 对应 mapper XML 军团读语句注入谓词
<if test="userId != null"> AND user_id = #{userId} </if>   // null ⇔ ROOT，不过滤
```

**ROOT（ADMIN）豁免全量**：`corporationScope()` 对 ROOT 返回 `null`（不过滤看全量），`requireOwnership()` 对 ROOT 直接放行。管理域豁免**只落在 ROOT 角色**，不扩散到普通用户（SC-003 反例已验证）。

## 3. corporationScope 语义（军团读过滤唯一口令）

`AccessGuard.corporationScope(String resource)` 是军团维读过滤的**唯一口令**，所有军团读调用点都必须经它取作用域，杜绝残留 `requireOwnership(corpId,...)` 造成 "成员共享" 漏洞：

```java
public Long corporationScope(String resource) {
    if (isCurrentUserRoot()) {
        return null;                      // ROOT → 不过滤（看全量）
    }
    Integer uid = UserUtil.getUserId();
    if (uid == null || uid <= 0) {        // 未认证 / 身份不可识别 → fail-closed
        log.warn("{}军团读越权：未认证或主体无法识别", resource);
        throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED);
    }
    return uid.longValue();               // 对齐 user_id BIGINT(Long)
}
```

语义逐条：

| 输入/状态 | 返回 | 后果 |
|-----------|------|------|
| 当前用户是 ROOT（ADMIN） | `null` | mapper `<if userId != null>` 不拼接 → 不过滤，看全量 |
| 认证的普通用户 | `Long`（`UserUtil.getUserId().longValue()`） | 拼接 `AND user_id = :scope` → 只看自己同步的部分 |
| 未认证 / 主体不可识别（uid 为 null 或 ≤0） | 抛 `ACCESS_UNAUTHORIZED` | **fail-closed**——绝不携带 null/非法的空过滤条件穿透到下游，杜绝误放全量军团数据 |

**Integer↔Long 转换收敛于此**：`user_id` 列是 `BIGINT`（对齐 `eve_account.user_id`（Long）），而 `UserUtil.getUserId()` 返回 `Integer`。`corporationScope()` 是这一转换的**唯一点**（`uid.longValue()`），军团读调用方与仓储/mapper 一律按 `Long` 透传，避免散落各处的 int 强转/丢失。

## 4. 联盟机制（预留，无数据消耗）

`ResourceOwnershipPolicy.isOwnedBy()` 增加联盟维度匹配分支——在既有 characterId / corpId 匹配之外，再加 `account.getAllianceId()` 匹配：

```java
return accounts.stream().anyMatch(account ->
        matches(ownerId, account.getCharacterId())
                || matches(ownerId, account.getCorpId())
                || matches(ownerId, account.getAllianceId()));
```

- `matches` 沿用**严格字符串比对**（`ownerId.equals(String.valueOf(candidate))`），防御同一数值多种表示（前导零等）绕过归一化差异。
- `allianceId` 是 `eve_account` 角色行的从属字段（角色身份的派生属性），与 `getCorpId()` 同等地位。
- 该分支当前**仅作为归属判定机制预留**（FR-005），联盟维度尚无独立读端点消耗它——`isOwnedBy` 仍是 `requireOwnership` 的底层判定，char/corp/alliance 三口径均可判属主。

## 5. 8 表 user_id schema 与写路径

### Schema（8 表，market_order 排除）

`user_id BIGINT NULL COMMENT '同步者系统用户ID'`，类型对齐 `eve_account.user_id`（Long）原样写入不截断；列 AFTER 各表现有归属性列；**软引**（无 FK 约束，存量可 NULL）。`market_order` 属**公共行情数据，明确不纳入**。

| 表 | 现有归属性列 | 维度 | 写路径 |
|----|------------|------|--------|
| assets | `owner_id`(=character_id) | 人物 | `AssetsService.saveAndUpdateAsserts` 活跃 |
| blueprints | `owner_id`(=character_id) | 人物 | **休眠，仅 schema+PO 无行为（YAGNI）** |
| mining_detail | `character_id` | 人物 | `MiningDetailService.saveObserverMining` 活跃 |
| wallet_journal | `owner_id` + `division`（0/NULL/1 人物；1-7 军团） | 人物/军团 | 角色/军团同步均活跃 |
| wallet_transaction | `owner_type`+`owner_id`+`division` | 人物/军团 | 角色/军团同步均活跃 |
| industry_job | `corporation_id` | 军团 | `IndustryJobService.batchInsertOrUpdateFromEsi` 活跃 |
| observer | `croporation_id`（既有 mis-spell 映射保留） | 军团 | **休眠，仅 schema+PO 无行为（YAGNI）** |
| structure | `corporation_id` | 军团 | `StructureService.batchInsertOrUpdateFromEsi` 活跃 |

> 注意 `observer` 表真实列名是 mis-spell 的 `croporation_id`——仓库既有映射保持不动，DDL：JOIN 均以实际列名定位，避免 `Unknown column`。

### 写路径 setUserId（6 条活跃链路统一模式）

所有活跃写链路在 `application/service`（对外服务）层，取已授权的 `eveAccount.getUserId()`（Long）回填领域实体 `userId`，**不新增仓储签名**（userId 走领域实体字段透传）；同年份 `setOwnerId(...)` 处一并 `setUserId(...)`：

| 表 | 写方法 | setUserId 位置 |
|----|--------|---------------|
| assets | `AssetsService.saveAndUpdateAsserts` | 在 `setOwnerId(characterId)` 已取值 eveAccount 处同补 |
| mining_detail | `MiningDetailService.saveObserverMining` | setOwner/observer 处同补 |
| wallet_journal | `syncCharacterJournal` / `syncCorporationJournal` | 设 ownerId/division 处同补 |
| wallet_transaction | 角色 / 军团同步 | ownerType=character / corporation 设点同补 |
| structure | `StructureService.batchInsertOrUpdateFromEsi` | 按 corp 批量行设同一 userId |
| industry_job | `IndustryJobService.batchInsertOrUpdateFromEsi` | corp/char 混合行统一归同步者 |

**守层**：userId 赋值只出现在 `application/security/AccessGuard` 与对外服务层（`eveAccount` 仅由授权链产生），领域/infrastructure 层不自行拼装凭据。

## 6. 存量迁移（一次性回填，0 丢失）

迁移在部署时由用户手动执行（`014_esi_user_binding.sql` 迁移段），目标：历史存量行归属正确、**0 丢失**（全部为 UPDATE——不删不插，仅改写 `user_id`，FR-008/SC-004）。

迁移三段（均带 `WHERE user_id IS NULL`，天然幂等可重跑）：

```
-- 1. 人物行 → join eve_account 按 character_id 取 e.user_id（自动回填角色属主）
UPDATE assets a JOIN eve_account e ON e.character_id = a.owner_id     SET a.user_id = e.user_id WHERE a.user_id IS NULL;
UPDATE mining_detail m JOIN eve_account e ON e.character_id = m.character_id SET m.user_id = e.user_id WHERE m.user_id IS NULL;
UPDATE wallet_journal w JOIN eve_account e ON e.character_id = w.owner_id SET w.user_id = e.user_id WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.character_id = t.owner_id AND t.owner_type='character' SET t.user_id = e.user_id WHERE t.user_id IS NULL;
--   （blueprints 同模式）

-- 2. 军团行 → 管理域（ROOT admin userId，部署占位 '?'；join eve_account 限定到系统已登记军团再 SET 常量 admin）
UPDATE structure s JOIN eve_account e ON e.corp_id = s.corporation_id SET s.user_id = ? WHERE s.user_id IS NULL;
UPDATE industry_job i JOIN eve_account e ON e.corp_id = i.corporation_id SET i.user_id = ? WHERE i.user_id IS NULL;
UPDATE observer o JOIN eve_account e ON e.corp_id = o.croporation_id SET o.user_id = ? WHERE o.user_id IS NULL;   -- 注意观察者表列名
UPDATE wallet_journal w JOIN eve_account e ON e.corp_id = w.owner_id SET w.user_id = ? WHERE w.user_id IS NULL;       -- 军团行(division 1-7)
UPDATE wallet_transaction t JOIN eve_account e ON e.corp_id = t.owner_id AND t.owner_type='corporation' SET t.user_id = ? WHERE t.user_id IS NULL;

-- 3. 孤儿行兜底（迁移后仍 user_id IS NULL 的未匹配行 → 管理域 admin）
UPDATE assets / blueprints / mining_detail / industry_job / observer / structure / wallet_journal / wallet_transaction
    SET user_id = ? WHERE user_id IS NULL;
```

语义要点：

- **人物行 → 角色属主**：由 `eve_account.character_id` 关联唯一属主；`user_id = e.user_id` 取角色行的系统用户。
- **军团行 → 管理域**：由 `eve_account.corp_id` 关联系统已登记军团，再 `SET user_id = ?`（常量，非 join 值）。ROOT 豁免使这些军团行**只对 ROOT 可见**——军团数据在迁移后进入管理域，符合 "团长此前同步的军团数据保留给管理域" 的设计。
- **孤儿行兜底**：迁移后仍 `user_id IS NULL`（无人认领）的剩余行，统一归管理域，防止 "漏归属 → 普通用户永远读不到"。
- **归并顺序**：人物段先、军团段后；同一 owner_id 若同时命中 character_id 与 corp_id（EVE ID 空间几乎不重叠），后者覆盖，属多种属主取其一，plan 已裁决不阻塞。
- **0 丢失校验**：迁移前后各表 `SELECT COUNT(*)` 一致；`user_id IS NULL` 归零。

## 7. 安全要点

| 要点 | 说明 |
|------|------|
| 越权统一空 200（非硬 403） | 军团维经 `user_id` 过滤无行 → **统一空 200**，不做硬 403。硬 403 需预探测 "该用户是否同步过此军团"，会形成数据存在性 oracle（FR-007）；统一空既不泄漏也免额外预查询。人物维非属主仍被 `requireOwnership` 挡 |
| fail-closed | `corporationScope` 未认证/身份不可识别抛 `ACCESS_UNAUTHORIZED`，绝不携带 null 空过滤穿透误放全量；单建筑读在 `user_id` 谓词之上**叠加应用层 corp 复核**双保险 |
| 军团 stale 删除按 user_id 隔离 | 军团陈旧数据清理（P6-R1）须按 `user_id` 界定删除范围，避免删到他人/ROOT 的数据 |
| ROOT 豁免有限 | 管理域豁免只落在 ROOT 角色（`isCurrentUserRoot` + `ROOT_ROLE_CODE`），不扩散；未认证令牌即便带 ADMIN 权限也不豁免 |
| 类型统一 | Integer↔Long 转换收敛于 `corporationScope`，`user_id` 全链按 Long 透传 |
| 严格字符串比对 | `isOwnedBy`/`matches` 用字符串比对防御数值表示差异绕过 |
| 迁移 0 丢失 | 迁移只 UPDATE 不改行数；`WHERE user_id IS NULL` 幂等可重跑 |
| 无外部新面 | 本特性纯内部授权加固，无新增 ESI 调用；新建 user_id 等值谓词命中既有归属性索引，无全表扫 |

## 8. 迁移脚本用法（部署维护者）

部署时手动执行 `src/main/resources/db/migration/014_esi_user_binding.sql`（仓库惯例，无 Flyway）：

1. **ADMIN userId 占位替换**：脚本含多处 `user_id = ?` 常量占位，需替换为 ROOT admin 的 userId。建议一键替换（例如 admin userId=1）：
   ```bash
   sed -i 's/user_id = ?/user_id = 1/g' 014_esi_user_binding.sql
   ```
   `person`（join）段取 `eve_account.user_id` 回填，无需占位。
2. **执行顺序**：先让 `ADD COLUMN` 段落库，再执行迁移 UPDATE 段。
3. **幂等标注**：
   - 全部迁移 UPDATE 均 `WHERE user_id IS NULL`——已归属/已兜底的行重跑不改写，天然幂等。
   - 顶层 `ADD COLUMN` 为 `ALTER`，MySQL 无 `ADD COLUMN IF NOT EXISTS`；**重复执行前须先 `CHECK information_schema.columns` 确认列已存在**（或手动确保），否则报 `Duplicate column`。
4. **0 丢失验证**：迁移前后各表 `SELECT COUNT(*)` 比对一致；`user_id IS NULL` 归零。
5. **抽样核对**（US4）：迁移前对人物与军团的存量行各抽样，迁移后验证归属与行数不变。

## 9. 关键代码文件索引

| 环节 | 文件 |
|------|------|
| 访问守卫（requireOwnership / corporationScope / isCurrentUserRoot） | `application/security/AccessGuard.java` |
| 归属策略（isOwnedBy 联盟分支） | `domain/service/security/ResourceOwnershipPolicy.java` |
| 军团读改造调用点 | `application/service/WalletJournalApplicationService.java`、`WalletTransactionApplicationService.java`、`WalletOverviewApplicationService.java`、`StructureQueryApplicationService.java` |
| 军团读谓词 | `src/main/resources/mappers/system/*Mapper.xml`（`<if test="userId != null"> AND user_id = #{userId}</if>`） |
| 写路径 setUserId | `domain/service/system/`（Assets/MiningDetail/WalletJournal/WalletTransaction/Structure/IndustryJobService） |
| 8 表实体/PO | `domain/model/entity/system/*.java`、`infrastructure/persistence/entity/system/*PO.java` |
| 持久化/ESO 转换器 | `infrastructure/assembler/persistence/*PoConverter.java`（MapStruct 同名字段自动映射） |
| 迁移部署产物 | `src/main/resources/db/migration/014_esi_user_binding.sql` |
| Schema 设计 | `specs/014-esi-data-user-binding/data-model.md` |
| 计划与裁决 | `specs/014-esi-data-user-binding/plan.md`（G2 双锁/空 200 裁决）、`tasks.md`（T001–T031） |