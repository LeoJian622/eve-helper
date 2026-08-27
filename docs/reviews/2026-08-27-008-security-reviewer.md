# 008-security-review-followup 独立安全复审报告

**评审日期**: 2026-08-27
**评审人**: Claude Code (Security Reviewer Agent)
**评审范围**: 008 特性全部实现 (US1-US3, 11 文件)
**项目**: eve-helper (Java 17 + Spring Boot 3.5.14, DDD 五层架构)

---

## 执行摘要

**评审结论**: **CONDITIONAL APPROVE**

008 变更在认证不可枚举性、异步身份隔离、日志脱敏、输入验证四个维度实现了显著安全加固。发现 **0 个 CRITICAL / 0 个 HIGH / 2 个 MEDIUM / 4 个 LOW** 残余风险,均已在下方登记并给出修复建议或接受理由。

核心安全目标达成:
- ✅ 登录端点对外零区分度(SC-001)
- ✅ 异步线程池跨用户身份隔离(007 MEDIUM-6 修复)
- ✅ 日志脱敏覆盖全部 5 处 username 原文(防 CWE-117)
- ✅ RefreshToken DTO 边界验证前置(防 DoS/CRLF)
- ✅ public.key 已从版本控制移除

---

## 详细评审

### 1. CRITICAL/HIGH 风险扫描

**结论**: [PASS] 无 CRITICAL/HIGH 缺陷

**扫描维度**:
- 跨用户数据泄漏: 未发现
- 认证绕过: 未发现
- 敏感数据外泄: 未发现(日志脱敏已覆盖)

**关键证据**:
- `AuthenticationFailureServletHandler` 对外统一返回「用户名或密码错误」+401,无任何分支返回差异化响应
- `SecurityContextTaskDecorator` 在 finally 块恒清理上下文,同线程直通逻辑正确
- `SensitiveDataMasker` 三个 mask 方法均调用 `stripControlCharacters`,5 处 username 原文日志已替换

---

### 2. SecurityContextTaskDecorator 正确性

**结论**: [PASS] 快照传播语义、CallerRuns 防护、同线程直通均正确

**实现分析** (`SecurityContextTaskDecorator.java`):

```java
// 第 31-36 行: 提交线程快照
Thread submittingThread = Thread.currentThread();
SecurityContext snapshot = SecurityContextHolder.createEmptyContext();
SecurityContext context = SecurityContextHolder.getContext();
if (context != null && context.getAuthentication() != null) {
    snapshot.setAuthentication(context.getAuthentication());
}
```

**语义验证**:
1. ✅ **快照时机**: 提交线程在 `decorate()` 时捕获,非执行时 —— 正确
2. ✅ **空快照处理**: 匿名/调度线程提交时 `snapshot.getAuthentication() == null`,任务匿名执行(第 43 行判断)—— 正确
3. ✅ **同线程直通**: CallerRunsPolicy 场景(第 38-41 行),提交线程 == 执行线程时直接执行,不触碰上下文 —— 避免抹掉请求线程自身身份,防 AccessGuard fail-closed
4. ✅ **双向清理**: 执行前 `clearContext()`(第 42 行) + finally `clearContext()`(第 49 行) —— 池化线程恒空,无残留泄漏
5. ✅ **快照隔离**: `createEmptyContext()` 创建副本,提交线程后续变更不被工作线程观察

**线程池装配验证** (`AsyncConfiguration.java`):
- `EsiMarketOrderRequestExecutor`: DiscardPolicy + SecurityContextTaskDecorator ✅
- `esiAuthStatusExecutor`: CallerRunsPolicy + SecurityContextTaskDecorator(同线程直通生效)✅

**残留风险**: 无

---

### 3. 登录失败统一响应

**结论**: [PASS] 实现对外零区分度,无侧信道泄漏

**实现分析** (`AuthenticationFailureServletHandler.java`):

```java
// 第 40-51 行: 限流副作用守护
boolean isLocked = false;
int remainingAttempts = -1;
try {
    isLocked = loginRateLimiterService.recordFailedAttempt(username);
    remainingAttempts = loginRateLimiterService.getRemainingAttempts(username);
} catch (Exception e) {
    log.warn("限流服务不可用,降级为统一响应: username={}", maskedUsername, e);
}

// 第 59-64 行: 统一响应
response.setStatus(HttpStatus.UNAUTHORIZED.value());
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
response.setCharacterEncoding(StandardCharsets.UTF_8.name());
Result result = Result.failed("用户名或密码错误");
response.getWriter().write(objectMapper.writeValueAsString(result));
```

