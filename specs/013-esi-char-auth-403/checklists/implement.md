# Implementation Quality Checklist: ESI 人物授权基准与 403 友好返回

**Purpose**: 校验 013 的**任务可执行性**(规格覆盖/依赖序/测试完备/范围边界/NFR/安全约束),G3 门禁证据
**Created**: 2026-08-19
**Feature**: [spec.md](../spec.md) · [plan.md](../plan.md) · [tasks.md](../tasks.md)

## Requirement Completeness（FR 全覆盖）

- [x] CHK001 人物授权唯一基准(FR-001)是否有任务落地? → T023/T024(不变量核查+模型审计)
- [x] CHK002 军团/联盟端点传关联角色ID(FR-002/002a)是否有任务? → T015/T016 签名统一 + T018/T019 控制器
- [x] CHK003 归属校验(FR-003)是否有任务与测试? → T013/T014/T017/T021
- [x] CHK004 角色-军团从属校验(FR-004)是否落地? → T015/T016(corpId null→403)+T022 静态核查
- [x] CHK005 403 专属码+HTTP 403(FR-005)是否有任务? → T001(错误码)+T004(映射)+T006/T012(测试)
- [x] CHK006 三类错误区分(FR-006)是否有任务? → T012/T021
- [x] CHK007 内部路径不退化(FR-007)是否有任务? → T017+T021
- [x] CHK008 消除双重语义(FR-008)是否有任务? → T013-T016

## Task Executability（任务可执行性）

- [x] CHK009 每个任务是否有明确文件路径与可测动作? → 全部含 `src/.../` 路径与 RED/GREEN 标注
- [x] CHK010 任务粒度是否独立可测(<2h)? → 32 项,每项单一文件/单一行为
- [x] CHK011 依赖序是否成环? → Phase1→2→3→4 线性;US2 并行标注正确
- [x] CHK012 是否含 TDD 铁律证据(先 RED 后 GREEN)? → 测试任务均标 RED→GREEN

## Coverage（测试完备）

- [x] CHK013 403 三路径(数据权限/5xx 故障/刷新失效)是否可区分测试? → T012/T021 覆盖 403+5xx;刷新失效既有路径,plan「不改动」声明
- [x] CHK014 越权(IDOR)回归是否覆盖? → T021(非关联 ID→403/404)
- [x] CHK015 PageTotalApi 既有 bug 是否纳入修复与测试? → T008/T009
- [x] CHK016 内部授权归属对账(M2)是否纳入? → T017

## Scope & Boundary（范围边界）

- [x] CHK017 错位服务(钱包流水/交易)与已正确服务(资产/蓝图/工业/采矿/建筑)边界是否明确? → T022 仅核查不改
- [x] CHK018 MiningTask 范围排除是否明确? → T011(不经 API 层,不在 403 改造范围)
- [x] CHK019 不改 ESI 授权/刷新锁/刷新失效路径是否声明? → plan「不改动」段

## NFR & Security（非功能/安全）

- [x] CHK020 开发路加 5s 超时是否纳入? → T008
- [x] CHK021 403 message 不回显 ESI 原始串(安全红线)是否强制? → T002/T003/T012 明确拒绝原始串
- [x] CHK022 覆盖率 ≥80% 是否入 Polish? → T027
- [x] CHK023 security-reviewer(T031)与 security-scan(T028)是否入 Polish? → 是

## Traceability（可追溯）

- [x] CHK024 每个任务是否映射到 FR/US? → 任务都有 [USn] 标注或 phase 归属
- [x] CHK025 plan 设计决策(角色ID派生/403 识别层/错误码位置)是否在 tasks 体现? → T002/T008/T015 一致

## Notes

- 全部通过，无失败项。P1/P2 分析项(签名 userId/范围 MiningTask)已由用户澄清并精化入 tasks。
- 满足 G3 门禁：spec/plan/tasks 一致性 + 任务可执行 + 测试完备 + 安全约束就绪。