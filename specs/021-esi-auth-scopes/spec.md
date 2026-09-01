# Feature Specification: 用户角色列表返回 ESI 已授权权限（scopes）

**Feature Branch**: `021-esi-auth-scopes`
**Created**: 2026-09-01
**Status**: Approved (G1, 2026-09-01)
**Input**: User description: "用户角色列表接口返回 ESI 已授权权限（scopes）：GET /user/{userId} 的响应（UserAccountDTO）新增 scopes 字段，返回该角色 ESI accessToken JWT 中 scp claim 解析出的原始 scope 名列表；仅在角色存在可用 accessToken（AUTHORIZED 状态，含 Redis 缓存或状态判定时刚刷新的 token）时解析，EXPIRED/NOT_AUTHORIZED/UNKNOWN 时为 null；不落库、不改表结构（无 DDL）；scp claim 需兼容数组与空格分隔字符串两种格式；前端用于展示该用户下各角色的授权详情。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 查看角色的 ESI 授权详情 (Priority: P1)

用户（或 ROOT 管理员）打开用户角色列表页面，除每个角色的 ESI 授权状态（AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN）外，还能直接看到该角色**已授权的 ESI 权限明细（scopes）**——即该角色当前有效 accessToken 实际携带的权限集合（如 `esi-wallet.read_character_wallet.v1`）。前端据此渲染"该用户下该角色的授权详情"，用户无需另行查询或猜测该角色被授予了哪些数据访问权限。

**Why this priority**: 这是本特性的全部核心价值——一次接口调用同时拿到授权状态与权限明细，无需追加请求。

**Independent Test**: 调用用户角色列表接口，对 authStatus=AUTHORIZED 的角色断言响应含非 null 的 scopes 列表，且内容与该角色 accessToken 中声明的权限集合一致。

**Acceptance Scenarios**:

1. **Given** 某角色存在有效 accessToken 且判定为 AUTHORIZED，**When** 查询该用户角色列表，**Then** 该角色的 scopes 为非 null 列表，内容等于其 accessToken 中声明的权限集合（原始 scope 名）
2. **Given** 角色刚完成 ESI 授权或刷新，**When** 查询角色列表，**Then** 返回的 scopes 与最新 token 中声明的权限一致
3. **Given** 用户绑定多个角色，**When** 查询角色列表，**Then** 每个角色独立携带各自的 scopes，互不串扰

---

### User Story 2 - 授权不可用时的明确降级 (Priority: P2)

当角色 ESI 授权不可用（EXPIRED/NOT_AUTHORIZED）或状态无法判定（UNKNOWN）时，该角色的 scopes 为 null——前端据此显示"无权限详情"，配合既有 authStatus 提示用户重新授权。系统不为获取 scopes 发起任何额外上游调用，也不改变现有状态判定的语义与性能。

**Why this priority**: 保证降级正确、不引入隐藏成本，是 P1 交付健壮性的必要组成。

**Independent Test**: 对无 refreshToken 的角色与刷新失败（EXPIRED/UNKNOWN）的角色分别调用接口，断言 scopes 为 null，且不产生任何额外的上游网络调用。

**Acceptance Scenarios**:

1. **Given** 角色 refreshToken 为空（NOT_AUTHORIZED），**When** 查询角色列表，**Then** 该角色 scopes 为 null
2. **Given** 角色刷新返回授权失效（EXPIRED），**When** 查询角色列表，**Then** 该角色 scopes 为 null
3. **Given** 角色状态因服务端/网络异常判定为 UNKNOWN，**When** 查询角色列表，**Then** 该角色 scopes 为 null，且接口整体正常返回其他角色数据

---

### Edge Cases