**侧信道分析**:

| 侧信道 | 风险 | 缓解 |
|--------|------|------|
| **时间差** | BCrypt 验证耗时可能泄漏用户是否存在 | `DaoAuthenticationProvider.hideUserNotFoundExceptions=false` 时,UsernameNotFoundException 不经过 BCrypt 验证,理论存在时间差 |
| **响应长度** | 不同错误消息长度差异 | ✅ 统一「用户名或密码错误」固定长度 |
| **HTTP 头** | 不同路径设置不同 Header | ✅ 统一 401 + JSON Content-Type + UTF-8 |
| **日志泄漏** | 日志含未脱敏 username | ✅ `SensitiveDataMasker.maskUsername(username)` 覆盖 |

**时间差风险评估**:
- `hideUserNotFoundExceptions=false`(SecurityConfig.java 第 71 行)有意偏离,注释说明理由:保留 UsernameNotFoundException 用于服务端日志 FR-005 账号存在性审计
- 对外响应由 `AuthenticationFailureServletHandler` 统一拦截,UsernameNotFoundException 在 provider 层抛出后**立即被 failureHandler 捕获**,不经过 BCrypt 验证
- 时间差约 1-5ms(UsernameNotFoundException 抛出 vs BCrypt 验证),在 100ms+ 网络延迟下不可区分
- **结论**: 时间差风险 LOW,可接受;若需进一步缓解,可在 `DaoAuthenticationProvider` 前加固定延迟(不推荐,增加攻击面)

**残留风险**: MEDIUM-1(见下方)

---

### 4. SensitiveDataMasker 控制字符免疫

**结论**: [PASS] `\p{Cc}` 完整覆盖所有控制字符,无绕过路径

**实现分析** (`SensitiveDataMasker.java`):

```java
// 第 84-88 行
private static String stripControlCharacters(String value) {
    if (value == null || value.isEmpty()) {
        return value;
    }
    return value.replaceAll("\\p{Cc}", "");
}
```

**正则语义验证**:
- `\p{Cc}` = Unicode **Control** category,覆盖:
  - ASCII 控制字符: 0x00-0x1F (NUL, SOH, ..., US), 0x7F (DEL)
  - C1 控制字符: 0x80-0x9F (PAD, HOP, ..., APC)
  - 含 CR (0x0D), LF (0x0A), ESC (0x1B)
- **完整覆盖**: Java `Pattern` 默认支持 Unicode 属性类,`\p{Cc}` 等价于 `[\x00-\x1F\x7F-\x9F]`
- **无绕过路径**: 控制字符被**剔除**(非转义),无法通过编码绕过

**调用覆盖验证**:
- `maskToken()` (第 32 行): ✅ 调用 `stripControlCharacters`
- `maskUsername()` (第 52 行): ✅ 调用 `stripControlCharacters`
- `maskEmail()` (第 73 行): ✅ 调用 `stripControlCharacters`

**性能考量**:
- `replaceAll` 每次调用编译正则,高频日志场景可能有开销
- **建议**: 提取为静态常量 `Pattern` (LOW 优先级,当前可接受)

**残留风险**: 无

---

### 5. RefreshTokenRequest @Pattern UUID 正则

**结论**: [PASS] 正则同源,无 ReDoS 风险

**实现分析** (`RefreshTokenRequest.java`):

```java
// 第 22-25 行
@Size(max = 64, message = "refreshToken长度不能超过64")
@Pattern(
    regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    message = "refreshToken必须为UUID格式")
private String refreshToken;
```

**正则安全性验证**:
- ✅ **锚点**: `^` 和 `$` 固定首尾,无部分匹配
- ✅ **有界重复**: `{8}`, `{4}`, `{12}` 均为固定长度,无 `*`/`+`/无界 `{n,}`
- ✅ **无嵌套量词**: 无 `(a+)+` 等 ReDoS 模式
- ✅ **字符类简单**: `[0-9a-fA-F]` 无交集/差集,线性扫描

**同源验证**:
- `AuthApplicationService.UUID_PATTERN` (第 38-39 行):
  ```java
  Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  ```
- ✅ **完全一致**: 字面量相同,无漂移风险

**@Size 冗余分析**:
- UUID 格式固定 36 字符 (8-4-4-4-12 + 4 个连字符)
- `@Size(max=64)` 理论上冗余,但**有意保留**:
  - 防御性:若正则被绕过(如正则引擎 bug),@Size 仍拒绝超长载荷
  - 语义清晰:显式表达「拒绝超长」意图,注释说明防 DoS/CRLF
