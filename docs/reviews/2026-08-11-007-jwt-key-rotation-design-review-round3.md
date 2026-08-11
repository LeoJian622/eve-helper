# 设计评审记录(第三轮)：007 JWT 签名密钥轮换

**评审阶段**: ③ 计划第三轮复审(代码仍未编写)
**评审对象**: `plan.md` **v3**(主体)+ `spec.md` 第三次修订
**评审工具**: `ecc:security-reviewer`(独立 agent)
**日期**: 2026-08-11
**Feature**: `007-jwt-key-rotation`
**结论**: **APPROVE(有条件)** — 无 CRITICAL;3 项 HIGH 须在进入阶段④(`/speckit-tasks`)前以文本修订闭合(均为设计细节/文档一致性问题,不动摇 v3 架构)。两项 v3 新设计的核心判断经独立读码与实测追踪,**均成立**。

> 前两轮记录:[第一轮 BLOCK](./2026-08-11-007-jwt-key-rotation-design-review.md)(3 CRITICAL)、[第二轮 BLOCK](./2026-08-11-007-jwt-key-rotation-design-review-round2.md)(3 CRITICAL,其中 2 项为 v2 新引入)

---

## 〇、评审方法与证据基线

本轮不重跑全量测试(序 0 基线 `509/F4/E219` 采信,21:47 测定)。全部结论来自独立读码 + 以下实测:

| # | 命令 | 关键输出 |
|---|------|---------|
| E1 | `keytool -list -v -keystore src/main/resources/test-only.jks -storepass 123456` | 别名 `test-only`、PrivateKeyEntry、**2048 位 RSA**、SHA-256 指纹 `FD:9F:19:27:61:...:CA:0F:B4`(与 plan §2 一致)、**⚠️ 密钥库类型: PKCS12**(见 M-4) |
| E2 | `git ls-files '*.jks'` | 仅 `src/main/resources/eve-jwt.jks`(旧生产密钥仍跟踪,与 spec 一致) |
| E3 | `git check-ignore -v src/main/resources/test-only.jks` + `git status --porcelain` | 命中 `.gitignore:65:!src/main/resources/test-only.jks` 反向规则;状态 `??`(未被吞掉,反向放行生效;**但尚未提交**,见 L-6) |
| E4 | `ls target/classes/*.jks` | `eve-jwt.jks`(2172B)+ `test-only.jks`(2750B)→ SC-003 的 RED 证据与 spec/plan 陈述一致 |
| E5 | `find . -name '*.jks'` | 工作区无 `eve-helper.jks`(已移出,与序 0 记录一致) |
| E6 | `git branch` | **无 `007-jwt-key-rotation` 分支**,007 文档提交在 `006-character-access-token-api` 上(见 L-7) |
| E7 | 读码(逐行,见各节引用) | `SecurityConfig`、`JwtAuthorizationTokenFilter`、`RbacAuthorizationManager`、`AuthApplicationService`、`TokenService`、`ResponseUtils`、`GlobalExceptionHandler`、`AuthController`、`UserController`、`LoginRateLimiterService`、`CacheGateway`、`KeyStoreKeyFactory`、`KeyPairConfig`、`SecurityProperties`、`AuthenticationFailureServletHandler`、`AuthenticationSuccessServletHandler`、`ResultCode`、`application.yml`、`application-prod.yml.example`、`JwtFilterDiagnosticTest`、`ResponseUtilsTest` |

---

## 一、A 项裁决:`WhiteUrlMatcher` 设计(plan §3.2)— **成立,可用**

### 1.1 过滤器链追踪(回答「带旧 token 的 `POST:/auth/tokens` 能否到达 controller」)

v3 设计落地后的完整链路(行号为实测):

