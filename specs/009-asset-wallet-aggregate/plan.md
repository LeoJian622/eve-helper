# Implementation Plan: Asset Multi-Role Aggregate & Wallet Journal

**Branch**: `009-asset-wallet-aggregate` | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-asset-wallet-aggregate/spec.md`

## Summary

为前端新增两个角色数据能力,全部基于现有 EVE 模块扩展:

1. **角色资产多角色聚合查询**:新增 `GET /assets/aggregate`,枚举当前登录用户 `EveAccountService.getAccountList` 下所有角色,逐个角色聚合资产(件数 `sum(quantity)`、价值 `sum(quantity*base_price)`、类目数 `count(distinct type_id)`,均 LEFT JOIN `inv_types`),按角色分组返回。
2. **人物钱包流水(Wallet Journal)对外补全**:EsiGateway 端口补人物 journal 两个方法(infra `WalletApi` 已具备)→ 新增 `WalletJournalApplicationService` 与 `WalletJournalController`(`POST /wallet/journal/{cid}/sync` 手动同步 + `GET /wallet/journal/{cid}` 分页查询)。

**本特性必须包含两处现状缺陷修复,否则新功能不可用**(研究揭示):
- **修复1-资产 ownerId 空置**:`EsiAssetsConverter.toDomain` ignore ownerId 且 `saveAndUpdateAsserts` 未回填 → 全库资产 `owner_id=NULL`,现有 `/assets/{cid}`、stale 删除、聚合按 owner_id 分组**全部空操作**。须在同步时回填 `ownerId=characterId`。
- **修复2-钱包流水幂等失效**:`wallet_journal` 表 `id` 无唯一键 → `insertOrUpdateSelective` 的 `on duplicate key update` 永不触发,重复同步产生重复行。FR-007 要求幂等,须 DDL 迁移加 `UNIQUE(id, owner_id)`。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14 (Web/Validation/Security/Data Redis/WebFlux)、MyBatis Plus 3.5.15、MapStruct 1.6.3、Reactor (Flux/Mono)
**Storage**: MySQL 双数据源(`eve`只读静态库含 `inv_types` 游戏静态表;`eve_helper` 运行时库含 `assets`/`wallet_journal`/`eve_account`);Redis 会话与缓存
**Testing**: Spring Boot Test(`@SpringBootTest`)、JUnit 5、AssertJ、Mockito;web 层用 MockMvc
**Target Platform**: REST API 服务(前端对接)
**Project Type**: 分层 DDD 单体(Interfaces→Application→Domain←Infrastructure)
**Performance Goals**: 资产汇总 ≤5s(角色≤10,资产已落库)、钱包分页 ≤500ms(95th)
**Constraints**: 技术栈冻结;角色归属越权(防 IDOR);`PageResultUtil` 规范分页(不放 assets 的手动 Page 缺陷)
**Scale/Scope**: 本特性 = 资产多角色聚合 + 人物钱包流水;钱包交易(Transactions)明确排除(独立 010 特性)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### API Performance Gates
- **资产汇总**:遍历角色用现成 `getAccountList`,每角色一次聚合 SQL(join 静态表),量级恒定;不引入 N+1 放大。满足实际使用规模(角色≤10)。✅
- **钱包分页**:单角色单条 `IPage` 分页 SQL + `index(owner_id)` 已存在,满足 <500ms。✅
- **外部依赖(ESI)**:沿用现有 `EsiApiService` 的 accessToken 缓存 + 单角色串行分页拉取,不新增重试放大。✅

### Test Coverage Gates
- 目标 ≥80%:覆盖 assets 聚合、wallet 同步/分页/幂等、越权拒绝。✅
- 集成测试:`@SpringBootTest` + MockMvc 覆盖 US1/US2 全旅程。✅
- 安全测试:越权访问 `requireOwnership` 拒绝路径断言。✅

## Project Structure

### Documentation (this feature)

```text
specs/009-asset-wallet-aggregate/
├── plan.md              # 本文(/speckit-plan 输出)
├── spec.md              # 规格(G1 已批)
├── checklists/
│   └── requirements.md  # 规格质量清单(已通过)
└── tasks.md             # /speckit-tasks 输出(P3)
```

### Source Code (repository root)

本特性仅**新增/修改**以下文件(其余为既有只读参照):

```text
# A. 资产多角色聚合
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/model/vo/AssetsAggregateVO.java                 [A] 新增:聚合读模型(record)
├── domain/repository/system/AssetsRepository.java         [M] 新增 aggregateByOwnerId
├── domain/service/system/AssetsService.java               [M] saveAndUpdateAsserts 回填 ownerId + 新增聚合透传
├── application/service/AssetsApplicationService.java      [M] 新增 aggregateAssetsByUser
├── infrastructure/persistence/mapper/system/AssetsMapper.java  [M] 新增 aggregateByOwnerId
├── infrastructure/persistence/repository/system/AssetsRepositoryImpl.java  [M] 实现聚合
├── domain/service/system/EveAccountService.java           [R] getAccountList 枚举角色(只读复用)
└── interfaces/web/controller/AssetsController.java        [M] 新增 GET /assets/aggregate

