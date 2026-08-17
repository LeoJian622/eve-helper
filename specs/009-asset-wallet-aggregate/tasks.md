---

description: "Task list for 009 asset-wallet-aggregate implementation"
---

# Tasks: Asset Multi-Role Aggregate & Wallet Journal

**Input**: Design documents from `/specs/009-asset-wallet-aggregate/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: 本特性按 TDD 铁律执行——每任务先写失败测试(RED)再实现(GREEN)。所有新增功能测试路径见各任务。

**Organization**: 按批次组织(治理修复→US1 资产聚合→US2 钱包流水→权限/文档收尾)。US1/US2 依赖批次 A。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行的任务(不同文件、无依赖)
- **[Story]**: US1(资产聚合) / US2(钱包流水)。批次 A/D 无 story 标签(跨故事前置/收尾)
- Java 根: `src/main/java/xyz/foolcat/eve/evehelper/`
- Test 根: `src/test/java/xyz/foolcat/eve/evehelper/`(镜像 main 包结构,`@SpringBootTest`)
- SQL 根: `src/SQL/convert/`
- 命令用 `./mvnw test -Dtest=<TestClass>#<method>(可加 -q 降噪)`

## Phase 1: Setup (本项目已有完整基建,无独立 setup 任务)

**Purpose**: 009 无新环境下手——复用既有 DDD 五层、EsiGateway、PageResultUtil、幂等 upsert、`@SpringBootTest` 体系。技术栈冻结,无依赖新增。

---

## Phase 2: Foundational - 治理修复(前置,US1/US2 均被 A 阻塞)

**Purpose**: 修复两个现状缺陷,否则资产聚合恒空、钱包重复同步产生脏数据。**A 未完成,US1/US2 不得开始。**

- [x] T001 修复资产 ownerId 空置:在 `.../domain/service/system/AssetsService.java` 的 `saveAndUpdateAsserts` 中,于 `batchInsertOrUpdate(assets)` 前对每个 `Assets a` 执行 `a.setOwnerId((long) eveAccount.getCharacterId())`
- [x] T002 [P] 钱包幂等对齐:基线脚本 `wallet_journal.id` 补 PRIMARY KEY + 新增 `src/SQL/convert/009_wallet_journal_unique.sql`(幂等兜底去重+补主键;废弃冗余 UNIQUE,见 plan D2 裁决)

**Checkpoint**: 两处治理修复提交后,进入 US1/US2。

---

## Phase 3: User Story 1 - 资产多角色聚合 (Priority: P1) 🎯 MVP

**Goal**: 单次请求返回当前用户所有角色的资产概览(件数/价值/类目数),按角色分组。

**Independent Test**: `GET /assets/aggregate` 在登录用户拥有已同步资产的多个角色时,返回按角色分组的聚合结果;空角色/无角色/越权边界不报错。

### Tests for User Story 1 (先 RED)

- [x] T003 [P] [US1] 集成测试 `AssetsAggregateIT`(interfaces/web)验证 `GET /assets/aggregate`:多角色返回分组、含件数/价值/类目数
- [x] T004 [P] [US1] 集成测试验证空资产角色→0 值、无角色→空列表、越权角色不出现(安全断言)

### Implementation for User Story 1

- [x] T005 [P] [US1] 新增聚合读模型 `.../domain/model/vo/AssetsAggregateVO.java`(record: `Integer ownerId, Long assetCount, Double assetValue, Long categoryCount`)
- [x] T006 [P] [US1] `AssetsMapper.java` + `src/main/resources/mappers/system/AssetsMapper.xml` 新增 `AssetsAggregatePO selectAggregateByOwnerId(Integer ownerId)`(LEFT JOIN `inv_types it` + `SUM(ass.quantity) AS assetCount, SUM(ass.quantity*COALESCE(it.base_price,0)) AS assetValue, COUNT(DISTINCT ass.type_id) AS categoryCount` WHERE `ass.owner_id=#{ownerId}` GROUP BY `ass.owner_id`)
- [x] T007 [US1] `AssetsRepository.java` + Impl 暴露 `AssetsAggregate acquireAggregateByOwnerId(Integer ownerId)`
- [x] T008 [US1] `AssetsService.java` 暴露薄透传 `AssetsAggregate getAggregateByOwnerId(Integer ownerId)`
- [x] T009 [US1] `AssetsApplicationService.java` 新增 `List<AssetsAggregateVO> aggregateAssetsByUser()`——`UserUtil.getUserId()` → `EveAccountService.getAccountList(userId)` 枚举角色 → 逐角色 `getAggregateByOwnerId(characterId)` → 组装 VO 列表;无角色/未认证返回空列表,无资产角色返回 0 值
- [x] T010 [US1] `AssetsController.java` 新增 `GET /assets/aggregate` → `Result<List<AssetsAggregateVO>>`