```
POST /auth/tokens + 旧密钥签发的 access token
 → JwtAuthorizationTokenFilter:66  SignedJWT.parse 成功
 → :71  signedJWT.verify(verifier) == false(新公钥验不过旧签名)
 → [v3]  whiteUrlMatcher.isWhiteListed(request)
         restfulPath = "POST:/auth/tokens" ∈ whiteUrlList → true
 → filterChain.doFilter(request, response)   ← 放行,不写 SecurityContext
 → …(UsernamePasswordAuthenticationFilter 等,均无动作)…
 → AuthorizationFilter → RbacAuthorizationManager.check
 → :56  非 OPTIONS,不走短路
 → :62  restfulPath = "POST:/auth/tokens"
 → :67-73  精确字符串匹配命中白名单
 → :76  return AuthorizationDecision(true)
         ★ 关键点:白名单判定在 :67-77,**先于** :79 的 authenticationSupplier.get(),
           匿名请求不会触发 NPE;也先于 :92 的 Redis RBAC 规则读取
 → AuthController.refreshToken(:51-54,@PostMapping("/tokens"),无认证注解) ✓ 到达 controller
```

**结论:能到达。** 设计成立。两个附带确证:

- 白名单匹配语义在两个消费方之间**逐字一致**(同为 `method + ":" + getRequestURI()` 精确相等),AC#4 的契约测试可固化防漂移。
- **白名单路径零 Redis 成本**:`RbacAuthorizationManager` 的白名单判定(:67-77)位于 Redis hash 读取(:92)之前 → 洪泛请求不进 RBAC 的 Redis 读路径。这是端点加白后重要的抗洪泛结构属性,建议写进 plan 作为显式设计不变量。

### 1.2 「放行但不写认证」是否引入新的授权绕过面?— **否**

- 白名单路径:RBAC 本就不看认证直接放行(:75-77),filter 放行只是把「异常逃逸」换成「匿名到达」,授权结果不变。
- 非白名单路径:filter 仍直写 401 + return,RBAC 不被触及,无变化。
- SecurityContext 侧:STATELESS 会话策略下每个请求从空 context 开始;filter 在放行分支不写入也不残留,无跨请求/跨线程泄漏面。

### 1.3 OPTIONS 短路遗留风险判断 — **正确,可关闭**

plan §3.2 末尾「大概率无影响」的判断经推演成立:CORS 预检不带 `Authorization` 头 → 命中 filter :55-60「非 JWT 不处理」分支 → RBAC :56-58 对 OPTIONS 直接放行。残余场景(OPTIONS 请求携带无效 token)在 v3 下得到 401 而非异常逃逸,无实际客户端影响。**唯一要求**:在 plan 中显式记录「两个消费方对 OPTIONS 语义有意分歧」(RBAC:一切 OPTIONS 放行;filter:OPTIONS 与其他方法同等对待),防止后人误以为是 bug 去「对齐」。

### 1.4 `POST:/user` 现有行为是否被放宽?— **是,但良性,须登记**

现状:带**无效** token 请求 `POST /user` → `InvalidCookieException` 异常逃逸(容器错误页,注册被事实阻断)。v3 后:改为匿名放行,注册正常进行。由于该端点本就允许完全匿名注册(不带 header 即可),攻击者能力无增量,**不构成绕过**。`UserController.addUser`(:33-37)不读 SecurityContext,无隐藏依赖。此项行为变化须作为**有意的变更**写入 plan/DEPLOYMENT,避免日后被当回归「修掉」。

### 1.5 本项发现的问题 → 见 HIGH-2(§5 伪码未含白名单分支)与 M-1(失败模式覆盖不全)

---

## 二、B 项裁决:两层限流(plan §3.3)

### 2.1 作者对第二轮评审前提的修正 — **成立,读码确证**

`AuthApplicationService.refreshToken:104-137` 实际校验链:

```
① 空值(:107)  ② UUID 格式(:110,纯正则,无 Redis)
③ isRefreshTokenValid(:114)→ TokenService:204-207 → cacheGateway.hasKey   ← 第 1 次 Redis
④ getUserIdFromRefreshToken(:119)→ TokenService:130-143 → cacheGateway.get ← 第 2 次 Redis
⑤ loadUserById(:125,查库)  ⑥ queryRoles + refreshAccessTokenWithUser(:132-134)
```

