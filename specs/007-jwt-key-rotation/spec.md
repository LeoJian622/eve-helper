# 规格：JWT 签名密钥轮换

**Feature**: `007-jwt-key-rotation`
**创建日期**: 2026-08-11
**状态**: 规格已第三次修订(2026-08-11 21:50)。**序 0 已完成**(测试 keystore 上 classpath、59 个 `@SpringBootTest` 恢复、新基线 `509/F4/E219` 已测定)。Q1 决策为**选项 B**(清空 refresh token + 六表审计,暂不强制改密)。SC-003 已按用户决策改为「不含**生产** keystore」。**`plan.md` 待写 v3** —— 须补白名单共享组件设计与按 userId 限流方案(第二轮评审 CRITICAL-1/2)
**来源**: 006 提交前安全检查发现 —— `src/main/resources/eve-jwt.jks` 自 `5d139d8「增加token认证」` 起已在版本控制中

---

## 1. 背景与问题陈述

### 1.1 事实认定(已读代码核实;**行号经阶段③评审更正**)

> ⚠️ 本节初稿曾写「已核实,勿重复质疑」。该措辞已删除 —— 阶段③安全评审正是因为**重新核实**才发现 3 项 CRITICAL(见 [设计评审记录](../../docs/reviews/2026-08-11-007-jwt-key-rotation-design-review.md))。**欢迎质疑本节任何一条。**

| 事实 | 证据 |
|------|------|
| `eve-jwt.jks`(2172 字节,含 RSA 私钥)已被 git 跟踪 | `git ls-files '*.jks'` 返回该路径;`git log --follow` 显示自 `5d139d8` 入库 |
| `.gitignore` 此前无 `*.jks` 规则 | 已于 `84c099a` 补上,但**已跟踪文件不受 .gitignore 约束**,故其状态未变 |
| keystore 口令**未**泄露 | `application.yml:113-118` 全部使用 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 环境变量;实测 `keytool -storepass changeit` 报 "Keystore was tampered with, or password was incorrect" |
| 私钥用于签发系统 JWT | `TokenService.java:98` `new RSASSASigner(keyPair.getPrivate())`,算法 RS256(`:95`) |
| 公钥用于验签 | `JwtAuthorizationTokenFilter.java:67-68` `new RSASSAVerifier((RSAPublicKey) keyPair.getPublic())` |
| **ESI 授权不受影响** | `EsiApiService.java:414` 解析的是 CCP 签发的 token,只读 claim **不验签**,与本 keystore 无关 |
| **refresh token 不含签名** | `TokenService.java:111-116` refresh token 是随机 UUID,存 Redis `refresh_token:{uuid} -> userId`。轮换密钥**不使其失效** |
| refresh 端点存在 | `POST /auth/tokens`(`AuthController.java:51-52` → `AuthApplicationService:134`)。⚠️ **但不在白名单,无有效 access token 时不可达 —— 见 1.4** |
| access token TTL = 900s | `JwtTokenProperties:30` `accessTokenExpirationTime = 900` |
| refresh token TTL = 604800s(7 天) | `JwtTokenProperties:35` |
| refresh token 只在两处签发 | `AuthenticationSuccessServletHandler:63`(表单登录,需真实口令)与 `TokenService:184`(需已存在的 refresh token)。**伪造 access token 无法换取 refresh token** —— 阶段③评审核实 |

### 1.4 ⚠️ 阶段③评审推翻的三项核心前提(实测确认)

初稿方案建立在三个前提上,**经诊断测试 `JwtFilterDiagnosticTest` 实测,全部不成立**:

| 初稿论断 | 实测结果 | 影响 |
|---------|---------|------|
| 「验签失败必然返回 401,客户端 401 重试会被触发」(原 6.1) | **异常直接逃逸**。`JwtAuthorizationTokenFilter:71-73` 抛的 `InvalidCookieException` 因该过滤器位于 `ExceptionTranslationFilter` **上游**而冒泡至容器,连响应都不生成 | US1 验收场景 1、SC-002 **不成立**;客户端 401 重试**不会触发** |
| 「客户端凭 refresh token 调 `POST /auth/tokens` 换新 token」 | **端点不可达**。无 token 时返回 401 `AUT00201`「用户未登录」,未到 controller;带失效 token 时异常逃逸 | US1、US3、SC-006 **不成立** |
| 「残余风险仅为攻击者可能窃取 refresh token」(原 US3) | **严重低估攻击者能力**。伪造 ROOT token 可篡改 `sys_permission` 的 url_perm→role 映射或给自注册账号赋 ROOT(`POST /user` 在白名单,无需伪造);其效果**完全不受密钥轮换约束** | Q1「不强制登出」的决策依据**被推翻**,需重新决策 |

**附带确证**:`ResponseUtils.java:26-35` 的 switch 中 `TOKEN_ACCESS_EXPIRED`(`AUT00210`)**不在** 401 分支,落 `default` → **400**。故原 FR-016/SC-010 描述的「401 + AUT00210」在现有代码下是**不可达状态**。

**结论:本规格需实质修订,当前状态为 BLOCK。** 修订清单见评审记录第七节。

### 1.2 风险评估

