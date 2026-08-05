# Data Model: 用户账户 ESI 授权状态返回

**Date**: 2026-08-04 | **Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

> Phase 1 产物:本功能不新增持久化表,仅在既有 `eve_account` 之上派生授权状态。新增 1 个枚举,1 个 DTO 字段。

## 新增枚举:EsiAuthStatus

**位置**: `src/main/java/xyz/foolcat/eve/evehelper/shared/kernel/enums/EsiAuthStatus.java`
**风格**: 与既有 `CorporationActivityEnum`/`IndustryActivityEnum` 一致(`code` + `description` 双字段构造)。

| 枚举值 | code | description | 语义 |
|--------|------|-------------|------|
| `AUTHORIZED` | `AUTHORIZED` | 授权正常 | 持有 refreshToken 且成功换取 accessToken |
| `EXPIRED` | `EXPIRED` | 授权过期 | 持有 refreshToken 但换取 accessToken 失败(4xx invalid_grant) |
| `NOT_AUTHORIZED` | `NOT_AUTHORIZED` | 未授权 | 无 refreshToken |
| `UNKNOWN` | `UNKNOWN` | 无法判定 | 判定过程失败(ESI 5xx / 网络 / 超时) |

**字段**:
- `private final String code;`
- `private final String description;`
- 构造器 + `getCode()` + `getDescription()`

**序列化**: 作为 `UserAccountDTO.authStatus` 字段返回前端,默认序列化为枚举名(`AUTHORIZED` 等)。

## DTO 变更:UserAccountDTO

**位置**: `src/main/java/xyz/foolcat/eve/evehelper/application/dto/UserAccountDTO.java`

**新增字段**:
```java
@Schema(description = "ESI 授权状态:AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN")
private EsiAuthStatus authStatus;
```

既有字段(characterId/characterName/corpId/corpName/allianceId/allianceName)不变。

## 实体:EveAccount(不变)

`domain/model/entity/system/EveAccount.java` 已含 `refreshToken` 字段,本功能直接读取用于状态判定,不新增字段、不改表结构。

## 持久化:无变更

`eve_account` 表结构不变。状态为运行时派生值,存于 Redis 状态缓存(`esi_auth_status:{characterId}`),不落 MySQL。

## 状态转移

```text
[无 refreshToken] ───────────────────────► NOT_AUTHORIZED(缓存 5min)

[有 refreshToken] ─► 刷新成功 ──────────► AUTHORIZED(缓存 5min,回写新 refreshToken)
                  ├► EsiException(4xx) ─► EXPIRED(缓存 5min)
                  └► 5xx/网络/超时 ──────► UNKNOWN(缓存 30s)
```

缓存命中时直接返回缓存态,不重新判定(直到 TTL 到期)。
