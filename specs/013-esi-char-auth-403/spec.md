# Feature Specification: ESI 人物授权基准与 403 友好返回

**Feature Branch**: `feat/013-esi-char-auth-403`
**Created**: 2026-08-19
**Status**: Draft
**Input**: ESI 授权与 403 友好返回：①ESI 调用如需授权，一律以人物授权为基准，不存在联盟/军团独立授权；②前端调用接口触发的 ESI 数据读取，无论人物/军团/联盟，都应传入该军团或联盟关联的人物ID来执行 ESI 调用；③ESI 调用可能返回 403，需友好返回前端

## User Stories & Testing

### User Story 1 - 军团/联盟数据读取统一以关联人物ID执行 (Priority: P1)

用户在前端对某个军团（或联盟）发起数据读取（同步钱包流水、钱包交易等）。系统应要求调用方**传入该军团/联盟关联的一个角色ID**，用该角色在 eve_account 中记录的授权（refreshToken）换取访问令牌，并从同一记录解析出**目标军团/联盟ID**，以其对 ESI 发起请求。用户可见结果：军团数据按预期返回，访问权限校验有效。

**Why this priority**: 这是本特性核心（需求②）。人物授权是唯一事实源，军团/联盟无独立令牌。当前军团钱包流水/交易实现出现「同一 ID 既当角色又当军团」的双重语义错位，必须校正，否则军团数据读取在页面与归属校验层面不可靠。

**Independent Test**: 可通过仅对某个军团钱包同步端点做契约级测试验证——传角色ID能正确解析出军团ID并成功请求 ESI；传军团ID（或非关联ID）则明确拒绝。

**Acceptance Scenarios**:

1. **Given** 一个已绑定角色（eve_account 记录含 characterId、corpId、allianceId、refreshToken），**When** 前端对军团钱包同步传入该角色ID，**Then** 系统用该角色 token 调 ESI，且 ESI 请求使用解析出的军团ID而非角色ID，成功后返回数据。
2. **Given** 同一场景，**When** 前端传入的不是该用户角色、或角色与目标军团不关联，**Then** 请求被拒绝（404/403），不触发 ESI 调用。
3. **Given** 定时任务等无安全上下文路径，**When** 用系统身份同步军团数据，**Then** 同样以人物授权为基准解析军团ID并执行，行为与请求路径一致。

---

### User Story 2 - 人物授权为唯一基准的不变量约束 (Priority: P1)

系统任何 ESI 授权调用都以**人物**（eve_account 角色行）上的 refreshToken 为唯一 token 来源；不存在独立的「军团 token」或「联盟 token」。军团/联盟数据被视为「某人物持有的角色身份」的派生视图。用户无需（也无法）对军团单独授权。

**Why this priority**: 这是需求①，约束性的架构不变量，决定了后续所有军团/联盟数据功能的实现方式。可独立于具体端点验证——审查所有 ESI 授权入口，确认无任何不依赖人物 token 的军团/联盟授权通道。

**Independent Test**: 通过静态审查 + 授权入口测试验证——每个 ESI 授权调用点都经由某人物角色行获取令牌，不存在按 corpId/allianceId 单独的令牌获取路径。

**Acceptance Scenarios**:

1. **Given** 系统全部 ESI 授权调用点，**When** 逐一核查令牌来源，**Then** 每个都从某人物角色行（characterId + userId）获取 refreshToken 换取 accessToken，不存在独立军团/联盟授权。
2. **Given** eve_account 数据模型，**When** 核查军团/联盟相关字段，**Then** corpId/allianceId 是角色的从属属性，refreshToken 仅存在于角色行上。

---

### User Story 3 - ESI 数据接口 403 友好返回 (Priority: P2)

前端发起数据读取时，若 ESI 因权限不足返回 HTTP 403（如该人物缺少目标军团的 Director/Bookkeeper 等角色、或已离开），前端应收到**可理解、可行动的友好提示**，而非模糊的「外部服务异常，请稍后重试」。用户可见结果：明确知道是「权限/角色/成员资格」问题，可据此重新授权或确认角色。

**Why this priority**: 这是需求③，增强现有错误透传（当前 ESI 数据 403 被折叠为 HTTP 400 + 笼统文案）。相比①②是增量改进，故 P2，但不可省略。

**Independent Test**: 通过模拟 ESI 返回 403 的控制器级/服务级测试验证——断言返回的 HTTP 状态码与错误码、文案符合友好约定，且与网络/服务故障（5xx/超时）路径可区分。

**Acceptance Scenarios**:

1. **Given** ESI 数据接口返回 HTTP 403，**When** 前端调用任一触发该 ESI 读取的端点，**Then** 前端收到明确的授权失败提示（专属错误码 + 可行动文案），HTTP 状态码为 403。
2. **Given** ESI 返回 5xx 或网络超时，**When** 前端调用同一类端点，**Then** 返回与 403 不同的、表示服务/网络故障的错误，不与权限问题混淆。
3. **Given** token 刷新本身失败（refreshToken 失效），**When** 触发 ESI 调用，**Then** 走既有的授权失效（EXPIRED）路径，不与数据接口 403 混淆。

---

### User Story 4 - 现值错位实现被校正（回归保障） (Priority: P2)

对已存在的军团数据读取实现做一致性校正，使「同一 ID 既当角色又当军团」的双重语义不再出现；统一采用「传入角色ID → 从 eve_account 解析军团/联盟ID」模式，与资产/蓝图/工业/采矿/建筑等服务既有正确模式保持一致。

**Why this priority**: 保证现有功能在采用人物授权基准后行为一致、无隐性回归。P2 因可能涉及多服务同步改造（钱包流水、钱包交易）。

