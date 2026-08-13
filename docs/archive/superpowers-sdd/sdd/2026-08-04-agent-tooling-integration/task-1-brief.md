### Task 1: 解除 .env 密码文件的 git 暂存(安全优先)

**Files:**
- Modify (index only): `.env.dev`, `.env.aliw`, `.env.prd` — 从索引移除,本地文件保留

**Interfaces:**
- Consumes: 无
- Produces: 后续所有提交不再携带 .env 文件;`.gitignore` 规则开始生效

- [ ] **Step 1: 确认当前暂存状态**

Run: `git diff --cached --name-only`
Expected: 列表中包含 `.env.aliw` `.env.dev` `.env.prd` 和 `src/main/resources/application-*.yml`

- [ ] **Step 2: 从索引移除 .env 文件(本地保留)**

```bash
git rm --cached .env.dev .env.aliw .env.prd
```

- [ ] **Step 3: 验证移除成功且 ignore 生效**

```bash
git status --short | grep '\.env' || echo "OK: .env 不在 git 视野中"
git check-ignore -v .env.dev
```
Expected: 第一条输出 `OK: .env 不在 git 视野中`;第二条输出 `.gitignore:行号:...env...  .env.dev`(证明 ignore 规则接管)

- [ ] **Step 4: 确认用户 WIP 未受影响**

Run: `git diff --cached --name-only`
Expected: 只剩 `src/main/resources/application-ali.yml` `application-aliw.yml` `application-prod.yml`(无 .env)。本任务**不提交**——文件从未入库,移出索引即完成。

---

