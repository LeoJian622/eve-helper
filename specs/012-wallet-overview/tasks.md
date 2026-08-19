# Tasks: 012 钱包总览（Wallet Overview）

**Input**: 设计文档来自 `specs/012-wallet-overview/`
**Prerequisites**: plan.md（已批准 G2）、spec.md（G1 已批准）
**Tests**: 本仓库工作流强制 TDD —— 每个故事先写失败测试(红)再实现(绿),再重构;含单元+集成。

**Organization**: 任务按用户故事分组,实现与测试各自独立。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件、无未完成依赖)
- **[Story]**: 所属用户故事(US1/US2/US3)
- 每个任务含精确文件路径

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 底座/项目初始化。本特性**无**新依赖与 DDL,设置极薄。

- [ ] T001 拉取最新分支基线:确认在 `012-wallet-overview` 分支,`git status` 干净、无无关改动混入
- [ ] T002 核对运行时依赖就绪:Redis 可达 + 两个 MySQL 库(eve/eve_helper)可达(`eve` 只读、`eve_helper` 运行),供集成测试使用

**Checkpoint**: 环境就绪,可进入基础层。

---

## Phase 2: Foundational（阻断性前置 —— 所有故事的共享基座）

**Purpose**: 领域读模型与 infra 结果载体,US1/2/3 全部依赖。

**⚠️ CRITICAL**: 未完成前不得开始任何用户故事实现(TDD)。

- [ ] T003 [P] 创建领域读模型 `WalletOverviewVO`(含嵌套 `CategorySummary`/`TrendPoint`/`DivisionSummary`)于 `src/main/java/xyz/foolcat/eve/evehelper/domain/model/vo/WalletOverviewVO.java`;字段 currentBalance/totalIncome/totalExpense/netFlow/journalCount/asOfTime(OffsetDateTime)/categories/trend/divisions
- [ ] T004 [P] 创建领域摘要载体 `WalletOverviewAggregate` 于 `src/main/java/xyz/foolcat/eve/evehelper/domain/model/vo/WalletOverviewAggregate.java`(currentBalance/totalIncome/totalExpense/netFlow/journalCount/asOfTime)
- [ ] T005 [P] 创建 4 个 infra 结果记录:`WalletOverviewAggregatePO`/`WalletOverviewCategoryPO`/`WalletOverviewTrendPO`/`WalletOverviewDivisionPO` 于 `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/persistence/entity/system/`(规避 mapper 直返 domain 类型的分层气味)

**Checkpoint**: Foundational 就绪——领域 VO 与 infra 载体可用,US1 可启动。

---

## Phase 3: User Story 1 — 人物钱包总览 (Priority: P1) 🎯 MVP

**Goal**: `GET /wallet/overview/{cid}` 返回人物摘要+类别分布+时间趋势。
**Independent Test**: 对已同步流水的角色调人物总览,返回当前余额(最新流水 balance)、区间收支净额、条数、asOfTime、类别分布与趋势;无权拒绝;空数据回零。

### Tests for US1（先写、必红）

- [ ] T006 [P] [US1] 单元测试 `WalletOverviewApplicationServiceTest#...`(Mockito):入参校验(非法 range/start>end 抛 PARAM_ERROR)、`accessGuard.requireOwnership` 被正确调用(人物)、range 预设解析(today/last7d/last30d/last90d/year)、趋势粒度日/月选择、空聚合转零值组装
- [ ] T007 [P] [US1] 集成测试 `WalletOverviewControllerIT#characterOverview...`(@SpringBootTest + @ActiveProfiles("test")):已同步角色总览五项正确、无权 401/403、空数据回零、非法入参 400

### Implementation for US1

