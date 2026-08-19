---

description: "Task list for ESI 数据 × 系统用户强关联 (014)"
---

# Tasks: ESI 数据 × 系统用户强关联

**Input**: Design documents from `specs/014-esi-data-user-binding/`（spec / plan / research / data-model）
**Prerequisites**: plan.md(status: G2 已批) + spec.md
**轨道**: 特性轨 P3 任务拆解
**TDD 铁律**：每个任务先写失败测试 → 亲证失败（RED）→ 最小实现 → 亲证通过（GREEN）→ 重构。Java 测试命令 `./mvnw test -Dtest=<TestClass>#<method>`。

**组织**：按用户故事分组，每故事独立实现+测试。Plan Phases: A(schema) / B(实体+PO+mapper) / C(写路径) / D(读安全) / E(迁移) / F(测试评审)。

**关键裁决（G2 已批）**：军团读经 `user_id` 过滤无行 → 统一空 200（不做硬 403，防空 oracle）；`user_id BIGINT` 对齐 `eve_account.user_id(Long)`；休眠表（observer/blueprints）仅 schema+PO 无行为。

## 统一改动清单（Phase B 各任务通用，避免重复）

对每张目标表，领域实体 + PO + converter + mapper XML 需同步：
1. `domain/model/entity/system/<X>.java` 加 `private Long userId;`
2. `infrastructure/persistence/entity/system/<X>PO.java` 加 `@TableField("user_id") private Long userId;`
3. `infrastructure/assembler/persistence/<X>PoConverter.java` 核对 MapStruct 自动映射 user_id（同名字段，无 @Mapping 冲突）
4. `src/main/resources/mappers/system/<X>Mapper.xml` 各 insert 语句（`insert`/`batchInsert`/`insertOrUpdateSelective`）加 `user_id` 列与占位符

包前缀 `xyz.foolcat.eve.evehelper`；测试镜像于 `src/test/java/`。

---

## Phase 1: Setup（本特性无需新工程基建）

无新增构建/依赖（技术栈冻结）。直接进入 Foundational。

---

## Phase 2: Foundational (阻塞前置)

**Purpose**: `user_id` 列落地，全部用户故事依赖其存在；本阶段未完成不得开工任何故事。

**⚠️ CRITICAL**: no user story work until all of Phase 2 complete.

- [ ] T001 生成迁移 SQL `src/main/resources/db/migration/014_esi_user_binding.sql`：8 表各 `ALTER TABLE ... ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID'`（对齐 `eve_account.user_id`；market_order 排除）；核对 test 环境建表脚本（若 `src/test/resources` 有独立 schema DDL，同加列否则测试落库失败）
- [ ] T002 [P] 人物 3 表 userId 字段：`Assets`/`Blueprints`/`MiningDetail` 实体 + `AssetsPO`/`BlueprintsPO`/`MiningDetailPO` + 各 `*PoConverter` + `AssetsMapper.xml`/`BlueprintsMapper.xml`/`MiningDetailMapper.xml` insert 加列；单测断言 PO↔domain userId 往返保留
- [ ] T003 [P] 军团 3 表 userId 字段：`IndustryJob`/`Observer`/`Structure` 实体 + `IndustryJobPO`/`ObserverPO`/`StructurePO` + 各 `*PoConverter` + `IndustryJobMapper.xml`/`ObserverMapper.xml`/`StructureMapper.xml` insert 加列；单测 userId 往返保留
- [ ] T004 [P] `WalletJournal` 实体 + `WalletJournalPO` + `WalletJournalPoConverter` + `WalletJournalMapper.xml`（人物 division 0/NULL·军团 1-7 同表）insert 加 user_id 列；单测 userId 往返保留
- [ ] T005 [P] `WalletTransaction` 实体（不继承 BaseEntity）+ `WalletTransactionPO` + `WalletTransactionPoConverter` + `WalletTransactionMapper.xml` insert 加 user_id 列；单测 userId 往返保留

**Checkpoint**: 8 表 `user_id` 列 + 实体/PO/converter/XML 就绪。故事可并行开工。

---

## Phase 3: User Story 1 - 人物数据只对自己可见（Priority: P1）🎯 MVP

