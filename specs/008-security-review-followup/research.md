# Phase 0 Research: 安全评审遗留修复(008)

**Date**: 2026-08-12(2026-08-13 设计评审后修订 v2)
**Input**: spec.md FR-001~012 + 两份 007 评审记录 + 008 设计安全评审(`docs/reviews/2026-08-13-008-design-security-reviewer.md`)
**方法**:全部决策基于本仓实测源码(文件/行号见各条),无外部推测。

> **v2 修订说明**:设计评审(APPROVE-WITH-NOTES)提出 1 HIGH + 3 MEDIUM,已全部吸收:HIGH-1(GlobalExceptionHandler 日志回显 rejected value)→ 新增 R9;MEDIUM-1(writeTokenInfo 零调用方死代码)→ R6 重写;MEDIUM-2(LoginRateLimiterService 原文 username 日志)→ R5 扩围;MEDIUM-3(DTO 边界拒绝改变 L2 观测输入)→ R4 补记。LOW 五项(限流 fail-open 守护/时序残余/`\p{Cc}` 扩集/FR-010 追溯/装配时序)分别并入 R2/R5/R1 与 tasks 注记。

---

## R1. 线程池身份传播机制(US1 / FR-001~003)

**Decision**:移除 `SecurityConfig.setStrategyName(MODE_INHERITABLETHREADLOCAL)` 覆写(回到默认 `MODE_THREADLOCAL`)+ 新增 `SecurityContextTaskDecorator` 装配到两个 `ThreadPoolTaskExecutor`:

```text
装饰器语义(提交线程上):
  snapshot = createEmptyContext(); snapshot.setAuthentication(当前线程的 Authentication)  // 可为 null(匿名提交)
  submittingThread = Thread.currentThread()

装饰器语义(执行线程上):
  if (Thread.currentThread() == submittingThread) → 原样执行,不触碰上下文   // 同线程直通
  else: clearContext() → 若 snapshot 有身份则 setContext(副本) → run → finally clearContext()
```

**Rationale**:
- `MODE_THREADLOCAL` 从根上切断「池化线程创建时继承提交线程身份」——残留的**源头**消失(FR-001 的一半)。
- 装饰器补上另一半:显式传播(FR-002)+ 任务级双向清理(执行前清残留、结束后清自身)。快照用 `createEmptyContext()` 拷贝而非共享活对象,避免提交线程后续变更被工作线程观察到的串扰(`Authentication` 实例本身由 `JwtAuthorizationTokenFilter:110-112` 每请求新建、全仓无变更点,事实上不可变,共享安全)。
- **同线程直通是本设计的硬约束**:`esiAuthStatusExecutor` 拒绝策略为 `CallerRunsPolicy`(`AsyncConfiguration:57`),队列满时任务在请求线程原地执行;若装饰器在 finally 无差别 `clearContext()`,会抹掉请求线程自身的 `Authentication`,导致该请求后续 `AccessGuard`/`UserUtil` fail-closed 拒绝——把安全修复变成可用性回归。直通分支使 CallerRuns 路径行为与未装饰时完全一致。设计评审复核:CallerRuns 时任务读到的是**本请求自己的**身份,同一用户、无跨用户面,直通不引入新漏洞。
- 快照为 null(调度线程、无身份请求线程提交)时任务匿名执行(FR-003)。
- **附加正收益**(评审发现):`AuthorizeUtil.authorizeInternal` 的「内部通道检测到请求上下文身份即拒绝」不变量,在新模型下从「依赖继承时机」变为「确定性成立」,IDOR 防线更稳。

**实现注记**(评审 LOW-5,实现期必须遵守 + 测试覆盖):
1. `setTaskDecorator` 必须在 `executor.initialize()` **之前**调用(装饰在 initializeExecutor 期固化);
2. 两条提交通路都走 `execute`,装饰均生效:`@Async("beanName")` 按名直连 `ThreadPoolTaskExecutor`,`CompletableFuture.supplyAsync(task, executor)` 同理——`SecurityContextTaskDecoratorTest` 须含「@Async 通路装饰生效」用例;
3. 长期守护:今后新增 `SecurityContextHolder` 读取点必须评估异步传播(固化为评审检查项)。