# B. 钱包流水
├── domain/port/esi/EsiGateway.java                        [M] 新增人物 journal 两端口
├── infrastructure/external/esi/EsiApiService.java         [M] 实现人物 journal 适配器
├── application/assembler/system/WalletJournalAssembler.java [A] 新增 MapStruct VO 装配器
├── application/dto/response/WalletJournalVO.java          [A] 新增显示 VO
├── application/service/WalletJournalApplicationService.java [A] 新增应用服务
├── domain/repository/system/WalletJournalRepository.java  [M] 新增分页签名
├── infrastructure/persistence/mapper/system/WalletJournalMapper.java [M] 新增 selectPageByOwnerId
├── resources/mappers/system/WalletJournalMapper.xml       [M] 新增分页 SQL(保留字反引号)
├── infrastructure/persistence/repository/system/WalletJournalRepositoryImpl.java [M] 新增分页
└── interfaces/web/controller/WalletJournalController.java [A] 新增 REST 控制器

# C. 数据库迁移脚本(src/SQL/convert/ 新增)
src/SQL/convert/009_wallet_journal_unique.sql             [A] UNIQUE(id, owner_id) 迁移
```

**Structure Decision**: 沿用项目 DDD 五层与既有模块(资产/建筑)同构结构;资产聚合进现有 Assets 模块,钱包进新建 WalletJournal 应用/接口层,复用既有持久化与 ESI 转换器。

## 数据模型与关键设计决策

### 决策 D1:资产 ownerId 回填(前提修复)
`AssetsService.saveAndUpdateAsserts` 在 `batchInsertOrUpdate(assets)` 前,对每个资产 `a.setOwnerId((long) eveAccount.getCharacterId())`。理由:ESI 人物资产端点不带 owner,角色归属由调用上下文决定;回填后聚合/IDOR/stale 删除才有效。**并发注意**:聚合接口与同步接口可并行,聚合可能读到未回填的历史行——文档化(不锁库),运维建议同步后再聚合。

### 决策 D2:钱包流水幂等(DDL 迁移)
`wallet_journal` 加 `UNIQUE KEY uk_id_owner (id, owner_id)`。`insertOrUpdateSelective` 的 `ON DUPLICATE KEY UPDATE` 依此键生效。
> 迁移风险:若线上已有重复行,建唯一键会失败——迁移脚本须先去重(`DELETE w FROM wallet_journal w JOIN wallet_journal w2 ON w.id=w2.id AND w.owner_id=w2.owner_id AND w.gmt_create>w2.gmt_create`),按 `(id,owner_id)` 组保留最早一条。

### 决策 D3:聚合价值口径
价值 = `SUM(quantity * base_price)`,`base_price` 来自 `eve` 静态库表 `inv_types`。Market 无行情表,spec Assumptions 已确认用现有字段。聚合 SQL 所在 mapper 归属到能访问 `inv_types` 的数据源(按项目多数据源 `@DS` 配置/分库归属确认)。

### 决策 D4:钱包分页走完整 PageResult 范式
不用 assets 的手动 `Page<>`(total=0 缺陷),仿 `StructureQueryApplicationService` + `PageResultUtil.copy`。

## Phase 0: 研究已就绪(research 结论内联)

研究与探索已穷尽,无需另行研究代理。关键事实已核实:
- `WalletApi.queryCharacterWalletJournal(MaxPage)` 已存在于 infra,`EsiGateway` 缺端口、`EsiApiService` 缺适配器(仅需 2 方法)。
- `EsiWalletJournalConverter.toDomain(resp, Integer ownerId, String character)` 存在(ownerId 为 Integer)。
- `WalletJournalService.batchInsertOrUpdateFromEsi` 串行分页+`saveOrUpdateBatch` 幂等 upsert 模板可直接仿写(改调人物端点)。
- `PageResultUtil.copy(IPage, Function)`、`PageQuery` 字段已确认。
- `EveAccount.getAccountList(userId)` → `List<EveAccount>`,角色键 `characterId`。
- 资产聚合无现成 SQL,须新增(如上 D3)。

## 实现任务(依赖排序,详见 tasks.md)

### 批次 A:治理修复(前置,两缺陷不修则后续不可验证)
- **A1** 资产 ownerId 回填:改 `AssetsService.saveAndUpdateAsserts`
- **A2** 钱包幂等 DDL 迁移:写 `src/SQL/convert/009_wallet_journal_unique.sql`(含去重+唯一键)

### 批次 B:资产多角色聚合
- **B1** 新增 `AssetsAggregateVO`(record: ownerId, assetCount, assetValue, categoryCount)
- **B2** `AssetsMapper` + XML 新增 `aggregateByOwnerId(ownerId)`(LEFT JOIN inv_types + 三聚合)
- **B3** `AssetsRepository` + Impl 暴露 `aggregateByOwnerId`
- **B4** `AssetsService` 暴露 `getAggregateByOwnerId(Integer ownerId)`(薄透传)
- **B5** `AssetsApplicationService.aggregateAssetsByUser()`:getAccountList→逐角色聚合→VO 列表;越权由 `getAccountList(userId)` 天然保证(仅返回本人绑定,无需逐角色 requireOwnership)
- **B6** `AssetsController`: `GET /assets/aggregate` → `Result<List<AssetsAggregateVO>>`

### 批次 C:人物钱包流水
- **C1** `EsiGateway` + 人物 journal 两方法(总页数/分页 Flux)
- **C2** `EsiApiService` 实现适配器(转 SERENITY datasource + `esiWalletJournalConverter.toDomain(resp, characterId, resolveWalletCharacter(resp))`)
- **C3** `WalletJournalVO`(response: id, amount, balance, date, refType, description, tax, ownerId)
- **C4** `WalletJournalAssembler`(MapStruct domain→VO)
- **C5** `WalletJournalMapper` + XML 新增 `selectPageByOwnerId(IPage, ownerId) ORDER BY date DESC`(保留字反引号)
- **C6** `WalletJournalRepository` + Impl 暴露分页(返回 `IPage<WalletJournal>` 用 PoConverter 转换)
- **C7** `WalletJournalService` 新增 `syncCharacterJournal(Integer cId)`(authorize + 人物端点串行分页 + saveOrUpdateBatch)
- **C8** `WalletJournalApplicationService`:`syncCharacterJournal(cid)`(requireOwnership)、`queryPage(cid, current, size)`(requireOwnership + PageResultUtil.copy)
- **C9** `WalletJournalController`:`POST /wallet/journal/{cid}/sync` + `GET /wallet/journal/{cid}`

### 批次 D:权限登记与收尾
- **D1** RBAC 权限登记 SQL(新增 sys_permission + sys_role_permission,端点 `GET:/assets/aggregate`、`POST:/wallet/journal/{cid}/sync`、`GET:/wallet/journal/{cid}`)——项目 RBAC 为 DB 驱动,不在 yml
- **D2** 知识文档:按 `knowledge-doc-convention`,在 `docs/knowledge/` 生成两模块流程文档并登记 INDEX

### 测试设计(每任务 TDD 红-绿)
- 资产聚合:多角色、空资产角色→0 值、无角色→空列表、越权。
- 钱包:同步(幂等重复调、ESI 失败不下脏数据)、分页(倒序、保留字列、空数据)、越权 403/401。
- 类型一致性:ownerId `Long`(聚合 VO)vs `Integer`(converter 参数)跨层显式转换,防 MapStruct 隐式选择歧义。

## Complexity Tracking

无违规项(未新增项目/模式,全部复用既有 DDD 五层、EsiGateway 端口、PageResultUtil、幂等 upsert 模板)。技术栈未动。

## 验证与评审

- **P5**:`./mvnw -q clean package -DskipTests` + `./mvnw test`(0 failure/0 error)、`ecc:test-coverage`(≥80%)、`ecc:security-scan`、`git diff --stat` 审查。
- **P6**:`ecc:java-reviewer`(必须)+ 涉及鉴权追加 `ecc:security-reviewer`,Critical/Important 清零。
- **P7**:finishing-a-development-branch 三选一。