**私钥一旦入库即应视为已泄露**:凡 clone 过本仓库的人(含任何 fork、CI 缓存、备份)本地都有一份。删除文件或改写历史都无法收回已分发的副本。

**攻击者若同时取得私钥与口令**,可伪造任意用户的 access token —— 包括 ROOT 角色,从而完全绕过 RBAC。`JwtAuthorizationTokenFilter` 只验签名、过期、黑名单三项,签名有效即信任 claim 中的 `userId` 与 `authorities`(`:90-96`)。

**当前缓解**:口令未入库(`.gitignore` 忽略 `application-*.yml`)。攻击者需另外取得 `KEYSTORE_PASSWORD` 与 `KEY_PASSWORD` 才能使用私钥。

### 1.2.1 ⚠️ 该缓解基本不成立(2026-08-11 编写规格时发现)

调研 `KeyPairConfig` 的测试依赖时发现:`application-test.yml` 与 **`application-aliw.yml:157-162`(生产环境之一)** 均以**明文写死** keystore 口令与密钥口令。**编写规格当时**两者为同一个 6 字符弱口令(纯小写字母+数字,字典词+数字后缀形态。**实际值不在本文档中记录**)。

> ⚠️ **该「同一个弱口令」的描述已不成立**(第二轮评审 MEDIUM-3 指出,本次修订更正):`application-test.yml` 现已改用独立的 `test-only.jks`,口令与生产**无任何关联**。**`application-aliw.yml`(生产)仍是 6 字符明文口令** —— 下表的紧急性论证**仅凭这一条即成立**。

由此:

| 原假设 | 修正后 |
|--------|--------|
| 「私钥已泄露但口令未泄露,攻击者无法使用」 | **口令强度不足以抵抗离线暴破。** JKS 私钥条目用 PBE 保护,对 6 字符字典词形态口令,单机破解在**秒级到分钟级** |
| 「轮换优先级:高但不紧急」 | **紧急。** 拿到 git 中的 `eve-jwt.jks` 即可在极短时间内取出私钥,进而伪造任意用户(含 ROOT)的 access token |
| 「只有 test 环境用弱口令」 | `application-aliw.yml` 是**生产 profile**,明文 6 字符弱口令 |

> **2026-08-11 21:35 现状更正**(第三次更正 —— 前两次描述均已过期,本次基于实测):
>
> | 密钥 | 实际身份 | 位置 | git 状态 |
> |------|---------|------|---------|
> | `eve-helper.jks` | **生产新密钥**(非测试密钥 —— 前版本误认) | **已移出工作区**(用户执行) | 从未入库,历史干净 |
> | `test-only.jks` | 测试专用(别名 `test-only`,2048 RSA,口令公开) | `src/main/resources/` | 故意入库,`.gitignore:65` 反向放行 |
> | `eve-jwt.jks` | **旧生产密钥,已泄露** | `src/main/resources/` | **仍跟踪**(用户决定暂留,FR-012 待执行) |
>
> - `application-aliw.yml`(**生产**)**仍是 6 字符明文口令** → 紧急性判断不变
> - `application-test.yml` 现为 `location: test-only.jks` / `alias: test-only` / 明文口令(测试专用,有意公开)
> - **59 个 `@SpringBootTest` 已恢复可加载** —— 序 0 完成,实测 `Failed to load ApplicationContext` 计数为 **0**
> - **新回归基线(2026-08-11 21:47,`mvn clean test` 实测)**:`509 tests / Failures 4 / Errors 219`。现存错误根因为外部依赖(MySQL 通信 4、Socket 3、超时 2、SQL 语法 2、SSL 1),**与 007 无关**;keystore 相关错误 **0 处**

**这不改变方案设计,但改变时间表**:轮换应尽快执行,而非排入常规迭代。

**追加要求**:
- **FR-017** 新口令必须为高强度随机串(建议 ≥32 字符随机生成),不得为字典词或其变形
- **FR-018** 所有 profile 的 `security.keystore.password` / `key-password` 必须改为 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 环境变量引用,禁止明文 —— 现仅 `application.yml`(入库那份)做到了,`application-test.yml` 与 `application-aliw.yml` 未做到
- **FR-019**(**已按 2026-08-11 现状与用户决策修订**)test profile 须使用**与生产不同**的 keystore 与口令。**已完成部分**:`test-only.jks`(别名 `test-only`,2048 RSA)置于 `src/main/resources/`,`.gitignore:65` 加反向放行规则 `!src/main/resources/test-only.jks` 使其入库,`application-test.yml` 三项配置已对齐并实测可加载。
  - **口令保持明文,这是有意决策**:测试 keystore 不保护任何真实凭证,口令公开无损失;入库 + 明文口令使 CI 与新克隆可直接跑测试(无需额外注入 keystore)
  - **故 FR-018 的「所有 profile 改环境变量」不适用于 test profile** —— 这是与 FR-018 的有意分歧,而非遗漏
  - **代价**:`test-only.jks` 会进生产 jar(SC-003 已按此修订)。防线是 FR-008 的 fail-closed 校验,而非文件缺席

> 注:`application-prod.yml` 与 `application-ali.yml` 无 `security:` 段,依赖 `application.yml` 的环境变量形态,这两个是正确的。

### 1.3 为何不能只删文件