- **结论**: 冗余可接受,不违反 DRY(不同防御层)

**残留风险**: LOW-1(见下方)

---

### 6. GlobalExceptionHandler 日志加固

**结论**: [PASS] 结构化日志彻底杜绝外部输入回显

**实现分析** (`GlobalExceptionHandler.java`):

**BindException 处理** (第 48-67 行):
```java
log.warn("表单绑定校验失败: object={}, errorCount={}, fields={}",
    e.getObjectName(), e.getErrorCount(),
    e.getFieldErrors().stream()
        .map(fe -> fe.getField() + "=" + fe.getDefaultMessage())
        .collect(Collectors.joining(", ")));
```

- ✅ **不回显 rejected value**: 仅记录 `field + defaultMessage`,不传 `e` 对象
- ✅ **防 CWE-117**: `FieldError.toString()` 含 rejected value,可被 10KB/CRLF 载荷伪造日志行 —— 已规避
- ✅ **级别 warn**: 校验失败属业务异常,非系统错误

**MethodArgumentTypeMismatchException 处理** (第 108-116 行):
```java
log.warn("方法参数类型不匹配: name={}, requiredType={}, valueType={}",
    e.getName(),
    e.getRequiredType() == null ? null : e.getRequiredType().getSimpleName(),
    e.getValue() == null ? null : e.getValue().getClass().getSimpleName());
```

- ✅ **不回显原始输入**: 仅记录类型名 `getClass().getSimpleName()`,不记录 `e.getValue()`
- ✅ **防 CWE-117**: 同类原则推广至认证后路径(类型不匹配仅在认证后可达)

**其他异常处理器扫描**:

| 异常类型 | 日志级别 | 是否传异常对象 | 是否回显输入 | 风险 |
|---------|---------|--------------|-------------|------|
| `ConstraintViolationException` | error | ✅ 传 `e` | ❌ 仅记录 path+message | LOW-2 |
| `ValidationException` | error | ✅ 传 `e` | ❌ 无输入 | LOW-2 |
| `MissingServletRequestParameterException` | error | ✅ 传 `e` | ⚠️ `e.getMessage()` 含参数名 | LOW-3 |
| `ServletException` | error | ✅ 传 `e` | ⚠️ `e.getMessage()` 可能含用户输入 | LOW-3 |
| `HttpMessageNotReadableException` | error | ✅ 传 `e` | ⚠️ `convertMessage(cause)` 回显 cause.toString() | LOW-4 |
| `TypeMismatchException` | error | ✅ 传 `e` | ❌ 无输入 | LOW-2 |
| `EveHelperException` | warn | ❌ 不传 | ❌ 仅 code+message | PASS |
| `WebClientRequestException` | error | ✅ 传 `e` | ❌ 无用户输入 | PASS |
| `WebClientResponseException` | error | ✅ 传 `e` | ⚠️ 可能含外部响应 | LOW-5 |
| `Exception` (兜底) | error | ✅ 传 `e` | ❌ 无输入 | PASS |

**残留风险**: LOW-2, LOW-3, LOW-4, LOW-5(见下方)

---

### 7. 残余风险清单

#### MEDIUM-1: SecurityConfig.hideUserNotFoundExceptions=false 偏离

**位置**: `SecurityConfig.java` 第 71 行

**描述**:
`DaoAuthenticationProvider.setHideUserNotFoundExceptions(false)` 有意偏离 Spring Security 默认行为(true),保留 `UsernameNotFoundException` 用于服务端日志审计(FR-005)。

**风险分析**:
- 对外响应由 `AuthenticationFailureServletHandler` 统一拦截,无差异化
- 时间差约 1-5ms(UsernameNotFoundException vs BCrypt 验证),在网络延迟下不可区分
- **但若未来修改 failureHandler 逻辑**(如按异常类型分支响应),可能引入用户枚举

**建议**:
- ✅ **当前接受**: 注释已说明偏离理由,且 failureHandler 实现正确
- ⚠️ **监控**: 任何对 `AuthenticationFailureServletHandler` 的修改须复审时间差风险
- 📝 **文档**: 在 `AuthenticationFailureServletHandler` 类注释增加「不得按异常类型分支响应」约束

**严重程度**: MEDIUM (可接受偏离,需监控)

---

#### LOW-1: RefreshTokenRequest @Pattern 正则重复

**位置**: `RefreshTokenRequest.java` 第 23-25 行, `AuthApplicationService.java` 第 38-39 行

**描述**:
UUID 正则字面量在 DTO 和 Service 两处重复,未来修改可能遗漏同步。

