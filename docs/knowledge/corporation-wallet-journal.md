# 军团钱包流水流程

> 面向开发者：理清「军团钱包流水全 division 同步（page-based 翻页 + 失败隔离）」与「按分账分页倒序查询」两条链路，以及 011 特性引入的 **division 约定**（character=0, corp=1..7）。
> 关联代码集中在 `interfaces/web/controller/WalletJournalController.java`、`application/service/WalletJournalApplicationService.java`、`domain/service/system/WalletJournalService.java`、`domain/repository/system/WalletJournalRepository.java`、`src/main/resources/mappers/system/WalletJournalMapper.xml`、`infrastructure/external/esi/WalletApi.java`。

## 1. 总览

军团钱包流水（corporation wallet journal）基于 EVE ESI 的 `/corporations/{cid}/wallets/{division}/journal/` 端点，提供**军团全分账同步**与**按分账分页查询**：

- **军团同步**：`POST /wallet/journal/corp/{corpId}/sync` —— 遍历军团全分账 **division 1..7** 逐一拉取并幂等落库，**单 division 失败不整批回滚**；
- **军团查询**：`GET /wallet/journal/corp/{corpId}?division=N` —— 按军团 `division` 过滤倒序分页返回。

两个入口的**共同安全前置**：路径 `corpId` 是用户可控入参，应用服务在进入业务逻辑前统一 `accessGuard.requireOwnership(corpId, ...)` 校验归属，防 IDOR。

```
军团同步: POST /wallet/journal/corp/{corpId}/sync
  -> WalletJournalApplicationService.syncCorporationJournal
       -> accessGuard.requireOwnership(corpId)              // 防 IDOR
       -> requireSyncCooldown(cooldownKey)                  // Redis 冷却限流
       -> WalletJournalService.syncCorporationJournal
            -> 遍历 division 1..7,逐一:
                 -> queryCorporationWalletJournalMaxPage(corpId, division, token)  // 总页数
                 -> Stream.iterate(1..maxPage) 串行翻页 queryCorporationWalletJournal(corpId, division, page, token)
                 -> 回填 ownerId=corpId / division=<当前>
                 -> saveOrUpdateBatch                          // M4 批量 upsert(500条/批)
                 -> 单 division try/catch 隔离(失败仅记结果,不回滚已成功分账)
            -> 任一失败 -> 汇总抛 EveHelperException(含失败 division 明细),但已成功 division 已在 DB

军团查询: GET /wallet/journal/corp/{corpId}?division=N&current=&size=
  -> WalletJournalApplicationService.queryCorporationPage
       -> 入参校验(division 1..7, current/size 边界)          // 先于归属校验,防越权探测
       -> accessGuard.requireOwnership(corpId)               // 防 IDOR
       -> WalletJournalRepository.selectPageByOwnerAndDivision(IPage, ownerId, division)
       -> WalletJournalAssembler.toVo -> PageResultUtil.copy
```

## 2. division 约定（character=0, corp=1..7）

011 特性为 `wallet_journal` 表新增 `division` 列，约定：

| 场景 | division 值 | 说明 |
|------|------------|------|
| 人物钱包流水 | 0 | 人物单分账，`owner_id = characterId`，同步时回填 `division=0` |
| 军团钱包流水 | 1..7 | ESI 军团 7 个分账钱包，各自独立编号，同步时回填对应 division |

**数据迁移**（`src/SQL/convert/011_wallet_journal_division.sql`）：

```sql
ALTER TABLE wallet_journal ADD COLUMN division INT;
UPDATE wallet_journal SET division = 1 WHERE division IS NULL;  -- 既有数据全为军团 division=1
CREATE INDEX idx_owner_div ON wallet_journal(owner_id, division);
```

> ⚠️ 迁移脚本**非幂等**（`ALTER TABLE` 重复执行会报 `Duplicate column name`），部署前须确认目标库尚未执行过。如需重跑请先 `DROP COLUMN division` 或改用 `IF NOT EXISTS` 封装。

## 3. 同步链路（page-based 翻页 + M4 批量 upsert + 失败隔离）

### 3.1 归属校验（双层）

`WalletJournalApplicationService.syncCorporationJournal`：

```java
accessGuard.requireOwnership(String.valueOf(corpId), "军团钱包流水同步");  // 防 IDOR
requireSyncCooldown(SYNC_COOLDOWN_PREFIX + "corp:" + corpId);             // Redis 冷却限流
walletJournalService.syncCorporationJournal(corpId);
```