**Alternatives considered**:
- **`DelegatingSecurityContextAsyncTaskExecutor` 包装**(评审建议方案一):Spring Security 既有类,语义接近;但①其 finally 同样无差别 `clearContext()`,CallerRuns 陷阱仍在(它不感知执行线程是否为提交线程);②`ThreadPoolTaskExecutor` 被包装后 `@Async("beanName")` 按名查找与 `initialize()` 生命周期的装配变复杂。自研装饰器 ~30 行,语义完全受控。
- **保留 INHERITABLETHREADLOCAL + 只在任务结束 clearContext**:评审建议方案二的字面读法。不采纳——继承发生在线程**创建**时,池化线程只在首次创建时继承一次,之后「清理」无法阻止「首个任务来自带身份请求线程」的初始污染;且每次任务都要先判「这个身份是继承的还是传播的」,复杂度更高。
- **`MODE_INHERITABLETHREADLOCAL` + `setThreadContextInheritable(false)`**:Spring 无此开关,不成立。

**任务提交点全量审计**(spec edge case 的回应,实测 grep 完成,设计评审独立复核一致):

| 提交点 | 池/调度器 | 任务体是否读 SecurityContext | 处置 |
|--------|-----------|------------------------------|------|
| `MarketOrderAsyncService.saveAndUpdateMarketOrder` `@Async("EsiMarketOrderRequestExecutor")` | 市场订单池(DiscardPolicy) | 否——纯参数 `List<MarketOrder>` 持久化 | 匿名执行,装饰器传播空快照 |
| `UserApplicationService:119` `CompletableFuture.supplyAsync(..., esiAuthStatusExecutor)` | ESI 状态池(CallerRunsPolicy) | 否——`getAuthorizationStatus(account)` 参数驱动 | 同上;直通分支护住 CallerRuns |
| `MiningTask`/`WalletTask`/`StructTask`/`IndustryTask` 共 8 个 `@Scheduled` | 默认调度器 | 否——DB 数据驱动;且调度线程启动期创建,默认模式下本就无继承 | 匿名执行(FR-003) |
| `UserUtil`/`AccessGuard` 全部调用方(`CharacterController:37`、`CharacterAccessTokenController:60`、`AuthorizeUtil:38,65`、`AccessGuard:47,78`) | 请求线程 | 是,但均在请求线程上 | 不受影响 |

**结论:零任务依赖继承身份,切换无静默失上下文风险。** 实现期以 grep 复核(新增 `SecurityContextHolder` 读取点必须显式评估)。

---

## R2. 登录失败统一化机制(US2 / FR-004~006)

**Decision**:
- `SecurityConfig:75` **保留** `setHideUserNotFoundExceptions(false)`,注释固化理由。
- `AuthenticationFailureServletHandler` 重写:全部 `AuthenticationException`(含锁定/禁用/过期/内部异常)对外**单一措辞「用户名或密码错误」**,401,`Result` 结构不变;细节(username 脱敏、异常类名、isLocked、剩余次数)仅写服务端日志;删除 LOW-7j 的 `exception.getMessage()` 回显分支。
- **(v2,评审 LOW-1)限流调用守护**:handler 内对 `recordFailedAttempt`/`getRemainingAttempts`/`getLockRemainingTime` 的调用以 try/catch 包裹,Redis 故障时仅记日志、**统一 401 恒写出**——否则限流抛异常会把「统一消息」退化为 500,违反 FR-004 字面。计数丢失方向 fail-open 可接受(锁定是缓解不是鉴权)。

**Rationale**:
- 评审建议 `setHideUserNotFoundExceptions(true)`,但实测 Spring Security 语义:hide=true 时 `DaoAuthenticationProvider` 在 **provider 层**把 `UsernameNotFoundException` 转为 `BadCredentialsException`——handler 连「账号是否存在」都看不到,FR-005「存在性仅记录于服务端日志」直接不可实现。spec 优先于评审建议,故偏离。设计评审已独立验证该语义与偏离正当性。
- 泄露面在**响应**不在**异常类型**:handler 统一响应后,`UsernameNotFoundException`(现状消息「用户名或密码错误」无计数)与 `BadCredentialsException`(现状带「剩余尝试次数: N」)对外不再可区分;`InternalAuthenticationServiceException`(现状「用户账号不存在」)并入同一措辞。SC-001 成对比对验收。
- 限流机制(`recordFailedAttempt`/计数/锁定判定)保留不动,仅改呈现(spec Assumption)。
- 现状 `SysUserDetailsService:37` 抛 `UsernameNotFoundException`,与上述分支分析吻合(实测)。

