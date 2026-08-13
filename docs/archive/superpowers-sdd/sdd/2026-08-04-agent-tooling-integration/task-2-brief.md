### Task 2: 删除过期文件并迁移 SKILL.md

**Files:**
- Delete: `DDD_ARCHITECTURE_MIGRATION.md`、`DDD_MIGRATION_PLAN.md`、`eve-helper230926120017005Config.json`、`eve-helpereveConfig.json`、`eve-helpersystemConfig.json`
- Rename: `SKILL.md` → `.claude/skills/eve-helper-patterns/SKILL.md`

**Interfaces:**
- Consumes: Task 1 完成(避免提交时误带文件——虽然本任务用路径限定提交)
- Produces: 根目录只剩 CLAUDE.md/Readme.md 等有效文档;`.claude/skills/eve-helper-patterns/` 成为可用项目技能

- [ ] **Step 1: 删除五个过期文件**

```bash
git rm DDD_ARCHITECTURE_MIGRATION.md DDD_MIGRATION_PLAN.md \
  eve-helper230926120017005Config.json eve-helpereveConfig.json eve-helpersystemConfig.json
```

- [ ] **Step 2: 迁移 SKILL.md 到技能目录**

```bash
mkdir -p .claude/skills/eve-helper-patterns
git mv SKILL.md .claude/skills/eve-helper-patterns/SKILL.md
```

- [ ] **Step 3: 验证技能文件 frontmatter 完整**

Run: `head -8 .claude/skills/eve-helper-patterns/SKILL.md`
Expected: 包含 `name: eve-helper-patterns` 和 `description:` 的 YAML frontmatter

- [ ] **Step 4: 路径限定提交**

```bash
git commit -m "chore: 清理过期文档与临时配置,迁移项目编码模式技能

- 删除已完成的 DDD 迁移规划文档(git 历史可查)
- 删除 IDEA MyBatisX 生成器临时配置(含失效绝对路径)
- SKILL.md 迁移至 .claude/skills/eve-helper-patterns/ 成为项目技能

Co-Authored-By: Claude <noreply@anthropic.com>" -- \
  DDD_ARCHITECTURE_MIGRATION.md DDD_MIGRATION_PLAN.md \
  eve-helper230926120017005Config.json eve-helpereveConfig.json \
  eve-helpersystemConfig.json SKILL.md .claude/skills/eve-helper-patterns/SKILL.md
```

- [ ] **Step 5: 验证提交范围**

Run: `git show --stat --oneline HEAD | head -15 && git diff --cached --name-only`
Expected: HEAD 提交只含上述 6 个路径变更;`application-*.yml` 仍在暂存区未被提交

---