随机 UUID(主攻击向量)在 **③** 即 `hasKey == false` → 抛 `EveHelperException` 于 :116,**④ 永远不执行,userId 解不出来**。故第二轮评审方案「一次 Redis GET 解出 userId → 以 userId 为桶」对主向量无效——**作者的修正是对的,不是又搞错了**。v3 据此把「无 userId 的失败」划给 L2 全局单键,是对评审方案的正确补丁,而非驳回。

### 2.2 L2 单键是否避免「Redis 键空间放大 DoS」?— **是**

`refresh:invalid:global` 为**常量单键**:攻击者无论更换多少随机 UUID,都只 INCR 同一个键;键空间不含任何 token 材料(SCAN/MONITOR/slowlog 无泄密面)。与第二轮驳回的方案 B(每个 token 值种一个新键)有本质区别。L1 键 `refresh:fail:{userId}` 同样有界:攻击者**无法主动构造** L1 键——要让计数落在某 userId 上,必须持有一个 Redis 中真实存在且能解出该 userId 的 refresh token,而后在 ⑤/⑥ 失败(用户恰被删除等竞态),不可伪造。**两层均无无界键空间。**

### 2.3 L2「仅告警/降级不硬拒」到底防住了什么?— **不是安慰性设计,但须如实表述**

如实拆解 L2 的实际作用:

| 作用 | 成立? | 机制 |
|------|-------|------|
| 检测/告警 | ✅ 真实价值 | 单键计数 + 阈值 → 洪泛可观测,运维有反应时间 |
| 速率整形(降级=固定延迟时) | ✅ 真实价值 | 每个失败请求占用 Tomcat 线程多一个延迟时长 → 攻击者 RPS ≈ 并发连接数/延迟,被迫付出连接成本 |
| 阻止 Redis 被洪泛打满 | ❌ 做不到 | 每个攻击请求仍消耗 1 次 EXISTS + 1 次 INCR,L2 不减少单请求成本 |
| 硬性拦截 | ❌ 设计上不做(正确) | 硬拒 = 第二轮已否决的自伤总闸 |

真正的结构性抗洪泛保障是**失败路径的廉价性**:洪泛在 ③ 即止,**不触 DB、不触 token 生成、不触 RBAC Redis 读**(§1.1)。L2 的定位应改写为「**检测 + 速率整形**,为运维响应争取时间;结构性防护来自失败路径零重操作」,而不是「缓解洪泛」之类的含糊表述。

### 2.4 降级形式裁决:**固定延迟,否决 503**(回答 plan §3.3 末尾的提问)

**倾向:固定延迟(建议 100–300ms,仅施加于 ②/③ 失败路径,永不施加于成功路径)。理由:**

1. **503 与 SC-006 直接冲突,且恰在 SC-006 的验收时刻冲突。** 选项 B 下轮换瞬间全体客户端的 refresh 都会在 ③ 失败 → L2 计数在轮换窗口**必然**冲破阈值。若降级动作是 503,则轮换时刻的合法请求全部被挡在 controller 之外——SC-006 要求「到达 controller 并返回明确的业务错误」,503 恰好让它失败。503 就是换了状态码的硬拒,等于把第二轮否决的自伤总闸从后门请回来。
2. 固定延迟不破坏可达性与确定性业务错误:客户端拿到延迟后的 `400 + "Refresh Token无效或已过期"`(`GlobalExceptionHandler:170-197` 对无 ResultCode 的 `EveHelperException` 返回 400,已读码确证),照样跳转登录页。
3. 延迟只加在失败路径 → 持有效 token 的正常 refresh(攻击期间仍在进行的合法流量)不受影响。
4. 多实例下计数器是 Redis 全局共享(真全局视图),延迟是各实例本地执行,无协调成本。

