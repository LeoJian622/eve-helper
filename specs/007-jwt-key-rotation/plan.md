# Implementation Plan: JWT 签名密钥轮换(选项 B)

**Feature**: `007-jwt-key-rotation`
**规格**: [spec.md](./spec.md)(已按阶段③评审修订)
**评审记录**: [设计评审 BLOCK](../../docs/reviews/2026-08-11-007-jwt-key-rotation-design-review.md)
**创建日期**: 2026-08-11(**v3**,序 0 完成后按实测重写)
**状态**: **v3 待评审** — 序 0 已完成(基线已重测);CRITICAL-1(白名单共享组件)与 CRITICAL-2(按 userId 限流)已给出设计,见 §3.2 / §3.3。**须经第三轮 `ecc:security-reviewer` 评审后方可进入 `/speckit-tasks`**

> **v1 已废弃**(基于「用户无感」方案,依据被推翻)。**v2 被第二轮评审 BLOCK**(3 项 CRITICAL,其中 2 项为 v2 修订新引入)。本 v3 基于 2026-08-11 21:47 的实测状态重写。

---

## 1. 目标与范围

### 做(代码)

> ⚠️ **序号已按第二轮评审 HIGH-4 与第三轮评审确认重排**(M-3①):白名单共享组件是 FR-016 的前提(CRITICAL-1),不再是 FR-016 打头。与 [spec 实现顺序表](./spec.md)及 §2 一致。

| 序 | 项 | 需求 | 依赖 |
|----|----|------|------|
| 0 | ✅ **已完成**:测试 keystore 上 classpath + 重测基线 | 序 0 | — |
| 1 | 抽出白名单共享匹配组件 `WhiteUrlMatcher` | CRITICAL-1、FR-016/020 前提 | 序 0 |
| 2 | 验签失败返回 401 + `AUT00210`(白名单路径放行) | FR-016 | 序 1 |
| 3 | `POST:/auth/tokens` 可达 + 两层限流(**不依赖 IP**,见 3.3) | FR-020 | 序 1、2 |
| 4 | 重写 `KeyStoreKeyFactory` | FR-023 | 无(可与 1~3 并行) |
| 5 | keystore 文件系统加载 + fail-fast | FR-005~007 | 序 4 |
| 6 | 启动基线校验(fail-closed) | FR-008 + 006 L-10 | 序 5 |
| 7 | 配置与模板补全 | FR-018、FR-019 | 序 5 |
| 8 | 清理与文档 | FR-012、FR-013、FR-021 | 全部 |

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

## 2. 实现顺序的强制约束(**序 0 已完成,v3 按实测更新**)

### ✅ 序 0 已完成(2026-08-11 21:47)

**执行内容与实测结果**:

| 动作 | 结果 | 证据 |
|------|------|------|
| 测试 keystore 上 classpath | `test-only.jks` 置于 `src/main/resources/`,别名 `test-only`,2048 位 RSA | `keytool -list` 确认别名与位数;证书指纹 `FD:9F:19:...:FB:4` |
| `.gitignore` 反向放行 | `!src/main/resources/test-only.jks`(`.gitignore:65`) | `git check-ignore -v` 返回负向匹配;`git status` 中以 `??` 出现 |
| 恢复 `@SpringBootTest` 可加载 | **`Failed to load ApplicationContext` 计数 = 0** | `mvn clean test` 全量日志 grep |
| keystore 相关错误清零 | **0 处** | grep `KeyPairConfig|KeyStoreKeyFactory|FileNotFoundException` 无命中 |
| **重测回归基线** | **`509 tests / Failures 4 / Errors 219`** | `mvn clean test`,EXIT=1 |
| 生产密钥移出工作区 | `eve-helper.jks` 已由用户移出;从未入库,历史干净 | `git log --all` 为空 |

**新基线的性质判定**(替代 v2 失效的 `502/F4/E216`):

现存 219 errors 的根因分布(`Caused by` 聚合)全部是**外部依赖不可达**:MySQL 通信异常 4、Socket 异常 3、超时 2、SQL 语法 2、SSL 1 等。**与 007 的改造范围无关**。

> **回归门禁改法(评审 MEDIUM-5)**:**不使用固定 Errors 阈值**(该数字随外部环境浮动,v2 的「≤216」已被证明无意义)。改为**同环境逐用例 diff**:以本次 `509/F4/E219` 的用例级结果为基准,改造后比对**用例名集合**的增删,只允许「原 ERROR 转 PASS」,不允许出现新的失败用例名。

### ⚠️ 序 0 的一项遗留发现:SC-003 原措辞不可满足

`mvn clean` 后实测 `target/classes/` 含 **`eve-jwt.jks` 与 `test-only.jks`**。

- `eve-jwt.jks` 进 jar → **真阴性 RED 证据**,由 FR-012 + US2 解决
- `test-only.jks` 进 jar → **有意为之**。故 SC-003 已按用户决策(2026-08-11)改为「不含**生产** keystore」,验证方式 `unzip -l target/*.jar | grep '\.jks'` 断言仅含 `test-only.jks`
- **防线不是「jar 里没有 jks」,而是序 5 的 fail-closed 基线校验** —— 与评审 HIGH-3② 判断一致

### 序 1:抽出白名单共享匹配组件(CRITICAL-1 的前提)

见 §3.2 —— FR-016 与 FR-020 在 v2 设计下**互相拆台**,必须先解决。

### 序 2:FR-016(401 链路)

