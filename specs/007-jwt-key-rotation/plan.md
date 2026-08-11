# Implementation Plan: JWT 签名密钥轮换(选项 B)

**Feature**: `007-jwt-key-rotation`
**规格**: [spec.md](./spec.md)(已按阶段③评审修订)
**评审记录**: [设计评审 BLOCK](../../docs/reviews/2026-08-11-007-jwt-key-rotation-design-review.md)
**创建日期**: 2026-08-11(v2,基于选项 B 重写)
**状态**: **BLOCK(第二轮)** — v2 经复审仍被驳回,3 项 CRITICAL(其中 2 项为 v2 修订**新引入**)。上轮 9 项清单仅 3 项真正闭合。本文件已按复审更新 §2(序 0)、§3.2(CRITICAL-1)、§3.3(限流方案撤回),但**仍需 v3 重写**:白名单共享组件设计、限流按 userId 方案、序 0 后重测基线

> **v1 已废弃**。原计划基于「用户无感」方案,其关键决策论证与测试策略均被评审推翻。本版按选项 B(清空 refresh token + 六表审计,暂不强制改密)重写。

---

## 1. 目标与范围

### 做(代码)

| 序 | 项 | 需求 | 依赖 |
|----|----|------|------|
| 1 | 验签失败返回 401 + `AUT00210` | FR-016 | 无 —— **其余一切的前置** |
| 2 | `POST:/auth/tokens` 可达 + 限流(**不依赖 IP**,见 3.1) | FR-020、M2 | 序 1 |
| 3 | 重写 `KeyStoreKeyFactory` | FR-023 | 无(可与 1/2 并行) |
| 4 | keystore 文件系统加载 + fail-fast | FR-005~007 | 序 3 |
| 5 | 启动基线校验(fail-closed) | FR-008 + 006 L-10 | 序 4 |
| 6 | 配置与模板补全 | FR-018、FR-019 | 序 4 |
| 7 | 清理与文档 | FR-012、FR-013、FR-021 | 全部 |

### 做(文档/清单,非代码)

- `docs/DEPLOYMENT.md` 的轮换步骤、六表审计清单、公告模板(FR-013、FR-021、Q5)

### 不做(运维动作,由用户执行)

| 项 | 为何不由 AI 做 |
|----|--------------|
| 生成新密钥对与新口令 | FR-002:口令不经 AI |
| 部署 keystore 到 `/etc/eve-helper/` 并设权限 | 需服务器访问 |
| 清空 `refresh_token:*` | 生产 Redis 操作 |
| **执行六表审计** | 需业务判断「哪些账号是预期的」—— AI 无此基线 |
| 重启实例、发布公告 | 运维动作 |
| **验证 SC-001/SC-006/SC-014** | 需新口令,故必须人工(M3) |

---

## 2. 实现顺序的强制约束(**已按第二轮评审更正**)

### ⚠️ 序 0 必须先做:恢复测试上下文可加载

**当前工作区状态(2026-08-11 20:06 之后,实测)**:

| 事实 | 证据 |
|------|------|
| 根目录新增 `eve-helper.jks`(2722 字节) | 用户于 20:06 生成,**不在任何 classpath 根上** |
| `application-test.yml:159` 指向 `eve-helper.jks`,别名 `eve-helper` | 已改(非 plan v2 文件清单里写的 `test-only-jwt.jks`) |
| `src/test/resources/` **不存在** | `ls` 确认 |
| `KeyPairConfig.java:44` 用 `ClassPathResource` 解析 | → `FileNotFoundException: class path resource [eve-helper.jks] cannot be opened` |
| **全部 59 个 `@SpringBootTest` 无法加载 ApplicationContext** | 实跑 `JwtFilterDiagnosticTest`:`Tests run: 3, Errors: 3`,全部因 keyPair bean 创建失败 |
| `eve-helper.jks` 未出现在 `git status` | **被 `.gitignore:55` 的 `*.jks` 静默吞掉** —— 评审 HIGH-3 预言的问题已实际发生 |

