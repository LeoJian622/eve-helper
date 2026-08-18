# 钱包交易流水流程

> 面向开发者：理清「人物钱包交易手动同步」与「军团钱包交易全 division 同步（含失败隔离）」以及「按角色/分账分页倒序查询」的充电链路。
> 关联代码集中在 `interfaces/web/controller/WalletTransactionController.java`、`application/service/WalletTransactionApplicationService.java`、`domain/service/system/WalletTransactionService.java`、`domain/repository/system/WalletTransactionRepository.java`、`src/main/resources/mappers/system/WalletTransactionMapper.xml`、`infrastructure/external/esi/WalletApi.java`。

## 1. 总览

钱包交易流水（wallet transaction）基于 EVE ESI 的 `/characters/{id}/wallet/transactions/` 与 `/corporations/{cid}/wallets/{division}/transactions/` 端点，提供**人物**与**军团**两类拉取与查询：

- **人物同步**：`POST /wallet/transaction/{cid}/sync` —— from_id 游标拉取该人物交易流水，幂等落库（人物侧 `owner_type='character'`、`division=0`）；
- **人物查询**：`GET /wallet/transaction/{cid}` —— 按时间 `date` 倒序物理分页返回该人物交易流水；
- **军团同步**：`POST /wallet/transaction/corp/{corpId}/sync` —— 遍历军团全分账 **division 1..7** 逐一拉取并幂等落库，**单 division 失败不整批回滚**；
- **军团查询**：`GET /wallet/transaction/corp/{corpId}?division=N` —— 按军团 `division` 过滤倒序分页返回。

两个同步入口的应用层**共同安全前置**：路径 `cid`/`corpId` 是用户可控入参，`accessGuard.requireOwnership(cid|corpId, ...)` 在进入业务逻辑前校验归属，防 IDOR。

```
人物同步: POST /wallet/transaction/{cid}/sync
  -> WalletTransactionApplicationService.syncCharacterTransactions
       -> accessGuard.requireOwnership(cid)                 // 防 IDOR
       -> WalletTransactionService.syncCharacterTransactions
            -> authorize / authorizeInternal                // 归属校验(eve_account 属于当前用户/系统)
            -> getAccessToken(cId, userId)
            -> pullTransactions(from_id 游标)                // 首页 null,以末条 transactionId 续拉更旧,空页/达上限终止
            -> 回填 ownerType='character'/ownerId/division=0
            -> walletTransactionRepository.saveOrUpdateBatch // 幂等 upsert(复合唯一键)

军团同步: POST /wallet/transaction/corp/{corpId}/sync
  -> WalletTransactionApplicationService.syncCorporationTransactions
       -> accessGuard.requireOwnership(corpId)              // 防 IDOR(ResourceOwnershipPolicy 支持军团ID)
       -> WalletTransactionService.syncCorporationTransactions
            -> 遍历 division 1..7,逐一:
                 -> pullTransactions(from_id 游标)            // 每 division 独立游标
                 -> 回填 ownerType='corporation'/ownerId/division=<当前>
                 -> saveOrUpdateBatch                          // 幂等 upsert
                 -> 单 division try/catch 隔离(失败仅记结果,不回滚已成功分账)
            -> 任一失败 -> 汇总抛 EveHelperException(含失败 division 明细),但已成功 division 已在 DB
```

## 2. 同步链路（from_id 游标 + 幂等 upsert）

### 2.1 归属校验（双层）

`WalletTransactionApplicationService` 外层：

```java
public void syncCharacterTransactions(Integer cid) {
    accessGuard.requireOwnership(String.valueOf(cid), "钱包交易同步");  // 防 IDOR
    try {
        walletTransactionService.syncCharacterTransactions(cid);
    } catch (ParseException e) {
        throw new EveHelperException("钱包交易同步失败", e);
    }
}
```

> 军团鉴权：`AccessGuard.requireOwnership` 注释明确支持**人物或军团 ID**，底层 `ResourceOwnershipPolicy.isOwnedBy` 一并处理军团访问，与人物共用同一 API，无需另行分支。`AuthorizeUtil.authorizeInternal` 仅用于无安全上下文的内部通道（领域服务同步内部）。

`WalletTransactionService` 领域层内再做一层授权（严格仿 009 journal 的 `authorizeAccount` 抽取）：

```java
EveAccount eveAccount;
Integer currentUserId = UserUtil.getUserId();
if (currentUserId != null && currentUserId > 0) {
    eveAccount = authorizeUtil.authorize(cId);               // 请求上下文路径
} else {
    eveAccount = authorizeUtil.authorizeInternal(SYSTEM_USER_ID, cId);  // 定时任务/无上下文
}
String accessToken = esiApiService.getAccessToken(cId, eveAccount.getUserId());
```

