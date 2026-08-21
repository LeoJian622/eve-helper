# 人物钱包流水流程

> 面向开发者：理清「单角色手动同步钱包 journal（ESI -> DB，幂等 upsert）」与「分页倒序查询资金去向」两条链路。
> 关联代码集中在 `interfaces/web/controller/WalletJournalController.java`、`application/service/WalletJournalApplicationService.java`、`domain/service/system/WalletJournalService.java`、`domain/repository/system/WalletJournalRepository.java`、`src/main/resources/mappers/system/WalletJournalMapper.xml`、`application/assembler/system/WalletJournalAssembler.java`。

## 1. 总览

人物钱包流水（wallet journal）提供两个面向单角色的用例：

- **同步**：`POST /wallet/journal/{cid}/sync` —— 从 ESI 拉取该人物的钱包流水并**幂等一落库**；
- **查询**：`GET /wallet/journal/{cid}` —— 按时间 `date` 倒序**物理分页**返回流水，供核对资金去向。

两个入口的**共同安全前置**：路径 `cid` 是用户可控入参，应用服务在进入业务逻辑前统一 `accessGuard.requireOwnership(cid, ...)` 校验该人物属于当前用户，防 IDOR。

```
求同步: POST /wallet/journal/{cid}/sync?forceRefresh=false
  -> WalletJournalApplicationService.syncCharacterJournal(cid, forceRefresh)
       -> accessGuard.requireOwnership(cid)               // 防 IDOR(无权 -> ACCESS_UNAUTHORIZED)
       -> requireSyncCooldown(cooldownKey)                // Redis 冷却限流(60s)
       -> WalletJournalService.syncCharacterJournal(cid, forceRefresh)
            -> authorize / authorizeInternal              // 归属校验(eve_account 属于当前用户/系统身份)
            -> getAccessToken(cId, userId)
            -> queryCharacterWalletJournalMaxPage(cId, token, forceRefresh)  // 总页数(含缓存判读,§2.3)
            -> Stream.iterate(1..maxPage) 串行分页 queryCharacterWalletJournal(cId, page, token, forceRefresh)
            -> saveOrUpdateBatch                          // 幂等 upsert(PK id)

查询: GET /wallet/journal/{cid}?current=&size=
  -> WalletJournalApplicationService.queryPage
       -> accessGuard.requireOwnership(cid)               // 防 IDOR
       -> WalletJournalRepository.selectPageByOwnerId(IPage, ownerId)  // 真实物理分页, order by `date` desc
       -> WalletJournalAssembler.toVo                     // 领域 -> 响应 VO
       -> PageResultUtil.copy(IPage)                      // total/页向量正确
```

## 2. 同步链路（幂等 upsert）

### 2.1 归属校验

`WalletJournalApplicationService.syncCharacterJournal(cid, forceRefresh)`：

1. `accessGuard.requireOwnership(String.valueOf(cid), "钱包流水同步")` —— 应用层归属校验（ROOT 豁免、未认证/无权 fail-closed 拒绝）。
2. `requireSyncCooldown(SYNC_COOLDOWN_PREFIX + "char:" + cid)` —— Redis 冷却限流（默认 60 秒，测试 profile 可设为 0 禁用）。
3. 进入 `walletJournalService.syncCharacterJournal(cid, forceRefresh)`；`ParseException` 包装为 `EveHelperException("钱包流水同步失败")`（不暴露内部异常给客户端）。

`WalletJournalService.syncCharacterJournal` 内再做一层授权：

```java
EveAccount eveAccount;
Integer currentUserId = UserUtil.getUserId();
if (currentUserId != null && currentUserId > 0) {
    eveAccount = authorizeUtil.authorize(cId);           // 请求上下文路径
} else {
    eveAccount = authorizeUtil.authorizeInternal(SYSTEM_USER_ID, cId);  // 定时任务/无上下文路径
}
String accessToken = esiApiService.getAccessToken(cId, eveAccount.getUserId());
```

