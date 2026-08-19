---

description: "Task list for 010 wallet-transactions implementation"
---

# Tasks: Wallet Transactions（钱包交易流水）

**Input**: Design documents from `/specs/010-wallet-transactions/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: 本特性按 TDD 铁律执行——每任务先写失败测试(RED)再实现(GREEN)。所有新增功能测试路径见各任务。

**Organization**: 按批次组织(底座→ESI 端口→同步/查询服务→控制器/权限收尾)。US1(人物)/US2(军团)部分共享底座与同步服务。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行的任务(不同文件、无依赖)
- **[Story]**: US1(人物交易) / US2(军团交易)。批次 A/B/D 无 story 标签(跨故事前置/收尾)
- Java 根: `src/main/java/xyz/foolcat/eve/evehelper/`
- Test 根: `src/test/java/xyz/foolcat/eve/evehelper/`(镜像 main 包结构,`@SpringBootTest`)
- SQL 根: `src/SQL/convert/`
- 命令用 `./mvnw test -Dtest=<TestClass>#<method>(可加 -q 降噪)`

## Phase 1: Setup (本项目已有完整基建,无独立 setup 任务)

**Purpose**: 010 无新环境下手——复用既有 DDD 五层、EsiGateway、PageResultUtil、幂等 upsert、`@SpringBootTest` 体系。技术栈冻结,无依赖新增。

---

## Phase 2: Foundational - 领域与持久化底座(前置,US1/US2 均被 A 阻塞)

**Purpose**: 新增 `wallet_transaction` 全链路(实体/建表/PO/仓储)。**A 未完成,US1/US2 不得开始。**

- [ ] T001 [P] 新增领域实体 `.../domain/model/entity/system/WalletTransaction.java`(ownerType, ownerId(Long), division, transactionId(Long), date(OffsetDateTime), typeId(Integer), quantity(Integer), unitPrice(Double), clientId(Integer), locationId(Long), isBuy(Boolean), isPersonal(Boolean), journalRefId(Long))
- [ ] T002 [P] 建表迁移:新增 `src/SQL/convert/010_wallet_transaction_create.sql`——`CREATE TABLE IF NOT EXISTS wallet_transaction`(自增主键 id BIGINT、`UNIQUE KEY uk_owner_div_tx (owner_type, owner_id, division, transaction_id)`、索引 `idx_date (date)`、索引 `idx_owner (owner_type, owner_id, division)`)
- [ ] T003 [P] 新增持久化 PO `.../infrastructure/persistence/entity/system/WalletTransactionPO.java` + 转换器 `.../infrastructure/assembler/persistence/WalletTransactionPoConverter.java`(PO↔domain,MapStruct)
- [ ] T004 [P] 新增仓储接口 `.../domain/repository/system/WalletTransactionRepository.java`＋实现 `.../infrastructure/persistence/repository/system/WalletTransactionRepositoryImpl.java`＋Mapper `.../infrastructure/persistence/mapper/system/WalletTransactionMapper.java`＋XML `src/main/resources/mappers/system/WalletTransactionMapper.xml`:`void saveOrUpdateBatch(List<WalletTransaction>)`(逐条 `insertOrUpdateSelective` 复合键 upsert)＋`IPage<WalletTransaction> selectPageByOwner(IPage, ownerType, ownerId, division)`(保留字 `` `date`` `/`` 反引号,`` ORDER BY `` `date` `` DESC``)

**Checkpoint**: 底座提交后,进入 ESI 端口。

---

## Phase 3: ESI 端口与源拉取(底层能力)

**Purpose**: 暴露人物/军团 transactions 的 from_id 游标端口,使同步服务可拉取 ESI 数据。

- [ ] T005 [P] `.../domain/port/esi/EsiGateway.java` 新增两端口:`Flux<WalletTransaction> queryCharacterWalletTransactions(Integer characterId, Long fromId, String accessToken)` 与 `Flux<WalletTransaction> queryCorporationWalletTransactions(Integer corporationId, Integer division, Long fromId, String accessToken)`
- [ ] T006 [P] `.../infrastructure/external/esi/EsiApiService.java` 实现两适配器(调 `walletApi.queryCharacterWalletTransactions(characterId, EsiClientConfig.SERENITY, fromId, accessToken)` / `walletApi.queryCorporationWalletTransactions(corpId, division, EsiClientConfig.SERENITY, fromId, accessToken)`,并 `.map(resp -> esiWalletTransactionConverter.toDomain(resp))`)
- [ ] T007 新增 ESI→domain 转换器 `.../infrastructure/assembler/esi/EsiWalletTransactionConverter.java`(`WalletTransactionsResponse`→`WalletTransaction`,归属字段 ownerType/ownerId/division **ignore**,service 层回填)