**已知权衡与接受残余**:
- (spec edge case 已录)合法用户无法自行区分输错用户名/口令;锁定对外表现与平常失败一致。
- **(v2,评审 LOW-2)物理时序侧信道接受为残余**:用户存在=2 次 DB 查询(+BCrypt),不存在=1 次查询;Spring 的 dummy BCrypt 比对(`mitigateAgainstTimingAttack`)先于 hide 转换发生、与 hide 取值无关,故保留 hide=false **不恶化**此项;响应级零信息(SC-001)成立,物理层时序非本 feature 能全闭。

**Alternatives considered**:
- hide=true + 在 `SysUserDetailsService` 内打存在性日志:两处执行点、日志与响应逻辑分离,违背「单一执行点」;不采纳。
- 按异常类型返回不同**错误码**同消息:错误码本身即枚举原语;不采纳。
- 锁定场景保留独立消息(「账户已锁定」):FR-004 显式列出锁定须统一,且锁定状态可探知正是 MEDIUM-5 危害面之一;不采纳。

---

## R3. 统一消息措辞与状态码(US2)

**Decision**:消息 = 「用户名或密码错误」;状态码维持 401;响应体 = 既有 `Result.failed(message)` 顶层结构。

**Rationale**:复用现状 `BadCredentialsException` 分支已用措辞,客户端升级面最小;spec Assumption 要求状态码与顶层结构不变。日志格式:`log.warn("登录失败: username={}, reason={}, locked={}, remainingAttempts={}", maskUsername(username), exception.getClass().getSimpleName(), isLocked, remaining)`——reason 用**异常类名**而非 `getMessage()`(后者可能含内部细节,LOW-7j 同源)。

**Alternatives considered**:「登录失败,请重试」类新措辞——无端改变客户端可见文案,无收益。

---

## R4. refresh DTO 边界校验(US3 / FR-008,LOW-2s)

**Decision**:`RefreshTokenRequest.refreshToken` 增加 `@Size(max = 64)` + `@Pattern`(UUID 正则,与 `AuthApplicationService.UUID_PATTERN` 同源);服务层既有 UUID 校验**保留**为第二层防御。

**Rationale**:
- 端点已加白(未认证可达),`AuthController:52` 有 `@Valid`——DTO 注解即边界生效,超长载荷在反序列化后、业务逻辑前被拒。
- max=64:合法 refresh token 为 36 字符 UUID(`TokenService` 生成),64 留足余量且挡掉 KB 级洪泛载荷(spec edge case:兼容 UUID)。
- 服务层保留:防御纵深,且服务层路径(如有非 HTTP 调用方)不依赖 Web 校验。
- 校验失败响应走既有 `MethodArgumentNotValidException`/`BindException` 通路(响应体仅 field+defaultMessage,已干净),与「格式合法但无效」的响应**存在差异**——但 refresh token 是 122bit 随机 UUID,格式差异不泄露任何账号维度信息(007 评审「未构成发现」一节已论证),不构成枚举原语。

**(v2,评审 MEDIUM-3)L2 观测输入语义变化——显式记录**:DTO 边界拒绝发生在 `AuthApplicationService` 之前,垃圾载荷洪泛不再触达 `observeInvalidRefresh()`,007 的 L2 计数器对「非 UUID 洪泛」主向量归零,探测信号移为校验拒绝日志通道(R9 修完后该通道干净、可告警)。处置:**接受**该语义变化并同步修订 007 对 L2 语义的描述(tasks 含文档注记任务);L2 此后只覆盖「UUID 形状但无效」探针。不改 `RefreshRateLimiterService` 代码(008 范围外)。

**(v2,评审 Q4 残余)请求体字节上限**:JSON body 在 DTO 校验前完整反序列化,属容器层纵深防御项,登记为后续 polish,非 008 义务。

**Alternatives considered**:仅服务层校验(现状)——超长字符串仍完整进入业务方法;不采纳。`@Size(max=36)` 精确值——未来若 token 格式演进会误伤,64 更稳。

---

## R5. 日志 CRLF/控制字符免疫(US3 / FR-008,LOW-2s;v2 扩围)

