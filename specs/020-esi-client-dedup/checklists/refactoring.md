# Checklist: 020 ESI 客户端重复代码消除（重构质量验证）

**Purpose**: 验证 tasks.md 的任务完整性、TDD 合规性、依赖排序、验收标准
**Created**: 2026-08-26
**Feature**: 020-esi-client-dedup (Phase 1)

---

## Requirement Completeness（任务完整性）

- [ ] CHK001 是否覆盖所有 204+ 处 `.onStatus().onStatus()` 重复？[Completeness, Spec §目标与范围]
- [ ] CHK002 是否覆盖所有 19 处 `exchangeToMono` 块？[Completeness, Spec §目标与范围]
- [ ] CHK003 是否覆盖所有 20 处 `ESI_DATA_TIMEOUT` 常量声明？[Completeness, Spec §目标与范围]
- [ ] CHK004 是否明确列出所有 33 个 API 类的改造任务？[Completeness, Plan §Source Code]

## TDD Compliance（TDD 合规性）

- [ ] CHK005 T002（单元测试）是否明确先于 T003（工具方法实现）？[Consistency, Tasks §Phase 3]
- [ ] CHK006 测试任务是否覆盖所有分支（2xx/4xx/403/5xx）？[Completeness, Tasks §T002]
- [ ] CHK007 是否要求测试失败（RED）后再实现（GREEN）？[Consistency, Tasks §Phase 3]

## Dependency Ordering（依赖排序）

- [ ] CHK008 T001（常量类）是否标注为 Phase 2 Foundational（阻塞后续任务）？[Consistency, Tasks §Phase 2]
- [ ] CHK009 Phase 4（API 类改造）是否依赖 Phase 3（工具方法）完成？[Consistency, Tasks §Phase Dependencies]
- [ ] CHK010 Phase 6（全量验证）是否依赖 Phase 4 + Phase 5 完成？[Consistency, Tasks §Phase Dependencies]

## Acceptance Criteria Quality（验收标准质量）

- [ ] CHK011 T024-T025 是否提供具体的 grep 命令验证无遗漏？[Measurability, Tasks §Phase 6]
- [ ] CHK012 T023 是否明确要求 `mvn test` → 0 failures, 0 errors？[Measurability, Tasks §Phase 6]
- [ ] CHK013 T027 是否要求产出 VERIFICATION REPORT？[Measurability, Tasks §Phase 6]

## Scenario Coverage（场景覆盖）

- [ ] CHK014 是否覆盖 `.block()` 特殊场景（PageTotalApi）？[Coverage, Tasks §T018]
- [ ] CHK015 是否覆盖 EsiApiService 的 ESI_DATA_TIMEOUT 引用替换？[Coverage, Tasks §Phase 5]

## Edge Case Coverage（边缘场景覆盖）

- [ ] CHK016 是否考虑日志简化对调试的影响（trace ID 保留）？[Edge Case, Spec §风险与缓解]
- [ ] CHK017 是否考虑 015 缓存需求（Auth header 注入保留现状）？[Edge Case, Spec §不处理]

## Non-Functional Requirements（非功能需求）

- [ ] CHK018 是否要求整体测试覆盖率 ≥80%（宪法要求）？[NFR, Tasks §Testing & Quality Gates]
- [ ] CHK019 是否要求 EsiStatusUtilTest 覆盖率 ≥90%（工具方法高覆盖）？[NFR, Tasks §Testing & Quality Gates]

## Dependencies & Assumptions（依赖与假设）

- [ ] CHK020 是否假设 Spring WebFlux API（ResponseSpec, ClientResponse）可用？[Assumption, Plan §Technical Context]
- [ ] CHK021 是否假设不引入新依赖（宪法 IV 技术栈冻结）？[Assumption, Plan §Constraints]

---

## Checklist Summary

**Total items**: 21
**Focus areas**: 任务完整性、TDD 合规性、依赖排序、验收标准
**Depth level**: Standard（重构任务，需求已明确）
**Actor/timing**: Reviewer (PR) / P3 阶段结束时

**Pass criteria**: 所有 CHK 项通过（或明确标注 N/A 并说明理由）