- 角色状态缓存为 AUTHORIZED 但 accessToken 缓存已被清除（如缓存服务重启）→ scopes 返回 null（无 token 可解析），authStatus 保持 AUTHORIZED 不变
- accessToken 缺失 scp claim 或内容损坏、无法按任一已知格式解析 → scopes 返回 null（fail-soft），不影响 authStatus 与其他角色
- scp claim 为空集合/空字符串 → scopes 返回空列表（token 有效但未声明任何受限权限），与 null（无法确定）语义区分
- accessToken 的 scp 为数组格式或空格分隔字符串格式（两种历史格式）→ 解析结果一致
- 多角色中某一角色解析失败 → 仅该角色 scopes 为 null，其他角色不受影响（逐角色隔离，与现有状态判定隔离策略一致）
- ROOT 查询他人用户 → scopes 口径与本人查询完全一致

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 用户角色列表接口响应中每个角色 MUST 包含 scopes 字段，类型为权限名列表（可 null）
- **FR-002**: 当角色 authStatus 为 AUTHORIZED 且存在可用 accessToken（缓存命中或本次状态判定中取得）时，System MUST 返回从中解析出的已授权权限集合（原始 ESI scope 名，如 `esi-assets.read_assets.v1`）
- **FR-003**: 当角色 authStatus 为 EXPIRED、NOT_AUTHORIZED 或 UNKNOWN 时，System MUST 返回 scopes 为 null
- **FR-004**: System MUST 兼容 accessToken 中 scp claim 的两种已知格式：JSON 字符串数组与空格分隔字符串
- **FR-005**: scopes 解析的任何失败（token 缺失、损坏、格式未知）MUST 以 null 降级，不得抛错、不得影响 authStatus 与其他角色
- **FR-006**: System MUST NOT 为获取 scopes 发起新的上游（ESI）网络调用或 refreshToken 刷新；scopes 只能从现有状态判定/取 token 流程已产出或已缓存的数据中解析
- **FR-007**: System MUST NOT 持久化 scopes（不改表结构、不落库），每次响应时即时解析
- **FR-008**: 接口访问控制 MUST 保持现状：仅允许查询本人角色列表；ROOT 可查询任意用户（既有 IDOR 防护不变）
- **FR-009**: scopes 响应内容 MUST 仅含权限名，MUST NOT 包含任何 token 值、签名或其他凭据材料

### Key Entities *(include if feature involves data)*

- **角色（EveAccount）**: 现有绑定角色实体；本特性不新增任何持久化字段，scopes 为响应层即时派生数据
- **accessToken**: ESI 签发的短期令牌（缓存层保存，约 19 分钟有效期）；其声明中的已授权权限集合（scp）是本特性唯一数据来源

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 前端调用用户角色列表接口一次即可同时获得每个角色的授权状态与已授权权限明细，无需追加请求
- **SC-002**: 在现有部署规模下（单用户角色数为个位到数十个），接口 p95 响应时间与现状持平（缓存命中路径 <200ms），新增字段不引入可测的延迟退化
- **SC-003**: 新增代码单元/集成测试覆盖率 ≥80%
- **SC-004**: 用户在界面上能明确区分"已授权（可查看具体权限列表）"与"授权失效/未授权（无权限详情）"两类角色，无需额外操作
- **SC-005**: 同一权限集合的两种 scp 格式表述（数组 / 空格分隔字符串）解析结果一致

## Assumptions

- 前端负责将原始 scope 名翻译为展示文案；后端不提供中文描述映射（用户已确认）
- scopes 不落库，EXPIRED 角色不返回"最近一次授权"的历史 scopes（用户已确认运行时口径）
- EVE SSO accessToken 为可解析令牌且其声明中含已授权权限字段（与现有对角色名/角色ID 声明的解析一致）；实现时需以真实晨曦（Serenity）token 实测验证字段格式
- 原始 scope 名的格式与语义遵循 EVE SSO 官方定义，后端不做过滤/排序/翻译
- 多角色状态判定沿用现有并行执行与超时隔离机制；scopes 解析为本地解析操作，不进入并行等待预算