**Checkpoint**: US1 独立可测试——`GET /assets/aggregate` 完整可用。

---

## Phase 4: User Story 2 - 人物钱包流水 (Priority: P2)

**Goal**: 单角色手动同步钱包流水 + 分页查询。

**Independent Test**: `POST /wallet/journal/{cid}/sync` 同步后,`GET /wallet/journal/{cid}?current&size` 倒序分页返回;越权 401/403;重复同步幂等。

### Tests for User Story 2 (先 RED)

- [x] T011 [P] [US2] 集成测试 `WalletJournalControllerIT`(interfaces/web):未登录越权拒绝(401/403)
- [x] T012 [P] [US2] 集成测试:同步后分页查询返回 `WalletJournalVO` 列表(倒序、含 amount/balance/date/refType/description)
- [x] T013 [P] [US2] 集成测试:重复同步不产生重复行(幂等);同步 ESI 失败返回错误不写脏数据

### Implementation for User Story 2

- [x] T014 [P] [US2] `EsiGateway.java` 新增人物钱包流水端口:`Integer queryCharacterWalletJournalMaxPage(Integer characterId, String accessToken)` 与 `Flux<WalletJournal> queryCharacterWalletJournal(Integer characterId, int page, String accessToken)`
- [x] T015 [US2] `.../infrastructure/external/esi/EsiApiService.java` 实现两方法(调 `walletApi.queryCharacterWalletJournal(MaxPage)(characterId, EsiClientConfig.SERENITY, page, accessToken)` 并 `.map(w -> esiWalletJournalConverter.toDomain(w, characterId, resolveWalletCharacter(w)))`)
- [x] T016 [P] [US2] 新增 `application/dto/response/WalletJournalVO.java`(含 id, amount, balance, date(OffsetDateTime), refType, description, tax, ownerId)
- [x] T017 [P] [US2] 新增 `application/assembler/system/WalletJournalAssembler.java`(MapStruct `WalletJournalVO toVo(WalletJournal)` + List 重载)
- [x] T018 [P] [US2] `WalletJournalMapper.java` + `src/main/resources/mappers/system/WalletJournalMapper.xml` 新增 `IPage<WalletJournalPO> selectPageByOwnerId(IPage, @Param("ownerId") Integer ownerId)`(保留字 `` `date` `/`` `character` `` 反引号,`ORDER BY `` `date` `` DESC`)
- [x] T019 [US2] `WalletJournalRepository.java` + Impl 新增 `IPage<WalletJournal> selectPageByOwnerId(IPage<WalletJournalPO> page, Integer ownerId)`(PoConverter 转换)
- [x] T020 [US2] `WalletJournalService.java` 新增 `void syncCharacterJournal(Integer cId)`——authorize/authorizeInternal + 人物端点 `queryCharacterWalletJournalMaxPage` 串行分页 + `walletJournalRepository.saveOrUpdateBatch`(仿 `batchInsertOrUpdateFromEsi` 改为人物端点)
- [x] T021 [US2] 新增 `application/service/WalletJournalApplicationService.java`:`void syncCharacterJournal(Integer cid)`(`accessGuard.requireOwnership`, ParseException→EveHelperException)、`PageResult<WalletJournalVO> queryPage(String cid, int current, int size)`(`requireOwnership` + 分页 + `PageResultUtil.copy(page, walletJournalAssembler::toVo)`)
- [x] T022 [US2] 新增 `interfaces/web/controller/WalletJournalController.java`:`POST /wallet/journal/{cid}/sync`、`GET /wallet/journal/{cid}`(current 默认 1,size 默认 20,经参数校验)

**Checkpoint**: US2 独立可用——同步+分页完整,越权拒绝。

---

## Phase 5: Polish & 收尾

- [ ] T023 RBAC 权限登记 SQL:`src/SQL/convert/009_rbac_permissions.sql`——新增 `sys_permission` 行(`GET:/assets/aggregate`、`POST:/wallet/journal/{cid}/sync`、`GET:/wallet/journal/{cid}`)及 `sys_role_permission` 绑定到 ADMIN/对应角色
- [ ] T024 [P] 知识文档:按 `knowledge-doc-convention` 在 `docs/knowledge/` 生成资产聚合与钱包流水两篇流程文档并登记 `docs/INDEX.md`
- [ ] T025 [P] 补充单元测试覆盖聚合/同步分页边界,达到 ≥80%

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup(Phase 1)**: 无(基建已存在,技术栈冻结)
- **Foundational(Phase 2)**: T001→T002,完成后放行 US1/US2
- **US1(Phase 3)**: T003-T010,依赖 T001(ownerId 回填)
- **US2(Phase 4)**: T011-T022,依赖 T002(幂等 DDL)——同步幂等依赖唯一键
- **Polish(Phase 5)**: T023-T025,依赖 US1/US2 完成后

### 顺序执行建议

```
T001 → T002 ─┬─→ US1: T003-T010
              └─→ US2: T011-T022
之后 → US1(资产聚合)→ US2(钱包流水)
最后 → T023 权限 → T024 文档 → T025 覆盖
```

### Parallel Opportunities

- **批次 A**: T001 与 T002 无文件重叠,可并行
- **US1 内**: T005/T006 相互独立(不同文件)可并行;T003/T004 测试先行;T007 依赖 T006、T009 依赖 T007/T008、T010 依赖 T009
- **US2 内**: T014/T016/T017/T018 相互独立(不同文件)可并行;T015 依赖 T014、T019 依赖 T018、T020 依赖 T015、T021 依赖 T019/T020、T022 依赖 T021
- **Polish**: T023/T024/T025 独立可并行

### 每故事内顺序(TDD)

Tests(RED)→ Models/VO → Mapper/Repository → Service → Application → Controller(GREEN)→ 提交

---

## Implementation Strategy

### MVP First (US1 only)

1. T001(ownerId 回填)
2. US1: T003-T010 → 验证 `GET /assets/aggregate` 独立可用 → 可部署演示
3. 再交付 US2(需 T002 DDL)

### Incremental Delivery

1. Foundation(批次 A)→ 聚合(US1)→ 钱包(US2)→ 权限/文档
2. 每个 US 独立增值,不破坏前序

---

## Testing & Quality Gates

- 覆盖 ≥80%(T025 补足)
- 集成测试覆盖 US1/US2 全旅程(T003/T004/T011/T012/T013)
- 安全测试覆盖越权拒绝(T004/T011)
- 权限端点已在 RBAC SQL 登记(T023)
- 全量 `./mvnw test`:0 failure / 0 error(先逐用例 diff,勿用固定阈值判回归)

### 关键类型一致约束

- `ownerId`:聚合 VO 与 `EsiWalletJournalConverter.toDomain` 参数类型显式对齐(`Integer`),跨层显式转换,防 MapStruct 隐式歧义
- 日期:`WalletJournal.date` 为 `OffsetDateTime`;分页 VO 沿用
- 保留字段:SQL 中 `` `date` ``、`` `character` `` 必须反引号

## Notes

- 每任务提交一次或一个逻辑组;提交信息 `feat:/fix:/refactor:` 前缀
- 每 US 结束 Checkpoint 独立验证
- 卡住超 3 次尝试→停下问用户
- T001 修 AssetsService 时勿动 converter(ownerId 改由 service 回填);T002 迁移先执行 DELETE 去重再 ADD UNIQUE(同文件两段)