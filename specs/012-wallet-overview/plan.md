# Plan: 012 — 钱包总览（Wallet Overview）

**Spec**: [spec.md](spec.md) (G1 已批准)
**Status**: G2 待批准
**Date**: 2026-08-19

---

## 关键口径（设计决策）

| 口径 | 决策 | 依据 |
|------|------|------|
| 当前余额 | 某主体（人物 / 军团某分账）`ORDER BY id DESC LIMIT 1` 的 `balance`（不含时间过滤，恒为最新） | journal id = ESI 流水序列号，随钱包单调递增 |
| 军团全量总额 | 各分账最新余额之和 | EVE 军团各分账为独立钱包 |
| 收支 | `amount>0` 收入、`amount<0` 支出(取绝对)、`SUM(amount)` 净额 | spec 口径 |
| asOfTime | `MAX(date)`（不含时间过滤，恒为最新流水时间） | `wallet_journal` **无** gmt_create/gmt_modified 列（PO 不继承 BaseEntity），无墙钟同步时间可依；以最新流水时间为"数据截止时间"唯一可信口径 |
| 时间过滤 | 仅作用于收入/支出/净额/类目/趋势/分账流水；**不**作用于当前余额与 lastSyncTime | 余额是账户现值，与展示区间无关 |
| 趋势粒度 | `end-start ≤ 92 天 → 日(%Y-%m-%d)`，否则 → 月(%Y-%m)；升序 | spec 时间粒度规则 |
| 类目条数 | 按 `income+expense` 降序，LIMIT 50 | 防响应过大（NFR） |
| 主体/分账校验 | 人物 division 传 **null（不过滤）**，兼容存量人物 journal division 为 NULL/1（011 迁移把历史 NULL→1，绝不可 `division=0` 过滤）；军团 division 1..7 或 null(全量)；null → 全分账+分账分布 | spec+存量数据 |

> **不改动**：ESI 层、同步逻辑、现有分页查询、数据库表结构（无 DDL 变更——本特性只"读"已有流水）。

---

## D1: 领域读模型 VO

**文件**: `src/main/java/xyz/foolcat/eve/evehelper/domain/model/vo/WalletOverviewVO.java`
**文件**: `src/main/java/xyz/foolcat/eve/evehelper/domain/model/vo/WalletOverviewAggregate.java`

```java
// WalletOverviewVO —— 总览聚合读模型(被仓储→应用服务→控制器多层消费,故置于 domain/model/vo)
public record WalletOverviewVO(
        Double currentBalance,
        Double totalIncome,
        Double totalExpense,
        Double netFlow,
        Long journalCount,
        java.time.OffsetDateTime asOfTime,
        List<CategorySummary> categories,
        List<TrendPoint> trend,
        List<DivisionSummary> divisions) {

    public record CategorySummary(String refType, Double income, Double expense, Long count) {}
    public record TrendPoint(String bucket, Double income, Double expense, Double net) {}
    public record DivisionSummary(Integer division, Double balance, Double income, Double expense) {}
}

// WalletOverviewAggregate —— 单主体摘要载体(仓储聚合返回)
public record WalletOverviewAggregate(
        Double currentBalance, Double totalIncome, Double totalExpense,
        Double netFlow, Long journalCount, java.time.OffsetDateTime asOfTime) {}
```

调用约定:人物 `divisions = null`;军团全量 `divisions` = 1..7(每分账 balance/income/expense);军团单分账 `divisions = null`。零值规则:`currentBalance` 无数据 → 0;各类表无数据 → 空 list（前端自行显示空态）。

---

## D2: 仓储层聚合查询

**接口** `WalletJournalRepository` 新增 5 个方法（返回类型只用领域类型，见 D1）：

```java
// 摘要(含不受时间过滤的 currentBalance 与 asOfTime)
WalletOverviewAggregate selectOverviewAggregate(Long ownerId, Integer division, Date start, Date end);
// 类别分布
List<WalletOverviewVO.CategorySummary> selectOverviewCategories(Long ownerId, Integer division, Date start, Date end);
// 时间趋势(granularity: "%Y-%m-%d" 或 "%Y-%m")
List<WalletOverviewVO.TrendPoint> selectOverviewTrend(Long ownerId, Integer division, Date start, Date end, String granularity);
// 分账最新余额(军团全量专用)
List<WalletOverviewVO.DivisionSummary> selectOverviewDivisionBalances(Long ownerId);
// 分账区间收支(军团全量专用)
List<WalletOverviewVO.DivisionSummary> selectOverviewDivisionFlow(Long ownerId, Date start, Date end);
```

**实现** `WalletJournalRepositoryImpl`:实现上述 5 个方法，把 mapper 返回值（`WalletOverview*PO`）映射为 D1 领域类型。分账收支/余额结果按 `division` 收集为 `Map<Integer, ...>` 供应用服务按 1..7 补齐零值。