**L2 配套实现约束(须写入 tasks):**
- 窗口用**固定窗口**(`INCR` + `EXPIRE`)而非 plan 所写的「滑动窗口」:滑动窗口需 ZSET(ZADD+ZREMRANGEBYBYSCORE+ZCARD ≥3 条命令/请求),在洪泛路径上反而加重 Redis 负载,与 L2 目的相悖。固定窗口的边界双倍突发对「告警+整形」用途可接受。
- 复用 `LoginRateLimiterService:39-49` 已验证的 `increment` + 首次 `expire` 模式;建议**每次 INCR 后都重设 EXPIRE**(窗口略滑动,但杜绝「INCR 与 EXPIRE 之间崩溃 → 键永不过期 → 永久降级」的竞态)。
- **Redis 故障时 fail-open**(try-catch 跳过 L2):Redis 不可用时 ③ 本身就会失败,refresh 整体不可用,L2 不应新增失败面。
- **告警接入 Prometheus**(pom 已有 metrics 依赖),而非仅日志。
- **阈值定标原则**:窗口内阈值 ≥ 预估轮换尖峰(在线用户数 × 每客户端 refresh 重试次数),使「L2 触发 ≈ 异常流量」,轮换本身不误触发;若阈值低于尖峰,也仅允许延迟型降级(见理由 1)。

### 2.5 对 L1 的诚实性评估 → M-2

---

## 三、SC-003 / FR-019 / FR-008 裁决

### 3.1 SC-003「允许 test-only.jks 进 jar」— **可接受**

测试私钥进生产 jar 的真实风险评估:

| 场景 | 后果 |
|------|------|
| 攻击者从 jar 提取 test-only 私钥并伪造 token | 生产用**真实生产公钥**验签,伪造 token 直接被拒。无影响 |
| 生产误加载 classpath 上的 test-only.jks | 被 FR-008 fail-closed 拦截(生产 profile + classpath location → 拒绝启动)。SC-005 覆盖 |
| 运维把 test-only.jks 复制到 `/etc/eve-helper/` 充当生产密钥 | 超出合理防御边界;但注意 **SC-014 检测不到**(见 L-5) |

结论:风险≈误用风险,防线 FR-008 充分,**前提是** `SecurityBaselineValidatorTest` 显式包含「生产 profile + `classpath:test-only.jks` → 拒绝启动」用例(现行 spec SC-005 表述通用,补一条具体 AC 即可)。

### 3.2 FR-019 例外(test profile 明文口令)— **正当**

测试 keystore 不保护任何真实凭证,口令公开无损失;入库+明文使 CI/新克隆可直跑(这正是其存在目的);与生产的隔离由 fail-closed 校验保证而非保密。别名 `test-only` vs 生产 `eve-jwt` 显著区分(满足第一轮 M3)。例外论证完整,**接受**。

---

## 四、第二轮遗留项逐项核验(不采信自评)

