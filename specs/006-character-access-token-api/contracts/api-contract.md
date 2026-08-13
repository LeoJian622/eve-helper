# API 契约：角色 ESI AccessToken 查询

**Feature**: `006-character-access-token-api`

## 端点

```
GET /character/{characterId}/access-token
```

**认证**: 必需(JWT Bearer)。该路径不在安全白名单内。
**可见性**: `@Hidden` — 不出现在 `/v3/api-docs` 与 Swagger UI(FR-017)。
**开关**: 由 `eve-helper.debug.access-token-endpoint.enabled` 控制,**默认 `false`**。关闭时该 Bean 不注册,请求返回 404(FR-017)。

## 请求

| 位置 | 名称 | 类型 | 约束 |
|------|------|------|------|
| path | `characterId` | Integer | `@Positive`,必填(FR-011) |

## 成功响应

**200 OK**
**Header**: `Cache-Control: no-store`(FR-010)

> **实测结论(T016)**:该头由 Spring Security 默认的 `CacheControlHeadersWriter` 全局提供,
> 实际值为 `no-cache, no-store, max-age=0, must-revalidate`,对成功与错误响应一律生效。
> 核实依据:`SecurityConfig.java` 中**不存在** `headers(...)` 配置,即未 disable 默认 header writer。
> 故本 feature **不新增任何代码**满足 FR-010(评审 L1 要求实测而非盲目添加)。

```json
{
  "code": "00000",
  "msg": "操作成功",
  "data": {
    "accessToken": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "characterId": 95465499,
    "expiresIn": 1140
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `accessToken` | String | 带 `Bearer ` 前缀的 ESI access token |
| `characterId` | Integer | 回显请求的角色ID |
| `expiresIn` | Long | 剩余有效秒数。缓存未命中时取自 ESI `expires_in`;命中时取 Redis 剩余 TTL 并归一化(TTL 为 -1/-2 时返回 0,**不透出哨兵值**,FR-018) |

**注意**:响应体**不含** `refreshToken`(FR-008)。

## 错误响应

### 归属校验失败(核心安全契约)

以下情况**必须返回完全相同的响应**,不得存在任何可区分差异(FR-005、SC-007):

1. 角色属于其他用户
2. 角色在系统中不存在
3. 角色与请求者同军团但不属于请求者(FR-003)
4. 请求者为 ROOT/ADMIN 但角色不在其名下(FR-004)
5. 未认证 / `userId <= 0`(FR-006、FR-012)

**400 Bad Request**

```json
{
  "code": "ESI00400",
  "msg": "ESI 未授权或授权过期，请重新授权",
  "data": null
}
```

> **契约说明**:此处用 400 / `ESI00400` 而非 403 是既有约定(`GlobalExceptionHandler.java:185-196` 将非 `RESOURCE_NOT_FOUND`/`ACCESS_UNAUTHORIZED` 的码统一映射为 400)。评审 L2 指出 403 语义更准确,但改动会影响既有全部 ESI 错误路径,本 feature 不改。
>
> **副作用**:WAF 按状态码统计越权探测会漏掉本端点的拒绝。已记入遗留待办。

### 参数非法

**400 Bad Request** — `characterId` ≤ 0 或非数字

```json
{
  "code": "SYS00400",
  "msg": "用户请求参数错误",
  "data": null
}
```

> 与归属失败的 `ESI00400` 不同码。这**不违反**不可区分约束:`0` / 负数 / 非数字都不是合法 EVE 角色ID,两个需要不可区分的用例(属他人 / 不存在)都是合法正整数(评审 M5)。
>
> **实现注记(T011/T013)**:参数校验有两道 —— Controller 的 `@Positive` 与应用服务入口的显式判断。
> 前者对 `characterId ≤ 0` 抛 `ConstraintViolationException`(经 `GlobalExceptionHandler:68-77` 返回 `SYS00400`),
> 后者作为纵深防御,直接抛 `EveHelperException(PARAM_ERROR)` —— 同为 `SYS00400`,故对外表现一致。
> 保留两道是因为应用服务可能被非 HTTP 路径调用(如未来的内部任务),Bean Validation 在那些路径不生效。

### 角色本人所有但 ESI 授权已失效

**400 Bad Request** — 同 `ESI00400`,文案归一化后与归属失败一致(FR-020)

> 原实现会回显上游原始文本(如 `invalid_grant:Invalid refresh token. Token has been revoked.`),泄露 ESI 内部细节并使此情形可被区分。本 feature 归一化为固定文案,细节仅进服务端日志(评审 M2)。

### 未认证 / 主体无法识别

**403 Forbidden**

```json
{
  "code": "AUT00301",
  "msg": "访问未授权",
  "data": null
}
```

> **实现注记(T013,FR-012)**:`UserUtil.getUserId()` 未认证时返回 `-1`,应用服务入口显式拒绝
> `userId == null || userId <= 0`,抛 `EveHelperException(ACCESS_UNAUTHORIZED)`,
> 经 `GlobalExceptionHandler:193-195` 映射为 **403**。
>
> 此处与归属失败的 `ESI00400`/400 **不同码**,但不构成信息泄露:两者的区分只反映
> 「请求方自己有没有通过认证」,不透露任何关于目标角色的信息。真正需要不可区分的是
> 「角色属他人」与「角色不存在」——它们在已认证前提下走同一条 `ESI00400` 路径。
>
> 注:该端点已在 JWT 过滤器与 RBAC 之后,正常不会以未认证身份到达应用层;
> 此守卫是纵深防御,防止 `MODE_INHERITABLETHREADLOCAL` 等上下文异常导致主体缺失时 fail-open。

### ESI 服务不可用 / 超时 / 并发抢锁失败

**400 Bad Request**

```json
{
  "code": "ESI00500",
  "msg": "ESI 服务异常",
  "data": null
}
```

> **实现注记(T004,FR-013)**:以下三种情况共用此码 ——
> ① ESI `/token` 返回 5xx;② 刷新超过 5 秒超时;③ 同一 `(userId, characterId)` 已有并发请求持锁。
> ③ 之所以拒绝而非等待,是因为 ESI refreshToken 一次性轮换:并发刷新会使一方用到已失效 token,
> 且竞态回写可能落库已作废值,导致该角色绑定永久失效。调用方遇此码应稍后重试(缓存届时通常已被填充)。
> 锁 TTL 为 10 秒,覆盖 5 秒超时 + DB 回写。

## 审计日志(FR-016)

| 路径 | 级别 | 内容 |
|------|------|------|
| 成功 | INFO | `userId`、`characterId`、缓存命中与否 |
| 归属失败 | WARN | `userId`、`characterId`、拒绝原因 |
| 参数非法 | WARN | `userId`、原始入参 |

**禁止**记录:`accessToken`、`refreshToken` 明文(FR-009、FR-014)。需要标识 token 时用 `SensitiveDataMasker.maskToken`。

## 性能

| 路径 | 目标 |
|------|------|
| 缓存命中 | P95 < 200ms(SC-001) |
| 缓存未命中 | 受 ESI 5 秒超时约束(SC-003、FR-013) |
| 归属查库 | < 100ms,走 `eve_account` 的 `unique (character_id)` 索引(SC-002) |
