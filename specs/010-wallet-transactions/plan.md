# Implementation Plan: Wallet Transactions（钱包交易流水）

**Branch**: `010-wallet-transactions` | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/010-wallet-transactions/spec.md`

## Summary

为前端补齐钱包交易(Wallet Transactions)数据能力,范围经澄清锁定为**人物 + 军团**,军团侧遍历 **division 1–7** 全账同步。ESI 底层已就位(`WalletApi` 人物/军团 transactions from_id 游标 API + `WalletTransactionsResponse` 模型),但 **EsiGateway 端口、持久化表、应用/接口层完全缺失**,本特性自零补齐:

1. **人物钱包交易(US1)**:`POST /wallet/transaction/{cid}/sync` 手动同步 + `GET /wallet/transaction/{cid}` 分页查询某角色买入/卖出流水。
2. **军团钱包交易(US2)**:`POST /wallet/transaction/corp/{corpId}/sync` 手动同步(遍历 division 1–7) + `GET /wallet/transaction/corp/{corpId}?division=N` 按 division 分页查询。

**关键现状**(研究揭示):
- 人物/军团 transactions ESI 底层均以 **from_id 游标**(非 page)拉取,军团侧**无 maxPage** 端点。同步统一用 from_id 翻页:首页 fromId=null,以每页末条 `transaction_id` 作下页 fromId,直至返回空或达页上限。
- 人物侧另有 maxPage 端点,仅在测试中用于断言拉取上限合理性,**同步主路径不用 page**。
- 需新增独立持久化表 `wallet_transaction`(本特性不触碰 `wallet_journal`)。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14 (Web/Validation/Security/Data Redis/WebFlux)、MyBatis Plus 3.5.15、MapStruct 1.6.3、Reactor (Flux/Mono)
**Storage**: MySQL 双数据源(`eve_helper` 运行时库存 `wallet_transaction`;type 名称可经 type_id 查静态库为可选项)
**Testing**: Spring Boot Test(`@SpringBootTest`)、JUnit 5、AssertJ、Mockito;web 层 MockMvc
**Target Platform**: REST API 服务(前端对接)
**Project Type**: 分层 DDD 单体(Interfaces→Application→Domain←Infrastructure)
**Performance Goals**: 交易分页 ≤500ms(95th);军团全 division 同步可渐进完成(单 division 失败不整批丢弃)
**Constraints**: 技术栈冻结;角色/军团归属越权(防 IDOR);`PageResultUtil` 规范分页;幂等 upsert;**不动 `wallet_journal` 结构**
**Scale/Scope**: 本特性 = 人物 + 军团钱包交易;军团全 division(1–7) 同步;分页查询统一走 domain VO

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### API Performance Gates
- **交易分页**:单 owner/division 单条 `IPage` 分页 SQL,复杂度恒定;按 owner_type+owner_id+division 建索引,满足 <500ms。✅
- **军团全 division 同步**:单个 division 一次 from_id 游标拉取,7 个 division 串行;单 division 失败 try/catch 隔离不整批丢弃(FR-006)。量级可控(单角色/军团交易流水通常有限)。✅
- **外部依赖(ESI)**:复用 `EsiApiService` accessToken 缓存 + from_id 游标,不新增重试放大。✅

### Test Coverage Gates
- 目标 ≥80%:覆盖交易同步(人物/军团全 division/幂等)、分页(按 owner 与 division 过滤、倒序、空数据)、越权拒绝。✅
- 集成测试:`@SpringBootTest` + MockMvc 覆盖 US1/US2 全旅程。✅
- 安全测试:越权访问 `requireOwnership` 拒绝路径断言。✅

## Project Structure

### Documentation (this feature)

```text
specs/010-wallet-transactions/
├── plan.md              # 本文(/speckit-plan 输出)
├── spec.md              # 规格(G1 已批)
├── checklists/
│   └── requirements.md  # 规格质量清单(已通过)
└── tasks.md             # /speckit-tasks 输出(P3)
```

### Source Code (repository root)

本特性仅**新增/修改**以下文件(其余为既有只读参照):

```text
# A. ESI 端口与适配器(人物 + 军团 transactions)
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/port/esi/EsiGateway.java                        [M] 新增人物/军团 transactions 端口
├── infrastructure/external/esi/EsiApiService.java         [M] 实现适配器(人物 + 军团 from_id 游标)
├── infrastructure/assembler/esi/EsiWalletTransactionConverter.java [A] 新增 ESI响应→domain 转换器

