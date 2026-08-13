# 设计评审记录(第二轮)：007 JWT 签名密钥轮换

**评审阶段**: ③ 计划复审(代码仍未编写)
**评审工具**: `ecc:security-reviewer`(独立 agent,54 次工具调用,约 15.2 分钟)
**日期**: 2026-08-11
**Feature**: `007-jwt-key-rotation`
**结论**: **BLOCK** — 3 项 CRITICAL,其中 **2 项为我在 v2 修订中新引入**

> 第一轮记录:[design-review.md](./2026-08-11-007-jwt-key-rotation-design-review.md)(BLOCK,3 CRITICAL + 4 HIGH,9 项清单)

---

## 一、上轮 9 项清单核验:仅 3 项真正闭合

| # | 清单项 | 判定 |
|---|--------|------|
| 1 | [C3] 401 链路改造 | **部分** — 方向对,但与第 2 项**互相拆台**(CRITICAL-1);「天然 RED」不成立(CRITICAL-3) |
| 2 | [C2] refresh 端点加白 + 限流 | **部分/新引入问题** — 加白论证成立;**限流纠偏错误**(CRITICAL-2);清单要求的「查生产 DB `sys_permission` 覆盖规则」**完全未落项** |
| 3 | [C1] 审计 + 重评估改密 | **部分** — 审计范围**漏了 RBAC 实际来源表**(HIGH-1);残余风险漏两条(HIGH-2) |
| 4 | [H1] 部署拓扑 + 禁止滚动 | **部分** — FR-022 已写,但**拓扑至今未确定**,且无「未确认则不得进入序 6」的门禁 |
| 5 | [H3] 重写 `KeyStoreKeyFactory` | **已解决** — 五项与代码缺陷一一对应。遗漏:`resource.getInputStream()` 从不关闭(MEDIUM-2) |
| 6 | [H4] location/默认值/fail-closed/SC-003 | **部分** — (a)(b) 已落;`.gitignore` 吞测试 keystore(HIGH-3);SC-003 仍无构建期强制手段 |
| 7 | [M4] 旧私钥现场签发并断言被拒 | **已解决** |
| 8 | [M3] 职责边界 + 测试 keystore 区分 | **部分** — 边界已明确;但**现状与 plan 脱节**(见 CRITICAL-3) |
| 9 | [L1] 行号修正 + 删除「勿重复质疑」 | **已解决** — 评审逐个复核通过;但修订**新引入**事实错误(MEDIUM-3) |

---

## 二、CRITICAL-3:我的实测结论已过期 —— 工作区在会话期间变了

**这是我该更早发现的。** 用户于 **20:06** 生成了新的 `eve-helper.jks` 并改了 test profile 配置,我不知情,仍在引用变更前的实测结果。

我立即复跑验证,评审所述**完全成立**:

```
Tests run: 3, Failures: 0, Errors: 3
Caused by: java.io.FileNotFoundException:
  class path resource [eve-helper.jks] cannot be opened because it does not exist
  at KeyStoreKeyFactory.getKeyPair(KeyStoreKeyFactory.java:57)
  at KeyPairConfig.keyPair(KeyPairConfig.java:48)
```

| 事实 | 状态 |
|------|------|
| 根目录 `eve-helper.jks`(2722 字节,20:06 生成) | **不在任何 classpath 根上** |
| `application-test.yml:159` → `location: eve-helper.jks`,别名 `eve-helper` | 已改(非 plan 写的 `test-only-jwt.jks`) |
| `src/test/resources/` | **不存在** |
| 全部 59 个 `@SpringBootTest` | **无法加载 ApplicationContext** |
| `eve-helper.jks` 在 `git status` 中 | **不出现 —— 被 `.gitignore:55` 的 `*.jks` 静默吞掉** |

**两处 plan v2 陈述由此失效**:
1. 「诊断测试 2 用例 ERROR = 天然 RED」—— **错**。现在 3 个**全部** ERROR,原因与 C2/C3 无关(上下文起不来)。修完 FR-016 后仍全红,GREEN 判据失效
2. 「基线 502 / Failures 4 / Errors 216」—— **失效**,测于 keystore 变更之前

> 变更前的 C2/C3 实测结论(401 `AUT00201`、异常逃逸)本身仍有效,但**现在无法复现**,须在序 0 后重测。

**评审 HIGH-3 预言的 `.gitignore` 吞文件问题,已经实际发生了。**

---

## 三、CRITICAL-1:FR-016 与 FR-020 互相拆台(我完全没想到)

过滤器执行顺序使两处改造互相抵消:

```
带旧 access token 的 refresh 请求
  → JwtAuthorizationTokenFilter(位于 UsernamePasswordAuthenticationFilter 之前,SecurityConfig:67)
  → 验签失败 → [FR-016] writeErrorInfo(401) + return   ← 请求在此终止
  ✗ 到不了 AuthorizationFilter(链末端)= 白名单判定处
  ✗ 到不了 AuthController.refreshToken
```

