# 建筑表只读查询接口 — L1 流程声明

## 级别判定
**L1(中大型功能 / 跨层变更)**:涉及 domain / infrastructure / application / interfaces 四层新增只读查询能力。

## 宪法依据
- **第五条(不可协商)**:L1 变更必须走 Spec-Kit 规格驱动流程(specify -> clarify -> plan -> tasks -> implement),**不得跳过 spec/plan/tasks 直接写代码**。
- **第一条(不可协商)**:REST API First + Test-First(Red-Green-Refactor),覆盖率 ≥80%。
- **第四条(不可协商)**:技术栈冻结,不升级核心依赖。

## 功能范围(来自用户指令,仅只读)
1. 主要信息展示接口(分页列表)
2. 详细信息接口(单建筑详情)
3. 燃料剩余展示接口(预警)
4. 启动服务展示接口(服务状态)
5. 其他功能:统计概览、按状态/缺油筛选、增强/解锚时间提醒
6. **绝对不提供新增/编辑/删除接口**(数据由现有定时任务从 ESI 同步入库)

## Spec-Kit 流程路径
1. `/speckit-specify` — 生成 `specs/005-structure-query-api/spec.md` + 质量检查清单(创建 005 feature 目录,更新 `.specify/feature.json`)
2. `/speckit-clarify` — 澄清规格模糊点(≤5 问)
3. `/speckit-plan` — 生成实现计划(符合宪法)
4. `/speckit-tasks` — 生成按依赖排序的 tasks.md
5. (可选)`/speckit-analyze` — 跨产物一致性检查
6. `/speckit-implement` — 按任务执行(TDD: RED→GREEN→IMPROVE)

## 实现后评审门禁(强制)
- `ecc:java-reviewer` 必审
- 涉及用户输入/数据访问 → 追加 `ecc:security-reviewer`(IDOR/SQL 注入)
- `superpowers:verification-before-completion` — 以测试/编译证据声明完成

## 当前进度
- ✅ `/speckit-specify` -> `specs/005-structure-query-api/spec.md`(6 用户故事 / 16 FR / 10 SC / 8 假设)+ `checklists/requirements.md`
- ⏭️ `/speckit-clarify` 跳过(spec 无 NEEDS CLARIFICATION,已在 specify 前澄清)
- ✅ `/speckit-plan` -> `specs/005-structure-query-api/plan.md` + `research.md`(R-01~07 技术决策)+ `data-model.md` + `contracts/api-contracts.md`(6 端点)+ `quickstart.md`;宪法检查通过(性能/覆盖率/技术栈冻结/分级流程)
- ✅ `/speckit-tasks` -> `specs/005-structure-query-api/tasks.md`(63 任务 T001~T063,按 6 用户故事分组,TDD RED 先)
- ⬇️ 下一步:`/speckit-implement` 按 T001~T063 执行(MVP 先 US1,TDD: RED->GREEN->IMPROVE)
