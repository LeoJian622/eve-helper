# 评审记录: 008 安全评审遗留修复 — 计划阶段设计安全评审

- **日期**: 2026-08-13
- **评审者**: ecc:security-reviewer(设计评审,只读)
- **关联 feature**: `specs/008-security-review-followup`
- **评审对象**: spec.md / plan.md / research.md(v1) / contracts(v1),并对照现状源码逐条实测
- **结论**: **APPROVE-WITH-NOTES**(1 HIGH + 3 MEDIUM 须设计修订后方可进入 /speckit-tasks)
- **口令处理声明**:本次评审所读文件未触及任何口令字面量;如涉及,一律 `<REDACTED>`。

## 发现表

| 级别 | 设计位置 | 问题 | 建议处置 |
|------|----------|------|----------|
| **HIGH-1** | contracts C-2 断言 4;plan 文件清单(缺 `GlobalExceptionHandler`) | DTO 边界拒绝的日志路径原样输出攻击者可控载荷:`GlobalExceptionHandler:48-49` `log.error("表单绑定异常: ", e)` 带完整异常对象,`FieldError.toString()` 含 `rejected value [<原始 refreshToken>]`——10KB/CRLF 载荷原样进日志,可伪造任意日志行;FR-008 免疫契约在其主要攻击面上不可达成 | `GlobalExceptionHandler` 纳入范围:BindException 日志不回显 rejected value(改 objectName+field+约束消息摘要),同步修订 C-2 断言 4 与 quickstart |
| **MEDIUM-1** | research R6 | FR-007 的设计映射落在死代码上:`ResponseUtils.writeTokenInfo` 在 src/main 零调用方(登录成功由 `AuthenticationSuccessServletHandler` 直写;refresh 成功走 MVC 序列化);现网 no-store 实际由 Spring Security 默认 `CacheControlHeadersWriter` 提供 | 显式决策:保留加固还是删死代码;并新增端点级契约测试把框架默认头钉死为受测行为 |
| **MEDIUM-2** | research R5 | 登录路径 CRLF 免疫遗漏活 sink:`LoginRateLimiterService:42,53,55` 以原文 username 打日志,username 是未认证攻击者完全可控的登录表单输入;该类日志在 handler 之前执行 | `LoginRateLimiterService` 纳入范围:失败路径日志改用 `maskUsername`(domain→shared 依赖方向已确认合法) |
| **MEDIUM-3** | research R4 | L2 洪泛观测输入 regression 未记录:DTO 边界拒绝发生在 `AuthApplicationService` 之前,垃圾载荷洪泛不再触达 `observeInvalidRefresh()`,007 的 L2 计数器对「非 UUID 洪泛」主向量归零 | R4 补决策:接受并修订 007 对 L2 语义的描述,或与 HIGH-1 修复合并使校验拒绝日志成为干净检测通道 |
| LOW-1 | research R2/R3 | 限流副作用先于响应写出:Redis 故障且 `CacheGateway` 抛异常时,统一 401 写不出去,客户端看到 500(对所有用户一致,不构成枚举) | handler 重写时对限流调用 try/catch(失败仅记日志),保证统一响应恒写出 |
| LOW-2 | research R2 | 残余时序侧信道:用户存在=2 次 DB 查询(+BCrypt),不存在=1 次;已核实 hide=false 不恶化此项(Spring dummy BCrypt 先于 hide 转换发生) | 接受为残余风险,记录避免复审重复质疑 |
| LOW-3 | research R5 / contracts C-4 | 剔除集仅 `[\r\n\t]`,ESC 等其他控制字符仍可污染;C-4「输出长度不变」对恶意输入不精确 | 剔除集扩 `\p{Cc}`;C-4 措辞改「对合法输入输出规则不变」 |
| LOW-4 | plan / research(FR-010) | FR-010(DEPLOYMENT.md REDISCLI_AUTH 化)无独立研究决策,追溯链最薄 | tasks.md 显式建 AC(grep 验证命令 quickstart 已备好) |
| LOW-5 | research R1 | 装配正确性依赖 Spring 机制细节:①`setTaskDecorator` 必须在 `initialize()` 之前;②`@Async` 与 `supplyAsync` 两条通路均走 `execute` 装饰生效——已核实成立,但实现时无测试感知即易错 | tasks 增加实现注记 + 装饰器测试覆盖 @Async 通路;「新增 SecurityContextHolder 读取点必须评估异步传播」固化为长期检查项 |

## 重点问题结论(5 问)