后果:US1 场景 2、SC-006 在**两项都修完后仍然失败**;`401 → refresh → 401` 死循环**依然存在**,只是从「异常逃逸」变成「干净的 401 死循环」。

**比未修更隐蔽** —— HTTP 语义看起来对了,链路依旧断的。

**修法**:抽出 `WhiteUrlMatcher` 供 `RbacAuthorizationManager` 与 JWT filter 共同消费,filter 在白名单路径上不拒绝;或覆写 `shouldNotFilter`。须补 AC:「带旧 access token 请求白名单端点 → 到达 controller」。

> 评审附带指出:现方案唯一能侥幸跑通的前提是「客户端 refresh 时摘掉 Authorization 头」—— 该假设从未写进 spec,也不该依赖。

---

## 四、CRITICAL-2:我的限流「纠偏」被否决

**事实前提成立**(全仓 0 处真实 IP 处理、未配 `forward-headers-strategy`,评审复核确认),**但我推出的方案错了,且比原问题更危险**:

| 我提的 | 评审判定 |
|--------|---------|
| 按 refresh token 值限流 | **无效**:换随机 UUID → 每次全新 key → 计数恒为 1,永不触发;它声称防的「同一 token 重放」本已被 `TokenService:181` 先撤销后换发挡住。**更严重**:端点加白后未认证可达,每个新 token 值在 Redis 种一个新 key → **无认证的 Redis 键空间放大 DoS**。Redis 是硬依赖(RBAC/refresh token/黑名单全在里面)。另:key 含 token 明文,使机密出现在键空间 |
| 全局速率限流 | **自伤开关**:轮换瞬间全体同时 refresh,攻击者打满阈值即可让**所有合法用户无法恢复会话** —— 恰在人人必须重新认证的窗口。**B+C 组合比不做限流更糟** |
| 「方案 A 是独立架构问题」 | **高估成本**:`forward-headers-strategy` + 受信代理列表是一处配置项,伪造问题正由受信列表解决 |

**应采纳**:按 **userId** 限流且**只计失败**(UUID 校验 → 一次 Redis GET 解出 userId → 以 userId 为桶,键空间被真实用户数有界约束;只计失败则合法尖峰不受影响);全局层**仅告警/降级**。

**另须避免**:不以 `LoginRateLimiterService` 为范本 —— 其按 username 计数、5 次锁 30 分钟(`:25-26`),任何人可定向锁死任意已知用户(既有 DoS,超出 007 范围但不可复制)。

---

## 五、HIGH

| # | 问题 | 我的核实 |
|---|------|---------|
| HIGH-1 | **审计范围漏了 RBAC 的实际来源表**。`SysPermissionMapper.xml:208-210` 是 `sys_permission` LEFT JOIN `sys_role_permission` LEFT JOIN `sys_role` 三表 JOIN;攻击者提权**不必碰 `sys_permission`**,在 `sys_role_permission` 插一行即可 | **已读 XML 核实,成立。** 我的 4 表清单确实漏了后两张 → 审计通过会给出**虚假的安全结论**。已扩至 6 表 |
| HIGH-2 | **暂不强制改密的代价未如实呈现**。我只承认「自注册账号」一条,漏了:①`sys_user` 的 BCrypt 哈希可能已被读走(**只能靠强制改密缓解**,且审计看不出是否被读过);②`eve_account.refresh_token`(ESI 长期凭证,明文存储)外泄后**不因本次轮换失效**,唯一处置是重新走 ESI 授权 | **成立。** 我把一个**凭证吊销问题**降格成了**行记录比对问题**。「审计是唯一防线」不准确 —— 表审计只能发现新增/篡改,对**读取型外泄完全失明**。已按此重写 US3 |
| HIGH-3 | 测试 keystore 入库方案不足:①`.gitignore:55` 会**静默吞掉**提交(未提 `git add -f` 或反向规则)—— **已实际发生**;②文件命名只降低误认概率,H4(b) 的真正防线是 fail-closed 基线校验,须补 AC;③`target/classes/` 下仍有旧 jks,SC-003 须在 `mvn clean` 后验证否则假阴性 | 全部成立。已确认 `target/classes/eve-jwt.jks` 与 `jwt.jks` 仍在 |
| HIGH-4 | 实现顺序错:当前连 ApplicationContext 都起不来,任何 MockMvc 观测都不可能。正确顺序 序0(恢复上下文+重测基线)→ 白名单组件 → FR-016 → FR-020 | 成立。plan 已按此更新 §2 |

---

## 六、MEDIUM / LOW 摘要