- **双通道归属**：请求线程走 `AuthorizeUtil.authorize`（`getAccountOne(userId, cId)`，绑定于当前登录用户）；定时任务等无安全上下文路径走 `authorizeInternal`（显式声明 `SYSTEM_USER_ID` 操作身份）。二者均 **fail-closed**。
- `EsiGateway` 提供人物单分账端口：`queryCharacterWalletJournalMaxPage(characterId, accessToken)` 与 `queryCharacterWalletJournal(characterId, page, accessToken)`（`domain/port/esi/EsiGateway.java` §钱包流水），由 `infrastructure/external/esi/EsiApiService` 实现。与军团端口 `queryCorporationWalletJournal(..., division, ...)` 的区别：**人物单分账，无 division 参数，ownerId = characterId**。

### 2.2 串行分页拉取 + 幂等 upsert

```java
Integer maxPage = esiApiService.queryCharacterWalletJournalMaxPage(cId, accessToken, forceRefresh);
int pages = maxPage == null ? 0 : maxPage;  // null 防御:无数据/错误路径按 0 页处理
List<WalletJournal> walletJournals = Stream.iterate(1, i -> i + 1).limit(pages)
        .map(i -> esiApiService.queryCharacterWalletJournal(cId, i, accessToken, forceRefresh).collectList().block())
        .sequential().filter(Objects::nonNull).flatMap(Collection::stream)
        .collect(Collectors.toList());
// 回填 ownerId + division=0 + userId(同步者归属,014 US1)
walletJournals.forEach(j -> {
    j.setOwnerId(cId.longValue());
    j.setDivision(0);
    j.setUserId(syncUserId);
});
if (!walletJournals.isEmpty()) {
    walletJournalRepository.saveOrUpdateBatch(walletJournals);
}
```

- **串行分页**：从第 1 页 `block()` 逐一拉取到 `maxPage`，拼接成列表。
- **幂等落库**：`saveOrUpdateBatch`（`WalletJournalRepositoryImpl`）对每条记录走 `insert or update ... on duplicate key update`：

```java
// id 为 PRIMARY KEY,触发 ON DUPLICATE KEY UPDATE,重同步不产生重复行
walletJournals.forEach(wj -> walletJournalMapper.insertOrUpdateSelective(poConverter.domain2Po(wj)));
```

> ⚠️ **幂等依赖 `PRIMARY(id)`**：MyBatis 生成 SQL 的 `ON DUPLICATE KEY UPDATE` 只有在主键或唯一键冲突时才生效。`wallet_journal.id` 在实况库已是主键，幂等依赖之；009 特性规划中「加 `UNIQUE(id, owner_id)`」的候选方案已由 D2 裁决**废弃**（单角色场景 `id` 已全局唯一，无需复合唯一键）。改动表结构前须先确认 `id` 仍是主键。

- **失败不写脏数据**：`WalletJournalService` 类级 `@Transactional(rollbackFor = RuntimeException.class)`——若 ESI 中途异常（`block()` 抛错 / `ParseException`），事务回滚，不落半截流水。

### 2.3 ESI 响应缓存判读（015 特性新增）

`EsiApiService` 在调用 `WalletApi` 前插入缓存判读层，对已认证请求（`userId > 0`）生效：

```
EsiApiService.queryCharacterWalletJournal*(characterId, page, accessToken, forceRefresh)
  ├─ 未认证(userId null/<=0) → 直接真调 WalletApi(无缓存,安全优先)
  └─ 已认证:
       ├─ 构造 Redis 键: esiResponseCache.cacheKey(datasource, userId, "/characters/{cid}/wallet/journal/", page)
       ├─ forceRefresh=true 且通过限流(forceRefreshLimiter) → 跳过缓存读,直接进入真调
       ├─ 否则查 Redis 缓存:
       │    ├─ 命中 → 直接返回 Flux.fromIterable(cached),跳过 ESI 真调
       │    └─ 未命中 → 进入真调
       └─ 真调:
            ├─ 防穿透:doListWithClaim/doMaxPageWithClaim 持锁回填,未抢锁有界轮询复用
            ├─ 调 WalletApi.queryCharacterWalletJournalResponse(含响应头入口)
            ├─ 从 ResponseEntity 提取 Cache-Control/Expires → esiCacheTtlExtractor.extract(headers) → TTL Duration
            ├─ 转换 ESI 响应 → 领域对象
            └─ esiResponseCache.put(key, domain, ttl) 回填缓存(ttl null → 跳过 put)
```