| 做法 | 为何不够 |
|------|---------|
| `git rm` + 提交 | 历史中仍在。`git show 5d139d8:src/main/resources/eve-jwt.jks` 仍可取出 |
| `git filter-repo` 改写历史 + 强推 | 与项目规则「禁止 `git push --force` 到共享分支」冲突;且已分发的副本收不回;协作者需重新 clone |
| 仅轮换口令 | 私钥本体未变。旧私钥 + 新口令无关 —— 攻击者用的是**旧私钥 + 旧口令** |

**唯一有效的处置是轮换密钥对本身**:生成新 RSA 密钥,使旧私钥签发的 token 全部失效。

---

## 2. 用户故事

### US1 标题更正 — 密钥轮换后旧会话干净终止(P1)

作为运维人员,我需要把 JWT 签名密钥换成新生成的密钥对,并**确保所有旧会话被干净地终止** —— 用户被明确地引导到登录页,而非撞上错误页或陷入重试循环。

**决策变更(2026-08-11,阶段③评审后)**:原 US1 追求「用户无感」,该目标已放弃 —— 依据(refresh 可用)被评审 C2/C3 推翻,且 Q1 已改为清空 refresh token(见 US3)。**新目标是「干净登出」**。

**为何仍必须修 C3**:清空 refresh token 后 refresh 必然失败,但若验签失败仍是异常逃逸(容器错误页),客户端**无法识别这是认证失效**,不会跳登录页。修好 401 后链路才闭合:`401 → 客户端触发 refresh → refresh 返回明确的 401 → 跳登录页`。

**验收场景**:
1. **Given** 用户持有旧密钥签发的 access token,**When** 完成轮换并重启应用,**Then** 请求返回 **HTTP 401** 且响应体 code 为 `AUT00210`(**不是**异常逃逸、不是 500、不是 400)
2. **Given** 轮换完成且 refresh token 已清空,**When** 客户端用旧 refresh token 调 `POST /auth/tokens`,**Then** 返回明确的 401/业务错误(**必须到达 controller**),使客户端能判定「需重新登录」而非无限重试
3. **Given** 用户重新登录,**When** 用新签发的 access token 访问受保护端点,**Then** 正常返回 200

### US2 — keystore 不再随源码分发(P1)

作为部署者,我需要 keystore 从**外部路径**加载,而非打进 jar 的 classpath,使其不可能再被误提交。

**当前实现的问题**:`KeyPairConfig.java:44` 用 `new ClassPathResource(location)` —— keystore 必须位于 `src/main/resources`,即**必然进入源码树与构建产物**。

**验收场景**:
1. **Given** keystore 置于 `src/main/resources` 之外的路径,**When** 通过 `KEYSTORE_LOCATION` 指定文件系统绝对路径,**Then** 应用正常加载并签发 token
2. **Given** `KEYSTORE_LOCATION` 指向不存在的路径,**When** 应用启动,**Then** **拒绝启动**并给出明确错误(不得退化为 classpath 回退,那会静默用回旧 keystore)
3. **Given** 构建产物,**When** 检查 jar 内容,**Then** 不含**生产** keystore(允许含 `test-only.jks`,见 SC-003 修订说明)

### US3 — 清空 refresh token 并审计入侵痕迹(P1,决策已修订)

**决策变更(Q1 重新决策,2026-08-11)**:原「不强制登出」已废弃 —— 其依据被评审 C2/C3 推翻。**新决策(选项 B)**:

| 动作 | 做? | 理由 |
|------|-----|------|
| 修 C2/C3(401 链路 + refresh 端点可达) | ✅ | 无论是否登出都必须修,否则用户撞容器错误页 |
| 清空 `refresh_token:*` | ✅ | 私钥在 git 中长期存在且口令弱,**无法证明未被利用**;清空是唯一能收敛已知未知的动作 |
| 审计 `sys_user` / `sys_user_role` / `sys_permission` / `eve_account` | ✅ | 伪造 ROOT token 可造成的持久化影响**不受密钥轮换约束**,必须人工核查 |
| 强制全体改密 | ❌ **暂不做** | 见下方残余风险 |

**明确接受的残余风险(用户已知悉;第二轮评审后补全 —— 原表述不完整)**:

不强制改密意味着以下三条向量**均不被本 feature 的任何动作覆盖**:

| # | 残余向量 | 为何轮换/清空/审计都无效 |
|---|---------|------------------------|
| 1 | **攻击者自注册账号 + 自设口令 + 已提权** | 口令是他自己设的真实口令,轮换密钥与清空 refresh token 都不影响其登录。**仅靠表审计能发现** |
| 2 | **`sys_user` 的 BCrypt 口令哈希可能已被读走** | 伪造 ROOT token 后读用户表是最自然的动作。哈希外泄后弱口令用户在离线爆破下失守 —— **这恰恰只能靠强制改密缓解**;审计**看不出**哈希是否被读过(无审计日志) |
| 3 | **`eve_account.refresh_token`(ESI 长期凭证)外泄后不因本次轮换失效** | 该列明文存储(006 L-2);`CharacterAccessTokenController` 可直接交出可冒用的 bearer 凭证。若攻击者曾以 ROOT 身份取走这些 ESI refresh token,**本 feature 无任何动作能撤销** —— 唯一处置是让角色重新走 ESI 授权 |