**Independent Test**: 通过针对被校正服务的 TDD 测试验证——服务层接收角色ID并解析目标军团ID后才调用 ESI，参数不再复用同一个值。

**Acceptance Scenarios**:

1. **Given** 军团钱包流水同步服务，**When** 传入角色ID，**Then** 用解析出的军团ID调 ESI 军团端点，且角色ID与军团ID解耦。
2. **Given** 军团钱包交易服务，**When** 传入角色ID，**Then** 行为同上。

---

### Edge Cases

- 传入的角色ID对应的 eve_account 中 corpId 为空（角色不属于任何军团）时如何处理？（校验军团ID存在）
- 传入的角色ID与目标军团不符（该角色不在目标军团）时，是否足以证明其有该军团数据访问权？（校验角色所在军团 == 目标军团）
- 定时任务/机器人等无安全上下文路径的军团数据同步，同样需人物授权基准（authorizeInternal），归属校验不能退化。
- token 刷新 4xx（refreshToken 失效）与数据接口 403 如何区分——刷新失效走既有 EXPIRED/重新授权路径，数据 403 走本特性的新 403 路径。
- 联盟数据读取当前是否存在独立端点（搜索未见 queryAlliance 类调用）——若尚无联盟端点，本特性是否需新增，或仅作为模式约定约束未来实现。

## Requirements

### Functional Requirements

- **FR-001**: 系统 MUST 以 eve_account 人物角色行的 refreshToken 作为全部 ESI 授权令牌的唯一来源，不存在独立军团/联盟令牌。
- **FR-002**: 军团（及联盟）数据读取端点 MUST 要求调用方传入一个关联角色ID，系统从该角色从属关系解析出目标军团后调 ESI。
- **FR-002a**: 全部军团/联盟同步服务（钱包流水、钱包交易、蓝图、工业、采矿、建筑、资产）MUST 统一签名为「传入角色ID → 校验归属 → 解析军团/联盟ID → 用角色 token 调 ESI」，消除「同一 ID 兼作角色与军团」语义。
- **FR-003**: 系统 MUST 校验传入角色ID归属于当前用户（或内部路径的显式操作身份），归属不符 MUST 拒绝且不触发 ESI 调用。
- **FR-004**: 系统 MUST 校验目标军团/联盟 ID 与传入角色的从属关系一致，避免以一个角色的令牌读取它无权访问的军团数据。
- **FR-005**: 系统 MUST 将 ESI 数据接口返回的 HTTP 403 映射为**新增专属失败码**（如 `ESI_AUTH_PERMISSION_LOW`）与可行动的友好文案，并响应 HTTP 403 状态码，区别于 ACCESS_UNAUTHORIZED 的应用层 403。
- **FR-006**: 系统 MUST 区分「ESI 数据权限 403」与「网络/服务故障（5xx/超时）」及「token 刷新失效」三类错误，返回不同错误码与文案。
- **FR-007**: 系统 MUST 保持无安全上下文路径（定时任务等）以显式系统身份执行时同样遵守人物授权基准，权限校验不得退化。
- **FR-008**: 系统 MUST 消除军团数据读取中「同一 ID 兼作角色与军团」的双重语义，采取**全部军团/联盟同步服务统一校正**方案（范围已澄清——见 FR-002a）。

### Key Entities

- **EveAccount（角色账户）**: 人物唯一授权载体，含 characterId、refreshToken（唯一令牌）、corpId/allianceId（从属军团/联盟）、userId（归属用户）。
- **军团数据（WalletJournal/WalletTransaction 等）**: 以角色身份派生的军团视图，读取时以「角色ID → 解析军团ID → 用角色 token」执行。

## Success Criteria

### Code Quality Criteria

- **SC-001**: 全部新代码单元测试覆盖率 ≥80%。
- **SC-002**: 军团钱包流水/交易服务经 TDD 改造后，契约测试覆盖「角色ID→解析军团ID→ESI 调用」与「归属/从属校验拒绝」路径。
- **SC-003**: 403 桥接的控制器/服务测试覆盖「数据权限 403、服务故障 5xx、token 失效」三路径可区分。

### Measurable Outcomes

- **SC-004**: 前端在收到 ESI 权限不足时，100% 场景能获得明确错误码与可行动文案（不再收到「外部服务异常，请稍后重试」这类笼统提示）。
- **SC-005**: 全部 ESI 授权入口不变量核查通过——无任何不依赖人物 token 的军团/联盟独立授权通道。
- **SC-006**: 军团钱包流水/交易同步在校正后，传入角色ID 能正常解析并完成 ESI 读取；传入非关联 ID 被可靠拒绝。

## Assumptions

- 人物（角色）授权是唯一令牌来源的现状已由代码确认（eve_account 在角色行存 refreshToken，EsiApiService.getAccessToken(characterId, userId) 取人物 token），本特性将其固化为约束而非重写。
- 联盟数据读取当前无独立端点实现；本特性先将「传关联角色ID」作扩展点约定，是否本期新增联盟端点由范围澄清决定。
- HTTP 403 决定映射为 HTTP 403 + 专属错误码（区别于 ACCESS_UNAUTHORIZED 的应用层 403）。具体错误码用共享 ResultCode 新增 ESI 专用码（如 ESI_AUTH_PERMISSION_LOW）。
- token 刷新 4xx 已走 EsiException(ESI_AUTHORIZATION_FAILURE)→EXPIRED 固定路径，本特性不改变，只确保数据接口 403 与之不混淆。
- 数据接口 403 的捕获点在 ESI 防腐层/调用服务层；错误透传经应用服务 → 全局异常处理器映射 HTTP 状态。