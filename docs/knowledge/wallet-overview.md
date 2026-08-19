# 钱包总览流程

> 面向开发者：理清「人物钱包总览」与「军团钱包总览（全 division / 单分账）」两个查询用例的聚合口径、时间过滤、趋势粒度、类目汇总、安全与 RBAC。
> 关联代码集中在 `interfaces/web/controller/WalletOverviewController.java`、`application/service/WalletOverviewApplicationService.java`、`domain/repository/system/WalletJournalRepository.java`、`src/main/resources/mappers/system/WalletJournalMapper.xml`（selectOverview* 五段）。

## 1. 总览

钱包总览基于已同步到 `wallet_journal` 表的数据（来自 010/011 同步链路）做**聚合读**，只读不触发 ESI 同步。它面向已落库的 journal，返回当前余额、收支合计、净流水、条数、统计截点，以及按交易类型（类目）与按时间桶（趋势）两个维度的汇总。

两个端点：

| 端点 | 人/军团 | 说明 |
|------|--------|------|
| `GET /wallet/overview/{cid}` | 人物 | 人物单主体总览，`divisions` **恒为 null**；`division` 传 null 不过滤 |
| `GET /wallet/overview/corp/{corpId}?division=N` | 军团 | `division` 空 = 全量（多返回分账分布 1..7，currentBalance=各分账最新余额和）；`division=1..7` = 单分账过滤，`divisions` 不填充 |

两个入口的**共同安全前置**：路径 `cid`/`corpId` 是用户可控入参，应用服务在业务逻辑前统一 `accessGuard.requireOwnership(...)` 校验归属，防 IDOR。

```
GET /wallet/overview/{cid}
  -> WalletOverviewApplicationService.getCharacterOverview(cid, range, start, end)
       -> 入参边界校验(isValidRange / range 与 start/end 二选一)   // 先于归属,防越权探测
       -> accessGuard.requireOwnership(cid, "钱包总览")             // 防 IDOR
       -> resolveRange(range,start,end)                             // 解析时间范围
       -> selectOverviewAggregate / Categories / Trend(division=null)
       -> granularity() 决定趋势桶粒度(日/月)
       -> 组装 WalletOverviewVO(divisions=null)

GET /wallet/overview/corp/{corpId}?division=N
  -> WalletOverviewApplicationService.getCorporationOverview(corpId, division, range, start, end)
       -> 入参校验(division null/1..7, isValidRange)                // 先于归属
       -> accessGuard.requireOwnership(corpId, "军团钱包总览")       // 防 IDOR
       -> resolveRange -> selectOverview{...}
       -> division==null 时额外:
            selectOverviewDivisionBalances / DivisionFlow -> mergeCorpDivisions 归并 1..7 补零
            cb = 各分账最新余额之和(覆盖聚合值)
       -> 组装 WalletOverviewVO(divisions=divs 或 null)
```

## 2. 聚合口径（关键语义）

| 项 | 口径 | 说明 |
|----|------|------|
| **currentBalance** | 最新一行（`order by id desc limit 1`）的 `balance` | 取最新 **id**（插入序）而非墙钟时间；人物/单分账取该主体最新行；军团全量 = **各分账最新余额之和**（服务层覆盖聚合值） |
| **totalIncome** | `SUM(CASE WHEN amount > 0 THEN amount)` | 正金额累加 |
| **totalExpense** | `SUM(CASE WHEN amount < 0 THEN -amount)` | 负金额取绝对值累加（恒为正数表示支出额） |
| **netFlow** | `SUM(amount)` | 收支净流（可正可负） |
| **journalCount** | `COUNT(*)` | 区间内流水条数 |
| **asOfTime** | 最新 `date`（`MAX(w3.date)`，**不受时间过滤影响**，全量最新） | 数据截止截点 |

> ⚠️ **关键裁决**：**currentBalance 与 asOfTime 不受时间过滤影响**。`range`/`start`/`end` 只作用于收支合计、净流、条数、类目、趋势；余额与截点始终按全量最新取。这是刻意设计——余额是时点值，时间过滤会令其失真。

## 3. 时间范围参数

应用服务 `resolveRange`：

- **`range` 预设**（与 start/end **二选一**，同时给则直接 `PARAM_ERROR` 拒绝）：`today` / `last7d` / `last30d` / `last90d` / `year`
  - `today`：UTC 今日 00:00 → now
  - `lastNd`/`year`：now 减去 N 天 → now
- **`start`/`end`**（ISO 日期，`@DateTimeFormat(iso=DATE)`）：显式区间，`end.before(start)` 拒绝
- **都为空** → 全量（`null,null`，不做时间过滤）

## 4. 趋势粒度（日/月规则）

`granularity(s, e)`：

- 区间**有界**（start 与 end 均非空）且跨度 `≤ 92 天` → **日桶** `%Y-%m-%d`
- 否则（全量、或跨度 > 92 天）→ **月桶** `%Y-%m`

趋势 SQL：`DATE_FORMAT(date, granularity)` 按桶 `group by`，桶**升序**返回，各桶含 income / expense / net。