- [ ] T008 [P] [US1] `WalletJournalMapper.java` + `WalletJournalMapper.xml` 新增 3 个聚合 SQL(摘要 selectOverviewAggregate、类别 selectOverviewCategories、趋势 selectOverviewTrend;`date` 反引号、金额 COALESCE(...,0));人物调用 **division 传 null 不过滤**
- [ ] T009 [P] [US1] `WalletJournalRepository.java` 新增 `selectOverviewAggregate`/`selectOverviewCategories`/`selectOverviewTrend` 签名;`WalletJournalRepositoryImpl.java` 实现并把 `PO` 映射为 `WalletOverviewAggregate`/`WalletOverviewVO.*`
- [ ] T010 [US1] 新建 `WalletOverviewApplicationService.java`:`getCharacterOverview(Integer cid, String range, Date start, Date end)`——入参边界→resolveRange→requireOwnership→调用 D2→组装(divisions=null)
- [ ] T011 [US1] 新建 `WalletOverviewController.java`(人物端点):`GET /wallet/overview/{cid}?range=&start=&end=`,`@DateTimeFormat(iso=ISO.DATE)`,`Result<WalletOverviewVO>`

**Checkpoint**: US1 独立可用(MVP)。运行 `./mvnw test -Dtest=WalletOverviewApplicationServiceTest,WalletOverviewControllerIT`。

---

## Phase 4: User Story 2 — 军团钱包总览(全分账+分账分布) (Priority: P2)

**Goal**: `GET /wallet/overview/corp/{corpId}` 返回军团整体摘要 + 按分账(1-7)的余额/收支分布。
**Independent Test**: 对已同步全分账流水的军团调军团总览,返回整体摘要与此分账分布、各分账最新余额与区间收支;无权拒绝;无分账历史显零。

### Tests for US2（先写、必红）

- [ ] T012 [P] [US2] 单元测试 `WalletOverviewApplicationServiceTest#...`:军团全量——分账 1..7 补零、`currentBalance`=各分账最新余额之和、division 越界抛 PARAM_ERROR、分账收支/余额按 division 归并
- [ ] T013 [P] [US2] 集成测试 `WalletOverviewControllerIT#corporationOverview...`:已同步军团全量含分布正确、单分账过滤正确、越权拒绝、空分账零值

### Implementation for US2

- [ ] T014 [P] [US2] `WalletJournalMapper.java` + XML 新增 2 个分账 SQL(selectOverviewDivisionBalances MAX(id) 自关联、selectOverviewDivisionFlow GROUP BY division)
- [ ] T015 [P] [US2] `WalletJournalRepository.java` 新增 `selectOverviewDivisionBalances`/`selectOverviewDivisionFlow`;Impl 实现并把 `WalletOverviewDivisionPO` 映射为 `WalletOverviewVO.DivisionSummary`
- [ ] T016 [US2] `WalletOverviewApplicationService.java` 新增 `getCorporationOverview(Integer corpId, Integer division, String range, Date start, Date end)`:division null 或 1..7 校验→resolveRange→requireOwnership→全量时拉分账余额/收支按 1..7 补零、`currentBalance`=各分账和;单分账时同人物仅过滤 division
- [ ] T017 [US2] `WalletOverviewController.java` 新增军团端点:`GET /wallet/overview/corp/{corpId}?division=&range=&start=&end=`

**Checkpoint**: US1 与 US2 均独立可用。运行 `./mvnw test -Dtest=WalletOverviewApplicationServiceTest,WalletOverviewControllerIT`。

---

## Phase 5: User Story 3 — 军团单分账钱包总览 (Priority: P3)

**Goal**: 军团总览可选 `division` 只看单分账。
**Independent Test**: 指定 division 返回该分账的摘要与分布(不含其它分账);division 越界(0/8)被拒。

### Tests for US3（先写、必红）

- [ ] T018 [P] [US3] 单元测试:军团单分账 `getCorporationOverview(corpId, 3, ...)` 仅传 `division=3` 过滤、`divisions=null`;division 为 1..7 边界合法
- [ ] T019 [P] [US3] 集成测试:指定 division=3 的军团总览只含该分账数据;division=0/8 → 400

