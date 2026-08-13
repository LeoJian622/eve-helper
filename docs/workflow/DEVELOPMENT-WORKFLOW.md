# 统一开发工作流（Spec Kit × Superpowers × ECC）

本文件是本仓库**唯一权威的开发流程**，对所有 Claude 实例强制生效（由 CLAUDE.md 通过 `@` 引入）。
任何代码变更——新功能、缺陷修复、重构——都必须按本流程执行。

## 三系统分工（一句话）

- **Spec Kit（`/speckit-*`）**：流程骨架与产物权威——阶段、门禁、`specs/` 下的 spec/plan/tasks 工件。
- **Superpowers（`superpowers:*`）**：执行纪律——头脑风暴、TDD 铁律、系统化调试、证据式验证、评审编排、分支收尾。
- **ECC（`ecc:*`）**：质量工具链与会话工程——Java 专家代理、代码评审、验证循环、安全扫描、检查点、会话保存/恢复。

## 任务分轨（开工前必须先声明轨道）

| 轨道 | 适用 | 路径 |
|------|------|------|
| **特性轨** | 新功能、行为变更、新依赖 | P0 → P1 → … → P7 全阶段 |
| **缺陷轨** | Bug 修复 | D1 定位根因 → D2 回归测试 → D3 最小修复 → P5 → P6（轻量） → 提交 |
| **轻量轨** | 纯文档/注释/配置格式修改，或用户明确要求"直接改" | L1 声明理由 → L2 修改 + build/test 验证 → 提交 |

**行为变更不存在轻量轨。** 拿不准时走特性轨。

## 特性轨阶段总览

| 阶段 | 名称 | 主力工具 | 门禁（不满足不得进入下一阶段） |
|------|------|----------|-------------------------------|
| P0 | 会话启动与前置条件 | `ecc:resume-session`、`ecc:checkpoint` | git 仓库可用（无 `.git` 则先 `git init` + 初始提交） |
| P1 | 需求与规格 | `superpowers:brainstorming` → `/speckit-specify` → `/speckit-clarify` | **G1**：用户明确批准 `spec.md` |
| P2 | 设计规划 | `/speckit-plan`（套用 `superpowers:writing-plans` 粒度标准）、`/speckit-analyze` | **G2**：用户明确批准 `plan.md` |
| P3 | 任务拆解 | `/speckit-tasks`、`/speckit-checklist` | **G3**：质量清单全部通过 |
| P4 | 实现 | `superpowers:subagent-driven-development`（默认）或 `executing-plans`；每任务 `superpowers:test-driven-development` | 每任务：TDD 完成 + 任务级评审通过 |
| P5 | 验证 | `superpowers:verification-before-completion` + `ecc:verification-loop`（Maven 适配） + `ecc:security-scan` | VERIFICATION REPORT = READY |
| P6 | 最终评审 | `superpowers:requesting-code-review` 派遣 `ecc:java-reviewer` / `ecc:security-reviewer` | **G6**：无 Critical / Important 未修复项 |
| P7 | 收尾 | `superpowers:finishing-a-development-branch`；PR 用 `ecc:pr`；`ecc:save-session` | 用户选择集成方式（合并/PR/保留） |

**详细步骤：进入任一阶段前，必须先 Read `docs/workflow/PHASE-DETAILS.md` 中对应章节。**
工具职责矩阵与调用速查见 `docs/workflow/TOOL-MAP.md`；项目现状与 Spec Kit 约定见 `docs/project/PROJECT-STATE.md`。

## 铁律（任何阶段都不得违反）

1. **TDD 铁律**：没有先失败的测试，就没有生产代码。写反了 → 删除代码重来。
2. **证据铁律**：声称"完成/通过/修复"之前，必须在当前轮次运行完整验证命令并出示真实输出。"应该可以了"视为违规。
3. **门禁铁律**：G1–G3、G6 只能通过**用户明确批准 + 工件证据**过关；不得自我批准、不得默认通过。
4. **单一事实源**：`specs/<NNN>-<feature>/` 是规格/计划/任务的唯一权威位置。不生成重复计划文件（覆盖 brainstorming / writing-plans 的默认落盘路径——`superpowers:using-superpowers` 明确用户指令优先于技能默认值）。
5. **不跳阶段**：唯一例外是用户明确说"跳过流程"，且必须在最终报告中记录该偏离。

## 冲突裁决原则（摘要）

- **规划文件**：`speckit` 的 `plan.md`/`tasks.md` 为准；`superpowers:writing-plans` 只作为任务粒度与无占位符标准，不另建文件。
- **设计文档**：`superpowers:brainstorming` 的对话成果直接注入 `/speckit-specify` 的 `spec.md`，不另写 design doc。
- **TDD**：`superpowers:test-driven-development` 为主（红-绿-重构铁律）；`ecc:tdd-workflow` 为辅（80% 覆盖门槛与 TDD 证据报告）。
- **评审**：Superpowers 负责编排（派遣子代理、处理反馈）；ECC 提供评审人——Java 变更**必须**用 `ecc:java-reviewer` 代理。
- **验证**：Superpowers 证据规则 + ECC 验证循环（本仓库适配为 Maven 命令，见 PHASE-DETAILS P5）。
- **调试**：`superpowers:systematic-debugging` 定位根因；构建类错误交 `ecc:build-fix` 或 `ecc:java-build-resolver` 代理。

## 开工仪式（每次会话、每个任务）

1. 声明轨道（特性/缺陷/轻量）与当前阶段。
2. Read `docs/workflow/PHASE-DETAILS.md` 对应章节。
3. 按阶段执行；每个门禁出示证据并等待用户。
