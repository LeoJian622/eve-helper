### Task 7: 更新 docs/INDEX.md

**Files:**
- Modify: `docs/INDEX.md`(多处定点编辑)

**Interfaces:**
- Consumes: Task 2(文件已删/迁移)、Task 4/5(新文件已就位)
- Produces: 无失效链接的文档索引

- [ ] **Step 1: 移除架构文档表中的 DDD 迁移文档两行**

Edit `docs/INDEX.md`,old_string:
```
| [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md) | DDD架构迁移说明 | 2025-01-29 |
| [DDD迁移计划](../DDD_MIGRATION_PLAN.md) | DDD迁移详细计划 | 2025-01-29 |
```
new_string: (空,整行删除)

- [ ] **Step 2: 更新工具文档表**

Edit `docs/INDEX.md`,old_string:
```
| [CLAUDE.md](../CLAUDE.md) | Claude AI使用指南 | 2026-02-01 |
| [SKILL.md](../SKILL.md) | 技能系统文档 | 2026-02-01 |
```
new_string:
```
| [CLAUDE.md](../CLAUDE.md) | Claude Code 项目指南(架构/约束/流程速查) | 2026-08-04 |
| [AGENTS.md](../AGENTS.md) | 跨 AI 工具入口指针 | 2026-08-04 |
| [AI开发工作流](./AI_WORKFLOW.md) | Spec-Kit + Superpowers + ECC 分级流程 | 2026-08-04 |
| [项目编码模式技能](../.claude/skills/eve-helper-patterns/SKILL.md) | Git 历史提炼的编码模式 | 2026-08-04 |
```

- [ ] **Step 3: 修正五处 DDD 迁移文档链接**

Edit 3a — old: `- **DDD架构**: [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)`
new: `- **DDD架构**: [开发指南 - DDD架构说明](./DEVELOPMENT.md#ddd架构说明)`

Edit 3b — old: `- [DDD架构](../DDD_ARCHITECTURE_MIGRATION.md)`
new: `- [DDD架构](./DEVELOPMENT.md#ddd架构说明)`

Edit 3c — old: `- ✅ [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md)`
new: `- ✅ [AI开发工作流](./AI_WORKFLOW.md)`

Edit 3d — old: `- ✅ [DDD迁移计划](../DDD_MIGRATION_PLAN.md)`
new: `- ✅ [CLAUDE.md](../CLAUDE.md)`

Edit 3e — old: `| 什么是DDD架构? | [DDD架构迁移](../DDD_ARCHITECTURE_MIGRATION.md) |`
new: `| 什么是DDD架构? | [开发指南 - DDD架构说明](./DEVELOPMENT.md#ddd架构说明) |`

- [ ] **Step 4: 修正外部资源版本链接**

Edit 4a — old: `- [Spring Boot 2.7.x](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)`
new: `- [Spring Boot 3.5.x](https://docs.spring.io/spring-boot/reference/)`

Edit 4b — old: `- [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/)`
new: `- [MySQL](https://dev.mysql.com/doc/)`

- [ ] **Step 5: 更新统计与页脚日期**

Edit 5a — old:
```
- **总文档数**: 15个
- **核心文档**: 5个
- **架构文档**: 7个
- **工具文档**: 2个
- **最近更新**: 2026-02-01
- **文档覆盖率**: 95%
```
new:
```
- **总文档数**: 14个
- **核心文档**: 5个
- **架构文档**: 5个
- **工具文档**: 4个
- **最近更新**: 2026-08-04
- **文档覆盖率**: 95%
```

Edit 5b — old: `**最后更新**: 2026-02-01`
new: `**最后更新**: 2026-08-04`

- [ ] **Step 6: 验证无失效引用**

Run: `grep -n "DDD_ARCHITECTURE_MIGRATION\|DDD_MIGRATION_PLAN\|\.\./SKILL\.md\|2\.7\.x" docs/INDEX.md || echo "OK: 无失效引用"`
Expected: `OK: 无失效引用`

- [ ] **Step 7: 路径限定提交**

```bash
git commit -m "docs: 更新文档索引,移除失效引用并登记新文档

Co-Authored-By: Claude <noreply@anthropic.com>" -- docs/INDEX.md
```

---

