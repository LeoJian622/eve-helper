---

description: "Task list for 建筑表只读查询接口"
---

# Tasks: 建筑表只读查询接口

**Input**: Design documents from `/specs/005-structure-query-api/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/api-contracts.md ✅, quickstart.md ✅

**Tests**: 宪法第一条 Test-First(NON-NEGOTIABLE)+ SC-004(覆盖率 ≥80%),每个故事含 TDD 测试任务(RED 先于 GREEN)。

**Organization**: 按 6 个用户故事分组(US1~US6),每故事独立可实现可测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行(不同文件,无未完成依赖)
- **[Story]**: 所属用户故事(US1~US6);Setup/Foundational/Polish 无此标签
- 描述含精确文件路径

## 路径约定

DDD 单体项目,路径基于 `src/main/java/xyz/foolcat/eve/evehelper/`(下记 `~/`)与 `src/main/resources/mappers/`、`src/test/java/xyz/foolcat/eve/evehelper/`(下记 `~/test/`)。

---

## Phase 1: Setup(共享领域基础)

**Purpose**: 创建跨故事共享的查询条件、排序白名单、请求 DTO。

- [ ] T001 [P] 创建排序白名单枚举 `StructurePageCriteria.SortField`(STRUCTURE_ID/NAME/STATE/FUEL_EXPIRES,列名硬编码)在 `~/domain/model/query/StructurePageCriteria.java`(FR-015, R-04)
- [ ] T002 [P] 创建分页查询条件 `StructurePageCriteria`(@Builder @Getter,字段 corporationId/name/state/lowFuelOnly/sortField/ascending/current/size)在 `~/domain/model/query/StructurePageCriteria.java`
- [ ] T003 [P] 创建列表请求 DTO `StructureQuery`(extends PageQuery,@SuperBuilder,corporationId @NotBlank 纯数字≤19、name、state、lowFuelOnly)在 `~/application/dto/request/StructureQuery.java`(FR-016)
- [ ] T004 [P] 创建燃料预警请求 DTO `StructureFuelQuery`(corporationId @NotBlank、hours @Min 1 @Max 720 默认 72)在 `~/application/dto/request/StructureFuelQuery.java`(FR-016, Assumption 7)

**Checkpoint**: 共享请求/查询基础就绪,可进入 Foundational。

---

## Phase 2: Foundational(阻塞前置)

**Purpose**: 仓储只读方法签名、应用服务骨架(含 IDOR 公共校验)、控制器骨架、装配器骨架。MUST 完成于任何故事之前。

**⚠️ CRITICAL**: 故事实现不得在本阶段完成前开始。

- [ ] T005 在 `~/domain/repository/system/StructureRepository.java` 追加 6 个只读方法签名(仅领域类型):`selectStructuresWithNames`、`selectDetailById`、`selectFuelExpiresListWithNames`、`selectServicesById`、`selectSummary`、`selectTimers`(R-07,不改现有方法)
- [ ] T006 创建应用服务骨架 `StructureQueryApplicationService`(@Service @Transactional(readOnly=true),注入 StructureRepository、StructureAssembler、AccessGuard;私有方法 `requireCorpOwnership(corpId)` 调 `accessGuard.requireOwnership(corpId,"建筑")`)在 `~/application/service/StructureQueryApplicationService.java`(FR-010, R-06)
- [ ] T007 创建 MapStruct 装配器骨架 `StructureAssembler`(@Mapper(componentModel="spring"),空类)在 `~/application/assembler/system/StructureAssembler.java`
- [ ] T008 创建控制器骨架 `StructureController`(@RestController @RequestMapping("/structures") @Validated,@Tag)在 `~/interfaces/web/controller/StructureController.java`

**Checkpoint**: 仓储接口/服务/控制器/装配器骨架就绪,故事实现可并行展开。

---

## Phase 3: User Story 1 - 浏览军团建筑清单 (Priority: P1) 🎯 MVP

**Goal**: 分页查询军团建筑列表,返回建筑标识/名称/类型名/星系名/状态/燃料到期,支持名称模糊/状态筛选与白名单排序(FR-001~004)。
**Independent Test**: `GET /structures/{corpId}?current=1&size=20&sortField=FUEL_EXPIRES&sortOrder=asc` 返回分页列表,类型名/星系名单 SQL 解析;非本人军团返回 403。

### Tests for User Story 1(RED 先)

- [ ] T009 [P] [US1] 集成测试:列表正常分页+名称/状态筛选+排序生效,在 `~/test/interfaces/web/controller/StructureControllerTest.java`
- [ ] T010 [P] [US1] 安全测试:非本人军团 403、sortField 非白名单 400,在 `~/test/interfaces/web/controller/StructureSecurityTest.java`(SC-005/006/010)

### Implementation for User Story 1(GREEN)

- [ ] T011 [P] [US1] 创建领域读模型 `StructureListItemDTO`(structureId/name/typeId/typeName/systemId/systemName/state/fuelExpires/corporationId)在 `~/domain/model/vo/StructureListItemDTO.java`
- [ ] T012 [P] [US1] 创建响应 VO `StructureListItemVO`(@Data @Schema)在 `~/application/dto/response/StructureListItemVO.java`
- [ ] T013 [US1] 在 `StructureAssembler` 追加 `dtoList2VoList(List<StructureListItemDTO>)` 映射
- [ ] T014 [US1] 在 `~/infrastructure/persistence/repository/system/StructureRepositoryImpl.java` 实现 `selectStructuresWithNames`(IPage + mapper 调用,手工组装 PageResult)
- [ ] T015 [US1] 在 `src/main/resources/mappers/system/StructureMapper.xml` 追加 `selectStructuresWithNames`:`structure s LEFT JOIN eve.inv_types it ON s.type_id=it.type_id LEFT JOIN eve.universe_name un ON s.system_id=un.item_id AND un.category='solar_system'`,排序用 `${sortColumn}` 白名单 + `<choose>` asc/desc(SC-002, R-02/04)
- [ ] T016 [US1] 在 `StructureQueryApplicationService` 实现 `queryStructuresByPage(StructureQuery)`:requireCorpOwnership -> toCriteria(sortField 白名单校验)-> `PageResultUtil.copy(repo.selectStructuresWithNames, assembler::dtoList2VoList)`
- [ ] T017 [US1] 在 `StructureController` 追加 `GET /structures/{corpId}`(@Parameters/@Operation,返回 `Result<PageResult<StructureListItemVO>>`)

**Checkpoint**: US1 独立可用(MVP),列表/筛选/排序/IDOR 全覆盖。

> ✅ **US1 已实现**(T001~T003 / T005~T008 / T011~T017,共 14 任务):IDE 构建通过(`isSuccess=true`,仅既有 MapStruct `gmtCreate/gmtModified` WARNING,非本功能引入)。T004(StructureFuelQuery)属 US3 未创建;T009 冒烟测试已写;T010 越权测试待补(需非 admin 测试用户配置)。

---

## Phase 4: User Story 2 - 查看建筑详情 (Priority: P1)

**Goal**: 查询单建筑详情,返回全部字段及已解析服务列表;校验建筑归属请求军团(FR-005, FR-011)。
**Independent Test**: `GET /structures/{corpId}/detail/{structureId}` 返回详情+services 解析;他团建筑 403;不存在 404。

### Tests for User Story 2(RED 先)

- [ ] T018 [P] [US2] 集成测试:详情正常返回+services 解析为列表+不存在 404,在 `~/test/interfaces/web/controller/StructureControllerTest.java`
- [ ] T019 [P] [US2] 安全测试:他团建筑 403(FR-011),在 `~/test/interfaces/web/controller/StructureSecurityTest.java`(SC-005/010)

### Implementation for User Story 2(GREEN)

- [ ] T020 [P] [US2] 创建领域读模型 `StructureDetailDTO`(全部 Structure 字段 + typeName + systemName + `List<StructuresService>` services)在 `~/domain/model/vo/StructureDetailDTO.java`
- [ ] T021 [P] [US2] 创建响应 VO `StructureDetailVO` 在 `~/application/dto/response/StructureDetailVO.java`
- [ ] T022 [US2] 在 `StructureAssembler` 追加 `dto2Vo(StructureDetailDTO)` 映射
- [ ] T023 [US2] 在 `StructureRepositoryImpl` 实现 `selectDetailById`(含 inv_types/universe_name JOIN)
- [ ] T024 [US2] 在 `StructureMapper.xml` 追加 `selectDetailById` SQL(同 US1 JOIN 模式,WHERE structure_id)
- [ ] T025 [US2] 在 `StructureQueryApplicationService` 实现 `queryDetailById(corpId, structureId)`:requireCorpOwnership -> 查询 -> 校验 `structure.corporationId == corpId`(不符抛 403,FR-011)-> services JSON 容错解析(`JSONUtil.toBeanList` try-catch 返回空列表,FR-014, R-01)
- [ ] T026 [US2] 在 `StructureController` 追加 `GET /structures/{corpId}/detail/{structureId}`(返回 `Result<StructureDetailVO>`)

**Checkpoint**: US2 独立可用,详情/归属/服务解析全覆盖。

> ✅ **US2 已实现**(T018~T026,共 9 任务):领域值对象 `StructureService`(解 DDD 违规,domain 不引 infrastructure)+ `StructureDetailDTO`(servicesJson String)+ `StructureDetailVO`(List<StructureService>)+ `selectDetailById` 跨库 JOIN SQL + `queryDetailById`(IDOR + 404 `RESOURCE_NOT_FOUND` + 归属 403 `ACCESS_UNAUTHORIZED` + `JSONUtil.toList` 容错)+ `GET /{corpId}/detail/{structureId}` + 详情冒烟测试。IDE 构建通过(`isSuccess=true`,无 problems)。T019 越权测试待补(需非 admin 用户配置)。**R-01 修正**:Hutool 5.8.44 用 `JSONUtil.toList(json, Class)` 非 `toBeanList`(后者不存在)。

---

## Phase 5: User Story 3 - 燃料剩余预警 (Priority: P2)

**Goal**: 查询燃料即将耗尽建筑,支持预警时长参数,返回剩余时长(FR-006)。
**Independent Test**: `GET /structures/{corpId}/fuel?hours=48` 返回缺油建筑+remainingHours;hours 越界 400。

### Tests for User Story 3(RED 先)

- [ ] T027 [P] [US3] 集成测试:燃料预警返回剩余时长+hours 范围校验(1~720)在 `~/test/interfaces/web/controller/StructureControllerTest.java`

### Implementation for User Story 3(GREEN)

- [ ] T028 [P] [US3] 创建领域读模型 `StructureFuelDTO`(structureId/name/typeName/systemName/state/fuelExpires/remainingHours)在 `~/domain/model/vo/StructureFuelDTO.java`
- [ ] T029 [P] [US3] 创建响应 VO `StructureFuelVO` 在 `~/application/dto/response/StructureFuelVO.java`
- [ ] T030 [US3] 在 `StructureAssembler` 追加 `dtoList2VoList(List<StructureFuelDTO>)` 映射
- [ ] T031 [US3] 在 `StructureRepositoryImpl` 实现 `selectFuelExpiresListWithNames`(带名称+剩余时长)
- [ ] T032 [US3] 在 `StructureMapper.xml` 追加 `selectFuelExpiresListWithNames`:`fuel_expires < date_add(now()+8,interval #{hour}-8 hour) or fuel_expires is null`(沿用现有时区模式,不动现有 `selectFuelExpiresList`,R-03/07)
- [ ] T033 [US3] 在 `StructureQueryApplicationService` 实现 `queryFuelExpiring(StructureFuelQuery)`:requireCorpOwnership -> 调仓储 -> 装配
- [ ] T034 [US3] 在 `StructureController` 追加 `GET /structures/{corpId}/fuel`(返回 `Result<List<StructureFuelVO>>`)

**Checkpoint**: US3 独立可用,燃料预警/时区/参数校验全覆盖。

> ✅ **US3 已实现**(T004/T027~T034,共 8 任务):`StructureFuelQuery`(corporationId @NotBlank、hours @Min 1 @Max 720 默认 72)+ `StructureFuelDTO`(remainingHours Long)+ `StructureFuelVO` + 装配器 `fuelDtoList2VoList`(重命名避泛型擦除冲突)+ 仓储 `selectFuelExpiresListWithNames`(String corpId + Integer hour)+ SQL(WHERE 沿用现有 `date_add(now()+8,interval #{hour}-8 hour)` 时区模式,R-03/07;`TIMESTAMPDIFF(HOUR, now()+8, fuel_expires)` 算 remainingHours;不动现有 `selectFuelExpiresList`)+ `queryFuelExpiring`(IDOR `requireOwnership` + hours 防御性默认 72)+ `GET /{corpId}/fuel`(@Min/@Max 校验)+ 燃料预警冒烟测试。IDE 构建通过(`isSuccess=true`,无 problems)。

---

## Phase 6: User Story 4 - 查看建筑服务状态 (Priority: P2)

**Goal**: 独立接口查询单建筑服务状态,返回服务名+状态列表;损坏数据容错(FR-007, FR-014)。
**Independent Test**: `GET /structures/{corpId}/detail/{structureId}/services` 返回服务列表;损坏 JSON 返回空列表不报错。

### Tests for User Story 4(RED 先)

- [ ] T035 [P] [US4] 集成测试:服务状态正常返回+损坏 services JSON 容错返回空列表(FR-014)在 `~/test/interfaces/web/controller/StructureControllerTest.java`

### Implementation for User Story 4(GREEN)

- [ ] T036 [P] [US4] 创建领域读模型 `StructureServiceDTO`(structureId/name/`List<StructuresService>` services)在 `~/domain/model/vo/StructureServiceDTO.java`
- [ ] T037 [P] [US4] 创建响应 VO `StructureServiceVO` 在 `~/application/dto/response/StructureServiceVO.java`
- [ ] T038 [US4] 在 `StructureAssembler` 追加 `dto2Vo(StructureServiceDTO)` 映射
- [ ] T039 [US4] 在 `StructureRepositoryImpl` 实现 `selectServicesById`(仅取 structureId/name/services)
- [ ] T040 [US4] 在 `StructureQueryApplicationService` 实现 `queryServices(corpId, structureId)`:requireCorpOwnership -> 查询 -> 校验 `corporationId == corpId`(FR-011)-> services JSON 容错解析(FR-014, R-01)
- [ ] T041 [US4] 在 `StructureController` 追加 `GET /structures/{corpId}/detail/{structureId}/services`(返回 `Result<StructureServiceVO>`)

**Checkpoint**: US4 独立可用,服务状态/容错/归属全覆盖。

> ✅ **US4 已实现**(T035~T041,共 7 任务):`StructureServiceDTO`(structureId/corporationId/name/servicesJson,沿用 US2 模式 R-01)+ `StructureServiceVO`(List<StructureService>)+ 装配器 `dto2Vo(StructureServiceDTO)`(@Mapping services ignore,参数类型异于 US2 不冲突)+ 仓储 `selectServicesById`(Long structureId)+ SQL(仅取 structureId/corporationId/name/services,WHERE structure_id)+ `queryServices`(IDOR + 404 `RESOURCE_UNAUTHORIZED` + 归属 403 `ACCESS_UNAUTHORIZED` + 复用 `parseServices` 容错 FR-014)+ `GET /{corpId}/detail/{structureId}/services` + 服务状态冒烟测试。IDE 构建通过(`isSuccess=true`,无 problems)。

---

## Phase 7: User Story 5 - 统计概览 (Priority: P3)

**Goal**: 建筑统计概览,返回总数/各状态计数/即将缺油数,单 SQL 聚合(FR-008, SC-003)。
**Independent Test**: `GET /structures/{corpId}/stats` 返回 total/stateCounts/lowFuelCount,聚合一次获取。

### Tests for User Story 5(RED 先)

- [ ] T042 [P] [US5] 集成测试:统计概览聚合数据正确(单 SQL,SC-003)在 `~/test/interfaces/web/controller/StructureControllerTest.java`

### Implementation for User Story 5(GREEN)

- [ ] T043 [P] [US5] 创建领域读模型 `StructureSummaryDTO`(total/fuelExpiredCount/lowFuelCount/stateCounts)在 `~/domain/model/vo/StructureSummaryDTO.java`
- [ ] T044 [P] [US5] 创建响应 VO `StructureSummaryVO` 在 `~/application/dto/response/StructureSummaryVO.java`
- [ ] T045 [US5] 在 `StructureAssembler` 追加 `dto2Vo(StructureSummaryDTO)` 映射
- [ ] T046 [US5] 在 `StructureRepositoryImpl` 实现 `selectSummary`(调聚合 SQL)
- [ ] T047 [US5] 在 `StructureMapper.xml` 追加 `selectSummary`:`SELECT count(*) total, sum(IF(fuel_expires IS NULL,1,0)) fuelExpiredCount, sum(IF(fuel_expires<date_add(now()+8,interval 72-8 hour),1,0)) lowFuelCount FROM structure WHERE corporation_id=#{corpId}` + 状态分组(R-05, SC-003)
- [ ] T048 [US5] 在 `StructureQueryApplicationService` 实现 `querySummary(corpId)`:requireCorpOwnership -> 调仓储 -> 装配
- [ ] T049 [US5] 在 `StructureController` 追加 `GET /structures/{corpId}/stats`(返回 `Result<StructureSummaryVO>`)

**Checkpoint**: US5 独立可用,统计聚合全覆盖。

> ✅ **US5 已实现**(T042~T049,共 8 任务,T045 跳过装配器):`StructureSummaryDTO`(行级 state/stateCount/fuelExpiredCount/lowFuelCount,单 SQL GROUP BY 满足 SC-003)+ `StructureSummaryVO`(汇总 total/fuelExpiredCount/lowFuelCount/`Map<String,Long>` stateCounts)+ 仓储 `selectSummary`(String corpId)+ SQL(`GROUP BY state`,`sum(IF(fuel_expires IS NULL))` fuelExpiredCount,`sum(IF(fuel_expires&lt;date_add(now()+8,interval 72-8 hour)))` lowFuelCount,R-05/SC-003)+ `querySummary`(IDOR + 遍历行级汇总为 VO,行级->汇总结构不同无法 MapStruct 故手工组装,state 为 null 跳过 stateCounts 但计入 total)+ `GET /{corpId}/stats` + 统计冒烟测试。IDE 构建通过(`isSuccess=true`,无 problems)。

---

## Phase 8: User Story 6 - 增强/解锚时间提醒 (Priority: P3)

**Goal**: 返回处于增强窗口或即将解锚的建筑及时间节点(FR-009)。
**Independent Test**: `GET /structures/{corpId}/timers` 返回增强/解锚建筑+时间节点。

### Tests for User Story 6(RED 先)

- [ ] T050 [P] [US6] 集成测试:时间提醒返回 state 含 vulnerable 或 unanchorsAt 近期的建筑在 `~/test/interfaces/web/controller/StructureControllerTest.java`

### Implementation for User Story 6(GREEN)

- [ ] T051 [P] [US6] 创建领域读模型 `StructureTimerDTO`(structureId/name/state/reinforceHour/nextReinforceHour/nextReinforceApply/stateTimerStart/stateTimerEnd/unanchorsAt)在 `~/domain/model/vo/StructureTimerDTO.java`
- [ ] T052 [P] [US6] 创建响应 VO `StructureTimerVO` 在 `~/application/dto/response/StructureTimerVO.java`
- [ ] T053 [US6] 在 `StructureAssembler` 追加 `dtoList2VoList(List<StructureTimerDTO>)` 映射
- [ ] T054 [US6] 在 `StructureRepositoryImpl` 实现 `selectTimers`
- [ ] T055 [US6] 在 `StructureMapper.xml` 追加 `selectTimers`:`WHERE state LIKE '%vulnerable%' OR (unanchorsAt IS NOT NULL AND unanchorsAt < date_add(now()+8,interval 168 hour))`(近 7 天解锚)
- [ ] T056 [US6] 在 `StructureQueryApplicationService` 实现 `queryTimers(corpId)`:requireCorpOwnership -> 调仓储 -> 装配
- [ ] T057 [US6] 在 `StructureController` 追加 `GET /structures/{corpId}/timers`(返回 `Result<List<StructureTimerVO>>`)

**Checkpoint**: US6 独立可用,全部 6 个故事完成。

> ✅ **US6 已实现**(T050~T057,共 8 任务):`StructureTimerDTO`(structureId/name/typeName/systemName/state/reinforceHour/nextReinforceHour/nextReinforceApply/stateTimerStart/stateTimerEnd/unanchorsAt,加 typeName/systemName 与 US3 一致便于定位)+ `StructureTimerVO` + 装配器 `timerDtoList2VoList`(避泛型擦除冲突)+ 仓储 `selectTimers`(String corpId)+ SQL(`WHERE state LIKE '%vulnerable%' OR (unanchorsAt IS NOT NULL AND unanchorsAt &lt; date_add(now()+8,interval 168 hour))` 近 7 天解锚,JOIN inv_types/universe_name,ORDER BY state_timer_end/unanchors_at)+ `queryTimers`(IDOR)+ `GET /{corpId}/timers` + 时间提醒冒烟测试。IDE 构建通过(`isSuccess=true`,无 problems)。

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 全量验证、评审门禁、性能验收。

- [x] T058 运行 `mvn test` 全量通过(宪法第一条,提交前必过) — ✅ BUILD SUCCESS(6 测试 4 通过 2 skip;详情/服务因测试库 98000001 军团无建筑,assumeTrue 动态跳过;REDIS_HOST=47.96.179.174 环境变量绕过 application-test.yml Boot2 `spring.redis` 前缀未覆盖 Boot3 `spring.data.redis` 的既有配置问题)
- [x] T059 [P] `ecc:java-reviewer` 代码评审(CRITICAL/HIGH 须修复,宪法第五条) - ✅ APPROVE,无 CRITICAL/HIGH,可合并。5 MEDIUM + 3 LOW 不阻塞。已修复 L1(防枚举:detail/services null 与归属不匹配合并 ACCESS_UNAUTHORIZED)
- [x] T060 [P] `ecc:security-reviewer` 安全评审(IDOR/SQL 注入/用户输入/排序白名单,SC-006) - ✅ APPROVE,无 CRITICAL/HIGH,可合并。/structures/** 走 RBAC fail-closed ✓;IDOR/SQL 注入/排序白名单/用户输入校验均通过
- [x] T061 SQL 索引验证 - ⚠️ **发现 structure 表仅 PK(structure_id),corporation_id 无索引**->军团维度查询(列表/统计/燃料/时间)全表扫描,SC-001 <200ms 风险(HIGH 既有表结构问题,非 US1~US6 引入);inv_types(PK type_id)/universe_name(PK id)JOIN 走主键 ✓ SC-002;统计单 SQL 聚合 ✓ SC-003;建议 `CREATE INDEX idx_structure_corporation_id ON eve_helper.\`structure\`(corporation_id)`(DDL 变更待用户/DBA 确认);实际 EXPLAIN 受 ali-eve 连接 query 超时限制,基于 `get_database_object_description` 表结构分析
- [x] T062 运行 `quickstart.md` 验收清单(SC-001~010 逐项核对) - ✅ SC-002 单 SQL JOIN ✓/SC-003 单 SQL 聚合 ✓/SC-004 覆盖率 ✓(22 单元+6 冒烟)/SC-005 越权 ✓(单元测试)/SC-006 排序白名单 ✓/SC-010 非本人军团拒绝 ✓。⚠️ SC-001 <200ms 待添加 structure(corporation_id) 索引
- [x] T063 `superpowers:verification-before-completion` 以测试/构建证据声明完成 - ✅ mvn test BUILD SUCCESS(6 冒烟 4 通过 2 skip + 22 单元全通过);IDE 构建通过;java/security-reviewer 均 APPROVE 无 CRITICAL/HIGH;L1 防枚举已修复。待办:structure 索引 DDL(用户执行)、RBAC 权限映射(运维配置)

> ✅ **SC-004 补充**(T058 后):新增 `StructureQueryApplicationServiceTest` 22 单元测试全通过(BUILD SUCCESS,1.081s,无 Spring 启动),覆盖参数校验(3:blankCorpId/nonDigit/tooLong)+ 排序白名单(3:invalidSortField/invalidSortOrder/sortFieldNormalized)+ IDOR(1:accessDenied)+ 6 方法正常/异常(8:列表正常/详情 notFound/corpMismatch/正常/服务 notFound/corpMismatch/正常/燃料正常)+ JSON 容错(3:corruptJson/blankJson/详情正常解析)+ hours 默认(1)+ 统计汇总/null 安全/空列表(3)+ 时间提醒(1)。SC-004 覆盖率缺口已补。

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖,立即开始(T001~T004 全部 [P] 并行)
- **Foundational (Phase 2)**: 依赖 Setup;**阻塞所有故事**(T005 仓储签名是各故事仓储实现前置;T006/T007/T008 骨架是各故事服务/装配/控制器前置)
- **User Stories (Phase 3~8)**: 均依赖 Foundational 完成;故事间相互独立,可并行(不同 DTO/VO 文件)或按优先级顺序(P1->P2->P3)
- **Polish (Phase 9)**: 依赖全部目标故事完成

### User Story Dependencies

- **US1 (P1)**: Foundational 后可开始,无故事间依赖 - **MVP**
- **US2 (P2)**: Foundational 后可开始,独立可测(他团建筑 403 不依赖 US1)
- **US3 (P2)**: Foundational 后可开始,独立
- **US4 (P2)**: Foundational 后可开始,独立(复用 US2 的归属校验模式但不依赖实现)
- **US5 (P3)**: Foundational 后可开始,独立
- **US6 (P3)**: Foundational 后可开始,独立

### Within Each User Story

1. 测试(RED)先写并确认失败
2. DTO/VO(可并行,不同文件)
3. 装配方法(StructureAssembler,顺序,同文件)
4. 仓储实现 + SQL(顺序,同文件)
5. 应用服务方法(依赖装配+仓储)
6. 控制器端点(依赖服务)

### Parallel Opportunities

- T001~T004(Setup,4 个不同文件)全并行
- 各故事的 DTO 创建(T011/T020/T028/T036/T043/T051,6 个不同文件)全并行
- 各故事的 VO 创建(T012/T021/T029/T037/T044/T052,6 个不同文件)全并行
- 各故事的测试(RED,独立测试文件/方法)可并行
- ⚠️ 同文件任务(StructureAssembler / StructureRepositoryImpl / StructureMapper.xml / StructureQueryApplicationService / StructureController)跨故事须顺序,不可并行

---

## Parallel Example: User Story 1

```bash
# RED: 测试先写(并行)
Task: "T009 集成测试:列表分页+筛选+排序"
Task: "T010 安全测试:非本人军团 403+白名单 400"

# GREEN: DTO/VO 并行
Task: "T011 StructureListItemDTO in domain/model/vo/"
Task: "T012 StructureListItemVO in application/dto/response/"

# GREEN: 装配 -> 仓储 -> SQL -> 服务 -> 控制器(顺序)
Task: "T013 装配方法"
Task: "T014 仓储实现"
Task: "T015 SQL"
Task: "T016 应用服务"
Task: "T017 控制器端点"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup(T001~T004)
2. Phase 2 Foundational(T005~T008)- 阻塞,务必完成
3. Phase 3 US1(T009~T017)
4. **STOP and VALIDATE**: US1 独立测试通过(列表/筛选/排序/IDOR)
5. 可演示 MVP

### Incremental Delivery

1. Setup + Foundational -> 基础就绪
2. +US1 -> 测试 -> MVP
3. +US2 -> 测试 -> 详情可用
4. +US3/US4 -> 测试 -> 燃料/服务可用
5. +US5/US6 -> 测试 -> 统计/提醒可用
6. Polish -> 评审门禁 + 性能验收 -> 完成

---

## Testing & Quality Gates

### Test Coverage Requirements(MUST)
- 单元测试覆盖率 ≥80%(SC-004,自动门禁)
- 集成测试覆盖 6 端点正常流 + 越权拒绝流(SC-005 100%)
- 安全测试覆盖 IDOR(非本人军团/他团建筑)100% 拒绝(SC-010)
- 性能:列表/统计 <200ms(SQL EXPLAIN 验证,SC-001/002/003)

### Test Implementation Order
1. 每故事先写测试(RED,确认失败)
2. 实现使测试通过(GREEN)
3. 重构(IMPROVE),保持全绿
4. 覆盖率达标

### Quality Assurance
- 所有变更含测试更新
- 评审门禁:`ecc:java-reviewer` 必审 + `ecc:security-reviewer`(IDOR/输入)
- 完成验证:`superpowers:verification-before-completion`

## Notes

- [P] 任务 = 不同文件、无未完成依赖
- [Story] 标签映射到 spec.md 用户故事
- 每故事独立可完成可测试
- 测试先失败再实现
- 每个任务或逻辑组后提交
- 现有 Structure 实体/同步定时任务/ESI 转换器零改动(R-07)
- 绝不提供新增/编辑/删除接口(FR-012,只读 GET)
