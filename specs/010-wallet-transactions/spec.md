# Feature Specification: Wallet Transactions（钱包交易流水）

**Feature Branch**: `010-wallet-transactions`
**Created**: 2026-08-18
**Status**: Draft
**Input**: User description: "为 EVE Online 玩家补齐钱包交易（Wallet Transactions）数据模块：基于已就位的 ESI 人物/军团交易流水底层 API，新增人物+军团 transactions ESI 端口、持久化表、应用服务、控制器（手动同步 + 分页查询）、domain VO。范围经澄清锁定为人物+军团，军团侧全 division(1-7) 同步。沿用现有模式：归属鉴权防 IDOR、手动同步+幂等 upsert、规范分页写法。技术栈冻结(Java17/SpringBoot3.5.14/MyBatisPlus/Redis)。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 同步并分页查看人物钱包交易流水 (Priority: P1)

作为 EVE 玩家，我希望只为某个角色手动发起钱包交易流水同步，然后分页查看其买入/卖出流水（交易时间、物品、数量、单价、方向、对手方），以便追踪该角色的市场经济行为。

**Why this priority**: 人物侧 ESI 底层（`queryCharacterWalletTransactions` 含 maxPage）已具备，与 009 人物 journal 完全同构，骨架可直接复用，是 010 的最小可交付单片。

**Independent Test**: 可独立测试。对单个角色执行同步后能从数据库分页读取其交易流水；未同步时返回空而非报错；无权访问角色被拒绝；重复同步不产生重复行。

**Acceptance Scenarios**:

1. **Given** 当前用户有权访问某角色，**When** 请求同步该角色钱包交易流水并成功，**Then** 系统从 ESI 拉取交易流水写入数据库并返回同步结果。
2. **Given** 已同步过的角色，**When** 再次同步，**Then** 不产生重复记录（幂等 upsert），且能增量拉取新增交易。
3. **Given** 已同步数据的角色，**When** 分页查询其钱包交易流水，**Then** 返回含总数与每笔（交易时间、物品类型、数量、单价、方向、对手方、关联 journal）的分页结果，按时间倒序。
4. **Given** 当前用户**无权**访问某角色，**When** 请求同步或查询该角色交易流水，**Then** 请求被拒绝，不返回任何数据。
5. **Given** 某角色从未同步过钱包交易，**When** 查询，**Then** 返回空分页结果（可提示前端先同步），而非报错。

---

### User Story 2 - 遍历全 division 同步并分页查看军团钱包交易流水 (Priority: P2)

作为军团负责人或管理员，我希望按军团的所有钱包分账（division 1–7）手动发起交易流水同步，然后分页查看任一 division 的买入/卖出流水，以便核对军团各账目的市场资金进出。

**Why this priority**: 军团侧 ESI 底层仅有 from_id 游标拉取 API（无 maxPage），需按 division 循环 1–7 逐账同步；本特性以「全 division」为已澄清范围，是军团钱包完备性的关键补充。

**Independent Test**: 可独立测试。对某军团同步后，各 division 的交易流水写入数据库并可按 division 分页查询；division 无数据时不报错；无权访问军团被拒绝；重复同步不产生重复行。

**Acceptance Scenarios**:

1. **Given** 当前用户有权访问某军团，**When** 请求同步该军团钱包交易流水，**Then** 系统遍历 division 1–7 从 ESI 拉取交易写入数据库并返回同步结果（含各 division 状态）。
2. **Given** 已同步过的军团，**When** 再次同步，**Then** 不产生重复记录（幂等 upsert），且能增量拉取新增交易。
3. **Given** 某军团 division 2 有交易数据，**When** 按该 division 分页查询，**Then** 返回该 division 的交易流水分页结果（按时间倒序），并可按 division 过滤。
4. **Given** 某军团某 division 无交易记录，**When** 同步或查询该 division，**Then** 不报错；查询返回空分页结果。
5. **Given** 当前用户**无权**访问某军团，**When** 请求同步或查询该军团交易流水，**Then** 请求被拒绝，不返回任何数据。

---

### Edge Cases