**关键修正**:原表述称「六表审计是唯一防线」—— **不准确**。准确表述:

> 表审计只能发现**新增/篡改**痕迹(向量 1);对**读取型**外泄(向量 2 的口令哈希、向量 3 的 ESI 凭证)**完全失明,亦无补偿控制**。「无法证明未被利用」这句话对读取型外泄同样成立。

选项 B 可以是合理的风险决策,但它接受的是**不可观测的**残余风险,而非「审计能兜住的」风险。

**审计范围(第二轮评审后由 4 表扩至 6 表)**:

RBAC 的 url→role 映射来自三表 JOIN(`SysPermissionMapper.xml:201-211`:`sys_permission` LEFT JOIN `sys_role_permission` LEFT JOIN `sys_role`)。攻击者提权**不必碰 `sys_permission`** —— 只要在 `sys_role_permission` 插一行把自己的角色关联到既有权限,或改 `sys_role`,即可获得访问权。原 4 表清单**漏了后两张**,审计通过会给出虚假的安全结论。

1. `sys_user`:全表比对预期账号清单,重点看 `5d139d8` 提交之后创建的行
2. `sys_user_role`:是否有非预期的 ROOT/管理员角色绑定
3. `sys_permission`:`url_perm` → 权限定义是否与预期基线一致
4. **`sys_role_permission`(新增)**:角色↔权限关联是否被插入新行
5. **`sys_role`(新增)**:角色定义是否被篡改
6. `eve_account`:是否有异常的角色绑定

> `sys_menu` / `sys_role_menu` 仅影响 UI 展示,低优先。

**验收场景**:
1. **Given** 轮换完成,**When** 执行 `SCAN MATCH refresh_token:* + DEL`,**Then** 全部 refresh token 失效,`POST /auth/tokens` 返回明确错误(而非异常逃逸)
2. **Given** 审计上述 6 张表,**When** 发现任何非预期账号/角色/权限映射/关联,**Then** **暂停轮换收尾,升级为入侵响应**(此时强制改密、吊销全部会话、ESI 重新授权均成为必须)
3. **Given** 审计未发现异常,**When** 记录审计结论,**Then** 在 `docs/DEPLOYMENT.md` 留档「已审计 6 表,未发现**新增/篡改**痕迹;读取型外泄无法检测」并注明时间与范围 —— **不得**表述为「确认未被入侵」

---

## 3. 功能需求

### 密钥生成与配置

- **FR-001** 新密钥对必须为 RSA 2048 位或以上(现有代码固定 RS256,见 `TokenService:95`;不改算法)
- **FR-002** 新 keystore 口令与密钥口令必须与旧值不同,且不得写入任何入库文件。**口令由用户单独生成与保管,不经 AI、不入文档**(Q2)
- **FR-003** keystore 别名可沿用 `eve-jwt`(`SecurityProperties:36` 默认值),或改用新别名并同步 `KEYSTORE_ALIAS`
- **FR-004** 生成命令必须记录于 `docs/DEPLOYMENT.md`,但**命令中的口令须为占位符**

### 加载方式改造

- **FR-005** `KeyPairConfig` 必须支持从文件系统绝对路径加载 keystore。目标路径 `/etc/eve-helper/eve-jwt.jks`,权限 600,属主为运行应用的服务账号(Q3)
- **FR-006** 当 `KEYSTORE_LOCATION` 以 `classpath:` 开头时保留 classpath 加载(兼容测试),否则按文件系统路径解析
- **FR-007** keystore 文件不存在或无法解密时必须 **fail-fast 拒绝启动**,禁止静默回退
- **FR-008** 生产 profile 下若 `KEYSTORE_LOCATION` 指向 classpath,必须拒绝启动(与 006 L-10 的 fail-fast 断言合并实现,见第 8 节)(Q4)

### 轮换执行

- **FR-009** 轮换过程中**不得**同时存在两套密钥签发或验签 token(不实现双密钥并行 —— 理由见第 4 节:与选项 B 的「全体登出」目标冲突,且会延长泄露私钥的有效期)
- **FR-010** 轮换后必须验证:旧 access token 返回 401、refresh 换取成功、新 access token 可访问受保护端点
- **FR-011** 轮换须在低峰期执行,并在执行前后记录 `refresh_token:*` 键数量作为影响面证据
- **FR-015**(**已修订**)**清空 `refresh_token:*`**(Q1 重新决策为选项 B)。轮换后执行 `SCAN MATCH refresh_token:* + DEL`;FR-011 记录清空前的键数量作为影响面证据
- **FR-016**(**升为交付前提**)验签失败必须返回 **HTTP 401** 且响应体 code 为 `AUT00210`(`TOKEN_ACCESS_EXPIRED`,`ResultCode.java:43`)。实测证明当前是**异常逃逸**(见 1.4),故本项须包含两处改造:
  - 改造 `JwtAuthorizationTokenFilter`:在 `doFilterInternal` 内捕获并直接写 401 响应(复用 `ResponseUtils`),不得依赖上游的 `ExceptionTranslationFilter`
  - 修正 `ResponseUtils.writeErrorInfo:26-35` 的 switch,使 `TOKEN_ACCESS_EXPIRED` 映射 401(现落 `default` → 400)