**Decision**:
- `SensitiveDataMasker` 的 `maskToken`/`maskUsername`/`maskEmail` 在返回前剔除 **`\p{Cc}` 全类控制字符**(实现为共享私有方法)。
- **(v2,评审 MEDIUM-2)`LoginRateLimiterService` 纳入范围**:`:42,53,55` 及同型 `:83,119` 共 5 处原文 username 日志改用 `SensitiveDataMasker.maskUsername`——username 是未认证攻击者完全可控的登录表单输入,该类日志在 handler 之前执行,是 `/login` 路径的活 sink。

**Rationale**:
- 三个 mask 方法的输出都会进日志;`maskToken` 的输入(refresh token)完全由攻击者控制且未认证可达,是主攻击面;`maskUsername` 输入(登录表单 username)同样攻击者可控——`AuthenticationFailureServletHandler:42` 现以原文打日志,US2 重写该 handler 时改用 `maskUsername`,一并免疫。
- **剔除集扩为 `\p{Cc}`**(评审 LOW-3):ESC 等其他控制字符仍可污染终端查看/特定消费方;合法输入不含任何控制字符,扩集零误伤。
- 剔除而非转义:转义(`\r`→`\\r`)会改变既有日志消费方的字面匹配假设(spec edge case:不破坏告警/取证解析);合法 token/用户名不含控制字符,剔除零误伤。
- `maskEmail` 同改:同工具类一致性,成本为零。
- **分层合规**(MEDIUM-2 处置确认):`LoginRateLimiterService` 在 domain 层,`SensitiveDataMasker` 在 shared/util;shared 为全层共享内核(CLAUDE.md 分层定义),domain→shared 依赖方向合法。

**Alternatives considered**:Log4j2 `%enc`/`%replace` 布局层过滤——需改日志配置、影响全部日志行、且本项目日志配置属用户管理的运行时资产;在源头脱敏工具收敛更小。长期守护:「外部输入日志必经脱敏/净化」固化为评审检查项(评审残余风险 5)。

---

## R6. token 响应头姿态(US3 / FR-007,LOW-1s)——v2 重写

**实测发现(设计评审 MEDIUM-1,已亲核)**:`ResponseUtils.writeTokenInfo` 在 `src` 内**零调用方**,是死代码。现网 token 响应的实际写出点:登录成功 = `AuthenticationSuccessServletHandler` 直写;refresh 成功 = `AuthController:52-54` 常规 MVC 序列化;二者均无 ACAO,`Cache-Control: no-store` 实际由 **Spring Security 默认 `CacheControlHeadersWriter`** 提供(`CharacterAccessTokenController` javadoc 已记载该依赖)。

**Decision**:
1. **删除死方法 `writeTokenInfo`**(与 LOW-1j 死代码重载同精神);LOW-1s 的两项字面修复(去通配 ACAO、no-store)随方法删除自然达成,SC-003 归档时记录该处置方式偏离(评审字面建议「修改」,实际「删除」,效果等价且更彻底)。
2. **新增端点级契约测试钉死活路径**:登录成功 + refresh 成功响应断言 `Cache-Control` 含 `no-store` 且无 `Access-Control-Allow-Origin`——把框架默认头行为从「事实成立」升级为「受测保证」,防止未来 `.headers(...)` 定制静默回退。
3. `writeErrorInfo` 的通配 ACAO **不动**(范围外,401 体无凭证;已登记后续 polish)。

**Rationale**:FR-007 的语义对象是「携带 token 的响应」而非某个方法;对死代码做加固对生产可观测行为零影响,还会让 SC-003 验收失真。删除 + 契约测试同时满足 FR-007 语义与 SC-003 逐条闭环。

**Alternatives considered**:保留死方法并加固(评审字面建议)——对现网零效果且留下第二个死代码陷阱;不采纳。

---

## R7. ResponseUtils 编码一致(US3 / FR-012,LOW-5j)——v2 重锚定

**Decision**:LOW-5j 原指两方法编码不一致;R6 删除 `writeTokenInfo` 后,LOW-5j 重锚定为:**`writeErrorInfo` 废弃 `";charset=UTF-8"` 字符串拼接**,改 `setContentType(MediaType.APPLICATION_JSON_VALUE)` + `setCharacterEncoding(StandardCharsets.UTF_8.name())`,与 `AuthenticationFailureServletHandler:78-79` 既有风格一致;工具类内仅存单方法,「同类方法行为一致」以「与全仓响应写出风格一致」为锚。

**Rationale**:Servlet 容器对 `setContentType` + `setCharacterEncoding` 的组合输出与拼接式 `application/json;charset=UTF-8` 等价(MockHttpServletResponse 可验证),但类型安全、无拼接魔法值。