理由仍成立(未修时无法观测「旧 token 被拒」),但**它不是最先** —— 序 1 是其前提。

### 序 3~7

见 [spec.md 末尾的实现顺序表](./spec.md)。**序 6 的 `git rm eve-jwt.jks` 有前置条件**:须在 `application-aliw.yml` 切到文件系统绝对路径之后,否则生产启动即因找不到 classpath keystore 而失败。

> **当前状态**:`eve-jwt.jks` 仍处跟踪状态(用户 2026-08-11 明确决定暂留)。FR-012 待序 6 执行。

---

## 3. 关键决策(v3)

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

**修法**:白名单判定须成为过滤器**可见**的单一事实来源。

#### v3 设计:抽出 `WhiteUrlMatcher`(方案 A,已具化)

**已核实的代码事实**(2026-08-11 21:50 读码):

| 事实 | 位置 |
|------|------|
| 白名单来源 | `EveHelperSecurityConfig`(`@ConfigurationProperties(prefix="eve.helper")`),字段 `List<String> whiteUrlList` |
| 判定逻辑 | `RbacAuthorizationManager:66-76`,`restfulPath.equals(white)` **精确字符串匹配**(非 Ant 通配) |
| `restfulPath` 构造 | `RbacAuthorizationManager:61` = `method + ":" + request.getRequestURI()` |
| OPTIONS 短路 | `RbacAuthorizationManager:55-58` 先于白名单判定直接放过 |
| JWT filter 位置 | `JwtAuthorizationTokenFilter extends OncePerRequestFilter`,`SecurityConfig` 中位于 `UsernamePasswordAuthenticationFilter` 之前 |
| 现有白名单内容 | `application.yml:122-124` 仅 `POST:/user`(另有注释掉的 `GET:/`) |

**新组件**:`infrastructure/config/security/WhiteUrlMatcher.java`

```java
@Component
@RequiredArgsConstructor
public class WhiteUrlMatcher {
    private final EveHelperSecurityConfig config;

    /** 与 RbacAuthorizationManager 完全一致的匹配语义:method + ":" + URI 精确相等 */
    public boolean isWhiteListed(HttpServletRequest request) {
        String restfulPath = request.getMethod() + ":" + request.getRequestURI();
        List<String> list = config.getWhiteUrlList();
        return list != null && list.stream().anyMatch(restfulPath::equals);
    }
}
```

**两处消费方改造**:

| 消费方 | 改动 | 目的 |
|--------|------|------|
| `RbacAuthorizationManager:66-76` | 替换内联 stream 为 `whiteUrlMatcher.isWhiteListed(request)` | 消除重复实现,保证语义永不漂移(**单一事实来源**) |
| `JwtAuthorizationTokenFilter.doFilterInternal` | **验签失败时**:若 `whiteUrlMatcher.isWhiteListed(request)` 为真 → **不写 401,直接 `filterChain.doFilter` 放行**(不写入 `SecurityContext`);否则写 401 并 return | 使白名单端点在携带旧/无效 token 时仍可达 controller |

**关键语义**:filter 在白名单路径上「**不拒绝**」而非「**不解析**」。区别在于:若 token 恰好有效,仍应写入 `SecurityContext`(便于 controller 侧可选地识别用户);仅在**验签失败**时才走「放行但无认证」分支。

> **为何不选方案 B(`shouldNotFilter`)**:`shouldNotFilter` 会让白名单路径**完全跳过** filter,连有效 token 也不解析。`POST:/auth/tokens` 的 controller 若日后需要识别「谁在 refresh」,将无从获取。方案 A 保留了这个可能性,成本仅多一个分支。

**须补的 AC**(spec US1 场景 2 的强化):

1. 带**旧密钥签发的** access token 请求 `POST /auth/tokens` → **到达 controller**(断言 controller 被调用,非仅断言状态码)
2. 带**格式非法的** token(`ParseException` 路径)请求白名单端点 → 同样到达 controller
3. 带旧 token 请求**非白名单**端点 → 401 + `AUT00210`(不得因本改造而放宽)
4. `WhiteUrlMatcher` 与 `RbacAuthorizationManager` 对同一组输入给出**相同判定**(防语义漂移的契约测试)

> ⚠️ **遗留风险(须在评审中确认)**:`RbacAuthorizationManager:55-58` 的 OPTIONS 短路**不在** `WhiteUrlMatcher` 内。若 JWT filter 也需对 OPTIONS 放行,须显式处理 —— 但 CORS 预检通常不带 `Authorization` 头,会走 filter 开头的「非 JWT 不处理」分支(`JwtAuthorizationTokenFilter:56-61`),故**大概率无影响**。此判断未经实测,列为待验证项。

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

#### v3 设计:按 userId 限流(采纳评审方案,但**修正其一处前提**)

**已核实的代码事实**(2026-08-11 21:52 读码 `AuthApplicationService.refreshToken:104-137`):

现有校验链顺序为:

```
① 空值校验            → EveHelperException「不能为空」
② UUID 格式校验:110   → EveHelperException「格式错误」        ← 无 Redis 访问
③ isRefreshTokenValid → EveHelperException「无效或已过期」    ← 第 1 次 Redis 访问
④ getUserIdFromRefreshToken:120 → userId                     ← 第 2 次 Redis 访问
⑤ loadUserById        → 查库
⑥ refreshAccessTokenWithUser → TokenService:181 先撤销后换发
```

**⚠️ 评审方案的前提在此处需要修正**:评审说「一次 Redis GET 解出 userId 后以 userId 为桶」,但**对最主要的攻击向量,userId 根本解不出来**:

| 攻击输入 | 在哪一步失败 | 能否解出 userId | 能否按 userId 计数 |
|---------|------------|----------------|------------------|
| 随机 UUID(**主要向量**) | ③ `isRefreshTokenValid` | **否** —— Redis 中无此键 | **不能** |
| 非 UUID 垃圾串 | ② 格式校验 | 否 | 不能 |
| 已撤销的真实 token | ③ | 否(键已删) | 不能 |
| 有效 token 重复调用 | 不失败 | 是 | 能,但**这不是攻击** |

**结论:纯「按 userId 计数」无法约束随机 UUID 洪泛** —— 而那恰是端点加白后最容易被打的方式。评审正确否决了「按 token 值计数」(会造成 Redis 键空间放大),但其替代方案覆盖不到主向量。

**v3 的方案(两层,均不新增无界键空间)—— 已按第三轮评审 §2.3/2.4 修订**:

| 层 | 机制 | 键空间 | 作用(诚实表述,round3 §2.3/2.5) |
|----|------|--------|------|
| **L1 按用户异常观测计数器** | 仅在 ④ 之后、⑤/⑥ 失败时计数(如用户恰被删除的竞态)。键 `refresh:fail:{userId}`,**TTL 60s**,超 **10 次/分钟**(用户 2026-08-12 决策)→ 打含 `ALERT_MARKER_TARGETED` 的结构化 WARN,触发动作**仅告警** | 有界(真实用户数),攻击者无法主动构造(round3 §2.2) | **定位是观测,不是防护** —— 持被盗 token 的攻击者走成功路径,只计失败的 L1 一次都不计数(M-2)。**禁止任何锁定动作**:按 userId 锁定 = 复制 `LoginRateLimiterService` 的定向锁死 DoS,本 plan 自己刚谴责过 |
| **L2 无效 token 洪泛的检测(全局单键)** | **单一固定键** `refresh:invalid:global` 计数「②/③ 阶段失败」总次数,**固定窗口**(`INCR`+`EXPIRE`)。超阈值时**仅告警,不施加任何时延、不拒绝** | **常量 1 个键** —— 无放大 | **真价值是检测,为运维响应争取时间**;结构性抗洪泛来自失败路径廉价(③ 即止、不触 DB/不触 token 生成/不触 RBAC Redis 读)。**既不硬拒也不整形** |

**L2 降级形式最终裁决:纯观测告警。否决 503,且【v4 推翻 v3】否决固定延迟(实现阶段 security-reviewer HIGH-1,T036)**:

- ~~返回 503~~ **删除此选项**。选项 B 下轮换瞬间**全体**合法客户端的 refresh 都在 ③ 失败,L2 必然触发;503 会把合法请求挡在 controller 外 —— 恰是 SC-006 验收的时刻。503 = 换状态码的硬拒 = 第二轮已否决的自伤总闸从后门请回
- ~~固定延迟(100–300ms)~~ **【v4 推翻,不得恢复】**。v3 判它"抬高攻击成本"**方向判反了** —— `Thread.sleep` 跑在 Tomcat 工作线程上,而 `POST:/auth/tokens` 已加白(未认证可达):
  - **成本不对称在攻击者一侧**。洪泛用并发连接,每请求延迟不降低其吞吐;我方每个超阈值请求白占一个工作线程 100–300ms。默认 200 线程 ⇒ 约 **1000 req/s 拖垮全站**(含所有已认证业务端点),无延迟时需 ~200,000 req/s。**延迟把局部端点压力放大成全站可用性故障**
  - **会自我触发**。FR-015 清空 `refresh_token:*` 后全体在线客户端同时在 ③ 失败 → L2 必然超阈值 → 恰在轮换窗口给自己叠加线程耗尽风险(与 503 被否决的理由**同源**,v3 只否了 503 却漏了延迟)
  - **整形价值本就可疑**:真正的抗洪泛来自失败路径 O(1) 廉价(随机 UUID 在 ③ 的 Redis 存在性校验即止),延迟并未增加这条路径上攻击者的边际成本
- **锁定为:超阈值 → 递增告警计数器 + 结构化 WARN 日志,方法立即返回**。不 sleep、不拒绝、不改响应体。速率限制若日后确有需要,应在**入口层**(反代/网关/`server.tomcat.max-connections`)做,而非在业务线程内自我阻塞

**L2 配套实现约束(须写入 tasks,round3 §2.4)**:

| 约束 | 内容 |
|------|------|
| 窗口 | **固定窗口**(`INCR`+`EXPIRE`),非滑动窗口 —— 滑动需 ZSET ≥3 条命令/请求,在洪泛路径上反而加重 Redis 负载 |
| 过期竞态 | **每次 INCR 后都重设 EXPIRE**,杜绝「INCR 与 EXPIRE 之间崩溃 → 键永不过期 → 永久降级」 |
| Redis 故障 | **fail-open**(try-catch 跳过 L2):Redis 不可用时 ③ 本身就会失败,refresh 整体不可用,L2 不应新增失败面 |
| 告警 | **结构化 WARN 日志 + 稳定告警标记 `[SECURITY_ALERT:REFRESH_FLOOD]`**(供 grep/Loki 规则匹配)。~~接入 Prometheus~~ **【v4 推翻,T037】**:pom 里三个 `io.prometheus` 依赖在 `src/main/java` **零引用**(无 `PrometheusRegistry`、无 exporter 启动)、全部 profile 无 `management:` 配置 ⇒ 指标落进 `SimpleMeterRegistry` 永不被抓取。「pom 已有 metrics 依赖」是**误判**——有依赖 ≠ 有通路。三个死依赖已于 T037 从 pom 删除,日志是本项目现存唯一可运维的告警通路 |
| 阈值定标 | 窗口内阈值 ≥ 预估轮换尖峰(在线用户数 × 每客户端 refresh 重试次数),使「L2 触发 ≈ 异常流量」,轮换本身不误触发;**阈值仅决定何时告警,不触发任何降级动作**(v4:延迟已否决) |
| 实现范本 | 复用 `LoginRateLimiterService:39-49` 已验证的 `increment`+首次 `expire` 模式;**但其「按 username 计数 + 锁 30 分钟」的模式禁止照搬**(可定向锁死任意已知用户) |