| 项 | 判定 | 核验证据 |
|----|------|---------|
| HIGH-3① gitignore 反向规则 | ✅ 闭合 | E3:规则存在于 `.gitignore:65`,`git status` 以 `??` 出现,未被吞。**遗留**:文件尚未提交(L-6) |
| HIGH-3② fail-closed 为真防线 | ✅ 闭合 | plan §3.4:「仅当 profile 明确属于 {test} 才允许 classpath;未知/缺失按生产处理」,方向正确 |
| HIGH-3③ SC-003 须 `mvn clean` 后验证 | ✅ 闭合 | spec SC-003 已写入;E4 证实 target/classes 残留确会造成假阴性 |
| MEDIUM-1(charset/isCommitted/日志降级) | ✅ 基本闭合 | plan §4 三处均已列。③CORS `Access-Control-Allow-Origin: *` 的登记未延续(L-4) |
| MEDIUM-2(try-with-resources) | ✅ 闭合 | plan §4 KeyStoreKeyFactory 行已列;`KeyStoreKeyFactory:57` 泄漏属实 |
| MEDIUM-3(事实错误更正) | ✅ 闭合 | spec 1.2.1 已更正「同一弱口令」表述;plan §4 test yml 行已对齐 `test-only.jks` |
| MEDIUM-4(SC-011 假门禁) | ✅ 闭合 | spec SC-011 已改人工核验。**但同型风险在 whiteUrlList 上复现,见 HIGH-1** |
| MEDIUM-5(基线门禁) | ⚠️ 部分 | plan §2 已改用例级 diff;但 **§6 仍残留「Errors 数不高于 216」与旧基线 `502/F4/E216」**,与 §2 矛盾(M-3) |
| LOW-1(alias 默认值方向) | ✅ 闭合 | plan §4 已附理由 |
| LOW-2(统一 AUT00210 为有意决策) | ⚠️ 未落 | plan §5 仍未写明「验签失败/过期/撤销统一返回 AUT00210 是防信息泄露的有意设计」,须补一句,防后人当 bug 修(L-3) |
| LOW-3(handler 行号) | ❌ 修错了 | 实测死分支在 `AuthenticationFailureServletHandler:69-70`(:69 `instanceof InvalidCookieException`,:70 message);plan §4「已实测更正」为 `:68-69` 反而错了(:68 是上一分支的 message 行)。L-8 |
| LOW-5(日志只打文件名) | ✅ 闭合 | plan §4 KeyPairConfig 行已列 |
| 第一轮清单#2 残留「查生产 DB sys_permission 覆盖规则」 | ⚠️ 降级为 LOW | 读码确证:`RbacAuthorizationManager` 白名单判定(:67-77)**先于** perm 规则加载(:92),白名单无条件优先 → 既有规则无法干扰加白。作为部署文档项保留即可(L-9) |

---

## 五、发现清单

> **【v3 新引入】** 标注 = 本轮修订新产生或新暴露的问题(前两轮各新引入 2 项,本轮模式延续:**§3 重写后未同步 §1/§5/§6/§7/§8**,共 4 项与此相关)。

### CRITICAL — 无

### HIGH(进入阶段④前必须闭合)

**HIGH-1【遗漏沿袭,SC-011 同型风险复现】`whiteUrlList` 是 List 属性,profile 文件整体覆盖 → 加白可能在生产静默失效**
- 问题:Spring Boot 中 `application-{profile}.yml` 对同名 List 属性是**整体替换**而非合并。plan 只改了入库的 `application.yml:122-124`,但 `application-prod.yml.example:137-138` 自带 `whiteUrlList`(仅 `POST:/user`)——该模板是部署者的起点,意味着实际生产 profile 文件(`ali/aliw/prod`,均不入库)极可能也带有 whiteUrlList 副本。若如此,**FR-020 的加白在生产不生效,SC-006 在测试通过、在生产静默失败**——与 SC-011 假门禁完全同构(grep 仓库恒通过,真文件在仓库外)。
- 证据:E7 读 `application-prod.yml.example:137-138`;`application.yml:122-124`;plan §4 仅列「application.yml 增 POST:/auth/tokens」与「prod 模板补 security.keystore 段」,未提 whiteUrlList 同步。
- 修法:① `application-prod.yml.example` 的 whiteUrlList 同步加 `POST:/auth/tokens`;② 增加交付前人工核验项(与 SC-011 并列):「核验所有生产 profile 实际文件的 whiteUrlList 均含 `POST:/auth/tokens`,或确认其未定义 whiteUrlList(由 application.yml 生效)」;③ 轮换验收(SC-006)必须在**生产 profile 实际配置**下执行,不得仅凭 test profile 结论外推。

**HIGH-2【v3 新引入】L2 降级选项「返回 503」与 SC-006/US1 场景 2 在轮换窗口必然冲突**
- 问题:plan §3.3 把 503 与固定延迟并列为降级选项。选项 B 下轮换瞬间**全体**合法客户端的 refresh 都在 ③ 失败,L2 必然触发;503 会把合法请求挡在 controller 外——恰是 SC-006 验收的时刻。503 = 换状态码的硬拒 = 第二轮已否决的自伤总闸。
- 证据:§2.4 的推演(SC-006 措辞 + `GlobalExceptionHandler:170-197` 业务错误路径 + 轮换清空后所有 refresh 必失败于 ③)。
- 修法:从 plan 中**删除 503 选项**,锁定「固定延迟(100–300ms,仅失败路径)」+ 告警;阈值按 §2.4 定标原则写入 tasks。

**HIGH-3【v3 未同步】plan §5 伪码与 §3.2 白名单放行设计直接矛盾**
- 问题:§5「目标形态伪码」是 v2 残留——验签失败/过期/黑名单/解析异常**全部** `writeErrorInfo + return`,**没有任何白名单分支**;而 §3.2 的消费方改造表与 AC#1/#2 要求白名单路径放行至 controller。若实现者照 §5 写,US1 场景 2/SC-006 在 ParseException 与验签失败路径上全部失败——这正是前两轮「组合验证缺失」的文档版。
- 证据:plan §5 伪码全文 vs §3.2 表格「验签失败时:若 isWhiteListed 为真 → 直接 filterChain.doFilter 放行」+ AC#1/#2。
- 修法:重写 §5 伪码,把白名单分支显式画进**每一个**失败出口(见 M-1 的覆盖范围),并让 §5 与 §3.2 互相引用。

### MEDIUM

**M-1【v3 新引入】白名单放行的失败模式覆盖不完整**
§3.2 表格只写了「**验签失败**时」放行,「关键语义」段也限定「仅在验签失败时」。但 filter 有 5 个失败出口:验签失败(:71-72)、过期(:78-79)、黑名单(:84-86)、ParseException(:100-102)、JOSEException(:103-105)。AC#2 只补了 ParseException。**过期/黑名单 token + 白名单路径**的行为未定义:按现文字面,将返回 401,白名单端点对「签名有效但过期」的旧 token 不可达。
修法:明确规则「白名单路径上**任何** token 校验失败一律匿名放行」(白名单语义 = 该端点不要求认证),AC 扩至全部 5 个出口,与 HIGH-3 的 §5 重写一并完成。

**M-2【v3 新引入】L1 防护价值被高估,且三要素缺失**
读码核验:L1 桶(④ 之后失败)仅覆盖「token 有效但用户恰被删除」等罕见竞态;攻击者无法主动制造(§2.2)。plan 称其「防针对特定账号的滥用」**不成立**——持被盗 refresh token 的攻击者走的是**成功**路径,L1(只计失败)一次都不计数。且 L1 未定义阈值、TTL、触发动作。
修法(二选一):① 最低:改写定位为「按用户异常观测计数器」,补 TTL(建议 60s)+ 告警动作,**明确禁止锁定动作**(按 username/userId 锁定 = 复制 `LoginRateLimiterService:25-26` 的定向锁死 DoS,plan 自己刚谴责过);② 增强(可选):对**每用户成功 refresh 次数**设宽上限(如 >10–20 次/分钟 → 告警)——正常客户端每 900s 才 refresh 一次,该上限只捕获「被盗 token 与受害者互相轮换争夺」这类真实账号级滥用。

**M-3【v3 未同步】§1/§6/§7/§8 残留 v2 陈述,其中 §7 仍在描述被否决的方案**
逐条:① §1 表格把 FR-016 列为序 1「其余一切的前置」且无 WhiteUrlMatcher——与 §2/spec 实现顺序(序 1=WhiteUrlMatcher)矛盾;② §6 仍写基线 `502/F4/E216`、门禁「Errors 数不高于 216」、`test-only-jwt.jks`(v2 命名)、「58 个 @SpringBootTest」——与 §0/§2 的 `509/F4/E219` + 用例级 diff + `test-only.jks` + 59 个直接矛盾(MEDIUM-5 闭合不彻底);③ **§7 风险表仍写「改用『按 refresh token + 全局速率』方案」——那是第二轮 CRITICAL-2 否决的方案**,现行设计是按 userId + 全局单键;④ §8 合宪性检查写测试 keystore「置于 src/test/resources 不进主 jar」——事实错误,实际在 `src/main/resources/` 且**有意进主 jar**(SC-003 修订的依据);⑤ §3 标题仍标「(v2)」。
修法:逐条对齐;§7 那行必须改,否则下一个读者会照被否决的方案实现。

**M-4【新发现(E1)】`test-only.jks` 物理格式是 PKCS12,靠 JDK DualFormat 兼容被 `KeyStore.getInstance("jks")` 加载**
keytool 实测密钥库类型为 **PKCS12**(JDK9+ keytool 默认),而 `KeyStoreKeyFactory:56` 用 `KeyStore.getInstance("jks")`——当前能加载全靠 JKS 实现的 DualFormat 兼容模式。FR-023 重写若把格式处理改「严格」(如显式校验 JKS magic),59 个 `@SpringBootTest` 会集体失败;生产 keystore 若也用新 keytool 生成,同样是 PKCS12。
修法:FR-023 的 AC 增加「能加载 PKCS12 与 JKS 两种物理格式(或统一声明支持的格式并同步生成命令)」;`KeyStoreKeyFactoryTest` 用两种格式的 fixture。

**M-5【设计优化】洪泛路径 Redis 往返可减半**
③ `hasKey` + ④ `get` 是两次 Redis 往返(:114→:119),且存在 TOCTOU(③④ 之间键过期 → `getUserIdFromRefreshToken:135` 抛 `IllegalArgumentException`)。合并为一次 `get`(null 即无效)后,洪泛单请求成本 2→1 命令,竞态消失。`refreshAccessTokenWithUser:158` 的第三次读取保留(撤销前权威校验)。

### LOW

- **L-1** 行号漂移簇(不影响结论,建议顺手修):plan §3.2「RbacAuthorizationManager:66-76/:61/:55-58」实测 `:67-73/:62/:56-58`;「JwtAuthorizationTokenFilter:56-61/63-106」实测 `:55-60/:52-109`。
- **L-2** §5「副作用」清单正确(响应不会被覆写:`writeErrorInfo` 后 return 不再进 chain;`ResponseUtils:41` 手写 UTF-8 字节,中文安全),但 `log.error("JWT解析失败", e)`(:101/:104)的堆栈虽不含 token 原文,nimbus 异常 message 偶含 token 片段——**降为 warn + 只打异常类名**的写法在 §4 已列,落实即可。
- **L-3** LOW-2 未闭合:plan §5 补一句「统一返回 AUT00210 是防信息泄露的有意决策(不区分验签失败/过期/撤销)」。
- **L-4** MEDIUM-1③(CORS `*` 将出现在更多 401 响应上)未在 v3 任何位置登记;`ResponseUtils:38` 属实,补一行风险登记。
- **L-5** SC-014 检测不到「误把 test-only.jks 当新生产密钥部署」(modulus 与旧密钥不同 → 断言通过)。建议 DEPLOYMENT.md 轮换清单加一条:新生产密钥指纹 ≠ test-only.jks 公开指纹(`FD:9F:19:27:61:...:CA:0F:B4`,E1)。
- **L-6** `test-only.jks` 目前是 `??` 未提交状态(E3)。其存在意义就是入库供 CI;序 1 提交时须一并 `git add`,否则「新克隆直跑」不成立。
- **L-7** 无 `007-jwt-key-rotation` 分支(E6):007 全部文档提交在 006 分支上,与 AI_WORKFLOW「feature 分支按编号创建」不符。实现(序 1)开始前建分支;文档提交是否迁移由用户定。
- **L-8** 第二轮 LOW-3 的「更正」方向错了:死分支实测在 `AuthenticationFailureServletHandler:69-70`,plan §4 写成 `:68-69`。
- **L-9** 第一轮「查生产 DB sys_permission 覆盖规则」遗留:代码层面白名单无条件优先(:67-77 先于 :92),无技术风险;保留为部署文档项即可,不再阻塞。
- **L-10**【既有,超范围,仅登记】`TokenService.refreshAccessTokenWithUser` 存在并发双发竞态:两个并发请求持同一 refresh token 时,:158 的 get 都非空 → 各自 revoke + generate,一次轮换发出两套新 token。先于 007 存在,SC-015 的「反复调用被拒」指顺序场景,不受影响。

---

## 六、其余核验结论(用户点名项)

| 问题 | 结论 |
|------|------|
| 实现顺序(spec 末尾表)是否正确 | **正确**。0✅→1 WhiteUrlMatcher→2 FR-016→3 FR-020→4 FR-023→5 FR-005~008→6 清理→7 生产轮换。依赖链验证:序 2 的 401 改造需要序 1 的白名单豁免(否则互拆,CRITICAL-1 复现);序 5 依赖序 4 的错误分类;FR-012 以 aliw 切绝对路径为前置已在文中标注。中间态安全:序 2 单独落地时白名单仅 `POST:/user`,行为严格优于现状(干净 401 替代异常逃逸)。唯一配套动作:plan §1 表格同步(M-3①) |
| 第二轮评审前提修正是否成立 | **成立**(§2.1,读码确证 ③ 先于 ④ 失败,userId 不可得) |
| L2 单键是否避免键空间放大 | **是**(§2.2,常量单键;L1 键亦不可被攻击者构造) |
| L2 是否安慰性设计 | **不是,但须如实表述**(§2.3:检测 + 速率整形为真价值,结构性防护来自失败路径廉价;删除「防住洪泛」类表述) |
| 降级形式 | **固定延迟,否决 503**(§2.4,核心理由:503 在轮换窗口必然打到合法流量并违反 SC-006) |
| SC-003 修订是否可接受 | **可接受**(§3.1);补「生产 + classpath:test-only.jks → 拒启」具体用例与 L-5 指纹核对 |
| FR-019 例外是否正当 | **正当**(§3.2) |

---

## 七、结论与放行条件

**APPROVE(有条件)**。

v3 的两项新设计经独立验证**方向与细节均成立**:WhiteUrlMatcher 的过滤器链追踪闭合(§1.1),限流前提修正属实、单键无放大、两层均无被构造的无界键空间(§2)。与前两轮不同,本轮**未发现推翻方案核心的 CRITICAL**;剩余问题是 v3 的熟悉失误模式——**重写了 §2/§3,未同步 §1/§5/§6/§7/§8**(4 项发现源于此),以及 SC-011 教训未在 whiteUrlList 上迁移(HIGH-1)。

**进入 `/speckit-tasks` 前必须完成(均为文本修订,不改架构):**

1. HIGH-1:prod 模板 whiteUrlList 同步 + 生产 profile 人工核验项 + SC-006 须在生产配置下验收;
2. HIGH-2:删除 L2 的 503 选项,锁定固定延迟 + §2.4 的阈值/窗口/fail-open/Prometheus 约束;
3. HIGH-3:重写 plan §5 伪码,白名单分支覆盖全部 5 个失败出口(含 M-1);
4. M-3:清理 §1/§6/§7/§8 残留 v2 陈述(尤其 §7 被否决方案的描述)。

M-2/M-4/M-5 与 LOW 项不阻塞放行,但须映射为 tasks.md 任务。**上述 1–4 若按本文给出的修法落实,无需第四轮全量评审;由评审人抽查修订 diff 即可放行。**

---

**评审人**: `ecc:security-reviewer`(独立 agent;证据基线见第〇节 E1–E7)
**归档日期**: 2026-08-11
