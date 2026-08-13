# Task 1 Report: 解除 .env 密码文件的 git 暂存

## Status: DONE

## Steps Executed

### Step 1: 确认当前暂存状态
**Command:** `git diff --cached --name-only`

**Output:**
```
.env.aliw
.env.dev
.env.prd
src/main/resources/application-ali.yml
src/main/resources/application-aliw.yml
src/main/resources/application-pro.yml
```

**Result:** ✅ Confirmed — all three `.env.*` files and three `application-*.yml` files were staged.

### Step 2: 从索引移除 .env 文件

**Command (first attempt):** `git rm --cached .env.dev .env.aliw .env.prd`

**Output:**
```
error: the following files have staged content different from both the
file and the HEAD:
    .env.aliw
    .env.dev
    .env.prd
(use -f to force removal)
```

**Note:** The staged content differed from the working tree files. Used `-f` flag as the error suggested. This is safe — `-f` with `--cached` only forces index removal; local files are not touched.

**Command (retry):** `git rm --cached -f .env.dev .env.aliw .env.prd`

**Output:**
```
rm '.env.aliw'
rm '.env.dev'
rm '.env.prd'
```

**Result:** ✅ All three `.env` files removed from the index.

### Step 3: 验证移除成功且 ignore 生效

**Command 3a:** `git status --short | grep '\.env' || echo "OK: .env 不在 git 视野中"`

**Output:**
```
OK: .env 不在 git 视野中
```

**Command 3b:** `git check-ignore -v .env.dev`

**Output:**
```
.gitignore:44:.env.dev	.env.dev
```

**Result:** ✅ `.env` files are no longer tracked by git, and `.gitignore` rule (line 44) has taken over.

### Step 4: 确认用户 WIP 未受影响

**Command:** `git diff --cached --name-only`

**Output:**
```
src/main/resources/application-ali.yml
src/main/resources/application-aliw.yml
src/main/resources/application-pro.yml
```

**Result:** ✅ Only the user's `application-*.yml` files remain staged. No `.env` files in the index.

### Additional Verification: Local files intact

**Command:** `ls -la .env.dev .env.aliw .env.prd`

**Output:**
```
-rw-r--r-- 1 Leojian 197610 471 Jul 30 17:06 .env.aliw
-rw-r--r-- 1 Leojian 197610 454 Jul 30 16:17 .env.dev
-rw-r--r-- 197610 444 Jul 30 16:17 .env.prd
```

**Result:** ✅ All three `.env` files remain on disk, untouched.

## Files / Index State After Task

- **Index (staged):** `application-ali.yml`, `application-aliw.yml`, `application-prod.yml`
- **Untracked (ignored by .gitignore):** `.env.dev`, `.env.aliw`, `.env.prd`
- **Commits created:** None (as required)

## Concerns

None. The task deviated from the brief only in requiring `-f` flag for `git rm --cached`, which was necessary because the staged content differed from both HEAD and the working tree. This is a safe and standard operation for index-only removal.
