# 质量清单: Wallet Transactions(010)spec/plan/tasks

**Purpose**: 核验 010 规格/计划/任务三项工件的质量(需求完备性/清晰性/一致性/可测性/覆盖),门禁 G3 依据。
**Created**: 2026-08-18
**Feature**: ../../specs/010-wallet-transactions/

## 核验依据(仅测需求质量,非实现)

按 speckit-checklist「需求单元测试」原则:每项核验 spec/plan/tasks **写得对不对**,不测代码是否工作。

## Requirements Completeness(需求完备性)

- [x] CHK001 范围是否完整覆盖用户澄清?——人物+军团、军团全 division(1-7)均在 spec US1/US2 明示。已覆盖
- [x] CHK002 数据管道是否端到端闭合?(ESI→转换→持久化→查询→VO→控制器)——plan 批次 A/B/C/D 覆盖全链路。已覆盖
- [x] CHK003 军团全 division 同步的失败语义是否定义完整?——FR-006(单 division 失败不整批回滚+指明失败)、plan D3、T017 均有。已覆盖
- [x] CHK004 幂等需求是否明确唯一的约束载体?——FR-004+plan D1(UNIQUE(owner_type,owner_id,division,transaction_id))。已覆盖

## Requirements Clarity(需求清晰性)

- [x] CHK005 幂等唯一键是否无歧义?——plan D1 明确复合键四元组与新旧差异(非 PRIMARY(id)),防与 journal 混淆。清晰
- [x] CHK006 同步策略 from_id 游标是否术语明确、无"翻页到哪停"歧义?——plan D2 明确首页 null→末条 transaction_id→空页/达上限终止。清晰
- [x] CHK007 ownerId/division 跨层类型是否显式对齐,防 MapStruct 隐式歧义?——tasks 关键类型约束节 + plan D5 显式规定(Long 库/Integer converter 参数)。清晰
- [x] CHK008 军团 division 查询入参语义(人物 0/军团 1-7)是否澄清?——tasks 关键类型约束节 + T020 param 校验。清晰

## Requirements Consistency(需求一致性)

- [x] CHK009 FR-006(单 division 失败保留已成功)与 tasks T017/T019 是否一致?——一致,均以「非服务级事务 + division 独立提交」落地
- [x] CHK010 plan 决策与 tasks 是否逐条可追溯?——D1→T002/T004,D2→T013,D3→T013(b)/T019,D4→T014/T019,D5→跨任务类型节。可追溯
- [x] CHK011 端点命名与既有 009 模块是否冲突?——`/wallet/transaction/...` 与 `wallet/journal`、`wallet/journal/corp` 区分,不冲突。一致

## Acceptance Criteria Quality(验收标准质量)

- [x] CHK012 每 US 的 Acceptance Scenarios 是否可独立验证?——US1(5 条)/US2(5 条)均含 Given/When/Then 可测断言。可测
- [x] CHK013 成功标准是否可量化?——SC-001(≤500ms p95)、SC-004(≥80% 覆盖率,已注不可测豁免)。可量化(除覆盖豁免)

## Scenario Coverage(场景覆盖)

- [x] CHK014 主流程(P1 人物/P2 军团)是否覆盖?——US1/US2 双故事,各含同步+分页。已覆盖
- [x] CHK015 异常流(ESI 失败/单 division 失败/越权)是否覆盖?——edge cases + FR-005/FR-006/FR-010 + T010/T017/T018。已覆盖
- [x] CHK016 边界(空数据/从未同步/幂等重复)是否覆盖?——FR-004/FR-009 + T010/T016/T018。已覆盖

## Edge Case Coverage(边界条件)

- [x] CHK017 军团单 division 拉取超单页上限截断风险是否定义?——spec edge case「from_id 游标翻页直至拉完不得截断」+ plan D2 上限/空页终止。已覆盖
- [x] CHK018 division 无数据/未同步边界是否定义?——US2 场景 4 + FR-009。已覆盖

## 异常/回滚(状态变更场景)

- [x] CHK019 军团 division 失败回滚语义是否无歧义?——FR-006 + plan D3「非服务级事务、division 独立提交、已成功保留」+ T017 测试断言。明确
- [x] CHK020 同步重复触发(连续点同步)是否定义?——spec edge case「幂等不产生重复」+ T010/T018。已覆盖

## Dependencies & Assumptions

- [x] CHK021 外部依赖(ESI 底层已就位、无 maxPage 军团侧)是否如实记录?——spec Assumptions + plan Phase 0 明确核实(WalletApi 方法/模型存在、军团无 maxPage)。已记录
- [x] CHK022 技术栈冻结约束是否声明?——spec Assumptions「技术栈遵循冻结版本」。已记录
- [x] CHK023 不动 wallet_journal 的边界是否声明?——spec Assumptions + plan Summary「不触碰 wallet_journal」+ tasks T002 仅建新表。已记录

## 结论

- 核验项:  23,  通过:  23,  失败:  0
- 无 [NEEDS CLARIFICATION] 残留;无缺漏维度;无跨工件冲突。
- **G3 通过**——task 拆解可直接进入 P4 实现。
- 遗留记录:覆盖 ≥80% 为不可客观测量项(项目无 jacoco,技术栈冻结),按 T023 标注以集成测试证据替代(与 009 T025 同处理)。