- **FR-020**(**限流条款经第二轮评审驳回后重写**)`POST:/auth/tokens` 必须在无有效 access token 时**可达 controller**。两部分:
  - **可达性**:加入 `whiteUrlList`(已核实 `RbacAuthorizationManager:67-73` 为精确字符串匹配,不扩散授权)。⚠️ **但仅加白名单不够** —— 白名单判定在过滤器链末端的 `AuthorizationFilter`,而 `JwtAuthorizationTokenFilter` 在其上游会先拒掉带旧 token 的请求。须抽出白名单匹配为过滤器可见的共享组件(见 plan 3.2 CRITICAL-1)
  - **限流**:**按 userId 计数,且只计失败**。流程:UUID 格式校验(`AuthApplicationService:110` 已有)→ 一次 Redis GET 解出 userId → 以 userId 为桶。键空间被真实用户数**有界约束**;只计失败则轮换瞬间的合法尖峰不受影响。全局层面**仅告警/降级,不做硬拒**
  - **已驳回的两个方案**(勿重提):按 refresh token 值计数 —— 攻击者换随机 UUID 即绕过,且每个新值在 Redis 种一个 key,构成**未认证的键空间放大 DoS**;全局硬限流 —— 攻击者打满阈值即可让全体用户无法恢复会话,是自伤总闸
  - **不得**以 `LoginRateLimiterService` 为范本:其按 username 计数、5 次锁 30 分钟(`:25-26`),任何人可定向锁死任意已知用户(既有 DoS,超出 007 范围但不可复制)
- **FR-021**(**范围经第二轮评审由 4 表扩至 6 表**)审计 `sys_user`、`sys_user_role`、`sys_permission`、**`sys_role_permission`**、**`sys_role`**、`eve_account`。补入后两张的理由:RBAC 的 url→role 映射来自三表 JOIN(`SysPermissionMapper.xml:208-210`),攻击者提权**不必碰 `sys_permission`** —— 在 `sys_role_permission` 插一行即可。范围与结论记入 `docs/DEPLOYMENT.md`;发现异常即升级为入侵响应(US3)
- **FR-022** 明确部署拓扑。若为多实例,轮换**必须停机窗口或全量同时重启,禁止滚动重启** —— 滚动期间新旧密钥并存会造成随机认证失败,且 `TokenService:181` 在生成新 token 前就删除旧 refresh token,重试循环会消耗掉 refresh token
- **FR-023** 重写 `KeyStoreKeyFactory`:去除无效的双重 `synchronized`(内层对同一 lock 重入,无作用)与可变 `store` 字段;别名不存在时显式 null 检查并给出「别名错误」而非误导性的「Cannot load keys from store」;异常分类(文件不存在/口令错误/别名错误/非 RSA 密钥)各给独立信息;**错误信息禁含口令**;加载后断言 RSA modulus ≥ 2048 位(FR-001 当前无任何位数校验,无法验证)

### 清理与文档

- **FR-012** 从工作区删除 `src/main/resources/eve-jwt.jks`(`git rm`)
- **FR-013** 在 `docs/DEPLOYMENT.md` 记录:旧私钥已泄露、轮换日期、旧密钥不得再用于任何环境
- **FR-014** **不改写 git 历史** —— 遵守「禁止强推共享分支」。历史中的旧私钥在密钥轮换后失去价值(不再验签任何 token),故无需改写

---

## 4. 明确排除(YAGNI)

| 项 | 排除理由 |
|----|---------|
| 双密钥并行(新私钥签发 + 新旧公钥验签) | **论证已按阶段③评审重写。** 初稿称「需加 `kid` header、维护 keyId→key 映射」—— 评审指出这**高估了复杂度**,实际只需 filter 依次尝试两个 verifier,无需 kid。<br>**但选项 B 下它变得不必要且有害**:①既然已决定清空 refresh token、全体登出,保留旧公钥验签就失去了目的(它的价值只在于让旧 token 平滑过渡);②接受旧公钥验签会**延长泄露私钥的有效期** —— 攻击者可继续用泄露私钥伪造 token,直到过渡期结束。若采用必须严格限定为一个 access TTL(900s)后立即移除,否则变成永久后门。<br>**结论:排除,但理由是「与选项 B 目标冲突」,而非「太复杂」** |
| 改写 git 历史 | 与项目规则冲突(禁止强推);且已分发副本收不回;密钥轮换后旧私钥失去价值 |
| 迁移到 JWKS / 外部 KMS | 属架构升级,应独立评估。本 feature 只解决「已泄露密钥必须轮换」 |
| 改用 EdDSA / ES256 | 技术栈冻结(宪法第四条);且 RS256 无安全缺陷 |
| 自动化定期轮换 | 先把一次性轮换做对。周期轮换需要双密钥并行才能无感,见上 |

---

## 5. 成功标准