1. **R1 装饰器设计足以闭合 MEDIUM-6**:源头切断 + 任务级清理双半区完整;提交点审计经独立 grep 复核属实(零任务依赖继承身份);同线程直通必需且无新漏洞(CallerRuns 读到的是本请求自己的身份,无跨用户面);`createEmptyContext` 副本避免串扰成立;附加正收益——`AuthorizeUtil` 内部通道不变量从「依赖继承时机」变为「确定性成立」。
2. **R2 偏离(hide=false)成立**:实测 Spring Security 语义与论证一致;响应侧零区分度,SC-001 可达成;单一执行点稳固(`ProviderManager` 仅被 formLogin 消费,无旁路写出点);风险转移到日志通道可信度(HIGH-1/MEDIUM-2 必须修)。
3. **US2 日志留痕字段合理**:maskUsername+异常类名+locked+剩余次数,与 FR-005/006 精确吻合,无过度采集;CRLF 免疫 handler 侧闭环,全链路缺口见 HIGH-1/MEDIUM-2。
4. **US3 边界足够**:@Size(max=64)+@Pattern 达成处置目标,格式差异不构成枚举原语;writeErrorInfo 保留通配 ACAO 可接受(范围外已登记);maskToken 剔除式无残余漏洞。残余:请求体字节上限缺失(容器层纵深防御,登记)。
5. **范围无越界,4 处映射缺口**(即 HIGH-1、MEDIUM-1/2/3 + LOW-4);FR-001~006、FR-009、FR-011、FR-012 映射完整且与源码实测吻合(KeyStoreKeyFactory 四处均核实)。

## 残余风险(设计修订并实现后仍留存)

1. 登录时序侧信道(物理层,与 hide 取值无关)
2. `/login` username 无长度上限 → Redis 键洪泛面(008 范围外,登记后续 polish)
3. 请求体字节上限缺失(容器层纵深防御项)
4. `writeErrorInfo` 通配 ACAO 保留至后续 polish
5. 日志免疫依赖源头收敛策略:绕过 `SensitiveDataMasker` 的新日志点会使 CRLF 复活 → 「外部输入日志必经脱敏/净化」固化为评审检查项
6. Spring Security 默认头依赖:处置后由端点级契约测试钉死,在此之前是「事实成立」而非「设计保证」
7. L2 观测语义变化:垃圾洪泛探测移入日志通道,告警规则需随之调整

## 复审约定

HIGH-1 与 MEDIUM-1/2/3 完成设计修订后可直接进入 `/speckit-tasks`(无需第二轮整体设计评审,仅对修订段落定向复核);SC-006 独立安全复审按 quickstart §6 在实现后执行。

---

## v2 修订处置记录(2026-08-13,修订人:speckit-plan 执行者)

| 发现 | 处置 | 落点 |
|------|------|------|
| HIGH-1 | ✅ 已吸收:新增 R9(`GlobalExceptionHandler` BindException 日志结构化摘要,不回显 rejected value,error→warn);C-2 断言 4 重写;quickstart FR-008 日志项补边界拒绝路径;新增 `GlobalExceptionHandlerLogTest` | research R9 / contracts C-2 / quickstart §4 / plan 源码树 |
| MEDIUM-1 | ✅ 已吸收:R6 重写——删除死方法 `writeTokenInfo`,新增 `TokenResponseHeadersContractTest` 钉死登录成功/refresh 成功活路径的 no-store + 无 ACAO;SC-003 归档时记录「删除替代修改」的处置偏离 | research R6/R7 / contracts C-3 / plan 源码树与测试清单 |
| MEDIUM-2 | ✅ 已吸收:R5 扩围——`LoginRateLimiterService` 5 处原文 username 日志改 `maskUsername`;domain→shared 依赖方向确认合法 | research R5 / contracts C-4 / plan 源码树 |
| MEDIUM-3 | ✅ 已吸收:R4 显式记录 L2 观测输入语义变化,接受 + tasks 含 007 描述修订注记;探测通道与 R9 修复合并 | research R4 / contracts C-2 断言 5 |
| LOW-1~5 | ✅ 全部吸收:R2 补限流 try/catch 守护 + 时序残余记录;R5 剔除集扩 `\p{Cc}` + C-4 措辞修订;LOW-4 交 tasks AC;R1 补实现注记(@Async 通路测试 + 长期检查项) | research R1/R2/R5 / contracts C-4 |

**定向复核结论(2026-08-13,ecc:security-reviewer)**:HIGH-1 与 MEDIUM-1/2/3、LOW-1~5 共 9 项修订全部实质闭合(9/9,无一项「字面吸收但实质未修复」);修订未引入范围越界,v2 文档集交叉引用在安全关键点上全部一致。新增 LOW-A~C 为 v2 引入的文档陈述陈旧(门禁表「domain 零改动」、quickstart 覆盖率清单缺 2 文件、决策计数与 FR-012 措辞),tasks 前修正——**已于 2026-08-13 修正完毕**;LOW-D(`GlobalExceptionHandler` `MethodArgumentTypeMismatchException` 分支同类日志面,仅认证后可达)登记为邻近残余,实现期择机处置(①顺手推广 R9 原则 / ②登记 polish,记入 tasks.md)。**放行进入 /speckit-tasks。**