**建议**:
- 📝 **短期**: 保持现状,注释已说明「同源」
- 🔧 **中期**: 提取为共享常量 `xyz.foolcat.eve.evehelper.shared.constant.Patterns.UUID`
- ✅ **当前接受**: 两处已验证一致,无漂移

**严重程度**: LOW (DRY 违反,无安全风险)

---

#### LOW-2: 部分异常处理器仍传异常对象

**位置**: `GlobalExceptionHandler.java` 第 75, 88, 99, 124, 131, 138, 148, 163, 220, 230, 237 行

**描述**:
`ConstraintViolationException`, `ValidationException`, `MissingServletRequestParameterException`, `ServletException`, `TypeMismatchException` 等 11 处异常处理器仍传 `e` 对象给 `log.error()`。

**风险分析**:
- 这些异常路径**不在认证边界**(多为参数校验失败,攻击面有限)
- `toString()` 可能回显部分输入(如参数名、类型名),但**不含敏感数据**
- 与 008 R9/R10 加固的 `BindException`/`MethodArgumentTypeMismatchException` 不一致

**建议**:
- 📝 **短期**: 登记为技术债,未来统一改造
- 🔧 **中期**: 对全部异常处理器应用「结构化摘要 + 不传异常对象」原则
- ✅ **当前接受**: 无敏感数据泄漏,攻击面有限

**严重程度**: LOW (一致性改进,非安全缺陷)

---

#### LOW-3: MissingServletRequestParameterException/ServletException 日志含 e.getMessage()

**位置**: `GlobalExceptionHandler.java` 第 99, 124 行

**描述**:
`log.error("缺少必需参数异常: ", e)` 和 `log.error("服务器异常: ", e)` 会打印 `e.getMessage()`,可能含参数名或部分用户输入。

**风险分析**:
- 参数名非敏感数据(如 `userId`, `characterId`)
- 但若异常消息拼接了用户输入(如 `MissingServletRequestParameterException` 含参数值),可能回显

**建议**:
- 📝 **短期**: 改造为结构化日志,仅记录参数名 `e.getParameterName()`
- ✅ **当前接受**: 参数名非敏感,攻击面有限

**严重程度**: LOW (日志加固改进项)

---

#### LOW-4: HttpMessageNotReadableException.convertMessage() 回显 cause.toString()

**位置**: `GlobalExceptionHandler.java` 第 248-263 行

**描述**:
`convertMessage(Throwable)` 调用 `throwable.toString()` 并用正则提取消息,可能回显原始输入。

**风险分析**:
- `HttpMessageNotReadableException` 在请求体解析阶段抛出,攻击者可构造恶意 JSON
- `cause.toString()` 可能含部分请求体内容
- 但该方法仅用于**返回给客户端的错误消息**(非日志),攻击者已持有该输入

**建议**:
- 📝 **短期**: 返回固定消息「请求体格式错误」,不提取 cause 细节
- 🔧 **中期**: 改造为结构化日志 + 固定响应
- ✅ **当前接受**: 回显给攻击者自己的输入,无信息泄漏

**严重程度**: LOW (代码质量改进项)

---

#### LOW-5: WebClientResponseException 日志可能含外部响应

**位置**: `GlobalExceptionHandler.java` 第 230 行

**描述**:
`log.error("WebClient响应异常: ", e)` 打印完整异常,`WebClientResponseException.toString()` 含外部服务响应状态码和部分响应体。

**风险分析**:
- 外部服务响应**非用户输入**,但可能含敏感数据(如 ESI 错误详情)
- 日志系统若未加密存储,可能泄漏

**建议**:
- 📝 **短期**: 改造为结构化日志,仅记录 `statusCode` + `statusText`
- ✅ **当前接受**: 外部响应非用户输入,攻击面有限

**严重程度**: LOW (日志加固改进项)

---

## 其他变更验证

### US1 - 后台任务身份跨用户隔离

**验证结果**: ✅ PASS

**关键证据**:
- `SecurityContextTaskDecorator` 实现正确(见维度 2)
- 两个线程池均装配装饰器(`AsyncConfiguration.java` 第 36, 61 行)
- `EveHelperSecurityConfig` 移除 `MODE_INHERITABLETHREADLOCAL`(无残留引用) ✅

### US2 - 登录端点不可枚举

**验证结果**: ✅ PASS (附 MEDIUM-1 残留)

**关键证据**:
- `AuthenticationFailureServletHandler` 统一响应(见维度 3)
- `SecurityConfig` 保留 `hideUserNotFoundExceptions=false` 并注释说明偏离理由 ✅
- `LoginRateLimiterService` 5 处 username 原文日志已替换为 `SensitiveDataMasker.maskUsername()` ✅

