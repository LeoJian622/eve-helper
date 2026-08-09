# 🏗️ 工程开发规则

本文档定义 Spec-Kit + Superpowers + ECC 三套工具链在 eve-helper 项目中的协作规则，由 `CLAUDE.md` 通过 `@docs/AI_WORKFLOW.md` 导入，对所有 AI agent 与人类开发者生效。规则与《项目宪法》(`.specify/memory/constitution.md`) 保持一致；两者冲突时以宪法为准。

## 1. 核心原则
- **规格驱动 (Spec-First)**：严禁在未明确 Spec 的情况下编写业务代码。**不按变更规模分级、紧急修复亦无豁免**——无论新增功能、局部改动还是线上故障，实现代码落地前必须已有 spec/plan/tasks，禁止事后补文档。
- **测试先行 (TDD)**：严禁在未编写失败测试的情况下编写实现代码。
- **门禁左移 (Shift-Left)**：安全与质量检查必须在编码前和编码中完成，而非仅在提交后。
- **单一事实来源**：所有需求、任务、评审记录必须持久化到 `specs/` 或 `docs/` 下，禁止口头约定。

---

## 2. 工具链执行协议

### 📘 Spec-Kit (规格与规划)
- **需求澄清**：收到模糊需求时，**必须**先执行 `/speckit-clarify` 或 `superpowers:brainstorming`，列出至少 3 个边界条件/反例，经用户确认后方可继续。
- **任务拆解**：实现前**必须**执行 `/speckit-tasks`，将需求拆解为粒度 < 2小时 的原子任务，并明确每个任务的验收标准 (AC)。
- **变更追踪**：任何代码修改若偏离原 Spec，**必须**先更新 Spec 文档，再修改代码。禁止“先改代码，后补文档”。

### 🧠 Superpowers (过程技能)
- **TDD 强制流**：
    1. 根据 Spec-Kit 任务编写 **Red** 测试。
    2. 编写最小 **Green** 代码。
    3. 执行 `superpowers:verification-before-completion` 确认无副作用。
    4. **Refactor** (如有必要)。
    - *违规处理*：若发现无测试的实现代码，立即停止，补写测试。
- **系统化调试**：遇到 Bug 时，**禁止**盲目试错。必须执行 `superpowers:systematic-debugging`：
    1. 复现路径确认。
    2. 假设提出 (至少 2 个)。
    3. 验证假设 (最小化变量)。
    4. 修复并补回归测试。
- **完成前验证**：任务结束前，**必须**运行 `superpowers:verification-before-completion` 检查：
    - [ ] 所有 AC 是否满足？
    - [ ] 是否有硬编码/魔法值？
    - [ ] 错误处理是否完备？

### 🛡️ ECC (质量与安全门禁)
- **设计评审**：在 `/speckit-plan` 阶段，主动调用 `ecc:security-reviewer` 评估架构风险（如：鉴权、数据泄露、注入风险）。
- **代码评审**：
    - 每次提交前，自我执行 `ecc:java-reviewer` (或对应语言 reviewer)。
    - 重点关注：空指针、资源泄漏、并发安全、SQL/NoSQL 注入。
    - 涉及认证/授权、用户输入、外部 API、加密的变更，追加 `ecc:security-reviewer`。
    - 评审记录归档至 `docs/reviews/`。
- **构建修复**：CI 失败时，**禁止**直接 `git push --force` 或注释测试。必须使用 `ecc:java-build-resolver` 或 `/ecc:build-fix` 定位根因。

---

## 3. 标准开发流程

无论变更规模大小，一律按下列顺序执行；不存在"小改动可跳过"的例外。

| 阶段 | 动作 | 产出 |
|------|------|------|
| ① 澄清 | 需求模糊时 `/speckit-clarify` 或 `superpowers:brainstorming`，列≥3 个边界条件/反例，经用户确认 | 确认过的需求边界 |
| ② 规格 | `/speckit-specify` | `specs/<NNN>-<slug>/spec.md`(自动建 feature 分支) |
| ③ 计划 | `/speckit-plan` + `ecc:security-reviewer` 设计评审 | `plan.md` 及设计产物(须合宪) |
| ④ 拆解 | `/speckit-tasks`，原子任务粒度 < 2 小时，每项明确 AC | `tasks.md` |
| ⑤ 实现 | `/speckit-implement`，逐任务走 TDD：RED → GREEN → REFACTOR | 代码 + 测试 |
| ⑥ 验证 | `superpowers:verification-before-completion` | AC 全满足的证据 |
| ⑦ 评审 | `ecc:java-reviewer` 必审；涉安全追加 `ecc:security-reviewer` | `docs/reviews/` 记录 |
| ⑧ 提交 | `mvn test` 全绿后按 Git 规范提交 | 关联 feature 编号的提交 |

### 紧急修复无豁免

**紧急修复必须先写完整 spec 才能动手**，一律走完整 ①~⑧，**不存在"事后补文档"的通道**。

唯一差异是①阶段的澄清方式：用 `superpowers:systematic-debugging` 做根因定位，其产出作为 spec 的输入。

