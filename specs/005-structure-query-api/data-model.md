# Phase 1 Data Model: 建筑表只读查询接口

> 现有实体零改动;新增领域读模型 DTO(跨层共享)、请求/响应 DTO、排序白名单枚举。字段名对齐现有 `Structure` 实体与 `StructurePO`。

## 现有实体(只读扩展,不改字段)

### Structure(领域实体,`domain/model/entity/system/Structure.java`)

| 字段 | 类型 | 说明 |
|------|------|------|
| structureId | Long | 建筑标识(PK) |
| corporationId | Integer | 所属军团 |
| fuelExpires | OffsetDateTime | 燃料到期时间 |
| name | String | 建筑名称 |
| nextReinforceApply | Integer | 下次增强应用 |
| nextReinforceHour | Integer | 下次增强小时 |
| profileId | Integer | 配置档 |
| reinforceHour | Integer | 增强小时 |
| state | String | 状态(anchor_fuel/low_power/shield_vulnerable/armor_vulnerable/hull_vulnerable/anchoring/unanchoring) |
| stateTimerEnd | OffsetDateTime | 状态计时结束 |
| stateTimerStart | OffsetDateTime | 状态计时开始 |
| systemId | Integer | 所在星系 |
| typeId | Integer | 建筑类型 |
| unanchorsAt | OffsetDateTime | 解锚时间 |
| services | String | 服务列表(JSON 字符串,序列化 `List<StructuresService>`) |

### StructuresService(值对象,`StructuresService.java`)

| 字段 | 类型 |
|------|------|
| name | String |
| state | String |

## 新增领域读模型(`domain/model/vo/`)

### StructureListItemDTO(列表项)

structureId, name, typeId, typeName, systemId, systemName, state, fuelExpires, corporationId

### StructureDetailDTO(详情)

全部 Structure 字段 + typeName + systemName + `List<StructuresService>` services(已解析)

### StructureFuelDTO(燃料预警)

structureId, name, typeName, systemName, state, fuelExpires, remainingHours(剩余时长,Long)

### StructureServiceDTO(服务状态)

structureId, name, `List<StructuresService>` services

### StructureSummaryDTO(统计概览)

total, fuelExpiredCount(已缺油), lowFuelCount(即将缺油), stateCounts(`Map<String,Long>` 各状态计数)

### StructureTimerDTO(增强/解锚提醒)

structureId, name, state, reinforceHour, nextReinforceHour, nextReinforceApply, stateTimerStart, stateTimerEnd, unanchorsAt

## 新增查询条件(`domain/model/query/`)

### StructurePageCriteria

| 字段 | 类型 | 说明 |
|------|------|------|
| corporationId | Integer | 军团 ID(必填) |
| name | String | 名称模糊筛选 |
| state | String | 状态筛选 |
| lowFuelOnly | Boolean | 仅缺油(fuel_expires 在预警窗内或 null) |
| sortField | SortField | 排序字段枚举(白名单) |
| ascending | Boolean | 升降序 |
| current | Integer | 页码(≥1) |
| size | Integer | 页大小(1~1000) |

### SortField 枚举(白名单,防注入 FR-015)

| 枚举值 | 列名 |
|--------|------|
| STRUCTURE_ID | `s.structure_id` |
| NAME | `s.name` |
| STATE | `s.state` |
| FUEL_EXPIRES | `s.fuel_expires` |

## 新增请求 DTO(`application/dto/request/`)

### StructureQuery(extends PageQuery)

corporationId(@NotBlank, 纯数字+长度≤19)、name、state、lowFuelOnly、sortField、sortOrder

### StructureFuelQuery

corporationId(@NotBlank)、hours(@Min 1 @Max 720,默认 72)

## 新增响应 VO(`application/dto/response/`)

StructureListItemVO / StructureDetailVO / StructureFuelVO / StructureServiceVO / StructureSummaryVO / StructureTimerVO -- 字段与对应 DTO 一一映射,加 `@Schema` 注解。

## 校验规则(从 FR/Assumption 提取)

- corporationId:非空、纯数字、长度≤19(防注入,对齐 Blueprints `requireOwnerId`)
- hours:1~720(Assumption 7,默认 72)
- 分页:current ≥1,size 1~1000(PageQuery 基类约束)
- sortField:必须在 SortField 枚举内(FR-015),否则拒绝
- services JSON:解析失败返回空列表(FR-014)
- 星系名缺失:降级返回 systemId(Assumption 3)
