# Implementation Plan: ESI 数据 × 系统用户强关联

**Branch**: `014-esi-data-user-binding` | **Date**: 2026-08-19 | **Spec**: [spec.md](../014-esi-data-user-binding/spec.md)
**Input**: Feature specification from `specs/014-esi-data-user-binding/spec.md`
**轨道**: 特性轨（P2 设计规划）

---

## Summary

**需求**（FR-001~FR-008）：给 ESI 同步的每一份业务数据记录落一个 `user_id`（系统用户归属），从根源阻断"知道角色/军团 ID 即越权读取"（IDOR）。核心语义变更：**推翻军团成员共享**——军团维度数据从"同团成员可读全团"收紧为"只对实际同步它的用户可见"；人物维度维持"该角色的全部属主共享"；系统管理域（ROOT）豁免看全量；并补齐联盟维度归属判定机制。

**技术方案总览（双锁模型）**：
1. **Schema**：8 张 ESI 业务表新增 `user_id BIGINT NULL`（assets / blueprints / industry_job / mining_detail / observer / structure / wallet_journal / wallet_transaction）。`market_order` 属公共行情数据，**不**纳入。
2. **写路径**：6 条活跃写入链路从 `EveAccount.getUserId()` 取值落 `user_id`；observer / blueprints 休眠表仅 schema+PO 改造，无行为。`user_id` 类型对齐 `eve_account.user_id`(Long) 原样写入，不截断。
3. **读路径**：
   - 人物维度：沿用 `requireOwnership(charId)`（角色属主共享 FR-004），**不按 user_id 过滤**。
   - 军团维度：**移除** `corpId` 成员匹配，改为 `corporaScoped=corporationScope()` 注入仓储查询 `user_id = :scope`（同步者私有 FR-003）；ROOT → scope null → 不过滤看全量。
4. **管理域豁免**：`AccessGuard.isCurrentUserRoot()`（FR-006）。
5. **联盟机制**：`ResourceOwnershipPolicy.isOwnedBy` 增加 `matches(ownerId, account.getAllianceId())` 分支（FR-005，机制预留，无数据消耗）。
6. **存量迁移**：人物行（assets/blueprints/mining + wallet_journal 人物行 + wallet_transaction ownerType=character）回填其角色属主；军团行（industry_job/observer/structure + 军团 wallet/journal/transaction）回填管理域；行数 0 丢失（FR-008 / SC-004）。

**读路径裁决（随 G2 提交用户审）**：军团维度经 `user_id` 过滤后无行则**统一返回空**（200 空列表），不做硬 403。理由：硬 403 需预探测"该用户是否同步过此军团"，会形成数据存在性 oracle，违背 FR-007；统一空既不泄漏（乙拿不到任何数据，满足 spec Independent Test 表述）也免去额外预查询。若用户希望非同步者硬拒绝，可在 G2 提出，本 plan 预留该分支。

## Technical Context

**Language/Version**: Java 17（Spring Boot 3.5.14，技术栈冻结）
**Primary Dependencies**: MyBatis Plus 3.5.15（ORM）、Spring Security（RBAC/JWT）、MapStruct（映射）、Hutool、Lombok
**Storage**: MySQL（eve_helper 运行时库；eve 静态库本次不动）
**Testing**: JUnit 5 + AssertJ + Mockito + SpringBootTest(@ActiveProfiles("test"))
**Target Platform**: Linux 服务器（Spring Boot 应用）
**Project Type**: Web service（DDD 五层：Interfaces → Application → Domain ← Infrastructure）
**Performance Goals**: 读接口 p95 ≤ 200ms；军团读新增 `user_id` 等值过滤落在既有 owner_id 索引之上，无全表扫
**Constraints**: 技术栈冻结不得增删核心依赖；领域层不得 import 上层类型；严格字符串比对以字符 id 防御 zorbor；`eve_account.user_id` 为 Long
**Scale/Scope**: 8 表 schema；9 个军团读调用点改造；6 条活跃写链路；一次性迁移；联盟维度机制

## Constitution Check

*GATE：Phase 0 前必须通过；Phase 1 设计后复核。*

### API Performance Gates
- 读接口 95 分位 <200ms：**通过设计**。军团读新增 `user_id` 等值谓词，基数 ≤ 用户数，命中既有 owner_id 索引；无 N+1。tasks 附"核对新增谓词命中索引"验证任务。
- 数据库查询优化计划：**满足**。新增过滤全等值；批量 upsert 沿用既有 SaveOrUpdate。
- 外部依赖熔断：本特性不新增外部调用；ESI 集成既有超时/熔断不动。