**Mapper** `WalletJournalMapper` + `WalletJournalMapper.xml` 新增 5 个 SQL（`date` 为 MySQL 保留字须反引号；金额 `COALESCE(...,0)` 防空聚合返回 null）。伪代码（省略动态 `division`/`date` 的 `<if>`）：

1. **selectOverviewAggregate** — 单条标量子查询合并摘要+最新余额+最新时间：
```sql
SELECT
  COALESCE(SUM(CASE WHEN amount>0 THEN amount ELSE 0 END),0) AS totalIncome,
  COALESCE(SUM(CASE WHEN amount<0 THEN -amount ELSE 0 END),0) AS totalExpense,
  COALESCE(SUM(amount),0) AS netFlow,
  COUNT(*) AS journalCount,
  (SELECT balance FROM wallet_journal w2
     WHERE w2.owner_id=#{ownerId} [AND w2.division=#{division}]
     ORDER BY w2.id DESC LIMIT 1) AS currentBalance,
  (SELECT MAX(w3.`date`) FROM wallet_journal w3
     WHERE w3.owner_id=#{ownerId} [AND w3.division=#{division}]) AS asOfTime
FROM wallet_journal w
WHERE w.owner_id=#{ownerId} [AND w.division=#{division}] [AND w.`date` BETWEEN #{start} AND #{end}]
```
2. **selectOverviewCategories** — `GROUP BY ref_type`，`ORDER BY (income+expense) DESC LIMIT 50`。
3. **selectOverviewTrend** — `DATE_FORMAT(\`date\`, #{granularity}) AS bucket`，`GROUP BY bucket ORDER BY bucket ASC`。
4. **selectOverviewDivisionBalances** — 群组分账最新余额（用 MAX(id) 自关联）：
```sql
SELECT w.division AS division, w.balance AS balance
FROM wallet_journal w
JOIN (SELECT owner_id, division, MAX(id) AS mid FROM wallet_journal
      WHERE owner_id=#{ownerId} GROUP BY owner_id, division) m
  ON w.owner_id=m.owner_id AND w.division=m.division AND w.id=m.mid
```
5. **selectOverviewDivisionFlow** — `GROUP BY division` 的 income/expense（区间过滤）。

**Infrastructure 结果载体**（`infrastructure/persistence/entity/system/` 下新建 4 个记录，规避 mapper 直接返回 domain 类型的分层气味）：
- `WalletOverviewAggregatePO`(totalIncome,totalExpense,netFlow,journalCount,currentBalance,asOfTime)
- `WalletOverviewCategoryPO`(refType,income,expense,count)
- `WalletOverviewTrendPO`(bucket,income,expense,net)
- `WalletOverviewDivisionPO`(division,balance,income,expense)

仓储实现内 `po→domain` 手工转换（金额/条数 null 归一为 0）。

---

## D3: 应用服务

**文件**: `src/main/java/xyz/foolcat/eve/evehelper/application/service/WalletOverviewApplicationService.java`

```java
WalletOverviewVO getCharacterOverview(Integer cid, String range, Date start, Date end);
WalletOverviewVO getCorporationOverview(Integer corpId, Integer division, String range, Date start, Date end);
```

**getCharacterOverview(`cid`, `range`, `start`, `end`)**：
1. 入参边界（先于归属校验，防越权探测）：`cid` 非空；时间范围解析——`range` 与 `start/end` 二选一，同时给出或 `start>end` 或非法 `range` → 抛 PARAM_ERROR。
2. `accessGuard.requireOwnership(String.valueOf(cid), "钱包总览")`。
3. 调用仓储（人物 **division 传 null，不过滤**）：`selectOverviewAggregate(cid, null, s, e)`、`selectOverviewCategories(cid, null, s, e)`、`selectOverviewTrend(cid, null, s, e, granularity(...))`。
4. 组装 `WalletOverviewVO`（`divisions = null`），零值由 SQL COALESCE 保证。

**getCorporationOverview(`corpId`, `division`, `range`, `start`, `end`)**：
1. 入参边界：`corpId` 非空；`division` 为 null 或 1..7（越界抛 PARAM_ERROR）；时间范围同人物。
2. `requireOwnership(String.valueOf(corpId), "军团钱包总览")`。
3. 单分账（`division != null`）：与人物同构，仅过滤 `division`，`divisions=null`。
4. 全量（`division == null`）：摘要 + 类目 + 趋势（均无 division 过滤）+ `selectOverviewDivisionBalances` / `selectOverviewDivisionFlow` → 按 1..7 补齐零值成 `List<DivisionSummary>`；`currentBalance` = 各分账余额之和。

