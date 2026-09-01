# Contract: GET /user/{userId} 响应契约变更

**Feature**: 021-esi-auth-scopes | **Type**: REST 响应扩展（向后兼容——仅新增可空字段）

## 端点

- `GET /user/{userId}`（用户服务-用户绑定的角色列表,含 ESI 授权状态与授权详情）
- 认证/授权：不变（JWT 认证 + `requireSelfOrRoot`：仅本人或 ROOT）

## 响应 Schema（OpenAPI 片段,仅展示新增部分）

```yaml
components:
  schemas:
    UserAccountDTO:
      type: object
      properties:
        # ... 既有字段不变 ...
        authStatus:
          type: string
          enum: [AUTHORIZED, EXPIRED, NOT_AUTHORIZED, UNKNOWN]
          description: ESI 授权状态
        scopes:
          type: array
          nullable: true
          items:
            type: string
          description: >
            ESI 已授权权限列表（原始 scope 名）。
            null = 无法确定（非 AUTHORIZED / 无可用 accessToken / 解析失败）;
            空数组 = token 有效但未声明受限权限。
```

## 响应示例

```json
{
  "code": "200",
  "data": [
    {
      "characterId": 95465499,
      "characterName": "角色甲",
      "corpId": 98000001,
      "corpName": "军团A",
      "allianceId": 99000001,
      "allianceName": "联盟X",
      "authStatus": "AUTHORIZED",
      "scopes": [
        "esi-assets.read_assets.v1",
        "esi-wallet.read_character_wallet.v1",
        "esi-industry.read_character_jobs.v1"
      ]
    },
    {
      "characterId": 95465500,
      "characterName": "角色乙",
      "corpId": 98000001,
      "corpName": "军团A",
      "allianceId": null,
      "allianceName": null,
      "authStatus": "EXPIRED",
      "scopes": null
    }
  ]
}
```

## 契约规则

| 规则 | 说明 |
|------|------|
| 只增不改 | 既有字段语义/取值完全不变,老前端可忽略 `scopes` 字段 |
| scopes 值域 | 仅权限名（EVE SSO 原始 scope 字符串）;**绝不包含** token 值、签名或任何凭据材料（FR-009） |
| null 语义 | 非 AUTHORIZED 三态、无可用 token、解析失败 → `null`;与 `[]`（确认无受限权限）区分 |
| 无新端点 | 不新增接口、不改路径、不改状态码语义 |
| 性能 | 缓存命中路径 p95 <200ms 与现状持平;scopes 解析不产生额外上游调用 |

## 错误响应

不变：沿用全局异常处理与 `ResultCode`（如本人/ROOT 校验失败仍由 `AccessGuard` 抛出既有错误码）。
