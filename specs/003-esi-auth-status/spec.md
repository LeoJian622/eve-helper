# Feature Specification: 用户账户 ESI 授权状态返回 (User Account ESI Authorization Status)

**Feature Branch**: `003-esi-auth-status`  
**Created**: 2026-08-04  
**Status**: Draft  
**Input**: User description: "public Result<List<UserAccountDTO>> addUser(@PathVariable Integer userId) 该方法额外返回用户绑定角色的授权状态。使用refreshToken去进行ESI认证 1）可以正常返回accesstoken,授权正常 2）获取不到accesstoken，授权过期 3）没有refreshToken，未授权"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 查看绑定角色的 ESI 授权状态 (Priority: P1)

作为具备查看权限的管理员或用户,当我打开某用户的账户列表(`addUser` 接口)时,希望返回的每个绑定角色(EVE Online 角色)都附带其 ESI 授权状态,以便我一眼看出哪些角色的数据授权仍有效、哪些已过期、哪些从未授权,从而决定是否需要引导用户重新完成 ESI 授权。

**Why this priority**: 这是本功能的核心价值--在既有的账户列表中直接呈现授权状态,免去用户逐一试探或等待后台同步失败才发现授权失效。三种状态共同构成完整视图,是功能成立的最小可用切片。

**Independent Test**: 给定一个绑定了若干角色的用户,调用 `addUser` 接口,断言返回列表中每个角色都包含授权状态字段,且取值落在 {授权正常, 授权过期, 未授权} 三者之一。

**Acceptance Scenarios**:

1. **Given** 某角色持有有效 refreshToken 且能通过 ESI 成功换取 accessToken,**When** 调用 `addUser`,**Then** 该角色的授权状态为"授权正常"。
2. **Given** 某角色持有 refreshToken 但无法通过 ESI 换取 accessToken(已失效),**When** 调用 `addUser`,**Then** 该角色的授权状态为"授权过期"。
3. **Given** 某角色不存在 refreshToken(从未授权或已注销),**When** 调用 `addUser`,**Then** 该角色的授权状态为"未授权"。
4. **Given** 一个用户绑定了多个角色且各自授权状态不同,**When** 调用 `addUser`,**Then** 返回列表中每个角色独立反映其真实状态,互不影响。

---

### User Story 2 - 状态判定失败的优雅降级 (Priority: P2)

当 ESI 服务在状态判定过程中临时不可用或某角色的判定发生异常时,接口仍应返回完整的账户列表而非整体失败;受影响角色以明确的"无法判定"标识返回,其余角色正常展示各自状态。

**Why this priority**: 授权状态判定依赖外部 ESI 服务,网络或服务波动不应阻塞整个账户列表的查看。此保障确保功能在非理想环境下仍可用,但其建立在 P1 的状态判定之上,优先级次之。

**Independent Test**: 模拟 ESI 服务对某角色的状态判定超时或异常,调用 `addUser`,断言接口仍成功返回,且仅受影响角色被标记为"无法判定",其余角色状态正确。

**Acceptance Scenarios**:

1. **Given** ESI 服务对角色 A 的判定超时或异常,**When** 调用 `addUser`,**Then** 接口整体成功返回,角色 A 被标记为"无法判定",角色 B/C 的状态不受影响。
2. **Given** 用户没有任何绑定角色,**When** 调用 `addUser`,**Then** 返回空列表,接口正常成功。

---

### Edge Cases

- 当 ESI 服务整体不可达时,所有角色的状态如何呈现?(按 User Story 2 降级为"无法判定",接口不失败)
- 当 refreshToken 存在但因网络抖动瞬时无法换取 accessToken 时,是否会被误判为"授权过期"?(需明确重试或容错策略,避免抖动导致误报)
- 当一次请求中多个角色并发判定时,某角色判定耗时过长是否会拖慢整体响应?(需有超时控制)
- 状态判定过程是否会修改存储的 token?(应为只读校验,不主动清除 refreshToken,accessToken 的获取与缓存遵循既有 ESI 刷新流程)
- 同一角色短时间内被反复查询状态,是否对 ESI 造成不必要的压力?(是否需要短期抑制/缓存判定结果)
- 当用户不存在或无权查看目标 userId 时,行为是否与既有接口一致?(不应因本功能改变既有鉴权与异常语义)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `addUser` 接口返回的每个绑定角色 MUST 包含一个 ESI 授权状态字段。
- **FR-002**: 授权状态 MUST 为以下取值之一:授权正常 (Authorized)、授权过期 (Expired)、未授权 (Not Authorized) 为三种业务状态;另设“无法判定”(Unknown) 作为状态判定过程本身失败时的运维态取值。
- **FR-003**: 当角色持有 refreshToken 且能通过 ESI 成功换取 accessToken 时,状态 MUST 判定为"授权正常"。
- **FR-004**: 当角色持有 refreshToken 但无法通过 ESI 换取 accessToken 时,状态 MUST 判定为"授权过期"。
- **FR-005**: 当角色不存在 refreshToken 时,状态 MUST 判定为"未授权"。
- **FR-006**: 状态判定 MUST 为只读操作,不得主动删除或修改存储的 refreshToken(accessToken 的自然获取与缓存遵循既有 ESI 刷新流程)。
- **FR-007**: 当某一角色的状态判定发生异常(含 ESI 不可达、超时),接口 MUST 仍成功返回,该角色的授权状态取值为“无法判定”(Unknown);不得因单角色判定失败而使整条接口失败。
- **FR-008**: 当用户无绑定角色时,接口 MUST 返回空列表并成功响应。
- **FR-009**: 状态判定 MUST 对每个角色独立进行,任一角色的判定结果不得影响其他角色。
- **FR-010**: 接口 MUST 仅对具备查看目标用户账户权限的已认证调用方开放(遵循既有 RBAC 与白名单)。
- **FR-011**: 状态判定不得改变 `addUser` 接口既有的添加/返回语义,授权状态仅作为返回条目的附加字段。