# B. 领域实体与仓储
├── domain/model/entity/system/WalletTransaction.java      [A] 新增领域实体
├── domain/repository/system/WalletTransactionRepository.java [A] 新增仓储接口(批量 upsert + 分页)
├── infrastructure/persistence/entity/system/WalletTransactionPO.java [A] 新增持久化 PO
├── infrastructure/persistence/mapper/system/WalletTransactionMapper.java [A] 新增 MyBatis mapper
├── resources/mappers/system/WalletTransactionMapper.xml   [A] 新增分页/批量 upsert SQL
├── infrastructure/persistence/repository/system/WalletTransactionRepositoryImpl.java [A] 新增仓储实现
├── infrastructure/assembler/persistence/WalletTransactionPoConverter.java [A] 新增 PO↔domain 转换器

# C. 应用服务与控制器
├── application/dto/response/WalletTransactionVO.java      [A] 新增显示 VO
├── application/assembler/system/WalletTransactionAssembler.java [A] 新增 MapStruct VO 装配器
├── application/service/WalletTransactionApplicationService.java [A] 新增应用服务(同步+分页+鉴权)
├── domain/service/system/WalletTransactionService.java    [A] 新增领域服务(from_id 游标拉取 + upsert)
├── interfaces/web/controller/WalletTransactionController.java [A] 新增 REST 控制器