**两处 plan v2 的陈述由此失效**:

1. ~~「诊断测试 2 个用例以 ERROR 结束 = 天然 RED 状态」~~ —— **错误**。当前 3 个用例**全部** ERROR,且原因是上下文起不来,**与 C2/C3 无关**。修完 FR-016 后它们依然全红,GREEN 判据失效
2. ~~「基线 502 tests / Failures 4 / Errors 216」~~ —— **失效**。该数字测于 keystore 变更之前

> 早前(keystore 变更前)的 C2/C3 实测结论仍然有效 —— 那次运行成功返回了 `401 AUT00201` 与异常逃逸。但**现在无法复现**,须在序 0 后重测。

**序 0 的内容**:
- 决定测试 keystore 的归属:放 `src/test/resources/`(需 `git add -f` 或 `.gitignore` 反向规则),或改 test profile 用绝对路径
- 别名与生产**显著区分**(现 `eve-helper` vs 生产 `eve-jwt` 仅一词之差,不满足 M3)
- 恢复 59 个 `@SpringBootTest` 可加载
- **重新测定回归基线并记录**(不再用固定 Errors 阈值,改为同环境逐用例 diff)

### 序 1:抽出白名单共享匹配组件(CRITICAL-1 的前提)

见 3.2 —— FR-016 与 FR-020 在当前设计下**互相拆台**,必须先解决。

### 序 2:FR-016(401 链路)

原 plan v2 的序 1。理由仍成立(未修时无法观测「旧 token 被拒」),但**它不是最先** —— 序 0、序 1 是其前提。

---

## 3. 关键决策(v2)

| 决策 | 理由 |
|------|------|
| **401 响应在 filter 内直接写出** | 不能依赖 `ExceptionTranslationFilter`(JWT filter 在其上游,实测异常逃逸)。方案:`doFilterInternal` 内 try-catch,调 `ResponseUtils.writeErrorInfo(response, TOKEN_ACCESS_EXPIRED)` 后 **return(不继续 filterChain)**。备选方案「改 filter 顺序至 `ExceptionTranslationFilter` 下游」风险更大 —— 会影响所有认证路径的既有行为 |
| **同时修 `ResponseUtils` 的 switch** | `TOKEN_ACCESS_EXPIRED`(`AUT00210`)现落 `default` → 400。仅改 filter 不改 switch,结果是 400 而非 401,SC-010 仍失败 |
| **白名单加 `POST:/auth/tokens` 是窄授权** | 已核实 `RbacAuthorizationManager:67-73` 用 `restfulPath.equals(white)` **精确字符串匹配**,非 Ant 通配。故加这一条不会误开 `/auth/**` 下其他路径 |
| **限流须新建按 IP 的实现,不能直接复用 `LoginRateLimiterService`** | 已核实其 API 全部按 username 计数(`recordFailedAttempt(String username)` 等 5 个方法)。refresh 请求**没有 username**。方案:抽出按 key 计数的通用逻辑,或新建 `IpRateLimiterService` 复用同一 Redis 计数模式。⚠️ **但"按 IP"本身在当前架构下不成立 —— 见 3.1** |

### 3.2 🔴 CRITICAL-1:FR-016 与 FR-020 在原设计下互相拆台

**这是第二轮评审最重要的发现,我完全没想到。**

过滤器执行顺序决定了两处改造会互相抵消:

```
带旧 access token 的 refresh 请求
  ↓
JwtAuthorizationTokenFilter        ← 位于 UsernamePasswordAuthenticationFilter 之前(SecurityConfig:67)
  ↓ 验签失败
  [FR-016] writeErrorInfo(401) + return     ← 请求在此终止
  ✗ 永远到不了 AuthorizationFilter(链末端)
  ✗ 白名单根本没被读到 —— RbacAuthorizationManager 才是白名单判定处
  ✗ 永远到不了 AuthController.refreshToken
```