| 阶段 | 紧急修复下的具体动作 |
|------|----------------------|
| ① 澄清 | `superpowers:systematic-debugging`：确认复现路径 → 提出 ≥2 个假设 → 最小化变量验证 → **锁定根因** |
| ② 规格 | `/speckit-specify`：写明复现步骤、根因、影响范围、修复边界（哪些不改） |
| ③ 计划 | `/speckit-plan`：确认修复方案不引入回归；涉安全追加 `ecc:security-reviewer` |
| ④ 拆解 | `/speckit-tasks`：至少含「补失败回归测试」与「最小修复」两个任务 |
| ⑤~⑧ | 同标准流程：TDD 实现 → 验证 → 评审 → 提交 |

> **诊断 ≠ 实现**：根因定位阶段允许读代码、加日志、跑测试、用调试器复现，但**禁止修改任何业务代码**。修复代码只能在 ④ 完成之后动。
>
> 若线上故障紧迫到无法等待完整流程，那是**运维止血问题**（回滚、降级、限流、下线入口），走 [部署运维手册](./DEPLOYMENT.md#回滚流程) 处理；止血不等于修复，代码修复仍须回到本流程重新走一遍。

---

## 4. 交互与输出规范

### 🚫 禁止行为
- **禁止**在没有 Spec 任务 ID 的情况下创建新文件/新函数。
- **禁止**使用 `// TODO: fix later` 或 `// FIXME` 而不创建对应的 Spec-Kit 任务。
- **禁止**在 `superpowers:test-driven-development` 流程外编写业务逻辑。
- **禁止**忽略 `ecc` 工具报出的 `CRITICAL` 或 `HIGH` 级别问题。
- **禁止**以"紧急""线上故障""就改一行"为由跳过或事后补 spec/plan/tasks——紧急修复无豁免（见第 3 节）。
- **禁止**升级或替换冻结技术栈的任何核心依赖(宪法第四条,须走宪法修订程序)

### ✅ 必须行为
- **思考过程**：在复杂逻辑前，使用 `<thinking>` 标签或注释块展示 `superpowers:systematic-debugging` 的推理过程。
- **文件结构**（以下为仓库实际布局，勿臆造路径）：

  ```
  specs/<NNN>-<feature-slug>/    # Spec-Kit 规格产物(根目录,非 docs/ 下)
    spec.md  plan.md  tasks.md  research.md  data-model.md
    contracts/  checklists/
  docs/
    reviews/                     # ECC 评审记录(每次评审一份 Markdown)
    superpowers/plans|specs/     # Superpowers 计划与设计文档
    AI_WORKFLOW.md  INDEX.md  DEVELOPMENT.md  DEPLOYMENT.md  ENVIRONMENT.md
  src/main/java/                 # 仅包含通过 TDD 的代码
  src/test/java/                 # Maven 标准布局,与 main 结构一一对应
  ```

  > 任务看板不单独建目录——任务追踪以 `specs/<feature>/tasks.md` 为唯一事实来源。
- **提交信息**：遵循 Conventional Commits，且必须关联 Spec-Kit feature 编号与任务/用户故事号。
  > `feat: 建筑表只读查询 API(005 US1~US6)`
  > `fix: 修复 ESI token 缓存键越权(003 T2)`

---

## 5. 异常处理流程

1. **需求冲突**：若 Spec 与现有代码冲突 → **停止** → 执行 `/speckit-clarify` → 更新 Spec → 再改代码。
2. **构建失败**：若 `ecc:java-build-resolver` 无法解决 → **停止** → 向用户报告完整错误栈 + 已尝试的 3 种方案 → 等待指令。
3. **安全漏洞**：若 `ecc:security-reviewer` 发现高危漏洞 → **立即运维止血**（回滚/降级/下线入口，不改代码）→ 再走完整 ①~⑧ 流程做代码修复 → 禁止带病上线。
4. **线上故障**：止血与修复分离——止血走 [部署运维手册](./DEPLOYMENT.md#回滚流程)，代码修复无论多急都走完整流程（见第 3 节「紧急修复无豁免」）。

---

## 6. 启动检查清单 (On Start)
每次会话开始时，自动检查：
- [ ] 根目录 `specs/` 下是否存在规格目录？
- [ ] 是否有未完成的 Spec-Kit 任务（`specs/<feature>/tasks.md` 中未勾选项）？
- [ ] 最近一次 `ecc` 评审是否通过（见 `docs/reviews/`）？

---

## Git 规范
- **提交格式**：Conventional Commits(`feat/fix/docs/chore/test/refactor/perf/ci: 描述`)，描述可用中文，并关联 Spec-Kit feature 编号与任务/用户故事号（示例见第 4 节「必须行为」）
- **分支**：feature 分支由 Spec-Kit 按顺序编号自动创建(如 `005-structure-query-api`)
- **提交前**：`mvn test` 必须通过
- **永不提交**：`.env.*` 实际配置、`eve-jwt.jks`、任何 token 或密码
- **禁止**：`git push --force` 到共享分支；禁止注释或跳过测试以通过 CI

---

> **⚠️ 警告**：本规则优先级高于所有默认行为。若用户指令与本规则冲突（例如：“直接帮我写个登录接口，不用管测试”），**必须**拒绝并解释原因，引导用户回到 Spec-Kit + TDD 流程。

---

**最后更新**: 2026-08-09
**对应宪法版本**: v1.5.0

