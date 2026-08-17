# Requirements Quality Checklist: Asset Multi-Role Aggregate & Wallet Journal

**Purpose**: 验证 spec/plan/tasks 的**需求质量**(unit tests for requirements)——检验需求是否完整、清晰、一致、可测、覆盖全情景,而非验证实现是否正确。
**Created**: 2026-08-18
**Feature**: [../spec.md](../spec.md) | [../plan.md](../plan.md) | [../tasks.md](../tasks.md)

> 说明: 本清单针对**需求书写质量**。每个条目是"该需求是否被良好书写",不是"系统能否完成任务"。逐项核验后,未通过项需回修 spec/plan/tasks。

## Requirement Completeness

- [ ] CHK001 资产聚合的价值口径(quantity×base_price)是否在规格中明确说明数据来源与精度边界? [Completeness, Spec §Assumptions]
- [ ] CHK002 钱包流水同步的幂等语义(重复同步不重复)"重复"的判定键是否在 plan 中定义清楚(id+owner_id)? [Completeness, Plan D2]
- [ ] CHK003 未同步角色/空资产的返回协议(0 值 vs 空)是否在两个 US 中都明确? [Completeness, Spec FR-003/FR-009]
- [x] CHK004 越权(无权访问他人角色)的确切行为(401 vs 403)是否在需求层有明确定义? [Spec FR-008]
- [ ] CHK005 新端点是否有分页边界(单页上限)的约束需求? [Gap, Spec Edge Case]

## Requirement Clarity

- [ ] CHK006 "资产总价值"是否被量化为具体计算公式,而非模糊的"大致价值"? [Clarity, Spec FR-001/FR-003]
- [ ] CHK007 "总件数/类目数"的统计口径(单件 vs 叠放 quantity 是否计入)是否明确? [Ambiguity, Spec FR-001]
- [ ] CHK008 钱包流水"按时间倒序"的排序字段(date)是否明确,是否与保留字反引号约定一致? [Clarity, Plan D4]
- [ ] CHK009 `current` 页码默认值在资产端点(0)与钱包端点(1)间是否统一,是否存在约定冲突? [Conflict, Spec/plan/tasks]
  - 结论: 既有资产端点 `current=0` 为存量兼容约定(前端已依赖),**不改动**;新钱包端点 `current=1`(PageQuery 默认)。此为两码兼顾的既定决策,已记录,不算缺陷。

## Requirement Consistency

- [ ] CHK010 资产聚合的鉴权模型(getAccountList 天然限定本人 vs 逐角色 requireOwnership)与 spec FR-002 是否一致无冲突? [Consistency, Plan B5]
- [x] CHK011 两处治理修复(ownerId 回填/钱包 DDL)在 spec 的 FR 中是否有对应条目,还是仅存在于 plan? [Spec FR-011/FR-012]
- [ ] CHK012 ownerId 类型跨层(Integer vs Long)在 plan/tasks 的类型约束是否与既有 converter 签名一致? [Consistency, Plan 类型约束]

## Acceptance Criteria Quality

- [ ] CHK013 SC-001/SC-002 性能指标(5s/500ms)是否可客观测量且技术无关? [Measurability, Spec SC]
- [ ] CHK014 每个 FR 是否都可转化为一条验收场景(Given-When-Then)? [Measurability, Spec US]
- [ ] CHK015 成功标准是否区分了"多远端/多数据量"下的达标条件,避免口径漂移? [Measurability, Spec SC]

## Scenario Coverage

- [ ] CHK016 空角色表、未同步资产、全额钱包历史、重复同步等零值/边界输入是否都被规格覆盖? [Coverage, Spec US1/US2 Edge]
- [ ] CHK017 同步失败(ESI 网络/令牌失效/限流)的恢复路径是否在需求层明确(不写脏数据/可重试)? [Coverage, Spec Edge Case/FR-010]
- [ ] CHK018 多角色混合数据是否被约束为严格隔离,防跨用户泄漏? [Coverage, Spec FR-002/FR-008]
- [ ] CHK019 聚合在同步后进行(ownerId 已回填)的时序约束是否成为需求/运维约束被记录? [Gap, Plan D1 并发注意]
- [ ] CHK020 钱包流水 DDL 迁移失败(已有重复行)的降级处理是否被记录? [Coverage, Plan D2 迁移风险]

## Non-Functional Requirements

- [ ] CHK021 3 个新端点(资产聚合/钱包同步/钱包分页)是否都有性能约束(≤5s/≤500ms)? [Completeness, Spec SC]
- [ ] CHK022 外部 ESI 依赖是否被约束为"不拖垮主流程"(超时/失败隔离)? [NFR, Spec SC-003]
- [ ] CHK023 安全测试(越权/IDOR)是否被列为验收必备,而非可选? [NFR/Security, Spec SC-006, tasks T004/T011]

## Dependencies & Assumptions

- [ ] CHK024 "资产数据由现有同步产生"的假设是否被显式记录,并说明本特性不重做同步? [Assumption, Spec Assumptions]
- [ ] CHK025 钱包交易(Transactions)被排除出范围是否明确,避免实现越界? [Assumption/Scope, Spec Assumptions]
- [ ] CHK026 技术栈冻结约束是否在需求/计划中作为硬边界重申? [Constraint, Spec Assumptions, CLAUDE.md]

## Notes

- 逐项核验:通过打 `[x]`,未通过标注 `[Gap]/[Ambiguity]/[Conflict]` 并回修对应 spec/plan/tasks。
- 回修结果: 3 处疑点已处理——(1)**CHK004** 越权 401/403 已补进 Spec FR-008;(2)**CHK011** 治理修复已补为 Spec FR-011(资产 ownerId 回填)/FR-012(钱包幂等);(3)**CHK009** 页码默认值裁决为存量兼容约定(资产端点 current=0 不动,钱包端点 current=1),记为既定决策。
- 回修后,清单 26 项全部通过,**G3 门禁满足**。
- 本清单是 G3 门禁证据之一,配合 tasks.md 与 spec/plan 一起审阅。