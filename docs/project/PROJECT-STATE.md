# 项目现状与 Spec Kit 约定（按需查阅）

开发流程权威见 [`docs/workflow/DEVELOPMENT-WORKFLOW.md`](../workflow/DEVELOPMENT-WORKFLOW.md)；
本文件记录项目**当前真实状态**与 Spec Kit 约定，供各阶段规划时对齐现状。

---

## 代码现状

- **Java 17 + Spring Boot 3.5.14**，DDD 五层架构（`Interfaces → Application → Domain ← Infrastructure`），技术栈已冻结（见 CLAUDE.md）。
- **多数据源**：`eve`（游戏静态数据，只读）与 `eve_helper`（运行时数据），独立 MyBatis Plus 配置。
- **核心能力**：JWT（RSA-256）认证、ESI OAuth2 + PKCE（晨曦/宁静）、RBAC（URL-权限-角色映射）、Redis 缓存与会话、多数据源持久化、市场订单异步线程池。
- **测试基线**：约 509 通过 / 4 失败 / 219 错误（2026-08-11，随工作区变更会波动，勿用固定阈值判回归，改逐用例 diff）。

## 当前工作流与 feature

- **当前分支**：`springboot3.0`（主开发分支）。
- **当前 feature**：`008-security-review-followup`（安全评审遗留修复），活动指针 `.specify/feature.json` 指向 `specs/008-security-review-followup`。
- **已完成 feature**：`specs/` 下 003（esi-auth-status）、004（ddd-layering-purity）、005（structure-query-api）、006（character-access-token-api）、007（jwt-key-rotation）。
- **开发流程**：统一工作流（特性轨 P0–P7 / 缺陷轨 / 轻量轨），门禁 G1–G3/G6 需用户明确批准 + 工件证据；TDD 铁律 + 证据铁律。详见 `docs/workflow/DEVELOPMENT-WORKFLOW.md`。

## Spec Kit 约定

- **版本**：`0.8.8.dev0`（`init-options.json` / `integration.json`）。
- **特性目录**：`specs/<NNN>-<short-name>/`，编号 sequential（`init-options.json` 中 `branch_numbering: sequential`）。
- **活动特性指针**：`.specify/feature.json`（`{"feature_directory": "specs/NNN-xxx"}`）。下游命令读它定位特性，不依赖 git 分支名。
- **模板**：`.specify/templates/{spec,plan,tasks,checklist,constitution}-template.md` 定义章节结构，填充时保持章节顺序与标题。
- **辅助脚本仅 PowerShell**（`integration.json` 中 `script: "ps"`）：`.specify/scripts/powershell/*.ps1`，无 bash 等价物。本会话 shell 是 bash，需用 `powershell -File ...` 调用。
- **扩展钩子**：`.specify/extensions.yml` **存在**且 `auto_execute_hooks: true`。`before_constitution` 挂 `speckit.git.initialize`；`before_specify` 挂 `speckit.git.feature`（自动按编号建 feature 分支）；其余 `before_/after_` 挂 `speckit.git.commit`（可选自动提交）。因此 feature 分支由钩子在 P1 自动创建，不再需 P0 手动建。
- **宪法**：`.specify/memory/constitution.md` 为 **v1.5.0 完整版**（2026-08-09 修订，统一 Spec-First 流程），非占位模板。含技术栈冻结（第四条）与流程强制条款。
- **完整 SDD 循环定义**：`.specify/workflows/speckit/workflow.yml`（specify → 评审门 → plan → 评审门 → tasks → implement）。

## 环境注意事项

- **Windows 环境**；Maven Wrapper 为 `mvnw.cmd`，bash 下可用 `./mvnw`（工作流文档用 `./mvnw` 写法）。
- **Redis 必须运行**；两个 MySQL 数据库（`eve` 与 `eve_helper`）必须可访问，否则 SpringBootTest context load 失败（勿误判为代码回归）。
- **敏感配置不入库**：`application-{ali,aliw,prod,test}.yml`、`.env.*` 实际配置、`eve-jwt.jks` 均被 `.gitignore` 忽略；走环境变量（参考 `.env.example`）。
- **评审记录** 归档于 `docs/reviews/`；历史工作流文档归档于 `docs/archive/`。