### Implementation for US3

- [ ] T020 [US3] 双检 `getCorporationOverview` 单分账分支:division 非 null 时各聚合调用仅带该 division 过滤、分账分布不填充(divisions=null)(复用 T016 既有实现,本任务为确认+边界修正)

**Checkpoint**: 三个故事独立可用。

---

## Phase N: Polish & 横切

**Purpose**: 影响多故事;部署/安全/文档收尾。

- [ ] T021 [P] 新建 `src/SQL/convert/012_rbac_permissions.sql`:登记 `GET:/wallet/overview/{cid}` 与 `GET:/wallet/overview/corp/{corpId}` 两条 RBAC 权限并绑定 ADMIN(先 DELETE 后 INSERT,仿 010/011)
- [ ] T022 `./mvnw test` 全量绿(0 failure/0 error) + `ecc:test-coverage` 新代码覆盖率 ≥80%(记录证据)
- [ ] T023 [P] 知识文档 `docs/knowledge/wallet-overview.md`(接口/聚合口径/asOfTime 语义)并登记 `docs/INDEX.md`
- [ ] T024 检查点:P5 验证报告(P5)、P6 `ecc:java-reviewer` 评审、`ecc:checkpoint create "P5-verified"` / 会话保存

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖,直接开始
- **Foundational (Phase 2)**: 依赖 Setup;阻断所有故事
- **User Stories (Phase 3+)**: 依赖 Foundational;US1→US2→US3 顺序或并行(共享 Mapper/仓储,建议顺序以免文件冲突——T008/T014 同改 Mapper XML)
- **Polish**: 依赖所有目标故事完成

### User Story Dependencies

- **US1 (P1)**: 依赖 Foundational;无其它故事依赖。MVP
- **US2 (P2)**: 依赖 Foundational;与 US1 共享 Mapper/仓储/应用服务(建议 US1 后)
- **US3 (P3)**: 依赖 US2 的 `getCorporationOverview`;为单分账分支补测修正

### 故事内顺序

- 测试先行并失败 → 模型/载体 → 服务 → 端点 → 集成
- 完成当前优先故事才进下一个

### Parallel Opportunities

- Foundational 的 T003/T004/T005 可并行
- 各故事内单元与集成测试(如 T006/T007)可并行
- 注意:同文件冲突(Mapper XML 被 T008 与 T014 修改)不宜并行

---

## Parallel Example: US1

```bash
# 并行启动 US1 测试(红):
Task(T006): 单元测试 WalletOverviewApplicationServiceTest（Mockito）
Task(T007): 集成测试 WalletOverviewControllerIT#characterOverview
# 并行实现非冲突部分:
Task(T008): Mapper+XML 3 个聚合 SQL
Task(T009): Repository 接口+实现
# 串行: T010 服务 → T011 端点
```

---

## Implementation Strategy

### MVP First（US1）

1. Phase 1 Setup → Phase 2 Foundational
2. Phase 3 US1（人物总览）→ 独立验证
3. 部署/演示

### Incremental Delivery

1. Setup + Foundational → 基座就绪
2. 加 US1 → 测 → MVP
3. 加 US2（军团全量+分布）→ 测
4. 加 US3（军团单分账）→ 测
5. 每故事增量不破坏既有

---

## Testing & Quality Gates

- 单元+集成覆盖率 ≥80%(`ecc:test-coverage` 证),阻断合并
- 集成覆盖关键路径(越权/空数据/时间过滤/分账)
- 安全:越权 401/403、非法入参 400 均有测试
- 性能:总览为单次聚合查询,响应在秒级内。

## Notes

- 人物 division **传 null 不过滤**(兼容存量 NULL/1),绝不可 `division=0`
- 字段名 `asOfTime`(数据截止时间),勿写 lastSyncTime
- 无 DDL/表结构变更,只读 wallet_journal
- [P]=不同文件无依赖;每逻辑组小步提交(`feat:/test:` 前缀)