要点：
- **`forceRefresh` 参数**：Controller 入口 `@RequestParam(defaultValue = "false")`，透传至 EsiApiService；`true` 时跳过缓存读（受 `forceRefreshLimiter` 限流保护，防滥用）
- **TTL 来源**：ESI 响应头 `Cache-Control: max-age=N` 或 `Expires`，由 `esiCacheTtlExtractor` 解析为 `Duration`；无响应头 → TTL null → 不缓存（下次仍真调）
- **WalletApi 含响应头入口**：`queryCharacterWalletJournalResponse` / `queryCharacterWalletJournalMaxPageResponse` 返回 `ResponseEntity`，供提取响应头；原 `queryCharacterWalletJournal` / `queryCharacterWalletJournalMaxPage` 保留（零破坏，供无需缓存路径使用）
- **缓存键归一化**：`datasource + userId + uri + page`，确保不同用户/页码/数据源隔离
- **未认证路径不缓存**：`userId == null` 时直接真调，避免缓存污染（定时任务等无上下文场景）

## 3. 查询链路（真实物理分页）

`WalletJournalApplicationService.queryPage(cid, current, size)`：

1. `accessGuard.requireOwnership(cid, "钱包流水")` 防 IDOR。
2. 构造 `new Page<>(current, size)`（MyBatis Plus 分页对象）传入 `walletJournalRepository.selectPageByOwnerId(page, Integer.valueOf(cid))`。
3. `WalletJournalRepositoryImpl` 调用 mapper，随后**复用原分页页向量**（保留 `size/current/total/pages` 等分页插件填充总数）：

```java
IPage<WalletJournalPO> poPage = walletJournalMapper.selectPageByOwnerId(page, ownerId);
List<WalletJournal> domains = walletJournalPoConverter.po2Domain(poPage.getRecords());
IPage<WalletJournal> result = new Page<>(poPage.getCurrent(), poPage.getSize(), poPage.getTotal());
result.setRecords(domains);
```

4. `WalletJournalAssembler.toVo` 映射为响应 VO，`PageResultUtil.copy(IPage<WalletJournal>, ...)` 转为 `PageResult`。

### 3.1 分页 SQL（保留字反引号）

`WalletJournalMapper.xml#selectPageByOwnerId`：

```sql
select
<include refid="Base_Column_List"/>
from wallet_journal
where owner_id = #{ownerId}
order by `date` desc   -- date 为 MySQL 保留字,须反引号
```

> ⚠️ **`date` 与 `character` 都是 MySQL 保留字**：`date`（排序、查询条件）与 `character`（列名）在 SQL 中一律反引号。`selectPageByOwnerId` 的 `order by` 显式加了反引号；`WalletJournalRepositoryImpl.selectMapByDatetime` 也用字符串列名 `` `character` ``/`` `date` `` 而非 lambda 方法引用，避免 MyBatis-Plus 生成无反引号的 `GROUP BY character` 触发语法错误。修改 SQL 时事无巨细检查保留字。

### 3.2 真实物理分页范式（勿照搬 assets）

- `selectPageByOwnerId` 走 **IPage + 分页插件**，`LIMIT ... OFFSET` 由插件生成，`total` 由 count 查询正确填充。
- 对比 `AssetsApplicationService.queryAssetsList`：手工 `setRecords(...)` 填列表字段，**没有走物理分页，`total` 恒为 0**。钱包流水是「真实 IPage 物理分页」的**参照实现**——需要正确 `total` 的同类端点请照此范式，不要复制 assets 的 `total=0` 缺陷。

## 4. 数据模型与出参（eve_helper 库）

`wallet_journal` 表非主键列：`amount`、`balance`、`context_id`、`context_id_type`、`date`、`description`、`first_party_id`、`reason`、`ref_type`、`second_party_id`、`tax`、`tax_receiver_id`、`character`、`owner_id`；`id` 为主键。

