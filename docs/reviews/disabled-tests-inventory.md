# 禁用测试清单与验证记录

> 记录因缺少真实入参(硬编码资源 ID 不存在 / 无权限 / 失效凭据)而 `@Disabled` 的 ESI 集成测试。
> 验证日期:2026-08-13。

## 验证方法

临时启用全部 `@Disabled` 测试并运行,确认每个在去掉注解后**确实失败**且失败原因是**缺少真实入参**,而非掩盖应用代码 bug。

结果:29 个 `@Disabled` 全部失败,无一能通过;失败原因分类见下。以下 22 个为 **400/403/404**(入参/权限/凭据问题),其余 7 个(Timeout / ESI 数据异常)由人工处理。

## 400 BAD_REQUEST — 失效凭据(3 个)

全部失败于 `POST /v2/oauth/token`(用硬编码一次性凭据换 token,已失效)。

| 测试 | 方法 | 硬编码凭据 | 原因 |
|------|------|-----------|------|
| `AuthorizeOAuthTest` | `updateAccessTokenAuthorizationCode` | 一次性授权码 `3MnB2naFkk6wyRsaZgfODg` | 授权码已用/失效 |
| `AuthorizeOAuthTest` | `updateAccessTokenRefreshToken` | refresh token `odZ2dRCzHUOJCa9KZqILQQ==` | token 无效 |
| `CharacterControllerTest` | `addCharacterAuth` | SSO 授权码 `GAUu5McvqEi40KY5ytn3CQ` | 授权码已失效 |

> 处理:需真实有效的一次性授权码/refresh token 时,替换硬编码值后临时启用。

## 403 FORBIDDEN — 无访问权限(2 个)

| 测试 | 方法 | 原因 |
|------|------|------|
| `IndustryApiTest` | `queryCorporationIndustryJobs` | 角色无军团工业数据访问权(scope/数据) |
| `MarketApiTest` | `queryStructureOrders_shouldReturnStructureOrders` | 硬编码结构 `1015148880281` 非角色可访问市场 |

> 处理:Industry 需角色具备军团工业 scope;Market 需替换为角色真实可访问的结构 ID。

## 404 NOT_FOUND — 硬编码资源 ID 不存在(17 个)

| 测试 | 数量 | 方法 | 原因 |
|------|------|------|------|
| `FleetApiTest` | 14 | `addFleetMember` / `addFleetWing` / `addFleetWingSquad` / `deleteFleetMember` / `deleteFleetSquad` / `deleteFleetWing` / `queryCharacterFittings` / `queryFleet` / `queryFleetMember` / `queryFleetWings` / `updateFleet` / `updateFleetMember` / `updateFleetSquadRename` / `updateFleetWingRename` | 硬编码舰队 ID `1022810945368` 不存在(`The fleet does not exist`) |
| `ContractsApiTest` | 3 | `queryPublicContractsBids` / `queryPublicContractsItems` / `queryCharactersContractsBids` | 硬编码合同 ID 不存在(`Contract not found`) |

> 处理:需替换为角色名下真实存在的舰队/合同 ID。

## 其它(7 个)— 待人工处理,非 400/403/404

| 测试 | 方法 | 失败原因 |
|------|------|----------|
| `OpportunitiesApiTest` | `queryCharacterOpportunities` / `queryOpportunitiesGroups` / `queryOpportunitiesGroupsDetails` / `queryOpportunitiesTaskDetails` / `queryOpportunitiesTasks` | ESI 网络超时(Timeout contacting serenity) |
| `FactionWarfareApiTest` | `queryCorporationFactionWarfareStats` | ESI 返回 200 但 body 异常(`Invalid 200 response`) |
| `FittingApiTest` | `deleteCharacterFittings` | ESI 内部错误(`Unhandled internal error encountered`) |