- 同步过程中 ESI 调用失败（网络异常 / 令牌失效 / 限流）：返回清晰错误，不写入半截数据；已存在交易流水不受影响。
- 军团 division 循环中某 division 调用失败：该 division 报错但**已成功的其他 division 数据须保留**（非原子整体回滚）；错误信息须指明失败的具体 division。
- 多角色/军团数据隔离：任何用户不得越权读到他人角色或军团的交易流水。
- 同步重复触发（连续点同步）：不产生重复交易记录。
- 军团全 division 同步量大：分页稳定，单页大小有限制；同步耗时可较长，前端须有明确的同步中/完成反馈。
- 军团交易量特别大导致单 division 超过单页拉取上限：同步策略须支持 from_id 游标翻页直至拉完或达到上限，不得截断。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 允许用户对某个其有权访问的角色发起钱包交易流水同步，从外部数据源拉取并持久化（人物侧）。
- **FR-002**: 系统 MUST 允许用户对某个其有权访问的军团发起钱包交易流水同步，遍历 ESI 军团钱包全分账（division 1–7）拉取并持久化（军团侧）。
- **FR-003**: 系统 MUST 允许用户分页查询某角色/某军团某 division 的钱包交易流水，结果 MUST 返回总数及每笔的交易时间、物品类型、数量、单价、方向、对手方、关联 journal。
- **FR-004**: 钱包交易同步 MUST 幂等——重复同步不产生重复记录，且能增量拉取新增交易。
- **FR-005**: 系统 MUST 对角色/军团归属强制鉴权——用户只能访问其自身或其所在军团拥有的数据；越权访问 MUST 被拒绝且不泄漏数据（未认证 401，已认证无权 403）。
- **FR-006**: 军团 division 全扫同步中，单 division 失败 MUST 不导致整批回滚、不丢失已成功的其他 division 数据；错误信息 MUST 指明失败 division。
- **FR-007**: 系统 MUST 区分角色与军团两类交易流的归属与展示维度：人物侧按角色归属，军团侧按军团+division 归属；存储 MUST 保留该区分（如 owner_type + owner_id + division）。
- **FR-008**: 军团侧同步 MUST 采用 from_id 游标翻页策略拉全单 division（因底层 ESI 无 maxPage），不得截断；达到单页上限 MUST 继续翻页直至拉完。
- **FR-009**: 对从未同步的角色/军团执行查询，系统 MUST 返回空分页结果而非错误。
- **FR-010**: 同步失败时系统 MUST 返回清晰错误，MUST NOT 写入半截/脏数据；已存在交易流水不受影响。

### Key Entities *(include if feature involves data)*

- **钱包交易流水（Wallet Transaction）**: 某角色或某军团某分账发生的一笔买入/卖出市场交易记录，含交易时间、物品类型（type_id）、数量、单价、方向（is_buy）、对手方（client_id）、关联 journal（journal_ref_id）、位置（location_id）；按 owner_type+owner_id（+division）归属，随时间演进。
- **角色（Character）**: EVE 游戏角色，与用户有绑定关系，是人物侧交易流水归属主体。
- **军团（Corporation）**: EVE 游戏军团；交易流水按 division（1–7 分账）归属；用户经军团成员关系访问军团资源。

## Success Criteria *(mandatory)*

### Performance Criteria
- **SC-001**: 角色/军团交易流水分页查询单页请求 MUST ≤ 500ms（95th percentile）。
- **SC-002**: 外部数据源调用 MUST 具备超时与失败处理，不得因远端异常拖垮主流程或写入脏数据。
- **SC-003**: 军团全 division 同步 MUST 可并发或渐进完成，不因单 division 失败而全部丢弃。

### Code Quality Criteria
- **SC-004**: 新增代码单元测试覆盖率 MUST ≥ 80%。
- **SC-005**: 集成测试 MUST 覆盖核心用户旅程（人物同步、军团全 division 同步、分页、越权拒绝、空数据、幂等）。
- **SC-006**: 所有鉴权与授权流 MUST 有安全测试覆盖。

### Measurable Outcomes

- **SC-007**: 玩家能为其任一角色独立完成交易流水同步并分页核对买入/卖出，重复同步不产生重复流水。
- **SC-008**: 军团负责人能按任一 division 分页查看军团交易流水，掌握各账目资金进出。
- **SC-009**: 任何用户在无权限/无数据边界下操作，都不会看到越权数据或得到难以理解的错误；军团同步失败会告知具体 division。

## Assumptions

- 交易流水数据取自 EVE ESI 官方接口，玩家需完成该角色/军团授权（具备对应数据访问权限）才能同步。
- 人物侧 ESI 底层已有 maxPage 端点（`queryCharacterWalletTransactionsMaxPage`）；若实测 maxPage 不可用，按人物 journal 的 page 翻页方式兜底。
- 军团侧 ESI 底层仅有 from_id 游标 API（无 maxPage），同步以 from_id 翻页拉全单 division；人物侧若同样可用 from_id 游标则优先采用，以与军团侧策略统一。
- division 有效范围为 1–7；本轮不引入 division 配置化，遍历写死 1–7（与用户澄清一致）。
- 数据模型新增 `wallet_transaction` 表（含 owner_type / owner_id / division），人物侧 owner_type='character'、division=0；军团侧 owner_type='corporation'、division=1..7。
- 现有角色/军团归属鉴权策略（含军团成员可访问本军团资源）继续沿用，不做扩展或放宽。
- 本特性与 009 完全平行但独立交付；不修改 wallet_journal 表任何结构。
- 技术栈遵循项目冻结版本（Java 17 / Spring Boot 3.5.14 / MyBatis Plus / Redis），不做任何升级或替换。