- **SC-001** 新密钥对签发的 token 可正常通过验签,受保护端点返回 200
- **SC-002** 旧密钥签发的 access token 一律返回 401
- **SC-003**(**已按用户决策修订,2026-08-11**)构建产物(jar)内**不含生产 keystore**(`eve-jwt.jks` 或任何生产密钥)。
  - ⚠️ **不再要求「不含任何 `.jks`」** —— `test-only.jks` 有意置于 `src/main/resources/` 并入库(供 CI 与新克隆直接跑测试),它**必然进 jar**,原措辞在此方案下永不可满足
  - **真正的防线不是「jar 里没有 jks」,而是 FR-008 的 fail-closed 基线校验**:生产 profile 若指向 classpath keystore 则拒绝启动。这与第二轮评审 HIGH-3② 的判断一致(「文件命名只降低误认概率,真正防线是 fail-closed 基线校验」)
  - **验证方式**:`mvn clean package` 后 `unzip -l target/*.jar | grep '\.jks'`,断言结果**仅含 `test-only.jks`**。须在 `mvn clean` 后执行,否则 `target/classes/` 残留导致假阴性(评审 HIGH-3③)
  - **实测现状(2026-08-11 21:47,`mvn clean test` 后)**:`target/classes/` 含 `eve-jwt.jks` 与 `test-only.jks` → 生产密钥确实会进 jar,**这是干净的 RED 证据**,由 FR-012 + US2 解决
- **SC-004** `KEYSTORE_LOCATION` 缺失或指向不可读文件时应用拒绝启动,错误信息明确指出缺哪个环境变量
- **SC-005** 生产 profile 下 keystore 位于 classpath 时拒绝启动
- **SC-006**(**已修订**)轮换且清空 refresh token 后,`POST /auth/tokens` 用旧 refresh token **到达 controller 并返回明确的业务错误**(而非异常逃逸或 403),使客户端能判定「需重新登录」。
  - ⚠️ **须在 `生产 profile 实际配置`下验收,不得仅凭 test profile 结论外推**(第三轮评审 HIGH-1):whiteUrlList 是 List 属性,profile 文件整体覆盖 application.yml,加白可能在生产静默失效
- **SC-007** `docs/DEPLOYMENT.md` 含轮换步骤与「旧密钥已泄露」的明确记录,且不含任何真实口令
- **SC-008**(**已修订**)轮换后 Redis `refresh_token:*` 键数量为 **0**(FR-015:已清空);清空前的数量已记录
- **SC-009** keystore 文件权限为 600、目录 700、属主为服务账号(`stat` 输出留证)
- **SC-010**(**已修订**)验签失败返回 **HTTP 401** 且 body code 为 `AUT00210` —— 当前实测为异常逃逸,该断言现在**必然失败**,正是 TDD 的 RED 起点
- **SC-013** **6 张表**审计完成,结论记入 `docs/DEPLOYMENT.md`,表述为「已审计 6 表,未发现**新增/篡改**痕迹;读取型外泄(口令哈希、ESI 凭证)无法检测」而非「确认未被入侵」
- **SC-014** 用**旧私钥现场签发一个全新的**(未过期)token,断言其被拒绝;并断言新公钥 modulus 与旧公钥不同 —— **这是唯一能证明密钥对确实换掉的验证**(仅验证「已存在的旧 token 失效」在别名/口令变更但密钥未变时会误判通过)
- **SC-015** `POST /auth/tokens` 有限流(两层,均不依赖客户端 IP,理由见 FR-020)。
  - **同一 refresh token 反复调用**:第二次起因旧 token 已被撤销(`TokenService:181` 先撤销后换发)而返回明确业务错误 —— **这是 token 轮换机制,非限流**(L-10)
  - **洪泛观测**:L2 全局单键计数无效 token 请求,超阈值**仅触发告警**(**既非硬拒也不施加时延**;硬拒 = 自伤总闸,第二轮已否决;固定延迟 = DoS 放大器,**实现阶段 T036 推翻 v3 裁决**——`Thread.sleep` 占住 Tomcat 工作线程,而该端点未认证可达,反使全站可用性依赖于攻击者的并发数)
  - **被盗 token 场景**:被盗 refresh token 走**成功路径**,两层计数均不触发 —— 此为**已接受的残余风险**,由 US3 六表审计兜底(M-2)
- **SC-011**(**验证方式经评审更正 —— 原写法是假门禁**)全部 profile 的 `security.keystore.password` / `key-password` / `location` 均为 `${...}` 环境变量引用。
  - ⚠️ **不能用「`grep` 全仓无明文口令」验证**:`application-{test,ali,aliw,prod}.yml` 全在 `.gitignore` 中,仓库里根本没有它们 → grep **恒为通过**,是个假门禁
  - **正确验证**:由**人工**核验部署环境的实际配置文件并留证(与 FR-002 的职责边界一致 —— AI 无法访问这些文件)
- **SC-012** 新 keystore 口令为高强度随机串,与旧口令无关联(FR-017;由用户自行核验,不留证于文档)
- **SC-016**(**新增,第三轮评审 HIGH-1**)所有**生产 profile 实际文件**的 `eve.helper.whiteUrlList` 均含 `POST:/auth/tokens`,或确认其未定义 whiteUrlList(由 application.yml 生效)。
  - ⚠️ 与 SC-011 同型:`application-{ali,aliw,prod}.yml` 不入库,grep 仓库恒通过是假门禁,**须人工核验部署环境实际文件并留证**
  - 背景:Spring Boot 对同名 List 属性是**整体替换**而非合并,`application-prod.yml.example:137` 自带 whiteUrlList 副本即为明证