**时间范围解析**（`resolveRange(range, start, end)` 私有，返回封装 `(Date start, Date end)`）：
- `range` 预设：`today`=`[今日0点, 明日0点)`；`last7d`=`[now-7d, now]`；`last30d`/`last90d` 同理；`year`=`[今年1月1日, now]`。
- 两者都传 → PARAM_ERROR；都不传 → 全量（start/end 均为 null）。
- 返回后校验 `start != null && end != null && end.before(start)` → PARAM_ERROR。

**趋势粒度** `granularity(Date s, Date e)`：全量（s,e 均 null）→ 默认**月**(`%Y-%m`)；有界区间 → `e-s ≤ 92 天` 日(`%Y-%m-%d`)，否则月。

---

## D4: 控制器

**文件**: `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/WalletOverviewController.java`

```java
@Tag(name = "人物/军团钱包总览")
@RestController
@RequestMapping("/wallet/overview")
public class WalletOverviewController {
    // GET /wallet/overview/{cid}?range=&start=&end=
    public Result<WalletOverviewVO> characterOverview(@PathVariable Integer cid,
        @RequestParam(required=false) String range,
        @RequestParam(required=false) @DateTimeFormat(iso=ISO.DATE) Date start,
        @RequestParam(required=false) @DateTimeFormat(iso=ISO.DATE) Date end)
    // GET /wallet/overview/corp/{corpId}?division=&range=&start=&end=
    public Result<WalletOverviewVO> corporationOverview(@PathVariable Integer corpId,
        @RequestParam(required=false) Integer division, ...)
}
```
沿用现有 `Result<T>` 封装、Swagger `@Operation/@Parameter` 中文注释。入参 Date 用 `@DateTimeFormat(iso=ISO.DATE)`（`yyyy-MM-dd`），空范围全量。

---

## D5: RBAC 权限登记

**文件**: `src/SQL/convert/012_rbac_permissions.sql`

仿 010/011 幂等脚本（先 DELETE 后 INSERT + 绑定 ADMIN）登记 2 条权限：
- `GET:/wallet/overview/{cid}`（人物钱包总览）
- `GET:/wallet/overview/corp/{corpId}`（军团钱包总览）

> 注:仓库现惯例仅绑定 ADMIN(ROOT 豁免归属校验)。钱包为个人数据、归属校验在应用服务内强约束;若后续需普通角色自助访问,仅需扩展 `sys_role_permission` 绑定,不在本特性范围。实现阶段以现有 wallet 端点绑定方式为准核对。

---

## D6: 测试策略

**单元(Mockito)**:应用服务——入参校验(非法 range / start>end / division 越界)、归属调用 `requireOwnership` 触发、range 预设解析、趋势粒度选择、军团全量分账 1..7 补零与金额求和、空数据零值组装。

**集成(Spring Boot + test profile,需 MySQL)**:人物总览正确性(余额/收支/类目/趋势)、军团全量含分账分布、军团单分账、时间范围过滤、空数据返回零、非法入参 400、越权 401/403。

**覆盖率**:新增代码 ≥80%(`ecc:test-coverage`)。

---

## 涉及文件清单

| 变更 | 文件 |
|------|------|
| 新增 | `domain/model/vo/WalletOverviewVO.java`、`WalletOverviewAggregate.java` |
| 新增 | `infrastructure/persistence/entity/system/WalletOverview{Aggregate,Category,Trend,Division}PO.java`(4) |
| 改 | `WalletJournalRepository.java`(+5)、`WalletJournalRepositoryImpl.java`(+5)、`WalletJournalMapper.java`(+5)、`WalletJournalMapper.xml`(+5 SQL) |
| 新增 | `application/service/WalletOverviewApplicationService.java` |
| 新增 | `interfaces/web/controller/WalletOverviewController.java` |
| 新增 | `src/SQL/convert/012_rbac_permissions.sql` |
| 测试 | `WalletOverviewApplicationServiceTest`(单)、`WalletOverviewControllerIT`(集成) |

## 不改动

- ESI 层、同步逻辑(角色/军团 journal 同步)、现有分页查询、数据库表结构。
- `WalletJournalVO`/`WalletTransactionVO` 及其组装器。
---

## Polish 检查点（T024：仅标记，交由协调方评审）

- **P5 验证报告**：待协调方执行（`./mvnw test` 全量绿 + `ecc:verification-loop` + `ecc:security-scan`），本任务未执行。
- **P6 评审标记**：待协调方派遣 `ecc:java-reviewer` / `ecc:security-reviewer` 执行 G6 门禁，本任务未执行。
- 本任务 T021（RBAC SQL）、T022（全量测试+覆盖率记录）、T023（知识文档）已完成；P5/P6 交回协调方继续。
