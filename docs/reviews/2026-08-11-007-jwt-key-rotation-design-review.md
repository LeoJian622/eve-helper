# 设计评审记录：007 JWT 签名密钥轮换

**评审阶段**: ③ 计划(设计评审,代码尚未编写)
**评审工具**: `ecc:security-reviewer`
**日期**: 2026-08-11
**Feature**: `007-jwt-key-rotation`
**结论**: **BLOCK** — 3 项 CRITICAL,方案的三个核心前提经实测均不成立

---

## 一、评审对象与我的初稿错误

评审对象:[`spec.md`](../../specs/007-jwt-key-rotation/spec.md)(阶段②)+ [`plan.md`](../../specs/007-jwt-key-rotation/plan.md)(阶段③)。

规格 1.1 节自称「已核实,勿重复质疑」。评审逐条读代码复核,**大部分为真,但发现三个会直接推翻核心方案的问题**。评审明确指出该措辞不当:「本次评审正是因为重新核实才发现 C1~C3」。**该批评成立,措辞已修正。**

---

## 二、我已实测确认的三项(不是照抄评审结论)

评审的 C2/C3 均可实测。我写了诊断测试 `JwtFilterDiagnosticTest`(3 用例,只观测不断言)实际发请求验证:

### C3 — 验签失败**不返回 401**,异常直接逃逸

**实测结果**:`InvalidCookieException` 穿透整个 MockMvc 调用链,测试方法以 ERROR 结束(`InvalidCookie TOKEN错误`),**连响应都未生成**。

比评审描述的更严重 —— 评审推断是 500,实测是**异常逃逸到容器**,生产下由 Tomcat 决定输出(默认错误页)。

**根因链**(评审已从 jar 反编译核实,我复核了配置侧):
- `SecurityConfig.java:67` `addFilterBefore(jwtAuthorizationTokenFilter, UsernamePasswordAuthenticationFilter.class)`
- Spring Security 6 过滤器顺序:`UsernamePasswordAuthenticationFilter` → … → `ExceptionTranslationFilter` → `AuthorizationFilter`
- 即 JWT 过滤器在 `ExceptionTranslationFilter` **上游**,其抛出的异常不被后者捕获
- `AuthenticationServletEntryPoint`(返回 401)由 `ExceptionTranslationFilter` 调用 → 不触及
- `AuthenticationFailureServletHandler:69` 的 `InvalidCookieException` 分支是 `formLogin` 的 failureHandler → 不触及,**是死代码**
- `GlobalExceptionHandler` 是 `@RestControllerAdvice`,只管 DispatcherServlet 内 → 不触及

**附带确证**(我读 `ResponseUtils.java:26-35`):`TOKEN_ACCESS_EXPIRED`(`AUT00210`)**不在** 401 分支,落 `default` → **400**。所以规格 FR-016/SC-010 描述的「401 + AUT00210」在现有代码下是**不可达状态**。

**这推翻了我在规格 6.1 写的**「轮换导致的验签失败必然是 401,裸 401 判据同样会触发重试」—— 该论断是**错的**,也是我据此把 FR-016 降级为建议项的理由。客户端的 401 重试逻辑**不会被触发**。

### C2 — `POST /auth/tokens` 在无有效 access token 时不可达

**实测结果**:

| 场景 | HTTP | body | 是否到达 controller |
|------|------|------|-------------------|
| 无 `Authorization` 头 | **401** | `{"code":"AUT00201","msg":"用户未登录"}` | **否** |
| 带失效 token | **异常逃逸** | 无响应 | **否** |

**配置侧确证**(我 grep 全部 5 个 profile):`whiteUrlList` 只有 `POST:/user`;`SecurityConfig.java:52-55` 的 `permitAll` 只有 swagger 三项 + `websocket/onebot/**`;其余 `anyRequest().access(authorizationManager)`。

**这意味着「凭 refresh token 换新 token」这条路在当前代码下走不通** —— US1、US3、SC-006 全部建立在它之上。

### C2 的后果比评审描述得更完整

轮换后的真实表现:用户既不是「无感」也不是「被弹回登录页」,而是**收到容器错误页且无任何自动恢复路径**。落入评审所称的最坏象限:承担了强制登出的全部用户影响(故 Q5「不通知用户」失去依据),却没获得强制登出的安全收益(未清空 refresh token),还额外制造一次 refresh 尖峰。