---

## 6. 边界决策(已由用户确认,2026-08-11)

| # | 问题 | **决策** | 说明 |
|---|------|---------|------|
| Q1 | 是否强制全体用户重新登录? | **是(选项 B)** —— 2026-08-11 重新决策 | 原「不强制」的依据被评审 C2/C3 推翻。现:清空 `refresh_token:*` + 审计六张表,**暂不强制改密**(残余风险见 US3) |
| Q2 | 现有 keystore 口令强度 | **由用户单独掌握** | 口令不进入本仓库、不进入任何文档、不由 AI 评估。**但编写规格时在 `application-test.yml` 与 `application-aliw.yml` 中发现明文弱口令 —— 见 1.2.1,该发现使轮换升为紧急** |
| Q3 | keystore 新位置 | **服务器固定路径** | `/etc/eve-helper/eve-jwt.jks`,文件权限 600,属主为运行应用的服务账号 |
| Q4 | 合并实现 006 的 L-10 fail-fast | **是** | 一个 `ApplicationRunner` 同时校验 4 项配置基线,见第 8 节 |
| Q5 | 是否通知用户 | **是(需公告)** | Q1 改为清空 refresh token 后全体用户会被登出,必须提前公告。原「不需要」的依据(无感)已不成立 |

### 6.1 客户端 401 重试的作用(2026-08-11 修订)

客户端已实现 401 → `POST /auth/tokens` → 重试(用户确认)。**但选项 B 下它的作用变了**:

| | 原「不强制登出」设想 | **实际(选项 B)** |
|---|---|---|
| 401 重试的作用 | 静默换新 token,用户无感 | refresh 必然失败(已清空)→ 引导用户到登录页 |
| 是否仍需修 C3 | 是 | **仍是必需** —— 见下 |

**为何清空了 refresh token 还要修 C3(401 链路)**:

```
未修 C3:  旧 token → 异常逃逸 → 容器错误页 → 客户端不知这是认证失效 → 不跳登录页
已修 C3:  旧 token → 401 AUT00210 → 客户端触发 refresh → refresh 明确失败 → 跳登录页 ✓
```

**为何还要修 C2(refresh 端点可达)**:理由已从「为了无感」变为「**为了避免重试循环**」。若 refresh 端点本身返回 401(实测当前如此),客户端可能陷入 `401 → refresh → 401 → refresh` 死循环。端点必须到达 controller 并返回明确的业务错误,客户端才能判定「该重新登录了」。

> 一个副作用:原风险表担心的「refresh 请求尖峰」在选项 B 下**依然存在**(所有客户端仍会各试一次 refresh),但因每次都快速失败(仅一次 Redis 查询),压力小于成功换取新 token 的场景。仍建议低峰期执行,且 FR-020 的限流是必要防护。

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 新 keystore 生成错误(别名/口令不匹配) | 应用启动失败,服务中断 | 先在 test profile 用新 keystore 跑通全部认证测试,再上生产 |
| `KEYSTORE_LOCATION` 环境变量未在生产设置 | 应用拒绝启动(FR-007 的预期行为) | 部署清单明列;fail-fast 的错误信息须指明变量名 |
| `/etc/eve-helper/` 权限配置错误 | keystore 被非授权用户读取,轮换失去意义 | 目录 700、文件 600、属主为服务账号;部署后 `stat` 核验并记录 |
| 旧 keystore 仍留在服务器上被误用 | 轮换失效 | 轮换后重命名/删除旧文件;FR-013 文档记录 |
| 认为「删了文件就安全了」 | 虚假的安全感 | 本 spec 第 1.3 节已明确论证 |
| 轮换瞬间 refresh 请求突发 | 全部在线客户端同时收到 401 并各试一次 refresh,形成尖峰 | 低峰期执行(FR-011)+ FR-020 的全局速率限流。选项 B 下每次 refresh 快速失败(仅一次 Redis 查询),压力小于成功换取 token 的场景。⚠️ 全局限流阈值须高于合法尖峰,否则误伤 |
| **审计不彻底导致攻击者账号存活** | 选项 B 暂不强制改密,若攻击者曾自注册并提权,清空 refresh token 与轮换密钥都不影响他登录 | **US3 的六表审计是唯一防线,不是可选收尾**。发现异常即升级为入侵响应(强制改密 + 吊销全部会话) |
| 多实例滚动重启期间新旧密钥并存 | 随机认证失败;`TokenService:181` 在生成新 token 前删除旧 refresh token,重试循环会消耗掉它 | FR-022:明确拓扑,多实例须停机窗口或全量同时重启,**禁止滚动重启** |

---

## 8. 与 006 遗留待办的关系

006 的 L-10(启动时安全基线 fail-fast 断言)与本 feature 的 FR-008 是同一处代码。建议合并实现:一个 `ApplicationRunner` 同时校验
`log-impl` / `logging.level.web` / `access-token-endpoint.enabled` / `KEYSTORE_LOCATION` 四项。

### 8.1 校验语义:正向白名单(T038 修正 HIGH-3)