**Goal**: 人物维度数据（字符属主）写路径落 user_id；读路径维持字符属主变共享（FR-002/FR-004），非属主被挡。
**Independent Test**: 甲持角色 A 同步资产后，甲查 A 返回完整；乙（未持 A）查 A 被挡且响应不泄漏数据存在性（FR-007）。

### Implementation for User Story 1

- [ ] T006 [US1] 字符写路径落 userId：`AssetsService.saveAndUpdateAsserts`（`domain/service/system/AssetsService.java`）在 `setOwnerId(characterId)` 处同补 `setUserId(eveAccount.getUserId())`；单测断言传入 `AssetsRepository.batchInsertOrUpdate` 的实体 userId==eveAccount.getUserId()
- [ ] T007 [US1] 字符写路径落 userId：`MiningDetailService.saveObserverMining`（`domain/service/system/MiningDetailService.java`）setOwner/observer 处补 `setUserId`；单测断言 userId==eveAccount.getUserId()
- [ ] T008 [US1] 字符写路径落 userId：`WalletJournalService.syncCharacterJournal`（`domain/service/system/WalletJournalService.java`）setOwnerId/division=0 处补 `setUserId`；单测 userId==eveAccount.getUserId()
- [ ] T009 [US1] 字符写路径落 userId：`WalletTransactionService` 角色同步（`domain/service/system/WalletTransactionService.java`）ownerType=character 处补 `setUserId`；单测 userId==eveAccount.getUserId()
- [ ] T010 [US1] 集成测试：甲/乙双用户，甲持角色 A 同步资产+钱包，乙登录但非持有 A；调用 A 资产/钱包接口 → 甲返完整、乙被挡、乙响应不泄漏 A 数据规模（`src/test/java/xyz/foolcat/eve/evehelper/application/CharacterPrivacyIT.java`，@ActiveProfiles("test")）

**Checkpoint**: US1 独立可测（甲见/乙挡）。

---

## Phase 4: User Story 2 - 军团数据只对自己同步的部分可见（Priority: P1）

**Goal**: 军团维度读推翻成员共享 → 同步者私有（FR-003/FR-007）；军团写路径落 userId；联盟机制（FR-005）。
**Independent Test**: 甲/乙同团。甲同步过军团钱包，乙从未同步。军团钱包接口甲可见其同步部分、乙为空（拿不到任何数据）。

### Implementation for User Story 2

- [ ] T011 [US2] 军团句管理层 `AccessGuard.corporationScope(String resource)`（`application/security/AccessGuard.java`）：ROOT→返回 null（不过滤看全量）；非 ROOT 返回 `UserUtil.getUserId().longValue()`；未认证/身份不可识别 → 抛 `ACCESS_UNAUTHORIZED`；单测三类路径（ROOT null / 认证返回 Long / 未认证抛）
- [ ] T012 [US2] 联盟维度机制：`ResourceOwnershipPolicy.isOwnedBy`（`domain/service/security/ResourceOwnershipPolicy.java`）加 `matches(ownerId, account.getAllianceId())` 分支，保留 char/corp 匹配；单测 char/corp/alliance 三口径均判定
- [ ] T013 [US2] 军团写路径落 userId：`WalletJournalService.syncCorporationJournal`（`domain/service/system/WalletJournalService.java`）setOwnerId/division 1-7 处补 `setUserId`；单测 userId==eveAccount.getUserId()
- [ ] T014 [US2] 军团写路径落 userId：`WalletTransactionService` 军团同步（`domain/service/system/WalletTransactionService.java`）ownerType=corporation 处补 `setUserId`；单测 userId==eveAccount.getUserId()
- [ ] T015 [US2] 军团写路径落 userId：`StructureService.batchInsertOrUpdateFromEsi`（`domain/service/system/StructureService.java`）L134 前批量 setUserId(eveAccount.getUserId())；单测 userId==eveAccount.getUserId()
- [ ] T016 [US2] 军团写路径落 userId：`IndustryJobService.batchInsertOrUpdateFromEsi`（`domain/service/system/IndustryJobService.java`）L119 前 setUserId；单测 userId==eveAccount.getUserId()
- [ ] T017 [US2] 军团读过滤改造——`WalletJournalApplicationService`（`application/service/WalletJournalApplicationService.java:129`）：`requireOwnership(corpId,"军团钱包流水")` → `Long scope=accessGuard.corporationScope("军团钱包流水")`，scope 透传仓储；`WalletJournalRepository`/Impl 签名加 `userId`，`WalletJournalMapper.xml` 军团读语句加 `<if test="userId != null"> AND user_id=#{userId}</if>`；单测 scope null/非 null 两分支
- [ ] T018 [US2] 军团读过滤改造——`WalletTransactionApplicationService`（`application/service/WalletTransactionApplicationService.java:135`）同 T017 模式；`WalletTransactionRepository`/Impl/XML 加 userId 谓词；单测 null/非 null
- [ ] T019 [US2] 军团读过滤改造——`WalletOverviewApplicationService`（`application/service/WalletOverviewApplicationService.java:113`：军团钱包总览）：corporationScope + `WalletJournalRepository.selectOverview*` 4查询（`where w.owner_id=?`）加 userId 谓词；单测 null/非 null + ROOT 全量
- [ ] T020 [US2] 军团读过滤改造——`StructureQueryApplicationService`（`application/service/StructureQueryApplicationService.java:82/98/133/148/167/199` 6 处 `"建筑"`）：corporationScope + `StructureRepository`/Impl + `StructureMapper.xml`（selectFuelExpires*/selectSummary/selectTimers 等 `s.corporation_id=?` 查询）加 userId 谓词；单测 null/非 null
- [ ] T021 [US2] 集成测试：甲/乙同团（`src/test/java/xyz/foolcat/eve/evehelper/application/CorporationPrivacyIT.java`）——甲同步军团钱包后甲查见其部分；乙从未同步 → 乙查询为**空 200、非 403**（FR-007 不泄漏、不透露乙同步过没）；甲只见自己同步部分不并入他人数据（FR-003 AC3）