**为何 L2 用单键而非按 token 值**:计数器是**一个固定键**,攻击者无论换多少随机 UUID 都只 `INCR` 同一个键 → **无键空间放大**(评审驳回方案 B 的核心理由被规避),且 token 明文不进键空间。

**为何 L2 不硬拒**:评审对「全局硬限流 = 自伤总闸」的判断成立 —— 轮换瞬间全体客户端同时 refresh,硬拒会让合法用户无法恢复会话。故 L2 **仅告警/降级**,与评审建议 2 一致。

> ✅ **多实例说明(第三轮评审已确认)**:L2 计数器在多实例下经 Redis 全局共享(真全局视图),告警在各实例本地发出、无协调成本;Redis 故障按上表 fail-open。

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
| `infrastructure/config/security/WhiteUrlMatcher.java` | **白名单匹配单一事实来源**(CRITICAL-1),供 `RbacAuthorizationManager` 与 JWT filter 共同消费 |
| `infrastructure/config/security/SecurityBaselineValidator.java` | `ApplicationRunner`,fail-closed 校验 4 项基线 |
| `domain/service/security/RefreshRateLimiterService.java` | 两层限流:L1 按 userId 只计失败 + L2 全局单键洪泛观测(**不依赖 IP**,见 §3.3) |
| ~~`src/test/resources/test-only-jwt.jks`~~ | ✅ **已完成,但路径与命名与 v2 计划不同**:实际为 `src/main/resources/test-only.jks`(别名 `test-only`),经用户决策留在 main resources 并入库 |
| `src/test/.../JwtAuthFailureResponseTest.java` | 401 链路回归测试(由 `JwtFilterDiagnosticTest` 改写而来) |
| `src/test/.../WhiteUrlMatcherContractTest.java` | `WhiteUrlMatcher` 与 `RbacAuthorizationManager` 判定一致性契约测试(防语义漂移,AC#4) |
| `src/test/.../SecurityBaselineValidatorTest.java` | 各违规组合 → 拒绝启动;非生产 profile → 跳过;**生产 + `classpath:test-only.jks` → 拒启**(round3 §3.1) |
| `src/test/.../KeyStoreKeyFactoryTest.java` | 异常分类、别名错误、RSA 位数断言、错误信息不含口令;**PKCS12 与 JKS 双格式 fixture**(M-4) |
| `src/test/.../RefreshTokenEndpointAccessTest.java` | 无 token / 失效 token 时可达 controller |
| `src/test/.../RefreshRateLimiterTest.java` | L2 超阈值 → **仅告警,不阻塞调用线程**(v4);成功路径不受影响;Redis 故障 fail-open;L1 无锁定动作 |

### 修改

| 文件 | 改动 |
|------|------|
| `RbacAuthorizationManager.java:67-73` | **替换内联白名单 stream 为 `whiteUrlMatcher.isWhiteListed(request)`**(CRITICAL-1,消除重复实现)。⚠️ 保留 OPTIONS 短路(:56-58)原样 —— 与 filter 对 OPTIONS 的语义分歧是**有意的**(round3 §1.3),注释说明防后人「对齐」 |
| `JwtAuthorizationTokenFilter.java:52-109` | **全部 5 个失败出口**(验签/过期/黑名单/Parse/JOSE)统一走 `rejectOrPass`:白名单路径→匿名放行不写认证;非白名单→直写 401 + return。不再抛 `InvalidCookieException`。**须加 `response.isCommitted()` 守卫**(评审 MEDIUM-1②);`log.error("JWT解析失败", e)` 降级为 warn + 只打异常类名(MEDIUM-1④) |
| `ResponseUtils.java:26-35` | switch 增加 `TOKEN_ACCESS_EXPIRED` → 401 分支;**`Content-Type` 补 `charset=UTF-8`**(评审 MEDIUM-1①)。**风险登记(L-4)**:`Access-Control-Allow-Origin: *` 硬编码将出现在更多 401 响应上(既有行为,本次不修,仅登记) |
| `KeyStoreKeyFactory.java` | 全面重写(FR-023);**`getInputStream()` 改 try-with-resources**(评审 MEDIUM-2,原代码从不关闭);**AC 增加「能加载 PKCS12 与 JKS 两种物理格式」**——现 `test-only.jks` 实为 PKCS12,靠 DualFormat 兼容被 `getInstance("jks")` 加载,重写若改严格将致 59 个测试集体失败(M-4) |
| `KeyPairConfig.java:43-51` | 按 `classpath:` 前缀分派 Resource;文件存在性检查;**日志只打文件名不打完整路径**(评审 LOW-5) |
| `SecurityProperties.java:26` | **移除 `location` 默认值** `"eve-jwt.jks"`(评审 H4a) |
| `AuthApplicationService.refreshToken:114-119` | **M-5(设计优化)**:③ `hasKey` + ④ `get` 两次 Redis 往返合并为单次 `get`(null 即无效),洪泛单请求成本 2→1 命令,并消除 ③④ 间 TOCTOU。`:158` 的第三次读取(撤销前权威校验)保留 |
| `application.yml:122-124` | `whiteUrlList` 增 `POST:/auth/tokens` |
| `application.yml:117` | `${KEYSTORE_ALIAS}` → `${KEYSTORE_ALIAS:eve-jwt}`(评审 LOW-1:与移除 location 默认值方向相反,但别名危害远小于 location,故可接受) |
| `.env.example` | 补 `KEYSTORE_LOCATION`、`KEYSTORE_ALIAS` |
| `application-prod.yml.example` | 补 `security.keystore` 段;**whiteUrlList 同步增 `POST:/auth/tokens`**(HIGH-1:List 属性被 profile 整体覆盖,模板不同步则加白在生产静默失效 —— 已随本 plan 修订落地) |
| ~~`application-test.yml`~~ | ✅ **已完成(序 0)**:`location: test-only.jks` / `alias: test-only`。**口令保持明文,不改环境变量** —— 见 spec FR-019 修订(测试 keystore 有意公开,以便 CI 直接跑) |
| `application-aliw.yml`(不入库) | 口令改 `${...}`;**location 改文件系统绝对路径**(评审 H4a)。**这是 FR-012 的前置条件** |
| `AuthenticationFailureServletHandler.java:69-70` | 清理 `InvalidCookieException` 死分支(评审 L2/LOW-3;**L-8:第二轮「更正」方向错了,实测死分支在 `:69-70`** 而非 `:68-69`) |
| `src/main/resources/eve-jwt.jks` | **`git rm`**(FR-012)。⚠️ **须在 aliw profile 切绝对路径之后**,否则生产启动失败。用户 2026-08-11 决定暂留,待序 6 |
| `docs/DEPLOYMENT.md` | 轮换章节 + **六**表审计清单 + 公告模板 + **新生产密钥指纹 ≠ `test-only.jks` 公开指纹(`FD:9F:19:27:61:...:CA:0F:B4`)核对项**(L-5:SC-014 检测不到「误把 test-only.jks 当新生产密钥部署」) |
| `specs/006-.../spec.md` L-10 | 标注「已由 007 实现」 |

---

## 5. 401 链路改造设计(FR-016)

> ⚠️ **本节与 §3.2 的白名单放行设计强耦合**。实现时两节必须同时对照 —— v2 伪码曾因缺少白名单分支与 §3.2 矛盾,被第三轮评审 HIGH-3 驳回(见 round3 §五)。本节伪码已覆盖 filter 的**全部 5 个失败出口**(M-1)。

```java
// JwtAuthorizationTokenFilter.doFilterInternal 目标形态(伪码,第三轮评审后重写)
private void rejectOrPass(HttpServletRequest request, HttpServletResponse response,
                          FilterChain chain, ResultCode code) throws IOException, ServletException {
    if (whiteUrlMatcher.isWhiteListed(request)) {
        chain.doFilter(request, response);        // 白名单路径:匿名放行,不写 SecurityContext
        return;
    }
    ResponseUtils.writeErrorInfo(response, code); // 非白名单:直写 401,不继续 chain
}

try {
    SignedJWT signedJWT = SignedJWT.parse(token);
    if (!signedJWT.verify(verifier)) {
        rejectOrPass(request, response, chain, TOKEN_ACCESS_EXPIRED);   // 出口①验签失败
        return;
    }
    if (jwtClaimsSet.getExpirationTime().getTime() < now) {
        rejectOrPass(request, response, chain, TOKEN_ACCESS_EXPIRED);   // 出口②过期
        return;
    }
    if (tokenBlacklistService.isBlacklisted(jti)) {
        rejectOrPass(request, response, chain, TOKEN_ACCESS_EXPIRED);   // 出口③黑名单
        return;
    }
    // ... 写 SecurityContext,继续 chain
} catch (ParseException | JOSEException e) {
    log.warn("JWT 校验失败: {}", e.getClass().getSimpleName());          // 不记 token 内容
    rejectOrPass(request, response, chain, TOKEN_ACCESS_EXPIRED);       // 出口④⑤解析/JOSE 异常
    return;
}
```

**白名单语义(与 §3.2 一致)**:白名单路径上**任何** token 校验失败一律匿名放行 —— 白名单 = 该端点不要求认证。覆盖范围:验签失败(①)、过期(②)、黑名单(③)、ParseException(④)、JOSEException(⑤),共 5 个出口。

**有意的设计决策(防后人当 bug 修,LOW-2)**:验签失败/过期/撤销**统一返回 `AUT00210`**,不区分具体原因 —— 这是防信息泄露的有意取舍(泄露「为何失败」会帮助攻击者区分密钥状态),语义上对撤销场景不够精确,但安全性优先。

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
- 白名单分支的 `chain.doFilter` 后不再写 `SecurityContext` —— 与 §3.2「放行但不写认证」一致,STATELESS 会话策略下无跨请求泄漏面(round3 §1.2 已核验)

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

## 6. 测试策略(v3)

> 基线与门禁已按序 0 实测更新(M-3②):旧值 `502/F4/E216` 与「Errors ≤ 216」均失效。

| 层 | 测试 | 关键断言 |
|----|------|---------|
| **401 链路** | `JwtAuthFailureResponseTest`(由诊断测试改写) | 验签失败 → **HTTP 401** 且 body code `AUT00210`;过期 token → 401;黑名单 token → 401;**响应体不含 token 片段** |
| **白名单放行** | `WhiteUrlMatcherContractTest` + `RefreshTokenEndpointAccessTest` | `WhiteUrlMatcher` 与 `RbacAuthorizationManager` 对同一组输入判定一致(防语义漂移,AC#4);带旧/非法 token 请求白名单端点 → **到达 controller**(AC#1/#2);带旧 token 请求非白名单端点 → 401(AC#3) |
| **refresh 可达** | `RefreshTokenEndpointAccessTest` | 无 token → 到达 controller(非 401 `AUT00201`);失效 token → 到达 controller;refresh token 不存在 → 明确业务错误;同一 refresh token 反复调用 → 第二次起因撤销而业务错误(L-10,非限流) |
| **限流** | `RefreshRateLimiterTest` | L2 超阈值 → **仅告警计数器递增,且不占用调用线程**(v4:断言超阈值路径耗时与低于阈值同量级);L2 不施加于成功路径;Redis 故障 fail-open;L1 **无锁定动作** |
| **KeyStoreKeyFactory** | `KeyStoreKeyFactoryTest` | 别名不存在 → 错误信息含「别名」而非「Cannot load keys」;口令错 → 错误信息**不含口令**;文件不存在 → 明确路径;RSA <2048 → 拒绝;**能加载 PKCS12 与 JKS 两种物理格式**(M-4:现 `test-only.jks` 实为 PKCS12,靠 DualFormat 兼容被 `getInstance("jks")` 加载,重写不得破坏) |
| **启动校验** | `SecurityBaselineValidatorTest` | 4 项各自违规 → 拒绝;profile 缺失/未知 → **按生产校验(fail-closed)**;`test` profile → 允许 classpath;**生产 profile + `classpath:test-only.jks` → 拒绝启动**(SC-005 具体用例,round3 §3.1) |
| **回归** | 既有 59 个 `@SpringBootTest` | 换 `test-only.jks` 后仍能签发/验签;**同环境逐用例 diff**(见下,不用固定 Errors 阈值) |

**回归门禁(MEDIUM-5 修订)**:以序 0 的 `509/F4/E219` 用例级结果为基准,**比对用例名集合的增删** —— 只允许「原 ERROR 转 PASS」,不允许出现新的失败用例名。**不用「Errors ≤ 216」类固定阈值**(该数字随外部环境浮动,已被证明无意义)。

**变异测试**(比照 006):
- `ResponseUtils` 的新 case 删掉 → 401 断言应失败(证明不是靠别的路径蒙对)
- filter 的白名单放行分支删掉 → `RefreshTokenEndpointAccessTest` 应失败(请求被 401 挡住)
- filter 的 `return` 删掉 → 应有测试捕获(请求继续走到 controller)
- 基线校验的 fail-closed 改为 fail-open(未知 profile 放行)→ 应有测试失败
- L2 降级改为硬拒(503)→ 限流测试应失败(断言的是「仅告警、不拒绝」)
- L2 恢复 `Thread.sleep` 延迟 → `l2_overThreshold_doesNotBlockCallingThread` 应失败(v4:该断言专为防止 HIGH-1 回归而设)

**基线(2026-08-11 21:47 实测)**:`509 tests / Failures 4 / Errors 219 / Skipped 2`。`Failed to load ApplicationContext` 计数 0,keystore 相关错误 0。现存 Errors 根因全为外部依赖(MySQL 通信 4、Socket 3、超时 2、SQL 语法 2、SSL 1 等),与本 feature 无关。

> ⚠️ **回归风险最高的一处**:改 401 链路会影响**所有**带无效 token 的请求路径。既有 4 个 Failures 中 `CharacterControllerTest.addCharacterAuth` 就是 401 —— 须确认其断言不因本改动而变化。

---

## 7. 风险

| 风险 | 缓解 |
|------|------|
| **401 链路改造影响既有认证行为** | 最大回归面。序 2 单独提交,跑全量逐用例 diff;特别核对 `CharacterControllerTest.addCharacterAuth`(现 401) |
| refresh 端点加白后被滥用 | FR-020 两层限流是交付前提而非可选;白名单精确匹配不扩散;白名单路径零 Redis RBAC 成本(抗洪泛结构属性) |
| ~~按 IP 限流取到反代 IP~~ **已规避** | 已查明全仓无真实 IP 处理且未配 `forward-headers-strategy`(见 3.1)。改用**按 userId(L1 观测)+ 全局单键(L2 检测/整形)**两层方案,不依赖 IP,该风险不存在。**⚠️ 不是「按 refresh token 值 + 全局速率」—— 那是第二轮 CRITICAL-2 否决的方案**(键空间放大 DoS),M-3③ |
| ~~59 个 `@SpringBootTest` 集体失败~~ **已解除** | 序 0 已完成:`test-only.jks` 上 classpath,`Failed to load ApplicationContext` 计数 0 |
| **`whiteUrlList` 在生产被整体覆盖、加白静默失效**(HIGH-1) | prod 模板已同步;SC-016 人工核验生产 profile 实际文件;SC-006 须在生产配置下验收 |
| 六表审计无基线可比 | **AI 无法判断「哪些账号是预期的」**。`DEPLOYMENT.md` 须给出审计**方法**(SQL + 判断依据),结论由用户填写 |
| 多实例滚动重启 | FR-022:禁止滚动;须先确认拓扑 |

---

## 8. 合宪性检查

| 条款 | 符合性 |
|------|--------|
| 第四条 技术栈冻结 | ✅ 无新依赖。`ApplicationRunner`/`FileSystemResource` 为 Boot 自带;限流复用既有 Redis 模式;告警走 slf4j 结构化日志(Boot 自带)。**T037 另删除三个零引用的 `io.prometheus` 死依赖** —— 删除零引用依赖不属"升级或替换核心依赖",对运行时行为零影响(编译 + 全量回归实证),且消除了「有依赖无通路」的名实不符;冻结表已同步(`CLAUDE.md`) |
| 第五条 Spec-First | ✅ 规格已修订;本文件为阶段③ **v3**(序 0 后按实测重写) |
| DDD 分层 | ⚠️ `RefreshRateLimiterService` 置于 `domain/service/security`(与既有 `LoginRateLimiterService` 同包)。它依赖 `CacheGateway` 端口,不直接依赖 Redis,符合分层 |
| TDD | ✅ 序 0 后 `JwtFilterDiagnosticTest` 的失败即 RED 起点;白名单放行/限流各配契约测试 |
| 安全红线 | ⚠️ 测试 keystore 入库属论证过的例外(不保护真实凭证,别名 `test-only`,置于 **`src/main/resources/`** 且**有意进主 jar**,防线为 FR-008 fail-closed,见 SC-003 修订)。**M-3④ 事实更正**:v2 曾误写「置于 src/test/resources 不进主 jar」 |

---

## 9. 阶段④ 拆解方向

按第 1 节的 8 个序号拆,每序一个提交。序 1、2、3 建议各自独立提交并跑全量逐用例 diff —— 它们改动认证主链路。

**交付边界**:代码 + 测试 + 文档就绪即为本 feature 完成。生产轮换与六表审计由用户执行,其结果回填 `DEPLOYMENT.md`。

**交付前置动作(第三轮评审 LOW 项落为交付项)**:
- **L-7**:实现(序 1)开始前建 `007-jwt-key-rotation` 分支 —— 当前 007 文档在 006 分支上,与 AI_WORKFLOW「feature 分支按编号创建」不符。文档提交是否迁移由用户定
- **L-6**:`test-only.jks` 现为 `??` 未提交。其存在意义就是入库供 CI/新克隆直跑;序 1 提交时须一并 `git add`,否则「新克隆直跑」不成立

---

**下一步**: 本 v3 已按第三轮评审放行条件(HIGH-1/2/3 + M-3)完成文本修订。**评审明确:无需第四轮全量评审,由评审人抽查修订 diff 即可放行 `/speckit-tasks`。** M-2/M-4/M-5 与 LOW 项已映射为本文件与 tasks.md 的任务。

---

## 10. 会话撤销设计(T048 + T049,实现期派生)

> T042 死代码审计发现:登出只拉黑 access token jti,**从不撤销 refresh token**(7 天 TTL),登出形同虚设。T048 修复并派生 T049。两任务共享 sid 锚点与 Redis 键族,设计合并记录。

### 10.1 Redis 键族(会话维度)

| 键 | 值 | TTL | 写入点 | 来源/用途 |
|----|----|-----|--------|-----------|
| `refresh_token:<uuid>` | userId | refresh TTL | generateRefreshToken | 既有。refresh 凭证本体 |
| `refresh_session:<sid>` | `<uuid>` | refresh TTL | generateTokenPair | T048 正向索引:登出由 sid 定位当前 refresh token |
| `refresh_owner:<uuid>` | `<sid>` | refresh TTL | generateTokenPair | T048 反向指针:轮换(白名单端点,请求无 access token)由 refresh token 反查 sid 继承 |
| `session_revoked:<sid>` | `"1"` | refresh TTL | logout | **T049-A** 会话级失效标记:刷新前校验,闭合"登出后刷新复活会话" |
| `session_access_jti:<sid>` | `<jti>` | access TTL | generateAccessToken | **T049-B** 当前 access 的 jti:轮换时拉黑旧 access token |

所有键 TTL 与所保护凭证同寿,不得长于凭证。`sid` 轮换不变,登录生成、轮换经反向指针原样继承。

### 10.2 T048 - 登出撤销 refresh token(已实现,commit `fc03461`)

- **锚点用 `sid` 而非 `jti`**:jti 每次轮换都变、轮换不拉黑旧 access、`POST /auth/tokens` 在白名单 -> 攻击者一个未认证请求即可让 jti 索引永久错位,受害者登出撤不到当前凭证却返回 204
- **顺序「先撤销后拉黑」**:反序则撤销失败时 access 已拉黑 -> 重试登出被 filter 401 -> 永久登不出
- **索引缺失 fail-open** + `[SECURITY_ALERT:LOGOUT_REVOKE_MISS]`:fail-closed 会永久锁死索引缺失用户退出
- **`Boolean.TRUE.equals`** 防 `delete` 返回 null 拆箱 NPE(NPE 发生在拉黑前 -> 整个登出 500)

### 10.3 T049 - 刷新校验会话失效 + 轮换拉黑旧 access(本任务,A+B)

**Concern A - `session_revoked:<sid>` tombstone**:
- 登出在 `AuthApplicationService.logout` 中、`revokeRefreshTokenBySession` 返回**之后**无条件 set tombstone(仅当 `parsed.sessionId()` 非空,TTL = refresh TTL)。**不得**置于 `revokeRefreshTokenBySession` 内部 -- 该方法有 5 条 early-return(索引驱逐/sid 空/token 已不在缓存/递归等),内置 tombstone 会在这些路径被跳过,而它们恰是攻击者可利用的 fail-open 路径(评审 HIGH-1)
- 刷新 `refreshAccessTokenWithUser` 在 claim 旧 refresh token **之后**、generate **之前** check tombstone;存在则抛与既有失效同一文案异常(不新增可区分信息)
- **闭合的交错**:刷新 claim 旧 token -> 登出跑完(删索引 + set tombstone)-> 刷新 check tombstone -> 拒绝
- **残留窗口(接受 + 文档)**:刷新 check tombstone(缺)-> 挂起 -> 登出 set tombstone -> 刷新 generate。亚毫秒级、需精确交错,新 access 最多活 15min。**评审确认不可主动拉长**(claim 与 check 间无 I/O 暂停点;claim 原子性保证只有一个 refresh 进入 check-generate 区间;tombstone 设置后所有后续 refresh 被阻断)。残留窗口内可能产生不止一个 access token,但均在 ~15min 内过期,总暴露 ~15min 非 15min×N。全闭合需 Lua,但 JWT 签名不能进 Lua,无法真正原子

**Concern B - 轮换拉黑旧 access jti**:
- `generateAccessToken` 写 `session_access_jti:<sid> -> <jti>`(TTL = access TTL)
- `refreshAccessTokenWithUser` generate 前 read 旧 jti,存在则拉黑;**null(键被驱逐或 pre-T049 滚动部署)时跳过拉黑并打 `[SECURITY_ALERT:ROTATE_BLACKLIST_MISS]`**(评审 MEDIUM-2:使旧 access 残活可观测)
- **`addToBlacklist` 签名(评审 HIGH-3)**:既有 `addToBlacklist(String jti, Date expirationTime)` 的 TTL 从 exp 计算,但轮换路径只有 jti、无旧 access 的 exp。新增 `addToBlacklist(String jti, long ttlSeconds)` 重载,黑名单 TTL 用固定 access TTL(900s)。可能比旧 token 实际剩余寿命长(旧 token 或已快过期)-- 无害的不对称(黑名单只是 marker,多活几秒不影响安全),须显式接受
- 登出已从请求解析 jti 拉黑(用真实 exp),不依赖此键;两路径独立,`addToBlacklist` 用 SETNX 幂等,无竞态(评审确认)

### 10.4 边界条件

1. **Tombstone TTL ≥ refresh TTL**(否则 refresh token 超活 tombstone -> fail-open)。取 = refresh TTL
2. **检查时机**:claim 后、generate 前。早于 claim 则登出可夹在 check/claim 间;晚于 generate 则无效
3. **Tombstone 不可复用**:登出 set,轮换从不 clear。session index 被 rotation 重写,不能作 tombstone 信号
4. **`allkeys-lru` 驱逐有三条 fail-open 路径**(评审 HIGH-2 + MEDIUM-1):① 驱逐 `refresh_token:*` = fail-safe(凭证失效);② 驱逐 `refresh_session:*` / `session_revoked:*` = fail-open(撤销标记丢失);③ **驱逐 `refresh_owner:*` = 更严重的 fail-open**:轮换新建 sid,tombstone 查不到(不仅标记丢失,sid 本身变了,原 tombstone 完全无用)。**`volatile-lru` 无效** -- 所有 auth 键都带 TTL,volatile-lru 与 allkeys-lru 对它们行为一致。唯一有效方案:auth 键独立 Redis 实例或不设 maxmemory。`docs/DEPLOYMENT.md:827` 据此修正(T049 关联项)
5. **Redis null**(异常):tombstone get 返回 null 视为"未登出"(fail-open)。**不可告警**(评审 MEDIUM-3):tombstone absent 是每次正常刷新的常态(用户未登出时 tombstone 不存在),无法区分"正常未登出"与"被驱逐"。与 T048 的 `LOGOUT_REVOKE_MISS` 不同(后者在 logout 侧,索引缺失=异常);refresh 侧 tombstone 驱逐不可观测,依赖部署侧消除 allkeys-lru
6. **滚动部署**:pre-T049 token 有 sid(T048 已加)但无 `session_access_jti` 键 -> B 跳过(旧 access 已在过期);pre-T048 无 sid -> T048 fallback

### 10.5 不做(超出 T049 边界)

- **Lua 原子化 claim+check+generate**:JWT 签名不能进 Redis Lua,无法真正原子;残留窗口靠 TTL 兜底 + 文档声明,不引入半原子方案(半原子比显式窗口更危险:让人误以为已闭合)
- **access token 单活语义**:Concern B 实现"轮换拉黑旧 jti",**best-effort 单活** -- 依赖 `session_access_jti` 键未被驱逐;驱逐时旧 access 残活最长 access TTL(评审 LOW-1)。无需额外语义
- **启动期校验 Redis maxmemory-policy**(评审 MEDIUM-2,非阻塞):需启动时 `CONFIG GET maxmemory-policy` 查询 Redis,属新能力(当前 `SecurityBaselineValidator` 只读 `Environment`),且多实例下难判定 auth 键是否与业务键共用。T049 仅文档化能力边界 + `[SECURITY_ALERT:ROTATE_BLACKLIST_MISS]` 日志,启动校验留作后续
