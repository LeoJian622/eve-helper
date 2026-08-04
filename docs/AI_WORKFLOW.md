# AI 开发工作流(Spec-Kit + Superpowers + ECC)

本文档定义三套工具链在 eve-helper 项目中的协作规则,由 `CLAUDE.md` 通过 `@docs/AI_WORKFLOW.md` 导入,对所有 AI agent 与人类开发者生效。

## 工具链职责

| 工具链 | 角色 | 主要入口 |
|--------|------|----------|
| **Spec-Kit** | 规格驱动开发:需求 → 澄清 → 计划 → 任务 → 实现 | `/speckit-specify` `/speckit-clarify` `/speckit-plan` `/speckit-tasks` `/speckit-implement` `/speckit-analyze` `/speckit-checklist` |
| **Superpowers** | 过程技能:头脑风暴、TDD、系统化调试、完成前验证 | `superpowers:brainstorming` `superpowers:test-driven-development` `superpowers:systematic-debugging` `superpowers:verification-before-completion` |
| **ECC** | 质量门禁:代码评审、安全评审、构建修复 | `ecc:java-reviewer` `ecc:security-reviewer` `ecc:java-build-resolver` `/ecc:build-fix` `/ecc:code-review` |

## 分级流程

### L1 — 中大型功能(新功能、跨层变更、架构调整)

1. 需求模糊时先用 `superpowers:brainstorming` 澄清
2. `/speckit-specify` 生成 spec.md(自动创建顺序编号的 feature 分支)
3. `/speckit-clarify` 澄清规格中的模糊点(最多 5 个问题)
4. `/speckit-plan` 生成实现计划等设计产物(必须符合宪法)
5. `/speckit-tasks` 生成按依赖排序的 tasks.md
6. 可选:`/speckit-analyze` 做跨产物一致性检查
7. `/speckit-implement` 按任务执行

### L2 — 小改动(单文件、局部逻辑、不影响架构)

Superpowers TDD:
1. 写失败测试(RED),运行确认失败
2. 最小实现(GREEN),运行确认通过
3. 重构(IMPROVE),保持测试全绿
4. 覆盖率目标 ≥80%

### L3 — 紧急修复

1. `superpowers:systematic-debugging` 定位根因(禁止盲目修改)
2. 最小修复
3. 补复现该 bug 的回归测试
4. 运行全量测试确认无回归

## 实现后评审门禁(所有级别强制)

1. **Java 代码变更**: `ecc:java-reviewer` 必审,CRITICAL/HIGH 问题必须修复后方可合并
2. **涉及安全**(认证、授权、用户输入、外部 API、加密): 追加 `ecc:security-reviewer`
3. **构建失败**: 使用 `ecc:java-build-resolver` 或 `/ecc:build-fix`,最小改动恢复构建
4. **完成前验证**: `superpowers:verification-before-completion` — 有测试/构建证据才能声称完成

## Git 规范

- 提交格式: Conventional Commits(`feat/fix/docs/chore/test/refactor/perf/ci: 描述`),描述可用中文
- L1 feature 分支由 Spec-Kit 自动创建(顺序编号)
- 提交前 `mvn test` 必须通过
- **永不提交**: `.env.*` 实际配置、`eve-jwt.jks`、任何 token 或密码

## 禁止事项

- ❌ 升级或替换冻结技术栈的任何核心依赖(宪法第四条,须走宪法修订程序)
- ❌ L1 变更跳过 spec/plan/tasks 直接写代码
- ❌ 未运行测试就声称完成
- ❌ 手工修改 `.specify/scripts/` 与 `.specify/workflows/`(由 Spec-Kit 维护)
