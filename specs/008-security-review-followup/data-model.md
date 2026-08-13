# Phase 1 Data Model: 安全评审遗留修复(008)

**Date**: 2026-08-12

## 结论:无持久化实体变更

本 feature 为行为加固(spec Key Entities 已声明):MySQL 两库模式、Redis 键空间、git 跟踪的密钥材料布局均不变(`public.key` 删除除外,见 BE-3)。以下记录两个**行为对象**的状态与生命周期约束,供 tasks.md 的测试设计引用。

---

## BE-1 登录失败响应(双通道)

| 通道 | 载体 | 修复前 | 修复后 |
|------|------|--------|--------|
| 对外响应 | HTTP 401 + `Result.failed(message)` | 按异常类型 7 种措辞(含「用户账号不存在」「剩余尝试次数: N」「账户已锁定,请在N分钟后重试」)+ else 分支回显 `exception.getMessage()` | **单一措辞**「用户名或密码错误」,全部凭证类失败不可区分(SC-001) |
| 对内日志 | `log.warn` 结构化字段 | 仅 `username + reason 原文` | `username=maskUsername(…)`,`reason=异常类名`,`locked=bool`,`remainingAttempts=int` |

**状态转换**:登录失败事件 →(记录失败计数,机制不变)→ 未锁定:统一消息;已锁定:同一统一消息(现状为独立锁定消息,FR-004 收编)。
**不变量**:① 任意两次不同原因的失败,对外响应逐字节一致(状态码 + 响应体);② 限流计数/锁定判定语义零变化;③ 日志必须可还原账号存在性与剩余次数(FR-005)。

## BE-2 任务身份上下文(生命周期重约束)

| 阶段 | 修复前 | 修复后 |
|------|--------|--------|
| 池化线程创建 | `InheritableThreadLocal` 从创建者线程继承 `Authentication`,终身残留 | 默认 `MODE_THREADLOCAL`,创建即空 |
| 任务提交 | 无快照 | 装饰器在提交线程捕获快照(`createEmptyContext` 副本,可为匿名) |
| 任务执行前 | 读得到残留身份 | 显式 `clearContext()` 后按快照设置(有则设、无则匿名) |
| 任务执行后 | 残留继续存在 | `finally clearContext()` |
| CallerRuns 回退 | 请求线程执行,上下文为其自身 | **同线程直通**:不触碰请求线程上下文(防可用性回归,research.md R1) |

**不变量**(SC-002 逐条对应):① 复用工作线程执行新任务前身份恒为空;② 任务结束后身份恒为空;③ 需身份任务拿到的是提交者快照而非线程残留;④ 调度任务恒匿名。

## BE-3 密钥材料布局(仅删一项)

| 对象 | 变化 |
|------|------|
| `src/main/resources/public.key` | `git rm`(零引用已核;不改写历史) |
| `test-only.jks` / `keystore-fixtures/*` | 不动(007 有意决策) |

## BE-4 卫生项行为对象(摘记)

| 对象 | 修复后不变量 |
|------|--------------|
| token 响应头(活路径) | 登录成功/refresh 成功响应恒含 `Cache-Control: no-store`、无 `Access-Control-Allow-Origin`(契约测试钉死;死方法 `writeTokenInfo` 删除,v2 R6) |
| refresh 输入边界 | 长度 ≤64 且 UUID 正则,否则边界拒绝;服务层第二层防御保留 |
| 校验拒绝日志(v2 R9) | `BindException` 日志为结构化摘要,不含 rejected value 原文 |
| `SensitiveDataMasker.mask*` 输出 | 不含 `\p{Cc}` 控制字符(合法输入输出规则不变) |
| 登录限流日志(v2 R5) | `LoginRateLimiterService` 全部 username 日志经 `maskUsername` |
| `ResponseUtils`(仅存 `writeErrorInfo`) | `setContentType` + `setCharacterEncoding` 风格,无字符串拼接 charset |
| `KeyStoreKeyFactory` | 仅双参 `getKeyPair`;无自重抛分支;位数文案与常量同源 |
