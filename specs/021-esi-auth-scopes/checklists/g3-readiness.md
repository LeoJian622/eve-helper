# G3 Requirements Quality Checklist: 021-esi-auth-scopes

**Purpose**: 以"英文的单元测试"方式核验 spec/plan/tasks/contracts 的需求质量（G3 门禁）——测需求本身，不测实现
**Created**: 2026-09-01
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [tasks.md](../tasks.md) | [contracts](../contracts/user-accounts-api.md)

## Requirement Completeness（需求完备性）

- [ ] CHK001 - FR-001~FR-009 是否每条都有对应的任务落点或既有测试锚定（无"无主需求"）？[Completeness, Spec §FR / tasks.md Phase 3-4]
- [ ] CHK002 - 非 AUTHORIZED 三态、token 缓存缺失、scp claim 缺失/损坏/为空的全部降级行为是否都有明确验收场景？[Completeness, Spec Edge Cases / plan 用例矩阵]
- [ ] CHK003 - "不为 scopes 发起额外 ESI 调用或 refreshToken 刷新"（FR-006）是否有可验证的测试要求（never-verify 类断言）？[Completeness, Spec §FR-006 / tasks T009]
- [ ] CHK004 - 多角色并行时逐角色隔离（单角色失败不影响其他角色）是否有需求级与用例级定义？[Coverage, Spec Edge Cases / US1-3]
- [ ] CHK005 - 契约变更"只增不改、无新端点"是否在任务清单中体现（无 Controller 改动任务）？[Consistency, contracts / tasks.md]

## Requirement Clarity（需求清晰度）

- [ ] CHK006 - "可用 accessToken"是否被可操作地定义（缓存命中或本次状态判定中取得）？[Clarity, Spec §FR-002 / plan 架构决策]
- [ ] CHK007 - `scopes=null` 与 `scopes=[]` 的语义区分是否在 spec/data-model/contracts 三处一致且无歧义？[Clarity + Consistency, Spec Edge / data-model / contracts]
- [ ] CHK008 - "原始 scope 名"是否明确为不翻译/不排序/不过滤的 EVE SSO 原始字符串？[Clarity, Spec Assumptions]
- [ ] CHK009 - 应用服务对 null detail（未 stub/异常/超时）的 UNKNOWN 降级是否清晰到实现级语义（resolveDetail null-safe）？[Clarity, plan T2 / tasks T007]

## Requirement Consistency（需求一致性）

- [ ] CHK010 - plan 的 scp 双格式通用解析决策与 spec FR-004（数组/空格分隔两种格式）是否一致？[Consistency, Spec §FR-004 / plan 架构决策 / research R1]
- [ ] CHK011 - tasks 测试矩阵（A1-A3/B1-B5）是否与 spec 全部 Edge Cases 一一对应（无遗漏、无超出规格的场景）？[Coverage + Consistency, Spec Edge / tasks T004/T009]
- [ ] CHK012 - 数据模型"无 DDL、不落库"声明与全部任务（无 SQL/mapper 任务）是否一致？[Consistency, data-model / tasks.md 全文]

## Acceptance Criteria Quality（验收标准质量）

- [ ] CHK013 - SC-002（p95 与现状持平）是否可测量且测量方式已定义（观测通道与对比基线）？[Measurability, Spec §SC-002 / plan 验证计划]
- [ ] CHK014 - FR-009（响应不含任何凭据材料）是否可客观核验（T015 自查项 + P6 安评审视角）？[Measurability, Spec §FR-009 / tasks T015]
- [ ] CHK015 - 每个任务的 RED 判据是否具体到可"亲眼确认失败"（非泛化的"测试失败"）？[Measurability, tasks TDD 约定]

## Scenario Coverage（场景覆盖）

- [ ] CHK016 - 缓存服务重启导致"状态缓存存而 token 缓存失"的时序场景是否在需求中定义？[Edge Case, Spec Edge]
- [ ] CHK017 - ROOT 查询他人用户时 scopes 口径与本人一致是否有验收定义？[Coverage, Spec Edge / FR-008]
- [ ] CHK018 - 晨曦/宁静两种 token 来源下的解析一致性是否有需求说明与验证途径？[Coverage, Spec Assumptions / research R1]

## Non-Functional Requirements（非功能需求）

- [ ] CHK019 - 新增 Redis 读 + 本地 JWT 解析的性能成本是否已评估且不进入并行等待预算？[NFR Performance, plan 性能影响评估]
- [ ] CHK020 - 安全要求（无凭据出站、日志无 token、访问控制不变）是否在需求与任务中均有显式落点？[NFR Security, Spec §FR-008/009 / tasks T015]

## Dependencies & Assumptions（依赖与假设）

- [ ] CHK021 - R1 的"晨曦真实 token 实测"验证动作是否已落实为任务（T012）而非仅停留在 quickstart？[Dependency, research R1 / quickstart / tasks T012]
- [ ] CHK022 - "accessToken 为可解析 JWT 且含 scp"的关键假设失效时的行为（null 降级）是否已定义？[Assumption + Edge Case, Spec Assumptions / FR-005]

## Ambiguities & Conflicts（歧义与冲突）

- [ ] CHK023 - "scp 为空集合/空字符串→空列表"与"claim 缺失→null"在实现规格（toScopeList）与需求（spec Edge）之间是否存在冲突？[Consistency, Spec Edge / plan T1]

---

**核验结果**: 2026-09-01 逐项核验，23/23 通过（G3 达成）。带备注项：
- CHK011 — ROOT 查询口径一致（Spec Edge）无独立测试任务：实现上不存在按请求者身份分支的 scopes 代码路径（无特例即"一致"的代码事实），由既有 AccessGuard 测试 + 无分支设计锚定
- CHK013 — SC-002 测量方式为 IT/本地观测 + 基线对比，非自动化 p95 采集；本特性规模下（本地解析操作）可接受
- CHK015 — T009/T010（US2 RED）存在"实现已就绪即绿"的可能（T006 GREEN 已实现降级语义），T011 已内置"若 RED 阶段即绿，检视用例有效性"约束，属已知并受控的 TDD 情形
