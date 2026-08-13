### Task 3: 精简 .claude/settings.local.json

**Files:**
- Modify: `.claude/settings.local.json`(整体替换)

**Interfaces:**
- Consumes: 无
- Produces: 干净的权限配置,后续任务提交此文件

- [ ] **Step 1: 用以下内容整体替换 `.claude/settings.local.json`**

```json
{
  "permissions": {
    "allow": [
      "Bash(mvn test:*)",
      "Bash(mvn -T 4 test)",
      "Bash(mvn -T 4 clean verify -DskipTests)",
      "Bash(git add:*)",
      "Bash(git restore:*)",
      "Bash(git mv:*)",
      "Bash(git push:*)",
      "Bash(tree:*)"
    ]
  }
}
```

(删除的是 DDD 迁移期遗留的一次性 sed/for 循环/git-mv 权限和巨型 commit 消息权限;保留通用 mvn/git 权限。)

- [ ] **Step 2: 验证 JSON 合法**

Run: `python -c "import json;json.load(open('.claude/settings.local.json'))" 2>/dev/null && echo OK || node -e "JSON.parse(require('fs').readFileSync('.claude/settings.local.json'))" && echo OK`
Expected: `OK`(两个解释器任一可用即可;若都不可用,人工检查括号配对)

- [ ] **Step 3: 路径限定提交**

```bash
git commit -m "chore: 清理 DDD 迁移期遗留的一次性工具权限

Co-Authored-By: Claude <noreply@anthropic.com>" -- .claude/settings.local.json
```

---