领域实体 `WalletJournal` 与响应 VO `WalletJournalVO` 的 `date` 均为 `java.time.OffsetDateTime`，**同名同型**，MapStruct（`WalletJournalAssembler`）直接映射无歧义。`WalletJournalVO` 出参字段：`id`、`amount`、`balance`、`date`、`refType`、`description`、`tax`、`ownerId`。

## 5. 安全与一致性要点

| 要点 | 说明 |
|------|------|
| 两端点均应用层 requireOwnership | `syncCharacterJournal` 与 `queryPage` 入口处 `accessGuard.requireOwnership(cid)`，防 IDOR；未认证 401 / 无权 403，fail-closed |
| Redis 冷却限流（同步） | `requireSyncCooldown`，默认 60 秒冷却（`wallet:journal:sync:char:{cid}`），防 ESI 限流滥用 |
| 双层授权（同步） | 应用层 `requireOwnership` + 领域层 `WalletJournalService` 内 `authorize/authorizeInternal` 归属校验（`eve_account` 记录属于当前用户/系统身份），EsiApiService 拉取前二者都通过 |
| ESI 响应缓存（015） | 已认证请求走 Redis 缓存判读，TTL 由 ESI 响应头决定；`forceRefresh=true` 跳过缓存读（受 `forceRefreshLimiter` 限流）；未认证路径不缓存（安全优先） |
| 幂等 upsert | `PRIMARY(id)` 触发 `ON DUPLICATE KEY UPDATE`，重复同步不产生重复行 |
| ESI 失败不写脏数据 | 类级 `@Transactional`，异常回滚 |
| 物理分页 total 正确 | 参照实现（§3.2），避免 assets 的 total=0 缺陷 |
| 保留字 | `date`/`character` 列须反引号 |
| 无上下文路径复用 | 定时任务等通过 `authorizeInternal(SYSTEM_USER_ID, ...)` 显式声明身份，勿被请求线程借用（运行时不变量） |

## 6. 关键代码文件索引

| 环节 | 文件 |
|------|------|
| 控制器（sync/query） | `interfaces/web/controller/WalletJournalController.java` |
| 应用服务 | `application/service/WalletJournalApplicationService.java` |
| 领域服务 | `domain/service/system/WalletJournalService.java`（`syncCharacterJournal`） |
| 仓储接口/实现 | `domain/repository/system/WalletJournalRepository.java`、`infrastructure/persistence/repository/system/WalletJournalRepositoryImpl.java` |
| 分页/批量 SQL | `src/main/resources/mappers/system/WalletJournalMapper.xml` |
| 组装器 | `application/assembler/system/WalletJournalAssembler.java` |
| 响应 VO | `application/dto/response/WalletJournalVO.java` |
| ESI 端口 | `domain/port/esi/EsiGateway.java`（`queryCharacterWalletJournal*`，含 `forceRefresh` 重载） |
| ESI API（含响应头入口） | `infrastructure/external/esi/api/WalletApi.java`（`queryCharacterWalletJournalResponse` / `queryCharacterWalletJournalMaxPageResponse`） |
| ESI 缓存适配 | `infrastructure/external/esi/EsiApiService.java`（`doQueryCharacterWalletJournal*` — 缓存判读 + TTL 回填） |
| 归属校验 | `application/security/AccessGuard.java`、`domain/util/AuthorizeUtil.java` |
| 页向量复制 | `shared/util/PageResultUtil.java` |
| 缓存网关 | `domain/port/cache/CacheGateway.java`（ESI 响应缓存 + 冷却限流底层） |
| 缓存 TTL 提取 | `infrastructure/external/esi/EsiCacheTtlExtractor.java`（解析 Cache-Control/Expires） |
| 强制刷新限流 | `infrastructure/external/esi/ForceRefreshLimiter.java`（`forceRefresh` 参数限流保护） |

---

**最后更新**: 2026-08-21（015 特性:添加 forceRefresh 参数与 ESI 响应缓存判读说明）