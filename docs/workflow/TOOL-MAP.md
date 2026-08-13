# 三系统工具职责矩阵与调用速查

冲突时按本表裁决：**Spec Kit 管产物，Superpowers 管纪律，ECC 管质量与会话**。

## 职责矩阵

| 维度 | 主力（Primary） | 辅助（Secondary） | 禁用/让位 |
|------|----------------|-------------------|-----------|
| 需求对话 | `superpowers:brainstorming` | — | `ecc:feature-dev` 的 Discovery 阶段（被 brainstorming 取代） |
| 规格工件 | `/speckit-specify`、`/speckit-clarify` | — | brainstorming 的默认 design doc 落盘 |
| 技术规划 | `/speckit-plan` | `superpowers:writing-plans`（仅粒度标准） | `ecc:plan`（备用，默认不用，避免双计划文件） |
| 一致性检查 | `/speckit-analyze` | `/speckit-checklist` | — |
| 任务拆解 | `/speckit-tasks` | — | — |
| 计划执行 | `superpowers:subagent-driven-development` | `superpowers:executing-plans`（无子代理时） | `ecc:feature-dev` 的 Implementation 阶段 |
| TDD | `superpowers:test-driven-development` | `ecc:tdd-workflow`（覆盖率与证据报告） | 事后补测试 |
| 调试 | `superpowers:systematic-debugging` | `ecc:build-fix`、`ecc:java-build-resolver` 代理（构建类） | 无测试猜测式修复 |
| 验证 | `superpowers:verification-before-completion`（证据规则） | `ecc:verification-loop`（阶段化清单，Maven 适配见 PHASE-DETAILS P5） | "应该通过"式声明 |
| 任务级评审 | `superpowers:requesting-code-review`（编排） | `ecc:java-reviewer` 代理（评审人） | 自己审自己的 diff |
| 最终评审 | `superpowers:requesting-code-review` | `ecc:java-reviewer` + `ecc:security-reviewer` 代理；反馈处理用 `superpowers:receiving-code-review` | 带 Critical 合并 |
| 安全 | `ecc:security-scan` | `ecc:security-reviewer` 代理 | — |
| 分支收尾 | `superpowers:finishing-a-development-branch` | `ecc:pr`（建 PR 时） | 未经用户选择擅自合并 |
| 会话连续性 | `ecc:save-session` / `ecc:resume-session` | `ecc:checkpoint`（里程碑打点） | — |
| 并行加速 | `superpowers:dispatching-parallel-agents`（tasks.md 中标注可并行的任务） | — | 对有依赖任务并行 |
| 隔离工作区 | `superpowers:using-git-worktrees`（可选，多特性并行时） | — | — |

## ECC 专家代理速查（通过 Agent 工具派遣）

| 代理 | 用途 | 何时必须 |
|------|------|----------|
| `ecc:java-reviewer` | Java/Spring 代码评审 | **所有 Java 代码变更** |
| `ecc:java-build-resolver` | Maven/编译错误修复 | 构建错误反复修不掉时 |
| `ecc:security-reviewer` | 安全评审 | 涉及输入、认证、敏感数据 |
| `ecc:code-explorer` | 既有代码路径分析 | P1/P2 需要了解现状时 |
| `ecc:code-architect` | 实现蓝图设计 | P2 复杂特性需要方案对比时 |

## 调用形式

- Spec Kit：斜杠命令 `/speckit-specify`、`/speckit-plan`…（技能在 `.claude/skills/speckit-*/`）。
- Superpowers：Skill 工具，`superpowers:<skill>`，如 `superpowers:test-driven-development`。
- ECC：命令/技能为 `ecc:<name>`（如 `ecc:tdd-workflow`、`ecc:build-fix`）；专家代理通过 Agent 工具以对应 `ecc:*` 代理类型派遣。

## 关键裁决说明

1. **为什么 speckit 产物压倒 superpowers 默认路径**：`superpowers:using-superpowers` 明确"用户指令（CLAUDE.md 等）优先于技能"。本工作流即用户指令，故 brainstorming/writing-plans 的落盘默认被覆盖，内容标准保留。
2. **为什么禁用 ecc:feature-dev 主流程**：它与 P1–P6 完全重叠但无门禁与工件体系，仅保留其代理（code-explorer/code-architect/code-reviewer 系列）供各阶段按需调用。
3. **为什么 ecc:plan 让位**：`ecc:plan` 产出 PRD 式文档，与 `plan.md` 单一事实源冲突；如用户明确要求 PRD，可临时启用并声明偏离。
