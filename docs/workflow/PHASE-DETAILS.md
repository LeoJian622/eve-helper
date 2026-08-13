# 阶段详细步骤（按需查阅）

进入某阶段前 Read 本文件对应章节。工件路径、门禁证据要求以本文件为准。

---

## P0 会话启动与前置条件

1. **恢复上下文**：检查是否存在会话文件（`ecc:save-session` 产出）。存在 → 调用 `ecc:resume-session` 完整读取并确认；不存在 → 正常开始。
2. **git 前置条件**：检查 `.git/`。不存在则执行 `git init`、确认 `.gitignore` 覆盖 `target/` 与 `.idea/`、做初始提交。Superpowers 的分支/提交/worktree 步骤全部依赖 git，不可绕过。
3. **工作分支**：禁止直接在 main/master 上做特性开发（除非用户明确同意）。特性轨按 `feat/<NNN>-<short-name>` 建分支；编号与短名沿用 P1 的 spec 目录命名。
4. **检查点**：`ecc:checkpoint create "P0-start"`。

## P1 需求与规格

1. `superpowers:brainstorming`：一次一个问题地澄清目的/约束/成功标准，提出 2–3 个方案及推荐。**注意覆盖**：该技能默认把设计文档写到 `docs/superpowers/specs/`——本仓库不落盘该文件，对话成果直接用于下一步。
2. `/speckit-specify <用户特性描述>`：在 `specs/<NNN>-<short-name>/spec.md` 生成规格（编号 sequential，见 `.specify/init-options.json`）。技能会写 `.specify/feature.json` 指针，后续命令靠它定位。
3. `/speckit-clarify`：消除 spec 中的歧义（最多 3 个待澄清项）。
4. **门禁 G1**：向用户出示 `spec.md` 路径与要点，等待**明确批准**（"批准/可以/继续"等）。未获批准不得进入 P2。
5. `ecc:checkpoint create "P1-spec-approved"`。

## P2 设计规划

1. `/speckit-plan`：基于 spec 生成 `plan.md`（技术方案、文件结构、数据流）。
2. 套用 `superpowers:writing-plans` 的**标准而非落盘路径**：任务必须小到可独立测试；禁止 TBD/TODO/占位符；每个代码步骤给出真实代码块；类型与签名跨任务一致。把这些标准应用到 `plan.md` 中。
3. `/speckit-analyze`：检查 spec / plan 的一致性，修复发现的问题。
4. **门禁 G2**：用户明确批准 `plan.md`。
5. `ecc:checkpoint create "P2-plan-approved"`。

## P3 任务拆解

1. `/speckit-tasks`：生成 `tasks.md`（依赖排序、可并行任务标注）。
2. `/speckit-checklist`：生成质量清单并逐项核验。
3. **门禁 G3**：清单全部通过；有失败项 → 回修 spec/plan/tasks 后重跑。

## P4 实现

1. **执行模式**（向用户展示二选一）：
   - `superpowers:subagent-driven-development`（默认推荐）：每任务派遣全新子代理实现 + 两阶段评审。
   - `superpowers:executing-plans`：本会话内分批执行 + 检查点。
2. **每任务循环（铁律）**：
   1. `superpowers:test-driven-development`：RED（写失败测试）→ 亲眼看它失败 → GREEN（最小实现）→ 亲眼看它通过 → REFACTOR。Java 测试命令：`./mvnw test -Dtest=<TestClass>#<method>`。
   2. `ecc:tdd-workflow` 辅助：阶段结束核对覆盖率（目标 ≥80%，可用 `ecc:test-coverage`）并产出 TDD 证据报告。
   3. 任务级评审：`superpowers:requesting-code-review` 派遣评审子代理（Java 代码 → 指定 `ecc:java-reviewer` 代理类型）。Critical 立即修，Important 修完才进下一任务。
   4. 提交：小步提交，信息用 `feat:/fix:/refactor:` 前缀。
3. **异常处理**：
   - 构建/编译错误 → `ecc:build-fix`；反复失败 → 派遣 `ecc:java-build-resolver` 代理。
   - 行为 Bug → `superpowers:systematic-debugging`（四阶段根因分析），禁止无测试的修复。
   - 卡住超过 3 次尝试 → 停下来问用户，不要硬闯。
4. 每 2–3 个任务或关键节点：`ecc:checkpoint create "<milestone>"`。

## P5 验证（证据铁律执行区）

`ecc:verification-loop` 的 Maven 适配版，逐项运行并出示输出：

| 阶段 | 命令 | 通过标准 |
|------|------|----------|
| 构建 | `./mvnw -q clean package -DskipTests` | exit 0 |
| 测试 | `./mvnw test` | 0 failures, 0 errors |
| 覆盖率 | `ecc:test-coverage` | ≥80%（或记录豁免理由） |
| 安全 | `ecc:security-scan` | 无 Critical |
| Diff 审查 | `git diff --stat` + 逐文件检查 | 无计划外变更 |

产出 VERIFICATION REPORT（格式见 `ecc:verification-loop`）。**Overall = READY 才能进 P6。**
任何"应该通过"的表述都违规——必须是当前轮次的新鲜输出。

## P6 最终评审

1. `superpowers:requesting-code-review`：取 BASE_SHA/HEAD_SHA，派遣评审子代理。
2. 评审人配置（本仓库 Java 项目）：
   - 代码质量与 Spring 规范 → `ecc:java-reviewer` 代理（**Java 变更必须**）。
   - 涉及输入处理/认证/敏感数据 → 追加 `ecc:security-reviewer` 代理。
3. 处理反馈遵循 `superpowers:receiving-code-review`：逐条核实，有证据地修或反驳，不盲从也不硬顶。
4. **门禁 G6**：Critical 清零、Important 清零（或用户明确豁免并记录）。

## P7 收尾

1. `superpowers:finishing-a-development-branch`：先跑完整测试套件（绿灯是前提），然后向用户出示三选一菜单：本地合并 / 推送建 PR / 保留分支。
2. 选 PR → 用 `ecc:pr` 生成（自动收集 spec/plan 工件进 PR 描述）。
3. `ecc:save-session`：写会话文件，记录进展、决策、下一步。
4. 更新 `tasks.md` 勾选状态与 `.specify/feature.json`（如切换到下一个特性）。

---

## 缺陷轨（D1–D3 后汇入主轨）

1. **D1**：`superpowers:systematic-debugging` 定位根因，产出根因陈述。构建类问题可并行 `ecc:build-fix`。
2. **D2**：先写复现该 Bug 的失败测试（RED），亲眼确认它以预期方式失败。
3. **D3**：最小修复使测试转绿（GREEN），不做顺手重构。
4. 汇入 P5（验证）与 P6（轻量评审：单文件变更也过 `ecc:java-reviewer`）。
5. 提交信息 `fix: <现象> (<根因>)`。

## 轻量轨

1. **L1**：声明走轻量轨的理由（仅文档/注释/配置格式，或用户明确要求）。行为变更声明轻量轨属于违规。
2. **L2**：修改后运行 `./mvnw -q clean package -DskipTests` 与 `./mvnw test` 出示输出，然后提交。