**Checkpoint**: US2 独立可测（士兵甲/乙同团不同可见性）。

---

## Phase 5: User Story 3 - 系统管理域可见全部（Priority: P2）

**Goal**: ROOT（ADMIN）看全量，不受私有过滤（FR-006）；普通用户保持私有（不是豁免扩散）。
**Independent Test**: 以 ROOT 身份调任意军团/字符接口返全量；普通用户调非属主仍空/被挡。

### Implementation for User Story 3

- [ ] T022 [US3] ROOT 豁免集成核验：`corporationScope` ROOT→null 路径 + 既有 `isCurrentUserRoot()` 在军团/字符读链路全量通过；构造 ROOT token 调军团钱包/建筑接口返全量（对甲同步与乙同步的数据都可见）（`src/test/java/xyz/foolcat/eve/evehelper/application/AdminDomainExemptionIT.java`，@ActiveProfiles("test")）
- [ ] T023 [US3] 反例断言：普通用户（非 ROOT）持非法/非属主身份调军团接口仍为空——验证"管理域豁免不扩散到普通用户"（SC-003 对照）

**Checkpoint**: US3 可测。

---

## Phase 6: User Story 4 - 存量数据获得正确归属（Priority: P2）

**Goal**: 一次性迁移回填历史行归属，0 丢失（FR-008/SC-004），人物行→角色属主、军团行→管理域。
**Independent Test**: 迁移前人物/军团行各抽样，迁移后人物行归角色属主、军团行归管理域，行数不变。

### Implementation for User Story 4

- [ ] T024 [US4] 追加迁移 UPDATE 至 `src/main/resources/db/migration/014_esi_user_binding.sql`：人物行 join `eve_account` 按 `character_id` 回填 `user_id`（assets/blueprints/mining_detail/wallet_journal 人物行/wallet_transaction owner_type='character'）；军团行 join `eve_account` 按 `corp_id` 回填 ROOT admin userId（含 `?` 部署占位符：industry_job/observer/structure/wallet_journal 军团行/wallet_transaction owner_type='corporation'）；`WHERE user_id IS NULL` 孤儿行归管理域兜底
- [ ] T025 [US4] 迁移正确性与 0 丢失测试：`src/test/java/xyz/foolcat/eve/evehelper/MigrationBackfillTest`（@ActiveProfiles("test") 或迁到测试 schema）——迁移前各表 COUNT 快照，执行迁移后 COUNT 相等（0 丢失）；人物行 user_id==属主、军团行 user_id==管理域；抽样核对（spec US4 Independent Test）
- [ ] T026 [US4] 迁移幂等/可重跑性：`ALTER` 用 `IF NOT EXISTS` 语义或脚本幂等标注；重复执行不产生重复列错误（在测试脚本核对）