### Key Entities *(include if feature involves data)*

- **用户账户 (User Account)**: 系统中的用户,可绑定一个或多个 EVE Online 角色。本功能不改变其既有结构,仅在其返回视图中附加角色的授权状态。
- **绑定角色 (Bound Character)**: 通过 ESI OAuth 绑定到用户账户的 EVE Online 角色。每个角色独立持有一份 ESI 授权凭证(refreshToken),其授权状态为本功能新增的派生属性。
- **ESI 授权状态 (ESI Authorization Status)**: 绑定角色相对 ESI 数据服务的授权有效性,取值为 {授权正常, 授权过期, 未授权} 三种业务状态,及“无法判定”(Unknown) 这一判定过程失败时的运维态。状态由“是否存在 refreshToken”与“refreshToken 能否换取 accessToken”两个判定派生;当判定过程本身不可完成(如 ESI 不可达)时取“无法判定”。

## Success Criteria *(mandatory)*

### Performance Criteria
- **SC-001**: 即便每个绑定角色都需独立判定授权状态,接口 95 百分位响应时间须保持在用户可接受范围内(多角色场景下不显著劣于既有响应体验)。
- **SC-002**: 单角色状态判定须有超时控制,避免单个角色的 ESI 调用无限期拖慢整体响应。
- **SC-003**: 状态判定不得在常规使用中产生不必要的重复 ESI 调用(对同一角色短时间内的重复查询应有合理抑制策略)。

### Code Quality Criteria
- **SC-004**: 新增代码单元测试覆盖率 ≥80%。
- **SC-005**: 集成测试 MUST 覆盖全部三种授权状态(授权正常 / 授权过期 / 未授权)及判定异常的降级路径。
- **SC-006**: 测试 MUST 验证状态判定为只读,不修改存储的 refreshToken。
- **SC-007**: 授权状态判定逻辑应可独立测试(不依赖真实 ESI 网络,可模拟 ESI 响应)。

### Measurable Outcomes
- **SC-008**: 100% 的 `addUser` 返回条目都附带准确的授权状态字段(或降级标识)。
- **SC-009**: 用户无需离开账户列表视图即可识别需要重新授权的角色。
- **SC-010**: 因授权失效导致的后台数据同步失败,可通过本功能的前置状态展示被提前发现,降低被动告警量。

## Assumptions

- "角色"指通过 ESI OAuth 绑定到用户账户的 EVE Online 角色(与项目"角色/军团"领域一致)。
- `addUser` 接口既已返回用户账户列表;本功能仅在每个返回条目上附加授权状态字段,不改变接口既有的添加/返回语义。
- 授权状态通过既有 ESI OAuth 刷新流程判定:尝试用存储的 refreshToken 换取 accessToken,依据结果映射为三种状态(实时判定,不在本功能中引入长期缓存)。
- 状态判定为只读;不主动清除失效 refreshToken(除非既有刷新流程在失败时已有清理约定,以既有行为为准)。
- 调用方为已认证且具备查看目标用户账户权限的管理员或用户(遵循既有 RBAC 与白名单)。
- ESI 服务可用性不在本功能控制范围内;接口须在 ESI 不可达时优雅降级——受影响角色状态取“无法判定”(Unknown),接口不失败(见 FR-007)。
- 瞬时网络抖动导致的 refreshToken 换取失败,默认按"授权过期"判定;如需重试容错以避免误报,留待 `/speckit-clarify` 或计划阶段确认。