**Alternatives considered**:反向统一为拼接式——拼接是 LOW-5j 被点名的写法;不采纳。

---

## R8. 孤立 public.key 移除流程(US3 / FR-009,LOW-3s)

**Decision**:实现时执行 `git rm src/main/resources/public.key` + 提交;**不**改写历史。前置:①全仓引用复核(计划期 grep 已完成:`src/`、`pom.xml`、`application*.yml` 零引用,仅 docs/reviews 与 007 tasks.md 作为评审对象提及);②**用户确认**(spec edge case 要求)。

**Rationale**:文件是公钥非秘密,移除只影响清点噪声;FR-014 精神禁止历史改写。

**Alternatives considered**:保留并加 README 说明——与 FR-009 相悖。

---

## R9. GlobalExceptionHandler 校验拒绝日志加固(v2 新增,HIGH-1)

**实测发现(设计评审 HIGH-1,已亲核)**:`GlobalExceptionHandler:48-49` 对 `BindException` 执行 `log.error("表单绑定异常: ", e)`——带完整异常对象输出,`FieldError.toString()` 含 `rejected value [<原始输入>]`。refresh 端点未认证可达,`@Size`/`@Pattern` 拒绝的 10KB/CRLF 载荷**原样进日志**,可伪造任意日志行;边界拒绝成为洪泛主拒绝点后,该日志是最高频攻击面。contracts C-2 断言 4 在 v1 设计下不可达成。

**Decision**:
- `GlobalExceptionHandler` 的 `BindException` 分支日志改为**结构化摘要**:objectName + field + defaultMessage(注解消息,项目自有文案,安全)+ 错误计数;**不回显 rejected value**,不打印异常对象本体;日志级别由 error 降 warn(参数校验失败属预期拒绝)。
- 响应行为不变(400 + field→defaultMessage 映射,现状已干净)。
- 该日志通道即 R4 中「垃圾洪泛探测信号」的承载通道——修完后干净、可被告警规则消费(MEDIUM-3 闭环)。

**Rationale**:FR-008 的日志免疫契约必须覆盖拒绝路径的实际 sink;只改 `SensitiveDataMasker` 不够,因为此路径根本不经过脱敏工具。收敛到「不回显外部输入」是源头修复。

**Alternatives considered**:对 rejected value 过 `maskToken`——rejected value 未必是 token 形态,且保留「首4末4」无取证价值;直接不回显更干净。

---

## 附:LOW-1j~4j(KeyStoreKeyFactory)处置决策

| 项 | Decision | Rationale |
|----|----------|-----------|
| LOW-1j 死代码重载 | 删除 `getKeyPair(String alias)`(:63-65);实现期 grep 确认零调用方后同步删测试用例 | 评审已核 7 处测试 + `KeyPairConfig:56` 全用双参;单参版把 store 口令当 key 口令,误用陷阱 |
| LOW-2j 全限定名 | `import java.security.Key`,局部变量改 `final Key key;` | 与同文件 6 个既有 `java.security.*` import 风格一致 |
| LOW-3j 自重抛 | `containsAlias` 检查提出 try 块,删 `catch (IllegalStateException e) { throw e; }` | 语义不变;catch 分支存在只为接住 try 内自抛,提出后消失 |
| LOW-4j 硬编码文案 | 「最低要求 2048 位」改由 `MIN_RSA_MODULUS_BITS` 拼接 | 常量改值文案不再撒谎;测试断言随之改为含常量值 |

## NEEDS CLARIFICATION 消解记录

Technical Context 无未决项;spec 无 `[NEEDS CLARIFICATION]`。两处「偏离评审建议」的决策(R2 hide=false、R6 删除替代修改)均已按「spec 优先 + 实测语义」论证并留回归/契约测试守护,设计评审已独立复核认可。

## 宪法复检(plan Phase 1 收尾,v2)

- 分层:新类落 infrastructure/config,domain 层仅 `LoginRateLimiterService` 日志行改用 shared 工具(shared 为共享内核,方向合法)✅
- 冻结条款:零新增依赖(`TaskDecorator`、Bean Validation、`SecurityContextHolder` 全在既有 Boot/Security 托管内)✅
- TDD 映射:9 项决策(R1~R9)→ tasks.md 每任务含 AC ✅
- 复检通过。