- **IDOR 防御**：`AccessGuard.requireOwnership` 支持人物或军团 ID，底层 `ResourceOwnershipPolicy.isOwnedBy` 一并处理军团访问。
- **Redis 冷却限流**：`wallet:journal:sync:corp:{corpId}` 键，默认 60 秒冷却（`eve-helper.sync.cooldown-seconds`），测试 profile 设为 0 禁用。冷却期内重复同步直接拒绝（`EveHelperException("同步操作过于频繁，请稍后再试")`）。

### 3.2 page-based 翻页（与 010 交易流水 from_id 游标的差异）

与 010 交易流水的 **from_id 游标**不同，ESI journal 端点以**页码分页**（`?page=N`）拉取。军团侧先查 `maxPage`，再 `Stream.iterate(1..maxPage)` 串行翻页：

```java
Integer maxPage = esiApiService.queryCorporationWalletJournalMaxPage(corpId, division, accessToken);
List<WalletJournal> list = Stream.iterate(1, i -> i + 1).limit(maxPage)
        .map(page -> esiApiService.queryCorporationWalletJournal(corpId, division, page, accessToken)
                .collectList().block())
        .sequential().filter(Objects::nonNull).flatMap(Collection::stream)
        .collect(Collectors.toList());
```

要点：
- **每 division 独立 maxPage**：各分账流水量不同，各自翻页到尽头。
- **空页不终止**：与 from_id 游标不同，page-based 以 `maxPage` 为上界，空页 `filter(Objects::nonNull)` 跳过即可。

### 3.3 幂等 upsert（PRIMARY id + M4 批量）

`WalletJournalRepositoryImpl.saveOrUpdateBatch` 走 M4 批量 `<foreach>` 的 `INSERT ON DUPLICATE KEY UPDATE`，500 条/批 flush：

> ⚠️ **与 010 交易流水的差异**：journal 幂等依赖 `PRIMARY(id)`（`id` 全局唯一，ESI 为每条 journal 分配全局唯一 ref_id）。transaction 的幂等则依赖复合唯一键 `UNIQUE(owner_type, owner_id, division, transaction_id)`——两者幂等机制不同，改表结构前须确认 `id` 仍是主键。

### 3.4 军团全 division 失败隔离（关键）

`WalletJournalService.syncCorporationJournal` **类级不标 `@Transactional`**（避免整体回滚），每个 division 作为独立提交单元：

```java
for (int division = 1; division <= 7; division++) {
    final int cur = division;
    try {
        // maxPage -> 翻页 -> 回填 ownerId/division -> saveOrUpdateBatch
        results.put(cur, Boolean.TRUE);
    } catch (Exception e) {
        failedDivisions.add(cur);
        results.put(cur, Boolean.FALSE);
        log.warn("钱包流水军团分账同步失败 corpId={} division={}: {}", corpId, cur, e.getMessage());
    }
}
if (!failedDivisions.isEmpty()) {
    throw new EveHelperException("军团钱包流水部分分账同步失败,失败 division=" + failedDivisions);
}
```

- **不整体回滚**：单 division 失败仅记录，其余 6 个成功分账数据已在 DB。
- **错误可定位**：汇总异常 `msg` 含失败 division 清单。

## 4. 查询链路（真实物理分页）

`WalletJournalApplicationService.queryCorporationPage(corpId, division, current, size)` —— 入参校验 → 归属校验 → 分页：

```java
if (corpId == null || division == null || division < 1 || division > 7) {
    throw new EveHelperException("军团分账参数不合法");
}
if (current < 1 || size < 1 || size > 1000) {
    throw new EveHelperException("分页参数不合法");
}
accessGuard.requireOwnership(String.valueOf(corpId), "军团钱包流水");
IPage<WalletJournal> page = new Page<>(current, size);
IPage<WalletJournal> domainPage =
        walletJournalRepository.selectPageByOwnerAndDivision(page, corpId.longValue(), division);
return PageResultUtil.copy(domainPage, walletJournalAssembler::toVo);
```

### 4.1 分页 SQL（保留字反引号 + 真实 IPage 范式）

`WalletJournalMapper.xml#selectPageByOwnerAndDivision`：

```sql
select <include refid="Base_Column_List"/>
from wallet_journal
where owner_id = #{ownerId} and division = #{division}
order by `date` desc   -- date 为 MySQL 保留字,须反引号
```

- 走 **IPage + 分页插件**，专用 `LIMIT ... OFFSET` 由插件生成，`total` 由 count 查询正确填充——与 009/010 一样是「真实物理分页」参照实现，**不要照搬 assets 的手工 `setRecords`（total=0 缺陷）**。
- 过滤索引 `idx_owner_div(owner_id, division)` 在 011 迁移 SQL 一并就位，保证 <500ms。