### 2.2 from_id 游标（本次新增的同步策略，非 page）

与 009 journal 的**页码分页**不同，ESI transactions 端点以 **from_id 游标**（「返回 from_id 之前按 id 倒序的最多 N 条」）拉取，军团侧**无 maxPage**。统一走私有 `pullTransactions(Function<Long,Flux<WalletTransaction>>)`：

```java
Long fromId = null;                    // 首页 null = 最新
while (pageCount++ < MAX_CURSOR_PAGES) {   // MAX_CURSOR_PAGES = 500 防死循环
    List<WalletTransaction> page = pageFetcher.apply(fromId).collectList().block();
    if (page == null || page.isEmpty()) break;      // 空页终止
    all.addAll(page);
    fromId = page.get(page.size() - 1).getTransactionId();  // 末条(当页最小 id)作为下一 fromId 续拉更旧
}
```

要点：
- **注入依赖**：人物的 fetchere 是 `queryCharacterWalletTransactions(cId)`，股东是 `queryCorporationWalletTransactions(corpId, division)`——一个私有方法供双端复用。
- **空页触发终止**：ESI 返回空列表代表已拉全。
- **上限防护**：500 页兜底，避免远端的异常游标行为导致死循环。

### 2.3 幂等 upsert（复合唯一键，本次关键差异）

`WalletTransactionRepository.saveOrUpdateBatch` 对每条记录走 `insert or update ... on duplicate key update`：

> ⚠️ **与 009 journal 的差异**：journal 幂等依赖 `PRIMARY(id)`（`id` 全局唯一）。但 transactions 的 `transaction_id` **跨军团 division 是否全局唯一无法保证**（不同 division 可能各自独立编号；人物与军团也可能重号）。故 **wallet_transaction 的幂等唯一键是复合键 `UNIQUE(owner_type, owner_id, division, transaction_id)`**，主键用自增 BIGINT（不依赖 ESI id）。改表结构前须保持该复合唯一键不变。

| 维度 | 人物 | 军团 |
|------|------|------|
| owner_type | `character` | `corporation` |
| owner_id | characterId | corpId |
| division | 0 | 1..7 |

### 2.4 军团全 division 失败隔离（关键）

`WalletTransactionService.syncCorporationTransactions` **类级不标 `@Transactional`**（避免整体回滚），每个 division 作为独立提交单元：

```java
for (int division = 1; division <= 7; division++) {
    final int cur = division;
    try {
        List<WalletTransaction> list = pullTransactions(
            fromId -> esiApiService.queryCorporationWalletTransactions(corpId, cur, fromId, token));
        list.forEach(tx -> { tx.setOwnerType("corporation"); tx.setOwnerId(corpId.longValue()); tx.setDivision(cur); });
        walletTransactionRepository.saveOrUpdateBatch(list);   // 每 division 自动提交
        results.put(cur, Boolean.TRUE);
    } catch (RuntimeException e) {
        failedDivisions.add(cur);
        results.put(cur, Boolean.FALSE);
        log.warn("军团 division {} 同步失败", cur, e);
    }
}
if (!failedDivisions.isEmpty()) {
    throw new EveHelperException("军团钱包交易同步失败 division=[" + failedDivisions + "]");
}
```

- **不整体回滚**：单 division 失败仅记录，其余 6 个成功分账数据已在 DB。
- **错误可定位**：汇总异常体 `msg` 含失败 division 清单（IT 断言 `containsString("[3]")`）。

## 3. 查询链路（真实物理分页）

`WalletTransactionApplicationService.queryCharacterPage(cid, current, size)` —— 入参校验 → 归属校验 → 分页：

```java
if (cid == null || current < 1 || size < 1 || size > 1000) throw new EveHelperException("分页参数不合法");
accessGuard.requireOwnership(String.valueOf(cid), "钱包交易");
IPage<WalletTransaction> page = new Page<>(current, size);
IPage<WalletTransaction> domainPage =
    walletTransactionRepository.selectPageByOwner(page, "character", cid.longValue(), 0);
return PageResultUtil.copy(domainPage, walletTransactionAssembler::toVo);
```

`queryCorporationPage(corpId, division, current, size)` 同理，`division` 校验到 1..7（与人物 0 区分），过滤参数 `("corporation", corpId.longValue(), division)`。

### 3.1 分页 SQL（保留字反引号 + 真实 IPage 范式）

`WalletTransactionMapper.xml#selectPageByOwner`：

```sql
select <include refid="Base_Column_List"/>
from wallet_transaction
where owner_type = #{ownerType} and owner_id = #{ownerId} and division = #{division}
order by `date` desc   -- date 为 MySQL 保留字,须反引号
```