**后果**:
- US1 验收场景 2「用旧 refresh token 调 `POST /auth/tokens` **必须到达 controller**」→ 两项都修完后**仍然失败**
- SC-006 → 失败
- 客户端的 `401 → refresh → 401 → refresh` **死循环依然存在** —— 只是从「异常逃逸」变成了「干净的 401 死循环」

**比未修更隐蔽**:HTTP 语义看起来正确了,链路依旧是断的。

**修法**:白名单判定须成为过滤器**可见**的单一事实来源:

| 方案 | 说明 |
|------|------|
| **A(推荐)** 抽出 `WhiteUrlMatcher` | 封装 `method + ":" + uri` 精确匹配,同时被 `RbacAuthorizationManager` 与 JWT filter 消费。filter 在白名单路径上**不拒绝**(仅不写入 `SecurityContext`) |
| B | 覆写 `OncePerRequestFilter.shouldNotFilter`,对白名单路径跳过整个 filter |

**须补的 AC**:「带旧 access token 请求白名单端点 → 到达 controller」。

> 附带:现方案唯一能侥幸跑通的前提是「客户端 refresh 时会摘掉 Authorization 头」—— 该假设从未写进 spec,也不该依赖。服务端必须自己兜住。

### 3.3 🔴 CRITICAL-2:我的限流「纠偏」被否决,须重做

**事实前提成立,但我推出的方案是错的,且比原问题更危险。** 评审逐层驳回:

实测结果(事实部分,评审确认成立):

| 检查项 | 结果 |
|--------|------|
| `X-Forwarded-For` / `X-Real-IP` / `getRemoteAddr` / 取客户端 IP 的代码 | **全仓 0 处** |
| `server.forward-headers-strategy` 配置 | **未配置**(5 个 profile 均无) |

含义:若生产部署在 Nginx / 阿里云 SLB 之后(`application-ali.yml`、`application-aliw.yml` 的命名强烈暗示如此),`request.getRemoteAddr()` 返回的是**负载均衡器的 IP**。后果是全体用户共享同一个限流桶 ——

- 阈值设低 → **误伤全体用户**(一个人触发即全员被限)
- 阈值设高 → **限流形同虚设**(单个攻击者远达不到阈值)

**这不是实现细节,而是 FR-020 的可行性前提。** 但我据此提出的方案被评审逐层驳回:

| 我提的方案 | 评审判定 |
|------|---------|
| ~~**B** 按 refresh token 值限流~~ | **❌ 无效且危险**。①攻击者每次换随机 UUID → 每次都是全新计数 key → 计数恒为 1,**限流永不触发**;它声称防的「同一 token 重放」本已被 `TokenService:181` 的先撤销后换发挡住。②**更严重**:该端点加白后未认证可达,每个不同 token 值都在 Redis 种一个新 key → **无认证的 Redis 键空间放大 DoS**。Redis 是本项目硬依赖(RBAC 规则/refresh token/黑名单全在里面),这比原问题严重得多。③限流 key 含 refresh token 明文,会使机密出现在键空间(`SCAN`/`MONITOR`/slowlog 可见) |
| ~~**C** 全局限流~~ | **❌ 自伤开关**。轮换瞬间全体客户端同时 refresh,全局桶意味着**攻击者只要打满阈值就能让所有合法用户无法恢复会话** —— 恰在人人都必须重新认证的窗口。阈值高则形同虚设(与我拒绝方案 A 的理由同构),低则误伤全员。**B+C 组合比不做限流更糟** |
| **A** 配 `forward-headers-strategy` + 受信代理 | 评审指出我**高估了成本**:这是一处配置项,伪造问题正由受信代理列表解决,不是「独立架构问题」 |

**评审给出的方案(应采纳)**:

1. **按 userId 限流,且只计失败**:先 UUID 格式校验(`AuthApplicationService:110` 已有)→ 一次 Redis GET 解出 userId → 以 userId 为桶。**键空间被真实用户数有界约束**,无 B 的膨胀问题;只计失败则合法尖峰不受影响
2. **全局仅告警/降级,不做硬拒**,避免单点可用性总闸
3. 若确需按 IP,重新评估方案 A