**初版实现是「黑名单」语义**,与 fail-closed 的名义不符:

| 键 | 初版判据 | 键缺失时的实际行为 |
|----|----------|-------------------|
| `access-token-endpoint.enabled` | `Boolean.parseBoolean(v)` 为 true 则拒 | `parseBoolean(null)` = false → **恒放行** |
| `log-impl` | 等于 `StdOutImpl` 则拒 | null ≠ StdOutImpl → **恒放行** |
| `logging.level.web` | 是 debug/trace 则拒 | null → **恒放行**,且 `root: debug` 完全绕过 |

三项都是「没发现危险值」而非「确认了安全值」,却统一打印「基线校验通过(4/4)」——**这是虚假保证**。

**T038 起改为正向白名单**:键必须显式存在且取值属于安全集合,缺失即拒启。

**日志基线扩展**(初版只查 `logging.level.web`,存在三条真实绕过路径):

| 绕过路径 | 泄露物 | 初版是否可见 |
|----------|--------|-------------|
| `logging.level.root: debug` | 所有未显式配置的 logger 一并降级 | ❌ |
| `logging.level.reactor.netty: debug` | ESI 客户端请求头 → `Authorization: Bearer <ESI token>` | ❌ |
| `logging.level.xyz.foolcat...mapper: debug` | 配合 Slf4jImpl 打 SQL 绑定参数 → `refresh_token` 明文 | ❌ |

第三条尤其要紧:`log-impl` 基线防的就是 refresh_token 入日志,而调 mapper 包日志级别可从另一侧达到**完全相同**的效果。故日志基线改为**扫描全部 `logging.level.*` 键**,任一为 debug/trace 即拒,而非枚举固定几个 logger 名(枚举法无法穷尽)。

> **危害等级说明**:入库的 `application.yml` 已给出 `enabled: ${ACCESS_TOKEN_ENDPOINT_ENABLED:false}`、`web: info`、`log-impl: Slf4jImpl`,故「键缺失」在真实启动中不会发生。HIGH-3 的实际危害是**校验器的保证不可信**(它宣称的强度远高于实际强度),而非当前存在敞口。日志绕过路径则是真实可用的。

#### 8.1.1 连带修正:删除 `application.yml` 的脚手架残留日志配置

`application.yml:106` 存在 `com.bolingcavalry.druidtwosource.mapper: debug`。**该包在本项目中不存在**(全树 grep 零命中;本项目包名为 `xyz.foolcat.eve.evehelper`),是脚手架抄写残留,当前不生效。

**它必须随 T038 一并删除**,两个理由:

1. **否则新校验会让生产直接拒启** —— 「扫描全部 `logging.level.*`,任一 debug/trace 即拒」会被这行绊倒。这是必然发生,不是概率风险。
2. **它是一个上了膛的模板** —— 该行的意图就是「给 mapper 包开 debug」。若有人把包名改对成 `xyz.foolcat...mapper`,`refresh_token` 会明文进日志,而这正是 `log-impl` 基线要堵的洞。留着它等于留一份可直接照抄的泄露配方。

故删除属于修复的必要组成,不是范围外夹带。

---

**下一步**:

1. ~~重写 `plan.md`~~ → **写 v3**:须补第二轮评审的两项 CRITICAL 设计 —— 白名单共享匹配组件(CRITICAL-1)、按 userId 只计失败的限流(CRITICAL-2);并更新序 0 为「已完成」
2. 重新执行 `ecc:security-reviewer` 设计评审(门禁:涉及认证与加密)
3. 评审通过后 `/speckit-tasks`

**实现顺序**(已按第二轮评审 HIGH-4 重排;序 0 已完成):

| 序 | 内容 | 状态 / 为何这个位置 |
|----|------|------------|
| **0** | 测试 keystore 上 classpath + 重测基线 | ✅ **已完成(2026-08-11 21:47)**。`test-only.jks` 就位、`.gitignore` 反向规则已加、`ApplicationContext` 加载失败数 0、新基线 `509/F4/E219` |
| 1 | **白名单共享匹配组件**(`WhiteUrlMatcher`) | **CRITICAL-1 的前提**。不先做,FR-016 与 FR-020 会互相拆台(详见 plan §3.2) |
| 2 | **FR-016** 401 链路(filter + `ResponseUtils`) | 依赖序 1。SC-010 的起点,也是其余验收场景可观测的前提 |
| 3 | **FR-020** refresh 端点可达 + 按 userId 限流 | 依赖序 1、2 |
| 4 | **FR-023** 重写 `KeyStoreKeyFactory` | 独立,可与 1~3 并行;是 FR-007 错误信息质量的基础 |
| 5 | **FR-005~008** 加载机制 + 启动基线校验(fail-closed) | 依赖序 4 |
| 6 | **FR-012/013/021** 清理、文档、审计清单 | 收尾。FR-012(`git rm eve-jwt.jks`)**须在 aliw profile 切到文件系统路径之后**,否则生产启动即失败 |
| 7 | 生产轮换(运维) | 全部代码就绪后由用户执行;**SC-001/SC-006/SC-014 的验证须人工完成**(FR-002 口令不经 AI) |

**状态**: 规格修订完成(选项 B)。`plan.md` 待重写。