**Checkpoint**: ESI 端口可用后,进入同步/查询服务。

---

## Phase 4: User Story 1 - 人物钱包交易 (Priority: P1) 🎯 MVP

**Goal**: 单角色手动同步钱包交易流水 + 分页查询。

**Independent Test**: `POST /wallet/transaction/{cid}/sync` 同步后,`GET /wallet/transaction/{cid}?current&size` 倒序分页返回;越权 401/403;重复同步幂等。

### Tests for User Story 1 (先 RED)

- [ ] T008 [P] [US1] 集成测试 `WalletTransactionControllerIT`(interfaces/web):未登录越权拒绝(401/403)
- [ ] T009 [P] [US1] 集成测试:同步后分页查询返回 `WalletTransactionVO` 列表(倒序、含 date/typeId/quantity/unitPrice/isBuy)
- [ ] T010 [P] [US1] 集成测试:重复同步不产生重复行(幂等);同步 ESI 失败返回错误不写脏数据

### Implementation for User Story 1

- [ ] T011 [P] [US1] 新增显示 VO `.../application/dto/response/WalletTransactionVO.java`(含 date(OffsetDateTime), typeId, quantity, unitPrice, isBuy, clientId, locationId, journalRefId, ownerType, ownerId, division)
- [ ] T012 [P] [US1] 新增 `.../application/assembler/system/WalletTransactionAssembler.java`(MapStruct `WalletTransactionVO toVo(WalletTransaction)` + List 重载)
- [ ] T013 [US1] 新增领域服务 `.../domain/service/system/WalletTransactionService.java`:(a) `syncCharacterTransactions(Integer cId)`——authorize/authorizeInternal + from_id 游标拉取(见 plan D2)＋回填 ownerType='character'/ownerId/division=0 + `walletTransactionRepository.saveOrUpdateBatch(幂等 upsert)`;(b) `syncCorporationTransactions(Integer corpId)`——军团 division 1–7 遍历循环 try/catch 隔离(见 plan D3)＋回填 ownerType='corporation'/ownerId/division
- [ ] T014 [US1] 新增 `.../application/service/WalletTransactionApplicationService.java`:`syncCharacterTransactions(Integer cid)`(`accessGuard.requireOwnership`,异常→EveHelperException)、`queryCharacterPage(Integer cid, int current, int size)`(`requireOwnership` + 分页 by ownerType/ownerId+division=0 + `PageResultUtil.copy(page, assembler::toVo)`)
- [ ] T015 [US1] 新增 `.../interfaces/web/controller/WalletTransactionController.java`:`POST /wallet/transaction/{cid}/sync`(人物)、`GET /wallet/transaction/{cid}`(人物分页,current 默认 1,size 默认 20,经参数校验)

**Checkpoint**: US1 独立可用——人物同步+分页完整,越权拒绝。

---

## Phase 5: User Story 2 - 军团钱包交易 (Priority: P2)

**Goal**: 遍历 division 1–7 同步军团钱包交易 + 按 division 分页查询。

**Independent Test**: `POST /wallet/transaction/corp/{corpId}/sync` 同步后,`GET /wallet/transaction/corp/{corpId}?division=N` 分页返回;单 division 失败保留已成功;越权拒绝;幂等。

### Tests for User Story 2 (先 RED)

- [ ] T016 [P] [US2] 集成测试:军团全 division 同步后可按 division 分页查询(如 division 2 有数据返回、division 无数据返回空)
- [ ] T017 [P] [US2] 集成测试:单 division 同步失败,已成功 division 数据保留不整批回滚;错误返回含失败 division 明细
- [ ] T018 [P] [US2] 集成测试:重复同步不产生重复行(幂等);无权访问军团 401/403

### Implementation for User Story 2

- [ ] T019 [US2] `WalletTransactionApplicationService.java` 新增军团两法:`syncCorporationTransactions(Integer corpId)`(`requireOwnership` + 调领域服务 division 循环 + 汇总结果)、`queryCorporationPage(Integer corpId, Integer division, int current, int size)`(`requireOwnership` + 分页 by ownerType/ownerId+division + `PageResultUtil.copy`;division 校验 1..7)
- [ ] T020 [US2] `WalletTransactionController.java` 新增军团两法:`POST /wallet/transaction/corp/{corpId}/sync`、`GET /wallet/transaction/corp/{corpId}?division=N`(current 默认 1,size 默认 20,经参数校验)

**Checkpoint**: US2 独立可用——军团全 division 同步+分页完整,失败隔离。

---

## Phase 6: Polish & 收尾