---

## 三、C1:我请评审专门审的问题,结论与我的猜测不同

我在评审请求中特别问:「若攻击者已用泄露私钥伪造 token 并借此获取过 refresh token,轮换后是否仍能继续访问?」

**评审结论:这条链不成立,但真正的问题更糟。**

**为何不成立**(评审核实的实现细节,规格从未提及):`refresh_token:{uuid}` 只在两处写入 Redis ——
- `AuthenticationSuccessServletHandler:63`(表单登录成功,需真实口令)
- `TokenService:184`(由 `refreshAccessTokenWithUser` 调用,而该方法要求传入一个**已存在于 Redis 的** refresh token)

伪造 access token 走 `JwtAuthorizationTokenFilter`,该路径**不签发任何 refresh token**。

**真正的持久化向量**(我完全没想到):

1. `POST /user` 在所有 profile 的 `whiteUrlList` 中 —— 攻击者**无需伪造 token**即可注册一个自己的账号(有真实口令)
2. 用伪造的 ROOT token(`RbacAuthorizationManager:128` 对 ROOT 直接放行)**篡改 `sys_permission` 的 url_perm→role 映射,或给自己账号赋 ROOT 角色**
3. 轮换密钥。攻击者的**真实账号 + 真实口令 + 已被提权的角色**完全不受影响

**轮换密钥对此毫无作用。** 而 SC-002(旧 token 401)会「通过」,导致团队错误宣布事件处置完毕。

我的规格 1.2 只写了「可伪造任意用户的 access token,完全绕过 RBAC」,然后在 US3 把残余风险窄化为「若攻击者已窃取某用户的 refresh token」—— 这是**对攻击者能力的严重低估**。

---

## 四、HIGH 项摘要

| 编号 | 问题 | 我的核实/判断 |
|------|------|--------------|
| H1 | 多实例滚动重启期间新旧密钥并存 → 随机认证失败;且 `TokenService:181` 在生成新 token **之前**就删除旧 refresh token,refresh 循环中会消耗掉 refresh token 导致彻底登出 | **成立且我完全遗漏。** 规格从未讨论部署拓扑。`application-aliw.yml` 的命名暗示存在多套部署 |
| H2 | 排除 kid 双密钥并行的**论证**是错的:实际无需 kid header,只需 filter 依次尝试两个 verifier —— 我高估了复杂度;同时低估了单密钥的代价(必须停机) | **论证确实错了。** 但评审也指出:验签接受旧公钥会**延长泄露私钥的有效期**,若采用须限定仅一个 access TTL(900s)过渡期后立即移除,否则变成永久后门 |
| H3 | `KeyStoreKeyFactory` 质量问题:内层 `synchronized` 对同一 lock 重入**完全无意义**;`store` 非 volatile;`store.load()` 抛异常后 `store` 已非 null → 重试会静默使用空 keystore;`:61-62` 无 null 检查,别名配错时报「Cannot load keys from store」**完全误导** | **成立。** 我的 plan 只打算换 `Resource` 类型,未察觉要重写该类。规格须增 FR |
| H4 | `classpath:` 回退路径可被滥用:(a) `SecurityProperties:26` 的 `location` 有默认值 `eve-jwt.jks`,且 **aliw(生产)当前就硬编码指向 classpath**,而我的 FR-018 只要求改口令、**遗漏了 location**;(b) 生产判定靠启动参数,误启为 test profile 即完整回退到明文弱口令 + classpath keystore;(c) `.gitignore` 忽略 `*.jks` 后,本地放回的 jks 不会被 git 提示却会被 Maven 打进 jar | **全部成立。** (b) 尤其关键:我的 FR-008 是 fail-open 设计(「若 profile==prod 则禁止」),应改为 fail-closed 正向白名单 |

---

## 五、MEDIUM / LOW 摘要