### Test Coverage Gates
- 单元测试 ≥80%：**满足**。TDD 逐任务覆盖：双锁守卫、军团私有过滤、人物属主共享、管理域豁免、联盟判定、存量迁移、越权空响应不泄漏。
- 集成测试覆盖关键旅程：**满足**。人物私有/军团私有/管理域豁免/存量迁移四条旅程（SC-008）各 ≥1 条。
- 安全测试覆盖认证/授权流：**满足**。本特征即授权加固；`ecc:security-reviewer` 评审（输入/认证/授权必过）。
- 性能 SLA 验证：以"新增谓词命中既有索引"静态核验 + `./mvnw test` 全绿为验收。

## Project Structure

### Documentation（本特性）

```text
specs/014-esi-data-user-binding/
├── spec.md          # G1 已批
├── requirements.md  # G1 已批（checklists/）
├── research.md      # P2 设计决策集中记录（本文件 Phase 0 输出）
├── data-model.md    # P2 8 表 user_id schema（Phase 1 输出）
├── plan.md          # 本文件
└── tasks.md         # P3 生成
```

`contracts/` 与 `quickstart.md`：**跳过**。理由——本特性为纯后端内部授权加固，无对外接口契约变更、无集成新依赖、无用户可运行的新入口；`data-model.md` 已承载 schema 事实。

### Source Code（repository root，单 Spring Boot 工程）

```text
# 领域层（domain，不得 import 上层）
src/main/java/xyz/foolcat/eve/evehelper/domain/
├── model/entity/system/
│   ├── Assets.java            # 加 userId(Long)
│   ├── Blueprints.java        # 加 userId(Long)
│   ├── IndustryJob.java       # 加 userId(Long)
│   ├── MiningDetail.java       # 加 userId(Long)
│   ├── Observer.java          # 加 userId(Long)
│   ├── Structure.java         # 加 userId(Long)
│   ├── WalletJournal.java     # 加 userId(Long)
│   └── WalletTransaction.java # 加 userId(Long)
├── repository/system/
│   ├── AssetsRepository.java          # save 签名不变（写入在领域实体回填 userId）
│   ├── BlueprintsRepository.java
│   ├── IndustryJobRepository.java     # selectByCorpIdAndStatus 加 userId 过滤
│   ├── MiningDetailRepository.java
│   ├── ObserverRepository.java
│   ├── StructureRepository.java       # selectByCorporationId 等 6 读方法加 userId
│   ├── WalletJournalRepository.java   # 军团读方法(division 1-7)加 userId
│   └── WalletTransactionRepository.java
└── service/
    ├── security/ResourceOwnershipPolicy.java  # isOwnedBy 加 allianceId 分支
    └── system/…(AssetsService/MiningDetailService/WalletJournalService/
                WalletTransactionService/StructureService/IndustryJobService) # 写路径 setUserId

# 基础设施层（infrastructure）
src/main/java/xyz/foolcat/eve/evehelper/infrastructure/
├── persistence/entity/system/
│   ├── AssetsPO.java (~ BlueprintsPO ~ WalletJournalPO ~ WalletTransactionPO)      # + user_id Long
│   ├── IndustryJobPO.java / MiningDetailPO.java / ObserverPO.java / StructurePO.java # + user_id Long
├── assembler/persistence/*PoConverter.java   # 8 个：MapStruct 自动带 userId 字段
├── persistence/repository/system/*RepositoryImpl.java # 读方法透传 userId 过滤
├── external/esi 同步器（写路径上游已持有 userId，不在此层改动）
src/main/resources/mappers/system/*.xml       # insert 加 user_id 列；军团读 select 加 <if userId> 谓词

# 应用层（application）
src/main/java/xyz/foolcat/eve/evehelper/application/
├── security/AccessGuard.java      # 新增 corporationScope(String)→Long；requireOwnership 保留
└── service/
    ├── WalletJournalApplicationService.java :129   # 军团读 → corporationScope
    ├── WalletTransactionApplicationService.java :135 # 军团读 → corporationScope
    ├── WalletOverviewApplicationService.java :113   # 军团总览 → corporationScope
    └── StructureQueryApplicationService.java :82/98/133/148/167/199 # 6 军团读 → corporationScope

# 增量迁移 SQL（部署产物，随用户手动执行，不入库）
src/main/resources/db/migration/014_esi_user_binding.sql
```

**Structure Decision**：沿用既有单工程 DDD 五层，不做任何新容器/模块。所有改动落在既有层内，非侵入式；休眠表（observer/blueprints）只补 schema+PO，不新增行为，避免 YAGNI。

## Design Decisions（research.md 合并稿）