## 5. 数据模型与出参（eve_helper 库）

`wallet_journal` 表列（011 后）：`id`(主键)、`amount`、`balance`、`context_id`、`context_id_type`、`date`、`description`、`first_party_id`、`reason`、`ref_type`、`second_party_id`、`tax`、`tax_receiver_id`、`character`、`owner_id`、**`division`**（011 新增）。索引：`idx_owner_div(owner_id, division)`。

领域实体 `WalletJournal` 与响应 VO `WalletJournalVO` 均含 `division` 字段（`Integer`），MapStruct（`WalletJournalAssembler`）直接映射。`WalletJournalVO` 出参：`id`、`amount`、`balance`、`date`、`refType`、`description`、`tax`、`ownerId`、**`division`**。

> 数据迁移：`src/SQL/convert/011_wallet_journal_division.sql`（`ALTER TABLE + UPDATE + CREATE INDEX`）；测试/生产库需执行后方可跑通集成测试。

## 6. 安全与一致性要点

| 要点 | 说明 |
|------|------|
| 两端点均应用层 requireOwnership | 军团同步、军团查询入口 `accessGuard.requireOwnership(corpId)`，防 IDOR；未认证 401 / 无权 403，fail-closed；军团由 `ResourceOwnershipPolicy` 原生支持 |
| 入参校验先于归属校验 | 军团查询 `division 1..7` / `current/size` 校验在 `requireOwnership` **之前**，防越权探测（避免无权者借参数错误列出数据存在性） |
| 双层授权（同步） | 应用层 `requireOwnership` + 领域层 `WalletJournalService` 内 `authorize/authorizeInternal` 归属校验 |
| Redis 冷却限流 | 同步入口 `requireSyncCooldown`，默认 60 秒冷却，防 ESI 限流滥用；测试 profile 可设为 0 禁用 |
| 幂等 upsert | `PRIMARY(id)` 触发 `ON DUPLICATE KEY UPDATE`，重复同步不产生重复行（区别于 010 transaction 的复合唯一键） |
| M4 批量 upsert | `<foreach>` 批量 `INSERT ON DUPLICATE KEY UPDATE`，500 条/批 flush，减少 round-trip |
| 军团 division 失败隔离 | 类级无 `@Transactional`，每 division 独立提交；单 division 失败不整体回滚、已成功分账保留，异常 `msg` 指明失败 division |
| ESI 失败不写脏数据 | 单 division 的 `block()` 抛错进 catch，不落半截数据 |
| 物理分页 total 正确 | 参照实现（§4.1），避免 assets 的 total=0 缺陷 |
| 保留字 | `date`/`character` 列须反引号 |
| division 约定不可变 | character=0, corp=1..7 贯穿全链路（同步回填、查询过滤、索引设计），改动须同步评估 |

## 7. 关键代码文件索引

| 环节 | 文件 |
|------|------|
| 控制器（sync/query，人物+军团） | `interfaces/web/controller/WalletJournalController.java` |
| 应用服务 | `application/service/WalletJournalApplicationService.java`（`syncCorporationJournal`、`queryCorporationPage`） |
| 领域服务 | `domain/service/system/WalletJournalService.java`（`syncCorporationJournal` — division 循环 + page-based 翻页） |
| 仓储接口/实现 | `domain/repository/system/WalletJournalRepository.java`、`infrastructure/persistence/repository/system/WalletJournalRepositoryImpl.java` |
| 分页/批量 SQL | `src/main/resources/mappers/system/WalletJournalMapper.xml`（`selectPageByOwnerAndDivision`） |
| M4 批量 upsert | `WalletJournalMapper.java`（`insertOrUpdateBatch`）+ XML `<foreach>` |
| 组装器 / 响应 VO | `application/assembler/system/WalletJournalAssembler.java`、`application/dto/response/WalletJournalVO.java` |
| ESI 端口 | `domain/port/esi/EsiGateway.java`（`queryCorporationWalletJournal*`，page-based） |
| ESI 适配 / 转换器 | `infrastructure/external/esi/EsiApiService.java`、`infrastructure/assembler/esi/EsiWalletJournalConverter.java` |
| 数据迁移 | `src/SQL/convert/011_wallet_journal_division.sql` |
| RBAC 权限登记 | `src/SQL/convert/011_rbac_permissions.sql` |
| 归属校验 | `application/security/AccessGuard.java`、`domain/util/AuthorizeUtil.java` |
| 页向量复制 | `shared/util/PageResultUtil.java` |
| 缓存网关 | `domain/port/cache/CacheGateway.java`（冷却限流底层） |