**另须避免**:不要把 `LoginRateLimiterService` 当范本 —— 它按 username 计数、5 次锁 30 分钟(`LoginRateLimiterService:25-26`),意味着任何人可用 5 次错误口令**定向锁死任意已知用户 30 分钟**(既有的账户锁定 DoS,超出 007 范围但不可复制)。

> **结论:FR-020 的限流条款须重写,现方案不得进入实现。这是我的判断错误,评审驳回成立。**

---

## 3.4 其余关键决策(承 3.1)

| 决策 | 理由 |
|------|------|
| **启动基线校验用 fail-closed 正向白名单** | 评审 H4(b):生产判定靠启动参数,误启为 test profile 即完整回退到明文弱口令 + classpath keystore。故判定改为「**仅当 profile 明确属于 `{test}` 才允许 classpath**;未知/缺失 profile 一律按生产处理」 |
| **不实现双密钥并行** | 与选项 B 的全体登出目标冲突,且会延长泄露私钥有效期(详见 spec 第 4 节,论证已按评审重写) |
| **保留 `classpath:` 分支** | 59 个 `@SpringBootTest` 全部加载 `KeyPairConfig`;测试需要可移植路径。由上述 fail-closed 判定在生产侧堵住 |

---

## 4. 新增/修改文件清单

### 新增

| 文件 | 用途 |
|------|------|
| `infrastructure/config/security/SecurityBaselineValidator.java` | `ApplicationRunner`,fail-closed 校验 4 项基线 |
| `domain/service/security/RefreshRateLimiterService.java` | 按 refresh token 值 + 全局速率限流(**不依赖 IP**,见 3.1) |
| `src/test/resources/test-only-jwt.jks` | 测试 keystore,别名 `test-only`(与生产显著区分,见 M3) |
| `src/test/.../JwtAuthFailureResponseTest.java` | 401 链路回归测试(由 `JwtFilterDiagnosticTest` 改写而来) |
| `src/test/.../SecurityBaselineValidatorTest.java` | 各违规组合 → 拒绝启动;非生产 profile → 跳过 |
| `src/test/.../KeyStoreKeyFactoryTest.java` | 异常分类、别名错误、RSA 位数断言、错误信息不含口令 |
| `src/test/.../RefreshTokenEndpointAccessTest.java` | 无 token / 失效 token 时可达 controller;限流生效 |

### 修改

| 文件 | 改动 |
|------|------|
| `JwtAuthorizationTokenFilter.java:64-106` | try-catch 内直接写 401 响应并 return,不再抛 `InvalidCookieException` |
| `ResponseUtils.java:26-35` | switch 增加 `TOKEN_ACCESS_EXPIRED` → 401 分支 |
| `KeyStoreKeyFactory.java` | 全面重写(FR-023) |
| `KeyPairConfig.java:43-51` | 按 `classpath:` 前缀分派 Resource;文件存在性检查 |
| `SecurityProperties.java:26` | **移除 `location` 默认值** `"eve-jwt.jks"`(评审 H4a) |
| `application.yml:122-124` | `whiteUrlList` 增 `POST:/auth/tokens` |
| `application.yml:117` | `${KEYSTORE_ALIAS}` → `${KEYSTORE_ALIAS:eve-jwt}` |
| `.env.example` | 补 `KEYSTORE_LOCATION`、`KEYSTORE_ALIAS` |
| `application-prod.yml.example` | 补 `security.keystore` 段 |
| `application-test.yml`(不入库) | 口令改 `${...}`;location 改 `classpath:test-only-jwt.jks`;alias 改 `test-only` |
| `application-aliw.yml`(不入库) | 口令改 `${...}`;**location 改文件系统绝对路径**(评审 H4a:我原 FR-018 遗漏了 location) |
| `AuthenticationFailureServletHandler.java:68-69` | 清理 `InvalidCookieException` 死分支(评审 L2) |
| `src/main/resources/eve-jwt.jks` | **`git rm`**(FR-012) |
| `docs/DEPLOYMENT.md` | 轮换章节 + 六表审计清单 + 公告模板 |
| `specs/006-.../spec.md` L-10 | 标注「已由 007 实现」 |