**Checkpoint**: US4 可测（迁移 0 丢失 + 归属正确）。

---

## Phase 7: Polish & Cross-Cutting

**Purpose**: 全量验证、索引核对、评审（G6）、文档。

- [ ] T027 军团读性能核验：核对新增 `user_id` 谓词命中既有 owner_id 索引（`EXPLAIN` 或 plan 静态分析语句），确认无全表扫引入（不新增复合索引，决策已定）
- [ ] T028 全量测试+覆盖率：`./mvnw -q clean package -DskipTests`（exit 0）→ `./mvnw test`（0 failures/0 errors）→ 覆盖率 ≥80%（本特性新增/修改逻辑）
- [ ] T029 安全评审：`ecc:security-reviewer`（认证/授权/输入变更必过：双锁守卫、军团私有过滤、越权空响应、迁移 SQL 注入面）
- [ ] T030 Java 评审：`ecc:java-reviewer`（双锁守卫、org 读谓词、写路径 userId 透传、休眠表范围符合 YAGNI）
- [ ] T031 [P] 文档：`docs/knowledge/` 生成 014 流程文档（ESI 数据归属双锁模型 + 军团同步者私有语义 + 迁移脚本用法）+ 登记 `docs/INDEX.md`

**Checkpoint**: G6 无 Critical/Important 未修项 → VERIFICATION REPORT READY。

---

## Dependencies & Execution Order

### Phase Dependencies
- **Foundational (Phase 2)**: 无依赖，先行；BLOCKS 全部故事。
- **US1 (Phase 3)**: 依赖 Phase 2。
- **US2 (Phase 4)**: 依赖 Phase 2；T011 `corporationScope` 与 T012 联盟机制是其余军团读任务的基座。
- **US3 (Phase 5)**: 依赖 US2 的 `corporationScope`（ROOT→null 路径）。
- **US4 (Phase 6)**: 依赖 Phase 2 schema（迁移依赖 user_id 列已存在）。
- **Polish (Phase 7)**: 依赖 US1–US4 完成。

### Story Dependencies
- US1 (P1): 只依赖 Foundational，独立先行。
- US2 (P1): 依赖 Foundational；与 US1 无耦合（字符/军团维度文件分治）。
- US3 (P2): 依赖 US2 的 T011。
- US4 (P2): 依赖 Foundational schema，与 US1/US2 可并行。

### Within Each Story
测试先写并 RED → 实现 GREEN → 重构；模型/字段 → service → 应用服务 → mapper → 集成。

### Parallel Opportunities
- Phase 2: T002–T005 四组互不依赖，可并行。
- US1: T006–T009 四条写链路互不依赖，可并行；T010 集成最后。
- US2 写路径: T013–T016 四条军团写链路并行；读改造 T017–T020 并行（不同文件）；T011/T012 安全基座先行。
- US3: 依赖 T011。
- US4: T024/T025 可并行 T026。

## Implementation Strategy

### MVP First
1. Phase 2（8 表 user_id 落地）→ 2. US1（字符写路径+人物私有）→ **STOP，独立验证 US1**（甲见/乙挡）。
3. US2（军团私有）→ 独立验证。→ US3（ROOT）→ US4（迁移）→ Polish。

### Parallel Team Strategy
Phase 2 四组并行 → US1 四条写链路并行 → US2 写/读分别并行。休眠表（observer/blueprints）仅 schema（T003 内，无行为）避免 YAGNI 蔓延。

## Testing & Quality Gates
- 单元 ≥80%；集成覆盖人物私有/军团私有/管理域豁免/存量迁移四旅程（SC-008）。
- 越权响应不泄漏数据存在性（FR-007）逐端断言。
- 全绿 `./mvnw test`（0 failures/0 errors）证据铁律当前轮次输出。
- G6 定义：Critical 清零 Important 清零（或用户明确豁免并记录）。

## Notes
- [P]=不同文件无依赖并行；[Story]=映射用户故事。
- 每个任务先写失败测试（RED）亲证失败，再实现（GREEN）。
- 每逻辑组提交（`feat:`/`fix:`/`test:`/`docs:` 前缀）。休眠表字段变更并入对应 schema 任务。
- 存量迁移 SQL 部署由用户手动执行（仓库惯例），结论【0 丢失】须以迁移前后各表 COUNT 证据为准。