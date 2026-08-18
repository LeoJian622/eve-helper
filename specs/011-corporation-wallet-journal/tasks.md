# Tasks: 011 Corporation Wallet Journal（军团钱包流水）

**Input**: [spec.md](spec.md), [plan.md](plan.md)
**Prerequisites**: plan.md (G2 已批准)

**Tests**: TDD 铁律——每任务先写失败测试(RED)再实现(GREEN)。
**Java 根**: `src/main/java/xyz/foolcat/eve/evehelper/`
**Test 根**: `src/test/java/xyz/foolcat/eve/evehelper/`
**SQL 根**: `src/SQL/convert/`

---

## Phase 1: Setup（无独立 setup，复用既有基建）

---

## Phase 2: Foundational — 实体/PO/转换器/仓储底座

- [ ] T001 [P] DDL 迁移:`src/SQL/convert/011_wallet_journal_division.sql`——`ALTER TABLE wallet_journal ADD COLUMN division INT`、`UPDATE SET division=1 WHERE division IS NULL`、`CREATE INDEX idx_owner_div(owner_id, division)`
- [ ] T002 [P] `WalletJournal.java` 加 `private Integer division`;`WalletJournalPO.java` 加 `private Integer division`;`WalletJournalMapper.xml` BaseResultMap 加 `<result column="division">`,Base_Column_List 加 `division`,INSERT/UPDATE SQL 加 division 字段
- [ ] T003 [P] `WalletJournalVO.java` 加 `private Integer division`(查询结果含 division)
- [ ] T004 [P] 仓储层:`WalletJournalRepository` 新增 `selectPageByOwnerAndDivision(IPage, Long ownerId, Integer division)`;`WalletJournalRepositoryImpl` 实现;`WalletJournalMapper` + XML 新增 `selectPageByOwnerAndDivision`(WHERE owner_id + division, ORDER BY `date` DESC)
- [ ] T005 [P] 仓储层 M4 batch:`WalletJournalMapper` 新增 `insertOrUpdateBatch(@Param("list") List<WalletJournalPO>)`;XML `<foreach>` 批量 INSERT ON DUPLICATE KEY UPDATE;`WalletJournalRepositoryImpl.saveOrUpdateBatch` 改为 500 条/批 flush

**Checkpoint**: 底座完成,进入同步/查询服务。

---

## Phase 3: Tests（先 RED）

- [ ] T006 [P] 单元测试 `WalletJournalServiceTest`:军团同步 division 1-7 循环、失败隔离、division 回填;角色同步 division=0 回填
- [ ] T007 [P] 单元测试 `WalletJournalApplicationServiceTest`:军团同步归属校验、冷却限流、入参校验;军团分页 division 校验 1-7、归属校验
- [ ] T008 [P] 集成测试 `WalletJournalControllerCorpIT`:越权拒绝(401/403)、同步+分页查询返回、division 参数校验、幂等性

---

## Phase 4: Implementation — 同步与查询服务

- [ ] T009 `WalletJournalService.syncCharacterJournal` 增加 division=0 回填逻辑(拉取后、保存前回填)
- [ ] T010 `WalletJournalService` 新增 `syncCorporationJournal(Integer corpId)`:division 1-7 循环 + page-based 翻页 + 回填 ownerId/division + saveOrUpdateBatch + try/catch 失败隔离 + 汇总失败抛 EveHelperException
- [ ] T011 `WalletJournalApplicationService` 新增 `syncCorporationJournal(Integer corpId)`(accessGuard + 冷却限流 + 委托领域服务)和 `queryCorporationPage(Integer corpId, Integer division, int current, int size)`(入参校验 + 归属校验 + 分页查询)
- [ ] T012 `WalletJournalController` 新增 `POST /corp/{corpId}/sync` 和 `GET /corp/{corpId}?division=N`

**Checkpoint**: 军团流水同步+分页完整可用。

---

## Phase 5: Polish & 收尾

- [ ] T013 RBAC 权限登记 SQL:`src/SQL/convert/011_rbac_permissions.sql`——2 新端点登记 sys_permission + 绑定 ADMIN
- [ ] T014 知识文档:按 `knowledge-doc-convention` 在 `docs/knowledge/` 生成 `corporation-wallet-journal.md` 并登记 `docs/INDEX.md`

---

## Dependencies & Execution Order

```
T001-T005(底座) → T006-T008(测试 RED) → T009-T012(实现 GREEN) → T013-T014(收尾)
```

### Parallel Opportunities

- T001/T002/T003/T004/T005 无文件重叠可并行
- T006/T007/T008 测试可并行
- T013/T014 可并行