| 决策 | 结论 | 依据 |
|------|------|------|
| user_id 类型 | `BIGINT NULL`(Long)，对齐 eve_account.user_id | FK 语义目标；写路径原样取 `eveAccount.getUserId()`(Long) 不截断 |
| 军团读过滤法 | 查询注入 `user_id = :scope`，ROOT→null 不过滤 | 同步者私有 FR-003；等值命中 owner_id 索引 |
| 军团读空态 | 统一空 200，不做硬 403 | 避免"是否同步过"存在性 oracle（FR-007） |
| 人物读 | 保留 `requireOwnership(charId)`，不按 user_id 过滤 | 角色属主共享 FR-004 |
| 联盟机制 | `isOwnedBy` 加 allianceId 匹配分支 | FR-005，机制预留 |
| 存量迁移目标 | 人物行→角色属主；军团行→管理域(ROOT admin user_id) | FR-008；ROOT 豁免使军团行仅 ROOT 可见 |
| 休眠表 | observer/blueprints 仅 schema+PO/converter | 无生产写/读行为，YAGNI |

## Implementation Steps

> 每步附真实文件与签名；每步产出 TDD 失败测试→实现→通过→评审。步骤粒度 <2h，可在 tasks 阶段原子化拆分。

### Phase A：Schema 迁移 SQL

生成 `src/main/resources/db/migration/014_esi_user_binding.sql`：8 张表各执行
```sql
ALTER TABLE assets ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER owner_id;
ALTER TABLE blueprints ADD COLUMN user_id BIGINT NULL ... ;
ALTER TABLE industry_job ADD COLUMN user_id BIGINT NULL AFTER corporation_id;   -- 表名以实际为准
ALTER TABLE mining_detail ADD COLUMN user_id BIGINT NULL AFTER character_id;
ALTER TABLE observer ADD COLUMN user_id BIGINT NULL AFTER corporation_id;
ALTER TABLE structure ADD COLUMN user_id BIGINT NULL AFTER corporation_id;
ALTER TABLE wallet_journal ADD COLUMN user_id BIGINT NULL AFTER owner_id;
ALTER TABLE wallet_transaction ADD COLUMN user_id BIGINT NULL AFTER owner_id;
-- 军团读为主的高频表可选复合索引（决策：依赖 owner_id 主索引，不加；tasks 核对）
```
**验证**：TDD 用 `@ActiveProfiles("test")` 建表 SQL 需同步（检查 test 环境建表脚本是否独立）；若 test 走独立 schema DDL，需在测试建表脚本同加列，否则测试容器无法落库。部署 SQL 由用户手动执行（本仓惯例），不写入 Flyway 引擎（技术栈冻结无 Flyway）。

### Phase B：领域实体 + PO + 转换器 + mapper（8 表）

统一模式，逐表执行：
1. 领域实体加字段 `private Long userId;`（WalletTransaction 及 6 表）。
2. PO 加 `@TableField("user_id") private Long userId;`。
3. 对应 `*PoConverter`（MapStruct）不需要手写映射逻辑——字段名/类型一致自动映射；仅核对 `@Mapping` 无覆盖冲突。
4. `*Mapper.xml` 的 `insert` / `batchInsert` / `insertOrUpdateSelective` script(`<trim>`/`SET`) **加 `user_id` 列与占位符**（对齐 owner_id / corporation_id 现有写法）。

**验证（每表）**：单测断言 PO↔domain 往返 userId 保留；mapper insert 后按 userId 查询命中。

### Phase C：写路径透传 userId（6 条活跃链路）

| 表 | 写方法 | 注入点 |
|----|--------|--------|
| assets | `AssetsService.saveAndUpdateAsserts`（:76-121） | L83 已取 `eveAccount` → L104 处 `asset.setUserId(eveAccount.getUserId())` |
| mining | `MiningDetailService.saveObserverMining`（:42-81） | L44 已取 ev → `detail.setUserId(...)` |
| wallet_journal | `syncCharacterJournal`（:117-157） | L130 → 设 ownerId/division 处 `setUserId` |
| wallet_journal | `syncCorporationJournal`（:178-232） | L187/193 → L208 处 `setUserId` |
| wallet_transaction | 角色/军团同步（:79 / :114） | L119/L201 ownerId 设点 `setUserId` |
| structure | `StructureService.batchInsertOrUpdateFromEsi`（:104-141） | L118 → L134 `setUserId(eveAccount.getUserId())`（按 corp 批量行同 userId） |
| industry_job | `IndustryJobService.batchInsertOrUpdateFromEsi`（:69-119） | L82 → L119 `setUserId`（corp/char 混合行统一归同步者） |

关键：`eveAccount.getUserId()` 为 Long 原样写入；不新增仓储签名（userId 走领域实体字段回填）。**守层**：userId 赋值在 application/对外 service 层，因 `eveAccount` 仅由授权链产生。

**验证**：每链路单测用 Mockito 断言传入仓储的实体 userId == eveAccount.getUserId()；integration 断言插入后 `user_id` 列值正确。

### Phase D：读路径安全模型

