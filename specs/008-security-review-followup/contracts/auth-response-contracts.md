# Interface Contracts: 认证失败与刷新校验响应(008)

> 本 feature 不新增端点。以下为**既有端点的行为契约变更**,供 tasks.md 契约测试与 SC-001 成对比对引用。
> **v2(2026-08-13 设计评审后)**:C-2 断言 4 修订(HIGH-1)、C-3 重锚定到活路径(MEDIUM-1)、C-4 措辞修订(LOW-3)。

## C-1 POST /login — 登录失败响应契约(US2)

### 变更前(现状,可区分 → 枚举原语)

| 失败原因 | HTTP | msg |
|----------|------|-----|
| 口令错误(BadCredentials) | 401 | `用户名或密码错误，剩余尝试次数: N` |
| 用户不存在(UsernameNotFound,hide=false 透传) | 401 | `用户名或密码错误`(无计数 → 与上行可区分) |
| 内部异常(InternalAuthenticationService) | 401 | `用户账号不存在` |
| 锁定 | 401 | `账户已锁定，请在N分钟后重试` |
| 其他 AuthenticationException | 401 | `登录失败: <原始 exception.getMessage()>`(LOW-7j 回显) |

### 变更后(契约)

| 失败原因(任意凭证类) | HTTP | 响应体 |
|------------------------|------|--------|
| 账号不存在 / 口令错误 / 锁定 / 禁用 / 过期 / 内部异常 / 其他 | **401** | `{"code":"<failed>","msg":"用户名或密码错误","data":null}`(顶层结构 = 既有 `Result.failed(String)`,逐字段不变) |

**契约断言**(SC-001):
1. `已注册账号+错误口令` 与 `未注册账号+任意口令` 的响应(状态码 + 完整响应体)**逐字节一致**
2. 锁定态失败与上述响应亦逐字节一致(FR-004)
3. 响应体不含剩余尝试次数、不含异常类名、不含 `exception.getMessage()` 任何片段(FR-005/006)
4. 服务端日志可还原:username(脱敏)、失败原因(异常类名)、locked、remainingAttempts
5. **(v2)** 限流后端(Redis)故障时,统一 401 响应仍恒写出(限流调用 try/catch 守护,research R2)

### 不变部分

- 状态码恒 401;`Content-Type: application/json;charset=UTF-8`
- `LoginRateLimiterService` 计数/锁定判定语义不变(仅呈现变化 + 日志脱敏)
- 成功登录路径零变化

## C-2 POST /auth/tokens — 刷新输入边界契约(US3 / FR-008)

### 变更后校验层

| 输入 | 拒绝层 | 行为 |
|------|--------|------|
| 空白 refreshToken | DTO `@NotBlank`(现状保留) | 边界拒绝,不进入业务逻辑 |
| 长度 > 64 | DTO `@Size(max=64)` **新增** | 边界拒绝(`BindException` 既有通路) |
| 非 UUID 格式 | DTO `@Pattern` **新增** | 边界拒绝 |
| UUID 格式但无效 | 服务层(现状保留,第二层防御) | `Refresh Token无效或已过期` + L2 观测 |

**契约断言**:
1. 10KB 超长载荷在 DTO 层被拒,不触达 `TokenService`(FR-008 边界)
2. 合法 36 字符 UUID 不被误伤(spec edge case)
3. 校验失败响应不含账号维度信息(refresh token 为 122bit 随机 UUID,无枚举语义);响应体仅 field+defaultMessage 映射(现状行为,保持)
4. **(v2,HIGH-1 修订)** 边界拒绝产生的服务端日志为结构化摘要(objectName/field/defaultMessage/计数),**不含 rejected value 原文**、不含异常对象本体;含 `\r\n` 的载荷不能在日志中伪造条目(`GlobalExceptionHandlerLogTest` 守护,research R9)
5. **(v2,MEDIUM-3 记录)** 垃圾载荷洪泛的探测信号由 L2 计数器移为本契约断言 4 的校验拒绝日志(L2 此后仅覆盖「UUID 形状但无效」探针)

## C-3 token 响应头契约(US3 / FR-007)——v2 重锚定

**活路径事实(设计评审 MEDIUM-1 实测)**:token 响应实际写出点为登录成功(`AuthenticationSuccessServletHandler` 直写)与 refresh 成功(MVC 常规序列化);`ResponseUtils.writeTokenInfo` 为零调用方死代码,**删除**。现网 `Cache-Control: no-store` 由 Spring Security 默认 `CacheControlHeadersWriter` 提供。

| 响应头 | 变更前(活路径) | 变更后(契约) |
|--------|------------------|----------------|
| `Access-Control-Allow-Origin` | 无(活路径本无;死方法有 `*`) | **无该头**(死方法删除,契约测试钉死) |
| `Cache-Control` | `no-store`(框架默认,未受测) | `no-store`(**契约测试钉死**,防未来 `.headers(...)` 定制静默回退) |

**契约断言**(`TokenResponseHeadersContractTest`):登录成功与 refresh 成功响应均含 `Cache-Control: no-store` 且无 `Access-Control-Allow-Origin`。

## C-4 日志免疫契约(US3 / FR-008)

`SensitiveDataMasker.maskToken/maskUsername/maskEmail` 对任意输入(含 `\p{Cc}` 控制字符构造的伪造条目)输出恒不含控制字符(剔除集全类 `\p{Cc}`,research R5);**对合法输入**(不含控制字符)输出规则(可见前缀/后缀、长度)不变,消费方解析假设不破坏。

免疫覆盖的未认证输入 sink:`AuthApplicationService` refresh 日志(maskToken)、`AuthenticationFailureServletHandler` 登录失败日志(maskUsername)、`LoginRateLimiterService` 失败/锁定日志(maskUsername,v2 扩围)、`GlobalExceptionHandler` 校验拒绝日志(不回显外部输入,R9)。