- **MEDIUM-1** FR-016 的 `writeErrorInfo` 复用:评审确认**在解决 CRITICAL-1 前提下写法可行**,响应不会被覆写;我的 `ResponseUtilsTest` 实测也确认编码无乱码风险。但须补:①`Content-Type` 补 `charset=UTF-8`;②**加 `response.isCommitted()` 守卫**(该方法将有 3 个调用方,二次调用会追加出两个 JSON 体);③`Access-Control-Allow-Origin: *` 硬编码会出现在远多于现在的 401 响应上(既有行为,登记);④`log.error("JWT解析失败", e)` 须降级去堆栈(plan 伪码已改但文件清单未列该行)
  - **三方案对比**:filter 内直写(+ 白名单豁免 + isCommitted 守卫)= 推荐;改 filter 顺序 = 拿不到 `AUT00210`(我说「风险更大」的理由不准确);注入 `AuthenticationEntryPoint` 委托 = 最符合 Spring Security 惯例,若愿多花结构成本更稳妥
- **MEDIUM-2** FR-023 漏了资源泄漏:`KeyStoreKeyFactory:57` 的 `getInputStream()` **从不关闭**,须 try-with-resources
- **MEDIUM-3** 我的修订**新引入**事实错误:①spec 1.2.1 称 test 与 aliw「同一个 6 字符弱口令」—— **现已不成立**(test 已改为长随机串且与 aliw 不同;aliw 仍短口令);②plan 文件清单写 `application-test.yml` 改 `test-only-jwt.jks`/`test-only` —— 现状已是 `eve-helper.jks`/`eve-helper`
- **MEDIUM-4** **SC-011 是假门禁**:「grep 全仓无明文口令」而 `application-{test,ali,aliw,prod}.yml` 都在 `.gitignore` 中、仓库里根本没有 → grep 恒通过。须改为人工核验部署环境实际文件 + 留证
- **MEDIUM-5** 回归门禁「Errors ≤ 216」在当前状态无意义,且该数字本身浮动。改为序 0 后重测基线 + 同环境逐用例 diff
- **LOW-1** 我一边给 `KEYSTORE_ALIAS` **加**默认值、一边按 H4(a) **移除** `location` 默认值,方向相反。别名危害远小于 location,可接受但须说明理由
- **LOW-2** FR-016 后验签失败/已过期/已撤销**统一返回 `AUT00210`**,对撤销场景语义不准,但**统一错误码在防信息泄露上更优** —— 应写成有意的设计决策,免得日后被当 bug「修掉」
- **LOW-3** plan 写 `AuthenticationFailureServletHandler.java:69-70`,实测在 `:68-69`
- **LOW-5** `KeyPairConfig:41` 改绝对路径后会打印服务器目录结构,建议只打文件名

---

## 七、评审对我三处自我判断的直接回应

| 我的判断 | 评审结论 |
|---------|---------|
| 「按 IP 限流不成立」 | **事实前提正确,替代方案错误且更危险 —— 应撤回重做** |
| 「FR-016 filter 内直写」 | **编码/覆写疑虑可放下**(已实测),但**与 FR-020 的冲突是本轮最严重问题** |
| 「审计是唯一防线」 | **不准确** —— 范围漏了 RBAC 来源表,且对读取型外泄完全失明。选项 B 可选,但代价**未被如实呈现** |

---

## 八、进入阶段④前必须解决

| 优先级 | 项 | 本次已做 |
|--------|-----|---------|
| CRITICAL-1 | 白名单共享匹配组件 + 补 AC | 已写入 plan §3.2,**设计待 v3** |
| CRITICAL-2 | 限流方案改按 userId 只计失败;全局仅告警 | 已撤回旧方案并记录评审建议,**FR-020 已重写** |
| CRITICAL-3 | 序 0:测试 keystore 上 classpath + 重测基线 | 已写入 plan §2,**待执行** |
| HIGH-1 | 审计扩至 6 表 | ✅ 已改(FR-021/SC-013/US3) |
| HIGH-2 | 补记两条读取型残余向量 | ✅ 已改(US3 三向量表) |
| HIGH-3 | `.gitignore` 反向规则 + 别名显著区分 + `mvn clean` 后验 SC-003 | **待做** |
| HIGH-4 | 重排实现顺序 | ✅ 已改(plan §2) |
| MEDIUM 1~5 | 见上,MEDIUM-3/4 应优先 | **待做** |

---

## 九、我的处置态度

CRITICAL-3 暴露了我的一个真实失误:**工作区在会话期间被改动(20:06 生成新 keystore),我没有察觉,继续引用过期的实测结论写计划。** 这是「实测」的价值被自己抵消掉的典型 —— 测过不等于结论永久有效。

CRITICAL-1 我完全没想到:我分别验证了「401 能返回」和「白名单能加」,但没验证**两者组合后的执行顺序**。这是单点验证充分、组合验证缺失。

CRITICAL-2 是我把「事实发现」直接当成了「方案依据」——查明「按 IP 不可行」是对的,但由此推出的 B+C 引入了更严重的未认证 Redis 放大 DoS。**发现问题的能力不等于设计方案的能力。**

**未修改任何 keystore 或 test profile 配置** —— 那是用户的密钥材料,且我不清楚其意图(准备轮换?仅本地测试?)。仅更新文档记录真实状态。

---

**归档日期**: 2026-08-11