- [ ] T021 RBAC 权限登记 SQL:`src/SQL/convert/010_rbac_permissions.sql`——新增 `sys_permission` 行(`POST:/wallet/transaction/{cid}/sync`、`GET:/wallet/transaction/{cid}`、`POST:/wallet/transaction/corp/{corpId}/sync`、`GET:/wallet/transaction/corp/{corpId}`)及 `sys_role_permission` 绑定到 ADMIN
- [ ] T022 [P] 知识文档:按 `knowledge-doc-convention` 在 `docs/knowledge/` 生成 `wallet-transaction.md` 流程文档并登记 `docs/INDEX.md`
- [ ] T023 [P] 补充单元测试覆盖同步/分页边界,达到 ≥80%——**不可客观测量标注**:本项目 pom 无 jacoco/coverage 插件(技术栈冻结,不新增依赖),"≥80%"无测量工具底座;项目测试基线本身含环境噪声 Error(~219,勿用固定阈值)。以 010 集成的测试覆盖证据(同步语义/越权拒绝/幂等/失败不写脏数据)替代,覆盖 US1/US2 全旅程。

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup(Phase 1)**: 无(基建已存在,技术栈冻结)
- **Foundational(Phase 2)**: T001-T004,完成后放行 US1/US2 (底座)
- **ESI(Phase 3)**: T005-T007,依赖 T004(T006 依赖 T005/T007)
- **US1(Phase 4)**: T008-T015,依赖批次 A(T004)+ESI(T006)
- **US2(Phase 5)**: T016-T020,依赖 US1 的 T013(共享 `syncCorporationTransactions` 领域服务)+T014
- **Polish(Phase 6)**: T021-T023,依赖 US1/US2 完成后

### 顺序执行建议

```
T001-T004(底座) → T005-T007(ESI) ─┬─→ US1: T008-T015
                                    └─→ US2: T016-T020(依赖 T013/T014)
最后 → T021 权限 → T022 文档 → T023 覆盖
```

### Parallel Opportunities

- **批次 A**: T001/T002/T003/T004 无文件重叠,可并行
- **批次 B**: T005/T006 相互独立可并行(不同文件);T007 独立;T006 实现运行时依赖 T005/T007 存在
- **US1 内**: T008/T009/T010(测试先行)、T011/T012 相互独立可并行(不同文件);T013 依赖 T006;T014 依赖 T013/T012;T015 依赖 T014
- **US2 内**: T016/T017/T018 测试可并行;T019 依赖 T013(领域服务)+T014;T020 依赖 T019
- **Polish**: T021/T022/T023 独立可并行

### 每故事内顺序(TDD)

Tests(RED)→ Models/VO → Mapper/Repository → Service → Application → Controller(GREEN)→ 提交

---

## Implementation Strategy

### MVP First (US1 only)

1. 批次 A: T001-T004(底座)
2. ESI: T005-T007(端口)
3. US1: T008-T015 → 验证人物同步+分页独立可用 → 可部署演示
4. 再交付 US2(T016-T020)

### Incremental Delivery

1. Foundation(批次 A)→ ESI(批次 B)→ 人物(US1)→ 军团(US2)→ 权限/文档
2. 每个 US 独立增值,不破坏前序;军团侧复用人物侧的领域服务同步入口

---

## Testing & Quality Gates

- 覆盖 ≥80%(T023 补足,不可客观测量标注)
- 集成测试覆盖 US1/US2 全旅程(T008/T009/T010/T016/T017/T018)
- 安全测试覆盖越权拒绝(T008/T018)
- 权限端点已在 RBAC SQL 登记(T021)
- 全量 `./mvnw test`:0 failure / 0 error(先逐用例 diff,勿用固定阈值判回归)

### 关键类型一致约束

- `ownerId`:库列 `owner_id BIGINT`(domain/WalletTransaction + VO 用 `Long`);`EsiWalletTransactionConverter` 参数 `Integer`(ownerId 来自 ESI charId/corpId Integer),service 层 `longValue()` 回填,跨层显式转换防 MapStruct 隐式歧义
- 日期:`WalletTransaction.date` 为 `OffsetDateTime`;分页 VO 沿用
- 保留字段:SQL 中 `` `date` `` 必须反引号
- division:人物=0,军团 1..7(查询入参校验到 1..7 或传空默认 0/1 语义按 ownerType 分支)

## Notes

- 每任务提交一次或一个逻辑组;提交信息 `feat:/fix:/refactor:` 前缀
- 每 US 结束 Checkpoint 独立验证
- 卡住超 3 次尝试→停下问用户
- T002 建表迁移先 `CREATE TABLE IF NOT EXISTS` 再 `ALTER` 补唯一键(若表已存在),保证幂等可重复执行
- 军团 division 循环(plan D3)在领域服务 `syncCorporationTransactions` 内,勿在应用/接口层裸循环(保持失败隔离与事务边界内聚)
- from_id 游标(plan D2)封装在领域服务私有方法,人物/军团共用;上限常量放足够大(建议 500 页)防死循环