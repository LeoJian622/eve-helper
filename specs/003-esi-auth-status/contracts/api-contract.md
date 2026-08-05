# API Contract: 用户绑定的角色列表(含 ESI 授权状态)

**Date**: 2026-08-04 | **Feature**: [spec.md](../spec.md)

> Phase 1 产物:本功能扩展既有接口的响应,不改变请求契约与鉴权。

## 端点

`GET /user/{userId}`

- **Operation**: 用户服务-用户绑定的角色列表
- **鉴权**: JWT + RBAC(`RbacAuthorizationManager`,非白名单需认证与权限)
- **路径参数**: `userId` (Integer,必填) - 目标用户 ID
- **响应包裹**: `Result<List<UserAccountDTO>>`(统一 `Result<T>` 信封 + `ResultCode`)

## 响应 Schema

```json
{
  "code": "200",
  "message": "成功",
  "data": [
    {
      "characterId": 95465499,
      "characterName": "角色名",
      "corpId": 98000001,
      "corpName": "军团名",
      "allianceId": 99000001,
      "allianceName": "联盟名",
      "authStatus": "AUTHORIZED"
    }
  ]
}
```

### authStatus 字段(新增)

| 取值 | 含义 | 触发条件 |
|------|------|----------|
| `AUTHORIZED` | 授权正常 | 持有 refreshToken 且成功换取 accessToken |
| `EXPIRED` | 授权过期 | 持有 refreshToken 但换取 accessToken 失败(refreshToken 失效) |
| `NOT_AUTHORIZED` | 未授权 | 无 refreshToken |
| `UNKNOWN` | 无法判定 | 判定过程失败(ESI 5xx / 网络 / 超时) |

- 类型: 枚举字符串
- 必返: 是(列表每个条目均含此字段)
- 幂等: 同一角色在状态缓存 TTL(成功态 5min / UNKNOWN 30s)内返回一致结果

## 行为契约

1. **空列表**: 用户无绑定角色时,`data` 为 `[]`,`code` 仍为成功。
2. **独立判定**: 每个角色的 `authStatus` 独立计算,单角色判定异常不影响其他角色(异常角色标 `UNKNOWN`)。
3. **只读**: 判定不主动删除 refreshToken;刷新成功时按既有 ESI 流程轮换回写新 refreshToken(对调用方透明)。
4. **超时**: 单角色 ESI 判定 5s 超时,超时归为 `UNKNOWN`,接口不整体失败。
5. **既有语义不变**: 接口仍返回角色列表;`authStatus` 为附加字段,不影响既有字段含义。

## 错误响应

沿用既有全局异常处理与 `ResultCode`:
- 未认证 / 无权限: 既有 JWT/RBAC 错误码
- `userId` 不存在: 返回空列表(与既有行为一致),非错误

## 兼容性

新增字段为向后兼容变更:既有调用方忽略 `authStatus` 即可,不影响既有字段。
