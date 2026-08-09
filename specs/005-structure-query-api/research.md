# Phase 0 Research: 建筑表只读查询接口

> spec 无 NEEDS CLARIFICATION 项。本研究聚焦 7 个技术决策点,全部基于现有代码模式与宪法约束得出。

## R-01: services 字段反序列化容错

- **Decision**: 使用 Hutool `JSONUtil.toBeanList(json, StructuresService.class)` + try-catch,解析失败或字段为 null 时返回空列表。
- **Rationale**: FR-014 要求对损坏服务数据容错返回空列表而非报错;ESI 同步链(`EsiStructureConverter`)已用 `JSONUtil.toJsonStr` 序列化 `List<StructuresService>`,反序列化对称;Hutool 属冻结技术栈,领域层已普遍使用。
- **Alternatives considered**:
  - Jackson(项目未在领域层引入,跨层成本高)
  - 手工字符串解析(脆弱,违反 KISS)

## R-02: 跨库 JOIN 名称解析

- **Decision**: 列表查询 SQL 跨库 LEFT JOIN `eve.inv_types`(类型名,`it.type_name`)+ `eve.universe_name`(星系名,`category=solar_system`);星系名缺失时降级返回 `system_id`。
- **Rationale**: SC-002 要求单 SQL 一次性解析名称,禁止逐条回查(N+1);Blueprints 已验证 `inv_types` JOIN 模式;数据库创建脚本确认无 `inv_solar_systems` 表,星系名来源于 `universe_name`(category=solar_system);Assumption 3 允许星系名缺失降级显示标识,故用 LEFT JOIN。
- **Alternatives considered**:
  - 应用层批量二次查询名称(多一轮 DB 往返,违反 SC-002)
  - 只解析类型名不解析星系名(用户体验不足)

## R-03: 燃料预警时区处理

- **Decision**: 沿用现有 `selectFuelExpiresList` 的 `now()+8` 时区模式;新增 `selectFuelExpiresListWithNames`(带类型名/星系名/剩余时长),不动现有方法。
- **Rationale**: Assumption 6 国服 UTC+8;现有 SQL 已用 `fuel_expires < date_add(now()+8,interval #{hour}-8 hour) or fuel_expires is null`;Assumption 8 要求现有方法不变(定时任务 `StructTask` 依赖 `selectFuelExpiresList`),故新增带名称版本供查询接口专用。
- **Alternatives considered**:
  - 应用层用 Java 时间 API 计算(与 DB 时区不一致风险)
  - 改造现有 `selectFuelExpiresList`(破坏定时任务同步链)

## R-04: 排序白名单防注入

- **Decision**: `StructurePageCriteria.SortField` 枚举,列名硬编码(如 `FUEL_EXPIRES("s.fuel_expires")`、`NAME("s.name")`、`STATE("s.state")`、`STRUCTURE_ID("s.structure_id")`);应用层校验 `sortField` 命中白名单后传入,SQL 用 `${sortColumn}` 拼接(白名单保证安全)。
- **Rationale**: FR-015 要求排序白名单防注入;Blueprints 已验证此模式(`SORT_FIELDS` map + 白名单 + `<choose>` asc/desc);SC-006 要求通过安全评审无注入风险。
- **Alternatives considered**:
  - MyBatis Plus `OrderItem`(仍需额外白名单校验,多一层)
  - 黑名单过滤(不安全,易绕过)

## R-05: 统计概览聚合

- **Decision**: 单 SQL 条件聚合 `SELECT count(*) total, sum(IF(state=?,1,0)) ...` 一次返回总数/各状态计数/即将缺油数,应用层组装 `StructureSummaryDTO`。
- **Rationale**: SC-003 要求聚合方式一次获取,禁止拉全量逐条计算;单次往返满足 <200ms。
- **Alternatives considered**:
  - 拉全量应用层统计(违反 SC-003,数据量大时超时)
  - 多次 COUNT 查询(多往返)

## R-06: IDOR 归属校验

- **Decision**: 复用 `AccessGuard.requireOwnership(corpId, "建筑")`;`ResourceOwnershipPolicy.isOwnedBy` 已支持 corpId 校验(匹配 `account.getCorpId()`)。单建筑查询先校验 corpId 归属,再校验建筑归属于该 corp(`structure.corporationId == corpId`)。
- **Rationale**: FR-010/011 要求军团所有权与单建筑归属校验;建筑归属军团(`corporationId`),非角色;Blueprints 已用 `requireOwnership` 模式;`ResourceOwnershipPolicy` 已支持 corpId,无需扩展。
- **Alternatives considered**:
  - 仅校验 characterId(建筑归属 corp,角色不直接持有,会误拒)
  - 自建归属校验(重复造轮,绕过统一安全策略)

## R-07: 现有方法零破坏

- **Decision**: `StructureRepository` 仅追加只读方法(`selectStructuresWithNames`、`selectDetailById`、`selectFuelExpiresListWithNames`、`selectServicesById`、`selectSummary`、`selectTimers`),不改现有 `updateBatch`/`batchInsert`/`insertOrUpdate`/`selectFuelExpiresList`/`selectByStructureId`/`selectByCorporationId` 等。
- **Rationale**: Assumption 8;定时任务 `StructTask` 与领域服务 `StructureService.batchInsertOrUpdateFromEsi` 依赖现有写/查方法。
- **Alternatives considered**: 改造现有方法签名(破坏调用方,违反 YAGNI/最小变更)。