**D1 `AccessGuard` 新增军团作用域**（读过滤唯一口令，未认证拒绝）：
```java
public Long corporationScope(String resource) {
    if (isCurrentUserRoot()) return null;              // ROOT → 不过滤，看全量
    Integer uid = UserUtil.getUserId();
    if (uid == null || uid <= 0) { /* warn */ throw new EveHelperException(ResultCode.ACCESS_UNAUTHORIZED); }
    return uid.longValue();                            // 对齐 user_id Long
}
```
**D2 `ResourceOwnershipPolicy.isOwnedBy`** 加联盟维度：
```java
return accounts.stream().anyMatch(account ->
        matches(ownerId, account.getCharacterId()) || matches(ownerId, account.getCorpId())
            || matches(ownerId, account.getAllianceId()));
```
**D3 军团读 9 调用点**：替换 `requireOwnership(corpId,...)` → `Long scope = accessGuard.corporationScope("...")`，scope 传入仓储/查询签名。`WalletOverview ApplicationService:113`、`WalletJournalApplicationService:129`、`WalletTransactionApplicationService:135`、`StructureQueryApplicationService` 6 处。对应 mapper XML 军团读语句加：
```xml
<if test="userId != null"> AND user_id = #{userId} </if>
```
（`userId == null` ⇔ ROOT，含人物侧查询不传该参数、恒不过滤。）
**D4 人物读**：`requireOwnership(charId,...)` 维持不动（char 属主共享），不引入 user_id 谓词。现有 11 个人物读/写调用点**零改动**。

**验证**：双锁守卫单测——非 ROOT 普通用户传他人 charId/corpId 被拒；ROOT 豁免；corporationScope 未认证抛 ACCESS_UNAUTHORIZED、ROOT 返回 null；联盟匹配单测；军团读 XML 谓词在 `userId=null` 时不拼接、非 null 时拼接（XML mapper 单测）。

### Phase E：存量迁移

`014_esi_user_binding.sql` 追加迁移部分（先抽样核对再全量，见 spec US4 Independent Test）：
```sql
-- 人物维度行回填角色属主（仅当唯一属主；多重属主取其一，读不经 user_id 故不影响可见性）
UPDATE assets a JOIN eve_account e ON e.character_id = a.owner_id SET a.user_id = e.user_id;
UPDATE blueprints b JOIN eve_account e ON e.character_id = b.owner_id SET b.user_id = e.user_id;
UPDATE mining_detail m JOIN eve_account e ON e.character_id = m.character_id SET m.user_id = e.user_id;
UPDATE wallet_journal w JOIN eve_account e ON e.character_id = w.owner_id SET w.user_id = e.user_id; -- 人物行
UPDATE wallet_transaction t JOIN eve_account e ON e.character_id = t.owner_id AND t.owner_type='character' SET t.user_id = e.user_id;
-- 军团维度行回填管理域（ROOT admin 的 user_id）
UPDATE structure  s JOIN eve_account e ON e.corp_id = s.corporation_id SET s.user_id = ? /* admin userId 占位，部署时填 ROOT 用户 */;
UPDATE industry_job i JOIN eve_account e ON e.corp_id = i.corporation_id SET i.user_id = ? ;
UPDATE observer   o JOIN eve_account e ON e.corp_id = o.corporation_id SET o.user_id = ? ;
UPDATE wallet_journal w JOIN eve_account e ON e.corp_id = w.owner_id SET w.user_id = ? ;           -- 军团行(division 1-7)
UPDATE wallet_transaction t JOIN eve_account e ON e.corp_id = t.owner_id AND t.owner_type='corporation' SET t.user_id = ? ;
-- 未匹配到属主的孤儿行归管理域兜底
UPDATE assets/wallet_journal/wallet_transaction/… SET user_id = ? WHERE user_id IS NULL;
-- 校验：SELECT COUNT(*) 各表，迁移前后行数一致
```
**迁移脚本先抽样核对（spec US4）**：迁移前对人物与军团的存量行各抽样，迁移后验证归属与行数不变。本仓无 Flyway，`user_id IS NULL` 兜底策略在 Plan 内确立。

### Phase F 测试与评审（贯穿，TDD 铁律）

- 人物私有 / 军团私有 / 管理域豁免 / 存量迁移四条旅程各 ≥1 集成测试（SC-008）。
- 越权空响应不泄漏数据存在性断言（FR-007）：非属主得到空结果而非详细错误。
- 全绿 `./mvnw test`；`ecc:java-reviewer`（必须）+ `ecc:security-reviewer`（本特征授权加固，必过）评审；文档 `docs/knowledge/` 流程文档登记。

## Phase 2（tasks）预告

P3 按 A→F 原子化为 tasks.md：每 task <2h + 显式 AC + RED→GREEN→REFACTOR。休眠表与活跃表分轨并行可作标注。

## Complexity Tracking

> Constitution 无强制违背项（技术栈冻结不动、单一工程、无新增依赖），本表留空。