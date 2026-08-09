# REST API Contracts: 建筑表只读查询接口

> 所有端点返回统一 `Result<T>` 信封(`{code, message, data}`);除白名单外需 JWT 认证,RBAC 由 `RbacAuthorizationManager` 执行;所有端点只读,HTTP 方法均为 `GET`(FR-012 禁止 CUD)。
> 基础路径:`/structures`;端口 9999。

## 端点总览

| # | 方法 | 路径 | 用途 | 对应 FR |
|---|------|------|------|---------|
| 1 | GET | `/structures/{corpId}` | 分页列表 | FR-001~004 |
| 2 | GET | `/structures/{corpId}/detail/{structureId}` | 单建筑详情 | FR-005 |
| 3 | GET | `/structures/{corpId}/fuel` | 燃料预警 | FR-006 |
| 4 | GET | `/structures/{corpId}/detail/{structureId}/services` | 服务状态 | FR-007 |
| 5 | GET | `/structures/{corpId}/stats` | 统计概览 | FR-008 |
| 6 | GET | `/structures/{corpId}/timers` | 增强/解锚提醒 | FR-009 |

所有端点均经 `AccessGuard.requireOwnership(corpId, "建筑")` 校验(FR-010);端点 2/4 额外校验 `structure.corporationId == corpId`(FR-011)。

---

## EP-1: 分页列表

`GET /structures/{corpId}`

**路径参数**: `corpId`(string,纯数字,长度≤19)
**查询参数**:

| 参数 | 类型 | 必填 | 默认 | 校验 |
|------|------|------|------|------|
| current | int | 否 | 1 | ≥1 |
| size | int | 否 | 20 | 1~1000 |
| name | string | 否 | - | 名称模糊 |
| state | string | 否 | - | 状态筛选 |
| lowFuelOnly | boolean | 否 | false | 仅缺油 |
| sortField | string | 否 | FUEL_EXPIRES | 白名单:STRUCTURE_ID/NAME/STATE/FUEL_EXPIRES |
| sortOrder | string | 否 | desc | asc/desc |

**响应**: `Result<PageResult<StructureListItemVO>>`

```json
{
  "code": 200, "message": "success",
  "data": {
    "records": [{
      "structureId": 1000001, "name": "甲建筑", "typeId": 35832,
      "typeName": "Astrahus", "systemId": 30000001, "systemName": "Zorsade",
      "state": "anchor_fuel", "fuelExpires": "2026-08-12T10:00:00+08:00",
      "corporationId": 98000001
    }],
    "total": 1, "current": 1, "size": 20, "pages": 1,
    "hasNext": false, "hasPrevious": false
  }
}
```

**错误**: 403(非本人军团,FR-010);400(参数校验失败/sortField 非白名单,FR-015/016)

---

## EP-2: 单建筑详情

`GET /structures/{corpId}/detail/{structureId}`

**路径参数**: `corpId`、`structureId`(Long)
**响应**: `Result<StructureDetailVO>` -- 全部 Structure 字段 + typeName + systemName + services(已解析为 `[{name,state}]`)
**错误**: 403(corpId 非本人 或 建筑不属于该 corp,FR-010/011);404(建筑不存在)

---

## EP-3: 燃料预警

`GET /structures/{corpId}/fuel`

**查询参数**: `hours`(int,默认 72,范围 1~720,FR-016/Assumption 7)
**响应**: `Result<List<StructureFuelVO>>`

```json
{ "code": 200, "message": "success",
  "data": [{ "structureId": 1000001, "name": "甲建筑", "typeName": "Astrahus",
    "systemName": "Zorsade", "state": "anchor_fuel",
    "fuelExpires": "2026-08-12T10:00:00+08:00", "remainingHours": 48 }] }
```

筛选规则:`fuel_expires` 在 `now()+8` 到 `now()+8+hours` 之间,或 `fuel_expires IS NULL`(已缺油)。

---

## EP-4: 单建筑服务状态

`GET /structures/{corpId}/detail/{structureId}/services`

**响应**: `Result<StructureServiceVO>` -- `{structureId, name, services:[{name,state}]}`
**容错**: services JSON 解析失败返回空 `services` 列表(FR-014)
**错误**: 403/404 同 EP-2

---

## EP-5: 统计概览

`GET /structures/{corpId}/stats`

**响应**: `Result<StructureSummaryVO>`

```json
{ "code": 200, "message": "success",
  "data": { "total": 12, "fuelExpiredCount": 1, "lowFuelCount": 3,
    "stateCounts": { "anchor_fuel": 8, "low_power": 2, "shield_vulnerable": 2 } } }
```

聚合单 SQL 获取(SC-003);`lowFuelCount` 默认 72 小时窗(可后续扩展参数)。

---

## EP-6: 增强/解锚提醒

`GET /structures/{corpId}/timers`

**响应**: `Result<List<StructureTimerVO>>` -- 处于增强窗口(state 含 `vulnerable`)或即将解锚(`unanchorsAt` 在近期)的建筑,含 reinforceHour/nextReinforceHour/stateTimerStart/stateTimerEnd/unanchorsAt。

---

## 通用错误码

| code | 含义 | 触发 |
|------|------|------|
| 200 | success | 正常 |
| 400 | 参数校验失败 | sortField 非白名单 / hours 越界 / corpId 格式非法 |
| 403 | 越权 | 非本人军团 / 建筑不属于该 corp |
| 404 | 不存在 | structureId 无对应建筑 |