# D. 数据库迁移与收尾
src/SQL/convert/010_wallet_transaction_create.sql          [A] 建表迁移(含幂等唯一键 + 索引)
docs/knowledge/wallet-transaction.md                       [A] 流程文档
docs/knowledge/INDEX.md                                    [M] 登记
```

**Structure Decision**: 沿用项目 DDD 五层与既有 wallet journal 模块同构结构;新增独立 `wallet_transaction` 模块,复用 EsiGateway 端口、PageResultUtil、幂等 upsert 模板。军团侧 division 遍历在领域服务层封装,供人物/军团共用 from_id 游标拉取逻辑。

## 数据模型与关键设计决策

### 决策 D1:幂等唯一键 = UNIQUE(owner_type, owner_id, division, transaction_id) 复合键
> **关键风险点**(与 009 journal 的 PRIMARY(id) 不同):journal 的 ESI id 全局唯一,可直接 PRIMARY(id)。但 **transactions 的 `transaction_id` 跨军团 division 是否全局唯一无法保证**——不同 division 可能各自独立编号。为安全起见,幂等唯一键用复合键。
> 人物侧:owner_type='character'、owner_id=characterId、division=0;军团侧:owner_type='corporation'、owner_id=corpId、division=1..7。owner_type 区分维度(人物与军团 transaction_id 可能重号),故复合键含 owner_type。
> `insertOrUpdateSelective` 的 `ON DUPLICATE KEY UPDATE` 依该复合唯一键触发,满足 FR-004(同步幂等)。
> 主键 id 用自增 BIGINT(不依赖 ESI id),唯一约束由复合键承担。

### 决策 D2:同步策略统一 from_id 游标翻页(人物与军团一致)
ESI transactions 端点以 from_id 游标(「返回 from_id 之前(按 id 倒序)的最多 N 条」)拉取,军团侧无 page。统一策略:
1. 从 fromId=null(最新)开始拉第一页。
2. 以该页**末条 `transaction_id`** 作为下一页 fromId,循环拉取。
3. 终止条件:某页返回空 / Flux 空 / 达到配置上限(防死循环,建议 500 页或按时间窗)。
人物侧即便有 maxPage,principal path 也走 from_id(与军团统一),maxPage 仅测试断言用。

### 决策 D3:军团全 division 同步的失败隔离
`WalletTransactionService.syncCorporationTransactions(corpId)` 遍历 division 1..7:
- 每个 division 独立 try/catch,单 division 失败 → 记录失败信息(division + 错误)
- 已成功的 division 数据**立即落库不被回滚**(FR-006)
- 方法返回各 division 的成败列表;任一失败 → 汇总抛出(含失败 division 明细),但库中已成功数据保留。
- 实现:遍历循环外不再套服务级 `@Transactional`(避免整批回滚),单 division 拉取+upsert 视为独立提交单元。

### 决策 D4:交易分页走完整 PageResult 范式
仿 009 wallet journal + `StructureQueryApplicationService` + `PageResultUtil.copy`,不用 assets 的手动 `Page<>`(total=0 缺陷)。查询按 `(owner_type, owner_id[, division])` 过滤,ORDER BY `date` DESC(date 为保留字须反引号)。

### 决策 D5:ownerId 回填与跨层类型
与 009 资产回填同源——ESI 交易端点不带归属,角色/军团归属由调用上下文决定。converter 保持 `ignore` 归属字段,由 service 层在拉取后回填 `ownerType/ownerId/division`。ownerId 统一用 `Long` 入库(与 owner_id BIGINT 列匹配),converter 参数用 `Integer`,跨层显式转换防 MapStruct 隐式歧义。

## Phase 0: 研究已就绪(research 结论内联)

研究与探索已穷尽,无需另行研究代理。关键事实已核实:
- `WalletApi.queryCharacterWalletTransactions(charId, datasource, fromId, token)` 与 `queryCorporationWalletTransactions(corpId, division, datasource, fromId, token)` 已存在于 infra;**EsiGateway 缺端口、EsiApiService 缺适配器**。
- `WalletTransactionsResponse` 模型已存在(date/transaction_id/quantity/type_id/unit_price/client_id/location_id/is_buy/is_personal/journal_ref_id)。
- 人物另有 `queryCharacterWalletTransactionsMaxPage`(page=1 探 max),仅测试断言用。
- 009 的 `WalletJournalService` 串行拉取 + `saveOrUpdateBatch` 幂等 upsert 模板可直接仿写(改调 transactions 端点 + from_id 游标)。
- `PageResultUtil.copy(IPage, Function)`、`PageQuery` 字段已确认。
- `AuthorizeUtil.authorize(cId)` 角色鉴权、`authorizeInternal` 系统身份;军团归属鉴权沿用(经 corp 路径)。
- 军团 division 遍历写死 1..7(无配置化,spec Assumptions 已定)。

## 实现任务(依赖排序,详见 tasks.md)

### 批次 A:领域与持久化底座(前置,后续全依赖)
- **A1** 领域实体 `WalletTransaction`(ownerType/ownerId/division/transactionId/date/typeId/quantity/unitPrice/clientId/locationId/isBuy/isPersonal/journalRefId)
- **A2** `wallet_transaction` 建表迁移 `010_wallet_transaction_create.sql`(UNIQUE(owner_type, owner_id, division, transaction_id) + 分页索引(date))
- **A3** `WalletTransactionPO` + `WalletTransactionPoConverter`(PO↔domain)
- **A4** `WalletTransactionRepository` + Impl + `WalletTransactionMapper` + XML:批量 upsert(insertOrUpdateSelective)+ 分页 selectPageByOwner

### 批次 B:ESI 端口与源—拉取(底层能力)
- **B1** `EsiGateway` 端口(人物 from_id、军团 with division from_id)
- **B2** `EsiApiService` 适配器(人物/军团 from_id,转 SERENITY datasource)
- **B3** `EsiWalletTransactionConverter`:ESI 响应→domain(ownership 字段 ignore,service 回填)

### 批次 C:同步与查询服务
- **C1** `WalletTransactionService`:from_id 游标拉取(统一人物/军团)+ 军团 division 1..7 失败隔离循环 + ownerId 回填 + 幂等 upsert
- **C2** `WalletTransactionVO` + `WalletTransactionAssembler`(MapStruct domain→VO)
- **C3** `WalletTransactionApplicationService`:人物同步/军团同步(requireOwnership)+ 分页(人物/军团 by division,requireOwnership + PageResultUtil.copy)

### 批次 D:接口与收尾
- **D1** `WalletTransactionController`:`POST /wallet/transaction/{cid}/sync`、`GET /wallet/transaction/{cid}`、`POST /wallet/transaction/corp/{corpId}/sync`、`GET /wallet/transaction/corp/{corpId}?division=N`
- **D2** RBAC 权限登记 SQL + 知识文档 `wallet-transaction.md` + INDEX 登记

### 测试设计(每任务 TDD 红-绿)
- 同步:人物单角色、军团全 division(单 division 失败保留已成功)、幂等重复调、ESI 失败不下脏数据、无权 403/401。
- 分页:按 owner/division 过滤、date 倒序、保留字列、空数据、页大小校验。
- 类型一致性:ownerId Long(库)vs Integer(converter 参数)跨层显式转换;division 人物=0/军团 1..7。
- from_id 游标:多页拉取正确以末条 transaction_id 翻页、空页终止、达上限防护。

## Complexity Tracking

无违规项(未新增项目/模式,全部复用既有 DDD 五层、EsiGateway 端口、PageResultUtil、幂等 upsert 模板)。唯一新复杂度为军团 division 遍历 + from_id 游标,封装于领域服务的单一同步入口,不扩散。技术栈未动。

## 验证与评审

- **P5**:`./mvnw -q clean package -DskipTests` + `./mvnw test`(0 failure/0 error)、`ecc:test-coverage`(≥80%)、`ecc:security-scan`、`git diff --stat` 审查。
- **P6**:`ecc:java-reviewer`(必须)+ 涉及鉴权追加 `ecc:security-reviewer`,Critical/Important 清零。
- **P7**:finishing-a-development-branch 三选一。