---

## 5. 401 链路改造设计(FR-016)

```java
// JwtAuthorizationTokenFilter.doFilterInternal 目标形态(伪码)
try {
    SignedJWT signedJWT = SignedJWT.parse(token);
    if (!signedJWT.verify(verifier)) {
        ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);
        return;                        // 关键:不继续 filterChain
    }
    // ... 过期检查、黑名单检查同样改为 writeErrorInfo + return
} catch (ParseException | JOSEException e) {
    log.warn("JWT 校验失败: {}", e.getClass().getSimpleName());   // 不记 token 内容
    ResponseUtils.writeErrorInfo(response, ResultCode.TOKEN_ACCESS_EXPIRED);
    return;
}
```

配套 `ResponseUtils.writeErrorInfo` 的 switch 增加:

```java
case ACCESS_UNAUTHORIZED:
case TOKEN_INVALID_OR_EXPIRED:
case TOKEN_ACCESS_EXPIRED:          // ← 新增,现落 default → 400
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
```

**须注意的副作用**:改为「写响应 + return」后,原先靠异常中断的路径变为正常返回。须确认:
- 响应未被后续 filter 覆写(`ResponseUtils` 已 `setStatus` + 写 body,`return` 后不再进 chain,应无覆写)
- 日志**不得**打印 token 内容(现 `:101` `log.error("JWT解析失败", e)` 会带堆栈,须确认堆栈不含 token)

### 5.1 已实测:`ResponseUtils` 的现状(排除了两个疑虑)

新增 `ResponseUtilsTest`(4 用例,现状固化型)实测结果:

| 观测项 | 实测值 | 对 FR-016 的含义 |
|--------|--------|-----------------|
| `TOKEN_ACCESS_EXPIRED`(`AUT00210`)状态码 | **400**(落 switch `default`) | ✅ 确认须加 401 分支。该测试的 `assertEquals(400, ...)` 就是 RED 判据 —— 实现后改为 401 |
| `TOKEN_INVALID_OR_EXPIRED`(`AUT00201`) | 401 | 已正确 |
| `ACCESS_UNAUTHORIZED`(`AUT00301`) | 401 | 已正确 |
| 中文 msg 按 UTF-8 解码 | ✅ 正确 | **编码疑虑排除** |
| `response.getCharacterEncoding()` | **UTF-8** | 容器默认已是 UTF-8 |
| `Content-Type` | `application/json`(**无 charset**) | 小瑕疏:JSON 默认 UTF-8(RFC 8259),多数客户端可正确处理。**不阻断 FR-016**,可选改进 |

**结论**:switch 只需**新增一个 case**(`TOKEN_ACCESS_EXPIRED` 并入现有 401 分支),无需重写;`ResponseUtils` 虽未显式 `setCharacterEncoding`(对比 `AuthenticationFailureServletHandler:79` 设了),但它手工 `body.getBytes(UTF_8)` 直写字节,绕开了容器编码,故中文安全。

---

## 6. 测试策略(v2)

| 层 | 测试 | 关键断言 |
|----|------|---------|
| **401 链路** | `JwtAuthFailureResponseTest`(由诊断测试改写) | 验签失败 → **HTTP 401** 且 body code `AUT00210`;过期 token → 401;黑名单 token → 401;**响应体不含 token 片段** |
| **refresh 可达** | `RefreshTokenEndpointAccessTest` | 无 token → 到达 controller(非 401 `AUT00201`);失效 token → 到达 controller;refresh token 不存在 → 明确业务错误;同一 refresh token 反复调用 → 限流拒绝;全局速率超限 → 拒绝 |
| **KeyStoreKeyFactory** | `KeyStoreKeyFactoryTest` | 别名不存在 → 错误信息含「别名」而非「Cannot load keys」;口令错 → 错误信息**不含口令**;文件不存在 → 明确路径;RSA <2048 → 拒绝 |
| **启动校验** | `SecurityBaselineValidatorTest` | 4 项各自违规 → 拒绝;profile 缺失/未知 → **按生产校验(fail-closed)**;`test` profile → 允许 classpath |
| **回归** | 既有 58 个 `@SpringBootTest` | 换 `test-only-jwt.jks` 后仍能签发/验签;**Errors 数不高于 216** |