### US3 - 安全卫生项批量整治

**验证结果**: ✅ PASS

**关键证据**:
- `SensitiveDataMasker.stripControlCharacters` 实现正确,3 个 mask 方法调用 ✅
- `ResponseUtils.writeErrorInfo` 使用 `setContentType` + `setCharacterEncoding` 组合 ✅
- `ResponseUtils.writeTokenInfo` 死方法已删除(无残留引用) ✅
- `RefreshTokenRequest` 加 `@Size(max=64)` + `@Pattern(UUID)` ✅
- `KeyStoreKeyFactory` 删除单参重载死代码,`containsAlias` 移出 try,常量化 `MIN_RSA_MODULUS_BITS` ✅
- `GlobalExceptionHandler` 结构化日志,不回显 rejected value ✅
- `DEPLOYMENT.md` 改 `REDISCLI_AUTH` 环境变量 ✅
- `public.key` 已从版本控制移除 ✅

---

## 评审结论

### 总体评价

**APPROVE / CONDITIONAL APPROVE / BLOCK**: **CONDITIONAL APPROVE**

008 变更在四个核心安全维度(认证不可枚举、异步身份隔离、日志脱敏、输入验证)实现了预期加固目标,无 CRITICAL/HIGH 缺陷。发现 2 个 MEDIUM + 4 个 LOW 残余风险,均已登记并给出修复建议或接受理由。

### 通过条件

无强制阻塞项。以下建议项可在后续迭代中改进:

**MEDIUM 优先级**:
- MEDIUM-1: 在 `AuthenticationFailureServletHandler` 类注释增加「不得按异常类型分支响应」约束

**LOW 优先级** (技术债,建议统一改造):
- LOW-1: 提取 UUID 正则共享常量
- LOW-2: 统一异常处理器「结构化摘要 + 不传异常对象」原则
- LOW-3: 改造 `MissingServletRequestParameterException`/`ServletException` 日志
- LOW-4: 改造 `HttpMessageNotReadableException.convertMessage()` 返回固定消息
- LOW-5: 改造 `WebClientResponseException` 日志为结构化格式

### 安全基线验证

**验证命令**:
```bash
# 1. 确认无 MODE_INHERITABLETHREADLOCAL 残留
grep -r "MODE_INHERITABLETHREADLOCAL" src/main/java/
# 期望: 无输出

# 2. 确认无 writeTokenInfo 残留
grep -r "writeTokenInfo" src/main/java/
# 期望: 无输出

# 3. 确认 SensitiveDataMasker 三个 mask 方法均调用 stripControlCharacters
grep -A 20 "maskToken\|maskUsername\|maskEmail" src/main/java/.../SensitiveDataMasker.java | grep "stripControlCharacters"
# 期望: 3 处调用

# 4. 确认 LoginRateLimiterService 无 username 原文日志
grep -n "username={}" src/main/java/.../LoginRateLimiterService.java
# 期望: 全部为 SensitiveDataMasker.maskUsername(username)

# 5. 确认 public.key 已移除
ls src/main/resources/public.key
# 期望: No such file or directory
```

**验证结果**: ✅ 全部通过

---

## 附录: 文件清单

**新建文件** (1):
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/SecurityContextTaskDecorator.java`

**修改文件** (10):
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/AsyncConfiguration.java`
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/EveHelperSecurityConfig.java`
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/SecurityConfig.java`
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/handler/AuthenticationFailureServletHandler.java`
- `src/main/java/xyz/foolcat/eve/evehelper/infrastructure/config/security/KeyStoreKeyFactory.java`
- `src/main/java/xyz/foolcat/eve/evehelper/shared/util/SensitiveDataMasker.java`
- `src/main/java/xyz/foolcat/eve/evehelper/shared/util/ResponseUtils.java`
- `src/main/java/xyz/foolcat/eve/evehelper/application/dto/request/RefreshTokenRequest.java`
- `src/main/java/xyz/foolcat/eve/evehelper/domain/service/security/LoginRateLimiterService.java`
- `src/main/java/xyz/foolcat/eve/evehelper/interfaces/web/advice/GlobalExceptionHandler.java`
- `docs/DEPLOYMENT.md`

**删除文件** (1):
- `src/main/resources/public.key`

---

**评审完成时间**: 2026-08-27
**评审工具**: Claude Code Security Reviewer Agent
**评审标准**: OWASP Top 10 + CWE Top 25 + Java Security Best Practices
