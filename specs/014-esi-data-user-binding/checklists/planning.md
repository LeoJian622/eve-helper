# Planning & Implementation-Ready Checklist: ESI 数据 × 系统用户强关联

**Purpose**: Validate that spec / plan / data-model / tasks are complete, consistent, and unambiguous before implementation (G3 gate)
**Created**: 2026-08-19
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [tasks.md](../tasks.md)

> 性质：**需求/计划质量核验**（unit tests for requirements），不核验实现正确性。逐项对 spec/plan/tasks 实际内容如实勾选；有 Gap 即回修工件后重跑，直至全部通过。

## Requirement Completeness

- [x] **CHK001** Are all 8 target ESI tables covered by the `user_id` schema, with `market_order` (public market data) explicitly excluded? [Completeness, spec §FR-001/Assumptions, data-model.md]（8 表全列：assets/blueprints/industry_job/mining_detail/observer/structure/wallet_journal/wallet_transaction；market_order 标明不在范围）
- [x] **CHK002** Is the `user_id` source on the write path (from `eveAccount.getUserId()`) identified for every active chain, and are dormant tables (observer/blueprints) flagged as schema-only (YAGNI)? [Completeness, plan Phase C + research §一] （6 条活跃链路逐一落在 T006–T009/T013–T016；休眠边界在 research & tasks 注明）
- [x] **CHK003** Are all `AccessGuard` call sites classified (character-dimension vs corporation-dimension) and every one assigned a task? [Completeness, research §一 + tasks T017–T020/US1] （11 人物调用点零改动但属实 + 9 军团调用点分派 T017–T020；休眠表蓝prints 守卫已含 US1 蓝图清单）
- [x] **CHK004** Is the migration backfill target policy (人物→角色属主、军团→管理域、孤儿兜底、0 丢失) fully specified for all 8 tables? [Completeness, spec §FR-008 + plan Phase E] （T024 枚举全部 UPDATE；含孤儿兜底与 0 丢失断言 T025）

## Requirement Clarity

- [x] **CHK005** Is the "军团读统一空 200 vs 硬 403" decision explicit and reconciled with spec's "被拒"/"拒绝" wording? [Clarity/Consistency, spec §FR-003/FR-007 + plan 读路径裁决] （plan 明示"统一空不硬 403，防空 oracle"，与 FR-007 意图对齐；G2 已随批）
- [x] **CHK006** Is the `user_id` column type (BIGINT/Long) and the single Integer↔Long conversion point (`corporationScope`) explicit? [Clarity, plan Tech Context + D1] （类型对齐 eve_account.user_id；转换收敛于 corporationScope 一处，避免散落）
- [x] **CHK007** Is the character vs corporation row split during migration unambiguous given overlapping `division` values (legacy char division NULL/1 vs corp 1-7)? [Clarity, plan Phase E] （以 owner_id 命中 character_id→人物 vs 命中 corp_id→军团 归属；wallet_transaction 以 owner_type 列直接区分；division 不作为分界主判据，消歧明确）

## Requirement Consistency

- [x] **CHK008** Do character-dimension reads consistently stay owner-scoped (no `user_id` filtering), matching FR-004 multi-owner share? [Consistency, plan D4 + FR-004] （人物读不变，守卫 `requireOwnership(charId)`；不引入 user_id 谓词，与角色共享语义自洽）
- [x] **CHK009** Are corporation-dimension reads consistently applying the `user_id` predicate across all 9 call sites? [Consistency, plan D3 + tasks T017–T020] （军团流水/交易/总览/建筑 6 处 9 调用点统一 `corporationScope`→仓储谓词，签名与 XML `<if userId>` 模式一致）
- [x] **CHK010** Are dormant-table (observer/blueprints) scope boundaries consistent between plan, data-model, and tasks? [Consistency] （三处均标注 schema-only，无行为任务牵连）

## Acceptance Criteria Quality

- [x] **CHK011** Are SC-001~SC-008 each mapped to a task and an integration/unit test? [Measurability, spec §SC + tasks] （人物/军团/管理域/迁移四旅程各 ≥1 集成测试：CharacterPrivacyIT/CorporationPrivacyIT/AdminDomainExemptionIT/MigrationBackfillTest，对应 SC-008，且覆盖 SC-001~007 安全断言）
- [x] **CHK012** Is FR-007 (rejection must not leak data existence/size) given a concrete, testable acceptance assertion? [Measurability, spec §FR-007 + tasks T010/T021] （集成测试断言乙收到空/统一响应、不泄漏数据规模）

## Scenario & Edge Case Coverage

- [x] **CHK013** Is the same-character multi-owner share (FR-004) specifically covered by a test? [Coverage, spec §Edge Case + tasks]（US1 集成 T010 覆盖甲持 A 场景；人物读不做 user_id 切分即自然满足多属主共享，配合守卫单测）
- [x] **CHK014** Is the "同一军团数据被多人各自同步→各见其部分（碎片化）" scenario (FR-003 AC3) covered? [Coverage, spec §Edge Case + tasks T021]（CorporationPrivacyIT 断言甲只见自己同步部分不并入他人）
- [x] **CHK015** Is migration idempotency / re-runnability specified (avoid duplicate-column error on re-run)? [Edge Case, plan Phase E + tasks T026]（`IF NOT EXISTS` 语义或幂等标注 + T026 可重跑核对）
- [x] **CHK016** Is the unknown/legacy `user_id` NULL state (pre-migration) explicitly handled so reads don't crash? [Edge Case, data-model + plan]（列可空 NULL；军团读谓词对 NULL 行在迁移前不影响既有功能，迁移后兜底归管理域）

## Non-Functional Requirements

- [x] **CHK017** Is the performance impact of the new `user_id` predicate stated (index-命中, no full scan) and given a verify task? [NFR, plan Tech Context + tasks T027]（等值谓词命中 owner_id 索引；T027 EXPLAIN 核验，不新增复合索引决策已定）
- [x] **CHK018** Are security requirements (owner-scoping, denial uniformity, no oracle) explicit in both spec and plan? [NFR-Security, spec §FR-003/006/007 + plan]（防守链路完整：owner 守卫/私有谓词/ROOT 豁免/统一空响应）
- [x] **CHK019** Is a security review gate mandated for this auth-hardening change? [NFR, plan Constitution Check + tasks T029]（`ecc:security-reviewer` 明确纳入 T029/G6）

## Dependencies & Assumptions

- [x] **CHK020** Are key assumptions documented: character/corp overflow (Integer), ROOT=ADMIN, dormant tables, migration manual-deploy (no Flyway)? [Dependencies/Assumptions, spec §Assumptions + plan Tech Context]（逐一落在 spec Assumptions 与 plan Constraints；迁移手动执行符合仓库惯例）
- [x] **CHK021** Is the ROOT admin `user_id` target for migration specified as a deployment placeholder (`?`), not hardcoded? [Assumption, plan Phase E + tasks T024]（军团行回填 `?` 占位，部署时填）

## Notes

- 无未勾选项：spec/plan/data-model/tasks 在上述 21 维度均满足质量要求，G3 通过。
- 关键裁决已在 plan「读路径裁决」与 tasks「关键裁决」标注并随 G2 获批：军团读统一空 200、user_id=BIGINT、迁移手动部署。
- 无 [NEEDS CLARIFICATION] 残留（P1 已消除）；无 TBD/TODO 占位（writing-plans 标准）。