- 走 **IPage + 分页插件**，专用 `LIMIT ... OFFSET` 由插件生成，`total` 由 count 查询正确填充——与 009 wallet journal 一样是「真实物理分页」参照实现，**不要照搬 assets 的手工 `setRecords`（total=0 缺陷）**。
- 过滤索引 `idx_owner(owner_type, owner_id, division)` 与排序索引 `idx_date(date)` 在 T002 建表迁移一并就位，保证 <500ms。

## 4. 数据模型与出参（eve_helper 库）

`wallet_transaction` 表列：`id`(自增主键)、`owner_type`、`owner_id`、`division`、`transaction_id`、`date`、`type_id`、`quantity`、`unit_price`、`client_id`、`location_id`、`is_buy`、`is_personal`、`journal_ref_id`。唯一键 `uk_owner_div_tx(owner_type, owner_id, division, transaction_id)`；索引 `idx_owner`、`idx_date`。

领域实体 `WalletTransaction` 与响应 VO `WalletTransactionVO` 的同名同型字段（`date` 均 `OffsetDateTime`）由 MapStruct（`WalletTransactionAssembler`）直接映射。`WalletTransactionVO` 出参：`transactionId`、`date`、`typeId`、`quantity`、`unitPrice`、`isBuy`、`clientId`、`locationId`、`journalRefId`、`ownerType`、`ownerId`、`division`。

> 建表迁移：`src/SQL/convert/010_wallet_transaction_create.sql`（`CREATE TABLE IF NOT EXISTS`，幂等可重跑）；测试/生产库需执行后方可跑通集成测试。

## 5. 安全与一致性要点

| 要点 | 说明 |
|------|------|
| 四端点均应用层 requireOwnership | 人物/军团同步、人物/军团查询入口 `accessGuard.requireOwnership(cid|corpId)`，防 IDOR；未认证 401 / 无权 403，fail-closed；军团由 `ResourceOwnershipPolicy` 原生支持 |
| 入参校验先于归属校验 | 分页 `current/size/division` 校验在 `requireOwnership` **之前**，防越权探测（避免无权者借参数错误列出数据存在性） |
| 双层授权（同步） | 应用层 `requireOwnership` + 领域层 `WalletTransactionService` 内 `authorize/authorizeInternal` 归属校验 |
| 幂等 upsert | 复合唯一键 `UNIQUE(owner_type, owner_id, division, transaction_id)` 触发 `ON DUPLICATE KEY UPDATE`，重复同步不产生重复行（区别于 journal 的 PRIMARY(id)） |
| 军团 division 失败隔离 | 类级无 `@Transactional`，每 division 独立提交；单 division 失败不整体回滚、已成功分账保留，异常 `msg` 指明失败 division |
| ESI 失败不写脏数据 | 单 division 的 `block()` 抛错进 catch，不落半截数据 |
| 物理分页 total 正确 | 参照实现（§3.1），避免 assets 的 total=0 缺陷 |
| 保留字 | `date` 列须反引号 |
| 无上下文路径复用 | 定时任务等通过 `authorizeInternal(SYSTEM_USER_ID, ...)` 显式声明身份 |

## 6. 关键代码文件索引

| 环节 | 文件 |
|------|------|
| 控制器（sync/query，人物+军团） | `interfaces/web/controller/WalletTransactionController.java` |
| 应用服务 | `application/service/WalletTransactionApplicationService.java` |
| 领域服务 | `domain/service/system/WalletTransactionService.java`（from_id 游标 + division 循环） |
| 仓储接口/实现 | `domain/repository/system/WalletTransactionRepository.java`、`infrastructure/persistence/repository/system/WalletTransactionRepositoryImpl.java` |
| 分页/批量 SQL | `src/main/resources/mappers/system/WalletTransactionMapper.xml` |
| 批量 upsert | `infrastructure/persistence/.../WalletTransactionMapper.java`（insertOrUpdateSelective） |
| 组装器 / 响应 VO | `application/assembler/system/WalletTransactionAssembler.java`、`application/dto/response/WalletTransactionVO.java` |
| ESI 端口 | `domain/port/esi/EsiGateway.java`（queryCharacter/CorporationWalletTransactions，from_id） |
| ESI 适配 / 转换器 | `infrastructure/external/esi/EsiApiService.java`、`infrastructure/assembler/esi/EsiWalletTransactionConverter.java` |
| 建表迁移 | `src/SQL/convert/010_wallet_transaction_create.sql` |
| 归属校验 | `application/security/AccessGuard.java`、`domain/util/AuthorizeUtil.java` |
| 页向量复制 | `shared/util/PageResultUtil.java` |