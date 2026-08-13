### Task 8: 最终验证(对照设计规格)

**Files:** 无变更,纯验证

- [ ] **Step 1: 逐条验证设计规格的验收标准**

```bash
# 1. CLAUDE.md 无过期版本
grep -cE "2\.7\.18|Java 11" CLAUDE.md                               # 期望 0
# 2. .env 不在索引且被 ignore
git diff --cached --name-only | grep '\.env' || echo "OK-env"       # 期望 OK-env
git check-ignore -v .env.dev | head -1                              # 期望有输出
# 3. 技能文件完整
ls .claude/skills/                                                  # 期望 eve-helper-patterns + speckit-* 各项
# 4. 宪法无 CLI 条款、有冻结条款
grep -c "Every feature exposes functionality via CLI" .specify/memory/constitution.md  # 期望 0
grep -c "Technology Stack Freeze" .specify/memory/constitution.md                      # 期望 ≥1
# 5. INDEX.md 无失效链接
grep -c "DDD_ARCHITECTURE_MIGRATION\|DDD_MIGRATION_PLAN" docs/INDEX.md                 # 期望 0
# 6. 用户 WIP 完整保留在暂存区
git diff --cached --name-only                                       # 期望恰好 3 个 application-*.yml
# 7. 业务代码零改动: 本次所有提交不涉及 src/ 与 pom.xml
git log --oneline 53e8893..HEAD --name-only | grep -E "^src/|^pom\.xml" || echo "OK-no-src-changes"
```
Expected: 全部符合注释中的期望值

- [ ] **Step 2: 若有遗漏的未提交变更,按归属路径限定提交;若工作区干净则跳过**

Run: `git status --short`
Expected: 仅剩用户 WIP(暂存的 yml + application-dev.yml 的未暂存修改);.env 文件被 ignore 不显示

- [ ] **Step 3: 向用户汇报**

汇报内容: 新文档体系入口(CLAUDE.md → AI_WORKFLOW.md)、宪法 v1.3.0 要点、清理清单执行结果、.env 安全处理结果、暂存区中用户 WIP 保持原样未提交。