- **M1** 黑名单**无需**清理(验签先于黑名单检查,残留条目 ≤900s 自然过期);且清理反而有害(回滚旧密钥时已登出 token 会复活)。附带:jti 由签发方生成,伪造时可自选 jti 必然避开黑名单 —— 说明黑名单不是任何形式的补偿控制
- **M2** `POST /auth/tokens` 无限流。若按 C2 建议加入白名单则变为完全未认证可达,须同步引入限流
- **M3** FR-002「口令不经 AI」正确应坚持,但须明确 **SC-001/SC-006 的验证必须由人工执行**,AI 无法自行验证轮换成功。另:测试 keystore 入库须保证永不被生产加载(结合 H4b)
- **M4** FR-010 只验证「已存在的旧 token 失效」,**不能证明旧私钥不再有效**。须增加:用旧私钥**现场签发一个全新 token** 并断言被拒 —— 这是唯一能证明密钥对确实换掉的测试
- **L1** 行号偏差:`SecurityProperties:38`→实为 `:36`;`application-aliw.yml:158-162`→实为 `:157-162`;`application.yml:114-118`→`security:` 在 `:113`
- **L2** `AuthenticationFailureServletHandler:69` 的 `InvalidCookieException` 分支是死代码
- **L3** FR-014「旧私钥失去价值」表述过强,应为「不再能签发被系统接受的 token;但已发生的滥用后果不因轮换而撤销」
- **L4** `KeyPairConfig:41` 日志打印 keystore 路径,轻微信息披露(已确认未打印口令)

---

## 六、评审对两个用户决策的明确表态

评审被要求「若认为某个已确认的用户决策在安全上是错的,请直接说」。其结论:

### 「不强制登出」在当前证据下是错的

但错的理由**不是**我设想的 refresh token 窗口(那条链因实现细节不成立),而是:**私钥已在 git 中长期存在,无法证明未被利用**,而伪造 ROOT token 可造成的持久化影响(新增账号、篡改角色映射)**完全不受密钥轮换约束**。

评审建议:不仅清空 `refresh_token:*`,还应**强制全体改密**并审计 `sys_user`/`sys_user_role`/`sys_permission`。若坚持不强制登出,须在规格中明确记录「接受可能存在未被发现的持久化立足点」这一残余风险。

> **此项需用户重新决策** —— Q1 的决策依据已被推翻。

### 「拒绝双密钥并行」可以接受,但论证需重写

见 H2。修完 C2+C3 后单密钥可行;否则双密钥反而是更小的改动。

---

## 七、进入实现阶段前必须解决的清单

1. **[C3]** 改造 `JwtAuthorizationTokenFilter` 使验签失败返回 401 + `AUT00210`(含修正 `ResponseUtils.writeErrorInfo` 的 switch)。FR-016/SC-010 从「建议」升为**交付前提**。先写 RED 测试
2. **[C2]** 将 `POST:/auth/tokens` 加入 `whiteUrlList` 列为 FR,并同步引入限流(M2);查生产 DB `sys_permission` 是否已有覆盖规则
3. **[C1]** 增加入侵核查用户故事:审计 `sys_user`、`sys_user_role`、`sys_permission`、`eve_account`;明确写出「轮换不撤销历史滥用」;**重新评估是否强制改密**
4. **[H1]** 明确部署拓扑;若多实例,规格须要求停机窗口或全量同时重启,**禁止滚动重启**
5. **[H3]** 增加 FR:重写 `KeyStoreKeyFactory`(去除无效 DCL、别名 null 检查、异常分类、错误信息禁含口令、RSA ≥2048 位断言)
6. **[H4]** FR-018 扩展覆盖 `security.keystore.location`;移除 `SecurityProperties.location` 默认值;FR-008 判定改为 **fail-closed 正向白名单**;SC-003 落地为构建期强制
7. **[M4]** FR-010 增加「用旧私钥现场签发新 token 并断言被拒」
8. **[M3]** 明确 AI/人工验证职责边界;测试 keystore 文件名与别名与生产显著区分
9. **[L1]** 修正行号偏差,去掉「勿重复质疑」措辞

---

## 八、我的处置态度

评审的 C2/C3 我已**实测复现**,不是照单采信 —— 诊断测试 `JwtFilterDiagnosticTest` 保留在仓库中作为证据,待修复后改写为断言式回归测试。

C1 是我**没想到**的攻击路径。我请评审审的方向(refresh token 窗口)被证伪,但它找到了一条更实质的:攻击者不需要保住会话,只需在数据库里留下一个合法账号。

**规格与计划均需实质修订,不是补几行说明。** 尤其 Q1「不强制登出」的决策依据已被推翻,需要用户重新判断。

---

**评审人**: `ecc:security-reviewer`(独立 agent,46 次工具调用,约 16.6 分钟)
**归档日期**: 2026-08-11