**变异测试**(比照 006):
- `ResponseUtils` 的新 case 删掉 → 401 断言应失败(证明不是靠别的路径蒙对)
- filter 的 `return` 删掉 → 应有测试捕获(请求继续走到 controller)
- 基线校验的 fail-closed 改为 fail-open(未知 profile 放行)→ 应有测试失败
- 限流阈值改为无限 → 限流测试应失败

**基线**:`502 tests / Failures 4 / Errors 216`。Errors 因测试库缺角色 `2112818290` 的 ESI 授权行,与本 feature 无关。

> ⚠️ **回归风险最高的一处**:改 401 链路会影响**所有**带无效 token 的请求路径。既有 4 个 Failures 中 `CharacterControllerTest.addCharacterAuth` 就是 401 —— 须确认其断言不因本改动而变化。

---

## 7. 风险

| 风险 | 缓解 |
|------|------|
| **401 链路改造影响既有认证行为** | 最大回归面。序 1 单独提交,跑全量对比 Failures/Errors 逐用例;特别核对 `CharacterControllerTest.addCharacterAuth`(现 401) |
| refresh 端点加白后被滥用 | FR-020 的限流是交付前提而非可选;白名单精确匹配不扩散 |
| ~~按 IP 限流取到反代 IP~~ **已规避** | 已查明全仓无真实 IP 处理且未配 `forward-headers-strategy`(见 3.1)。改用「按 refresh token + 全局速率」方案,不依赖 IP,该风险不存在 |
| 58 个 `@SpringBootTest` 集体失败 | 序 3/4 前先备好 `test-only-jwt.jks`,分两次提交 |
| 六表审计无基线可比 | **AI 无法判断「哪些账号是预期的」**。`DEPLOYMENT.md` 须给出审计**方法**(SQL + 判断依据),结论由用户填写 |
| 多实例滚动重启 | FR-022:禁止滚动;须先确认拓扑 |

---

## 8. 合宪性检查

| 条款 | 符合性 |
|------|--------|
| 第四条 技术栈冻结 | ✅ 无新依赖。`ApplicationRunner`/`FileSystemResource` 为 Boot 自带;限流复用既有 Redis 模式 |
| 第五条 Spec-First | ✅ 规格已修订并提交(`dd3d3c1`);本文件为阶段③ v2 |
| DDD 分层 | ⚠️ `RefreshRateLimiterService` 置于 `domain/service/security`(与既有 `LoginRateLimiterService` 同包)。它依赖 `CacheGateway` 端口,不直接依赖 Redis,符合分层 |
| TDD | ✅ 诊断测试已提供天然 RED 状态 |
| 安全红线 | ⚠️ 测试 keystore 入库属论证过的例外(不保护真实凭证,别名 `test-only`,置于 `src/test/resources` 不进主 jar) |

---

## 9. 阶段④ 拆解方向

按第 1 节的 7 个序号拆,每序一个提交。序 1、2 建议各自独立提交并跑全量回归 —— 它们改动认证主链路。

**交付边界**:代码 + 测试 + 文档就绪即为本 feature 完成。生产轮换与六表审计由用户执行,其结果回填 `DEPLOYMENT.md`。

---

**下一步**: `ecc:security-reviewer` 复审本计划(v1 已 BLOCK,须确认 9 项清单均已落入设计),通过后 `/speckit-tasks`。