## 5. 类目汇总

按 `ref_type` 分组，income/expense 正负分别累、count，按 `(income + expense)` 降序排序，**LIMIT 50**（最多返回 50 类）。

## 6. 军团全量分账分布

`division==null` 时返回 `divisions`（`WalletOverviewVO.DivisionSummary` 列表），1..7 补零有序：

- `selectOverviewDivisionBalances`：每个 division 最新 id 行的 `balance`（自关联 `MAX(id)` group by owner_id, division）
- `selectOverviewDivisionFlow`：按 division 汇总区间收支
- `mergeCorpDivisions`：余额按 division、收支按 division 归并，1..7 各补零，形成有序列表；**currentBalance = 各分账 balance 之和**（覆盖聚合标量值）

## 7. 数据模型与出参

聚合/类目/趋势/分账分布分别落 `WalletOverviewAggregatePO` / `CategoryPO` / `TrendPO` / `DivisionPO`，经领域 VO `WalletOverviewVO`（含 `DivisionSummary`/`CategorySummary`/`TrendPoint`）出参。`date` 为 MySQL 保留字，SQL 中全部反引号。

## 8. 安全与设计要点

| 要点 | 说明 |
|------|------|
| 两端点均应用层 requireOwnership | 人物 `cid`、军团 `corpId` 入口均 `accessGuard.requireOwnership`，防 IDOR；未认证 401 / 无权 403，fail-closed |
| 入参校验先于归属校验 | 非法 `range` / `start>end` / 非法 `division` 在 `requireOwnership` **之前**拒绝，防越权探测（避免无权者借参数错误列出数据存在性） |
| division 校验 | 人物：无 division 概念；军团：`null` 或 `1..7`，`<1 || >7` 拒绝 |
| 只读聚合 | 总览端点为纯查询，不触发 ESI 同步、无冷却限流（同步限流在 010/011 同步端点） |
| 有意偏离（勿当 bug 修） | currentBalance/asOfTime 不受时间过滤影响；人物总览 division=null 不过滤（存量人物 journal division 为 NULL/1，写死 0 会造成存量全零）；人物 divisions 恒 null |

## 9. RBAC 权限

登记于 `src/SQL/convert/012_rbac_permissions.sql`（幂等，事务内先删后插，绑定 ADMIN）：

| url_perm（`METHOD:PATH`，与 `RbacAuthorizationManager.restfulPath` 精确匹配） | 端点 | 权限说明 |
|------|------|----------|
| `GET:/wallet/overview/{cid}` | 人物钱包总览 | ADMIN |
| `GET:/wallet/overview/corp/{corpId}` | 军团钱包总览 | ADMIN |

## 10. 常见 FAQ

- **为何 balance 不是同步墙钟？** `currentBalance = order by id desc limit 1` 取最新 **id**（插入序）。同一时刻多次同步、或 ESI 返回时间拥挤时，插入序与 `date` 不一定同序；以 id 取最新落库行保证「当前库内最新状态」，与 asOfTime（最新 date）分开表达。若要墙钟口径需改用 `order by date desc`，会影响「统计至何时」语义，改动前评估。
- **为何军团全量 currentBalance 是各分账之和？** 军团 7 个分账钱包余额分别演进，一个主体没有单一余额，故以各分账最新余额求和表达「军团钱包整体现值」；单分账则直接取该分账最新行。
- **为何传入 range 又传 start/end 会报错？** 二选一硬性约束，避免语义歧义；都要时间过滤时请只用其中一种。
- **为何人物总览 divisions 恒为 null？** 人物单主体无分账维度；只有军团全量才有分账分布。
- **为何时间过滤不影响余额/截点？** 余额是时点值、asOfTime 是数据截止截点，过滤会失真（见 §2/§7 有意偏离）。

## 11. 关键代码文件索引

| 环节 | 文件 |
|------|------|
| 控制器（人物/军团两端点） | `interfaces/web/controller/WalletOverviewController.java` |
| 应用服务（聚合/解析/归并） | `application/service/WalletOverviewApplicationService.java` |
| 领域读模型 / 出参 | `domain/model/vo/WalletOverviewVO.java`、`domain/model/vo/WalletOverviewAggregate.java` |
| 仓储接口 | `domain/repository/system/WalletJournalRepository.java`（selectOverview*） |
| 聚合/类目/趋势/分账 SQL | `src/main/resources/mappers/system/WalletJournalMapper.xml`（selectOverviewAggregate/Categories/Trend/DivisionBalances/DivisionFlow） |
| 持久化 PO | `infrastructure/persistence/entity/system/WalletOverview*PO.java` |
| 归属校验 | `application/security/AccessGuard.java`、`domain/util/AuthorizeUtil.java` |
| RBAC 权限登记 | `src/SQL/convert/012_rbac_permissions.sql` |
| 数据来源同步链路 | 参见 [wallet-journal-character.md](./wallet-journal-character.md)、[corporation-wallet-journal.md](./corporation-wallet-journal.md) |