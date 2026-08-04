# 设计文档:Spec-Kit + Superpowers + ECC 工具链集成与过期文件清理

**日期**: 2026-08-04
**状态**: 已批准
**范围**: agent 相关文件(CLAUDE.md、AGENTS.md、.claude/、.specify/、docs/),不含业务代码

## 背景与问题

eve-helper 项目已完成 DDD 迁移并升级到 Java 17 / Spring Boot 3.5.14,但:

1. **CLAUDE.md 严重过期**: 仍声称 Java 11 / Spring Boot 2.7.18,依赖版本全部过期,AI agent 会基于错误信息工作
2. **三套工具未形成合力**: Spec-Kit 已初始化(v0.8.8.dev0)但宪法是模板套话(含不适用的 "CLI Interface" 条款);Superpowers 和 ECC 仅在用户级安装,项目文件中无任何工作流指引
3. **过期文件堆积**: 已完成的 DDD 迁移规划文档、MyBatisX 临时配置 JSON、错放根目录的 SKILL.md、settings.local.json 中迁移期遗留的一次性权限
4. **安全隐患**: 含真实明文密码的 `.env.dev/.env.aliw/.env.prd` 已进入 git 暂存区

## 硬约束

- **技术栈不可变更**(用户明确要求): Java 17、Spring Boot 3.5.x、MyBatis Plus、Druid、MySQL、Redis、MapStruct、Lombok、Hutool 等不得升级或替换,此约束须固化到宪法和 CLAUDE.md
- 不改任何业务代码和 `pom.xml`
- 不动用户暂存中的 `application-*.yml`(进行中的工作)

## 已确认的决策

| # | 决策 | 选择 |
|---|------|------|
| 1 | 过期文件处理 | 删除 + 迁移(DDD 文档和 MyBatisX JSON 删除;SKILL.md 迁入 .claude/skills/) |
| 2 | 含密码的 .env 暂存文件 | `git rm --cached` 移出暂存区,本地保留 |
| 3 | Spec-Kit 宪法 | 修订:移除 CLI 条款,改为 REST API/DDD 导向,新增技术栈冻结条款 |
| 4 | 开发流程强度 | 分级流程(按变更规模分流) |
| 5 | 文件组织方式 | 方案 B:精简 CLAUDE.md + @导入工作流文档 + 单行 AGENTS.md 指针 |

## 设计

### 1. 重写 CLAUDE.md(精简至 ~150 行)

章节结构:

1. **项目概述**: EVE Helper 用途、版本、端口、Swagger 地址(保留)
2. **技术栈(已冻结)**: 按 pom.xml 实际版本修正 — Java 17、Spring Boot 3.5.14、Spring Security、Spring WebFlux(ESI 客户端)、MyBatis Plus 3.5.15、Druid 1.2.24、MySQL Connector/J 9.x、Redis、Lombok 1.18.40、MapStruct 1.6.3、Hutool 5.8.44、SpringDoc OpenAPI 2.8.16、Nimbus JOSE JWT 10.0.2、Prometheus metrics。附冻结声明:"技术栈已冻结,未经宪法修订程序不得升级或替换任何核心依赖"
3. **DDD 架构概览**: 五层结构、依赖规则 `Interfaces → Application → Domain ← Infrastructure`、CQRS(CommandBus/QueryBus)、仓储模式(保留精华,压缩篇幅)
4. **构建和开发命令**: mvn clean package / spring-boot:run / test 等(保留)
5. **分级开发流程(速查)**: L1/L2/L3 三级摘要表 + `@docs/AI_WORKFLOW.md` 导入详细规则
6. **安全红线**: `.env.*`、`eve-jwt.jks`、ESI token 绝不提交;所有输入校验;参数化查询
7. **重要文件**: pom.xml、application.yml、.env.example、宪法位置

删除内容: 所有过期版本号、与 DDD 迁移文档重复的细节、"常见模式"大段代码示例(压缩为要点)。

### 2. 新建 docs/AI_WORKFLOW.md(工具链协作规则)

**分级流程**:

| 级别 | 适用场景 | 流程 |
|------|----------|------|
| L1 | 中大型功能、跨层变更、架构调整 | Spec-Kit 全流程:`/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`;feature 分支顺序编号 |
| L2 | 小改动(单文件/局部逻辑) | Superpowers TDD:先写失败测试(RED)→ 最小实现(GREEN)→ 重构(IMPROVE) |
| L3 | 紧急修复 | `superpowers:systematic-debugging` 定位根因 → 最小修复 → 补回归测试 |

**实现后评审(所有级别强制)**:
- `ecc:java-reviewer`: 所有 Java 代码变更必审
- `ecc:security-reviewer`: 涉及认证、用户输入、外部 API、加密时追加
- 构建失败: `ecc:java-build-resolver` agent 或 `/ecc:build-fix`

**完成前验证**: `superpowers:verification-before-completion`(测试全绿才声称完成)

**Git 规范**: Conventional Commits(feat/fix/docs/chore/test/refactor/perf/ci),中文描述可;提交前跑测试

### 3. 新建 AGENTS.md

单行指针:`Read CLAUDE.md` — 为 Codex/Cursor 等非 Claude 工具提供入口。

### 4. 宪法修订(经 speckit-constitution 流程)

- 原则 I 中 "Every feature exposes functionality via CLI; Text in/out protocol: stdin/args → stdout" 改为 **REST API First**:功能通过 REST API 暴露,统一 `Result<T>` 响应封装,DDD 分层依赖规则
- 保留 Test-First、Integration Testing、API Performance(200ms P95)
- 新增 **技术栈冻结** 原则: 核心依赖清单锁定,升级/替换须走宪法修订程序(MAJOR/MINOR 版本递增 + Sync Impact Report)
- 宪法版本号递增,按模板要求同步 `.specify/templates/`

### 5. 清理动作清单

| 动作 | 对象 | 理由 |
|------|------|------|
| 删除 | `DDD_ARCHITECTURE_MIGRATION.md` | 迁移已完成,git 历史可查 |
| 删除 | `DDD_MIGRATION_PLAN.md` | 同上 |
| 删除 | `eve-helper230926120017005Config.json` | IDEA MyBatisX 临时配置,含失效绝对路径(F:/) |
| 删除 | `eve-helpereveConfig.json` | 同上 |
| 删除 | `eve-helpersystemConfig.json` | 同上 |
| 迁移 | `SKILL.md` → `.claude/skills/eve-helper-patterns/SKILL.md` | 项目编码模式文档,变成 agent 可用的项目技能 |
| 清理 | `.claude/settings.local.json` | 删除 DDD 迁移期一次性 sed/git-mv 权限,保留通用 mvn/git 权限 |
| 移出暂存 | `.env.dev` `.env.aliw` `.env.prd` | 含明文密码,`git rm --cached` 后由 .gitignore 兜底 |
| 更新 | `docs/INDEX.md` | 移除已删文件引用,修正 Spring Boot 2.7.x 链接为 3.5.x,登记 AI_WORKFLOW.md |

### 6. 明确不做的事

- 不改任何 `src/` 业务代码、`pom.xml`、`src/main/resources/application*.yml`
- 不提交用户暂存中的 yml 工作成果(仅移出 .env)
- 不重新初始化 Spec-Kit(已正常)
- 不改变项目的中文文档惯例

## 交付物

1. `CLAUDE.md`(重写)
2. `docs/AI_WORKFLOW.md`(新建)
3. `AGENTS.md`(新建)
4. `.specify/memory/constitution.md`(修订) + 模板同步
5. `.claude/skills/eve-helper-patterns/SKILL.md`(迁移自根目录)
6. 清理:5 个文件删除、settings.local.json 精简、.env 移出暂存、INDEX.md 更新
7. git 提交(按逻辑分组,Conventional Commits)

## 验证标准

- CLAUDE.md 中所有版本号与 pom.xml 一致
- `git status` 中 `.env.*` 不再处于暂存状态且被 ignore
- `.claude/skills/` 下 speckit-* 与 eve-helper-patterns 技能文件完整
- 宪法不含 CLI 条款,含技术栈冻结条款,版本号已递增
- `docs/INDEX.md` 无失效链接(指向已删文件)
- 业务代码零改动(`git diff src/` 中无本次引入的变更)
