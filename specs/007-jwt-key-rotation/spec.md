# 规格：JWT 签名密钥轮换

**Feature**: `007-jwt-key-rotation`
**创建日期**: 2026-08-11
**状态**: **BLOCK** — 阶段③安全评审驳回(2026-08-11),3 项 CRITICAL 使方案三个核心前提均不成立(见 1.4)。**Q1「不强制登出」的决策依据已被推翻,需用户重新决策**
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

调研 `KeyPairConfig` 的测试依赖时发现:`application-test.yml:158-163` 与 **`application-aliw.yml:157-162`(生产环境之一)** 均以**明文写死** keystore 口令与密钥口令,且为**同一个 6 字符弱口令**(纯小写字母+数字,字典词+数字后缀形态。**实际值不在本文档中记录**)。

由此:

| 原假设 | 修正后 |
|--------|--------|
| 「私钥已泄露但口令未泄露,攻击者无法使用」 | **口令强度不足以抵抗离线暴破。** JKS 私钥条目用 PBE 保护,对 6 字符字典词形态口令,单机破解在**秒级到分钟级** |
| 「轮换优先级:高但不紧急」 | **紧急。** 拿到 git 中的 `eve-jwt.jks` 即可在极短时间内取出私钥,进而伪造任意用户(含 ROOT)的 access token |
| 「只有 test 环境用弱口令」 | `application-aliw.yml` 是生产 profile,同样明文同样弱口令 |

**这不改变方案设计,但改变时间表**:轮换应尽快执行,而非排入常规迭代。

**追加要求**:
- **FR-017** 新口令必须为高强度随机串(建议 ≥32 字符随机生成),不得为字典词或其变形
- **FR-018** 所有 profile 的 `security.keystore.password` / `key-password` 必须改为 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 环境变量引用,禁止明文 —— 现仅 `application.yml`(入库那份)做到了,`application-test.yml` 与 `application-aliw.yml` 未做到
- **FR-019** test profile 亦须改为环境变量,并使用**与生产不同**的 keystore 与口令(测试 keystore 可单独生成、可入库,因其不保护任何真实凭证 —— 但须显式声明「仅测试用」)

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

### US1 — 运维人员在不中断服务的前提下完成密钥轮换(P1)

作为运维人员,我需要把 JWT 签名密钥换成新生成的密钥对,且**已登录用户不被强制重新登录**。

**为何可行**:refresh token 是 Redis 中的随机 UUID、不含签名。轮换后旧 access token 验签失败(401),但客户端凭 refresh token 调 `POST /auth/tokens` 即可换到新密钥签发的 access token。

**验收场景**:
1. **Given** 用户持有旧密钥签发的 access token(未过期)与 refresh token,**When** 完成轮换并重启应用,**Then** 旧 access token 返回 401,而 `POST /auth/tokens` 携带 refresh token 返回新的 token 对
2. **Given** 轮换完成,**When** 用户用新 access token 访问受保护端点,**Then** 正常返回 200
3. **Given** 轮换完成,**When** 检查 Redis,**Then** `refresh_token:*` 键未被清空(除非选择 US3 的强制登出策略)

### US2 — keystore 不再随源码分发(P1)

作为部署者,我需要 keystore 从**外部路径**加载,而非打进 jar 的 classpath,使其不可能再被误提交。

**当前实现的问题**:`KeyPairConfig.java:44` 用 `new ClassPathResource(location)` —— keystore 必须位于 `src/main/resources`,即**必然进入源码树与构建产物**。

**验收场景**:
1. **Given** keystore 置于 `src/main/resources` 之外的路径,**When** 通过 `KEYSTORE_LOCATION` 指定文件系统绝对路径,**Then** 应用正常加载并签发 token
2. **Given** `KEYSTORE_LOCATION` 指向不存在的路径,**When** 应用启动,**Then** **拒绝启动**并给出明确错误(不得退化为 classpath 回退,那会静默用回旧 keystore)
3. **Given** 构建产物,**When** 检查 jar 内容,**Then** 不含任何 `.jks` 文件

### US3 — 保留 refresh token,不强制全体登出(P2,决策已定)

**决策(Q1)**:**不强制登出**。`refresh_token:*` 保留,旧 access token 靠验签失败自然作废(≤900 秒内全部失效)。

**接受的残余风险**:若攻击者已窃取某用户的 refresh token,本次轮换不影响其继续换取新 access token。之所以接受:refresh token 存于 Redis 且从不出现在 git 中,与本次「私钥入库」事件无因果关系;若日后发现 refresh token 泄露,再单独执行清空。

**验收场景**:
1. **Given** 轮换完成,**When** 检查 Redis `refresh_token:*` 键数量,**Then** 与轮换前一致(FR-011 记录前后数字)
2. **Given** 用户持有轮换前签发的 refresh token,**When** 调 `POST /auth/tokens`,**Then** 返回新密钥签发的 token 对
3. **Given** 需要紧急清除全部会话,**When** 执行 `SCAN MATCH refresh_token:* + DEL`,**Then** 全部 refresh token 失效 —— 保留为**应急手段**,本次轮换不执行

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

- **FR-009** 轮换过程中**不得**同时存在两套密钥签发 token(不实现双密钥并行,避免复杂度;代价是重启瞬间的旧 token 立即失效)
- **FR-010** 轮换后必须验证:旧 access token 返回 401、refresh 换取成功、新 access token 可访问受保护端点
- **FR-011** 轮换须在低峰期执行,并在执行前后记录 `refresh_token:*` 键数量作为影响面证据
- **FR-015** **不清空 `refresh_token:*`**(Q1 决策)。轮换脚本/步骤中禁止出现针对该键前缀的 DEL/FLUSH 操作
- **FR-016**(建议,非交付前提)401 响应携带可判别错误码 `AUT00210`(`TOKEN_ACCESS_EXPIRED`,`ResultCode.java:43`),使客户端能与权限不足的 `AUT00301`(`:46`)区分。客户端 401 重试已确认存在,本次轮换不依赖此项;列为后续改进 —— 裸 401 判据会使权限不足场景触发一次无意义 refresh(既有行为,非本轮换引入)

### 清理与文档

- **FR-012** 从工作区删除 `src/main/resources/eve-jwt.jks`(`git rm`)
- **FR-013** 在 `docs/DEPLOYMENT.md` 记录:旧私钥已泄露、轮换日期、旧密钥不得再用于任何环境
- **FR-014** **不改写 git 历史** —— 遵守「禁止强推共享分支」。历史中的旧私钥在密钥轮换后失去价值(不再验签任何 token),故无需改写

---

## 4. 明确排除(YAGNI)

| 项 | 排除理由 |
|----|---------|
| 双密钥并行(kid 路由) | 需给 JWT 加 `kid` header、维护 keyId→key 映射、改 `JwtAuthorizationTokenFilter` 支持多验签者。为一次性轮换引入长期复杂度不值得。代价仅为「重启瞬间旧 access token 失效」,而 refresh 流程已能兜住 |
| 改写 git 历史 | 与项目规则冲突(禁止强推);且已分发副本收不回;密钥轮换后旧私钥失去价值 |
| 迁移到 JWKS / 外部 KMS | 属架构升级,应独立评估。本 feature 只解决「已泄露密钥必须轮换」 |
| 改用 EdDSA / ES256 | 技术栈冻结(宪法第四条);且 RS256 无安全缺陷 |
| 自动化定期轮换 | 先把一次性轮换做对。周期轮换需要双密钥并行才能无感,见上 |

---

## 5. 成功标准

- **SC-001** 新密钥对签发的 token 可正常通过验签,受保护端点返回 200
- **SC-002** 旧密钥签发的 access token 一律返回 401
- **SC-003** 构建产物(jar)内不含任何 `.jks` 文件
- **SC-004** `KEYSTORE_LOCATION` 缺失或指向不可读文件时应用拒绝启动,错误信息明确指出缺哪个环境变量
- **SC-005** 生产 profile 下 keystore 位于 classpath 时拒绝启动
- **SC-006** 轮换后 `POST /auth/tokens` 用旧 refresh token 可换到新 token 对(US1 无感前提)
- **SC-007** `docs/DEPLOYMENT.md` 含轮换步骤与「旧密钥已泄露」的明确记录,且不含任何真实口令
- **SC-008** 轮换前后 Redis `refresh_token:*` 键数量一致(FR-015:未清空)
- **SC-009** keystore 文件权限为 600、目录 700、属主为服务账号(`stat` 输出留证)
- **SC-010** 401 响应体错误码为 `AUT00210`,与权限不足的 `AUT00301` 可区分(FR-016,建议项)
- **SC-011** 全部 profile 的 `security.keystore.password` / `key-password` 均为 `${...}` 环境变量引用,`grep` 全仓无明文口令(FR-018)
- **SC-012** 新 keystore 口令为高强度随机串,与旧口令无关联(FR-017;由用户自行核验,不留证于文档)

---

## 6. 边界决策(已由用户确认,2026-08-11)

| # | 问题 | **决策** | 说明 |
|---|------|---------|------|
| Q1 | 是否强制全体用户重新登录? | **不强制** | 保留 `refresh_token:*`,靠 refresh 流程过渡。客户端 401 重试已确认存在,故真正无感(见 6.1) |
| Q2 | 现有 keystore 口令强度 | **由用户单独掌握** | 口令不进入本仓库、不进入任何文档、不由 AI 评估。**但编写规格时在 `application-test.yml` 与 `application-aliw.yml` 中发现明文弱口令 —— 见 1.2.1,该发现使轮换升为紧急** |
| Q3 | keystore 新位置 | **服务器固定路径** | `/etc/eve-helper/eve-jwt.jks`,文件权限 600,属主为运行应用的服务账号 |
| Q4 | 合并实现 006 的 L-10 fail-fast | **是** | 一个 `ApplicationRunner` 同时校验 4 项配置基线,见第 8 节 |
| Q5 | 是否通知用户 | **不需要** | Q1 无感且客户端已有 401 重试;仅需低峰期执行以缓解 refresh 尖峰(FR-011) |

### 6.1 客户端 401 重试:已确认存在(2026-08-11,用户确认)

Q1 选「不强制」保证的是 **`refresh_token:*` 键不被清空**;而「用户是否无感」另取决于客户端行为:

| 客户端是否实现 401 → `POST /auth/tokens` → 重试 | 用户实际体验 |
|---|---|
| **是 ← 本项目属此列(用户已确认)** | 真正无感。一次 401 被静默吞掉,自动换新 token 继续 |
| 否 | 看到 401 被弹回登录页 —— 效果等同强制登出,只是 refresh token 还留在 Redis 里 |

**结论:Q1 的「无感」前提成立,US1 可按原方案执行,无需提前公告(Q5)。**

**残余细节(不阻断,计划阶段确认即可)**:客户端触发 refresh 的判据是「裸 HTTP 401」还是「错误码 `AUT00210`」,本仓库无前端代码故未核实。

- 对**本次轮换**而言两者均可行 —— 轮换导致的验签失败必然是 401,裸 401 判据同样会触发重试
- 但裸 401 判据有既有隐患:`AUT00301`(访问未授权/权限不足)也是 401,会触发一次无意义的 refresh,刷新成功后重试原请求仍被拒。属既有行为,非本轮换引入
- 故 **FR-016 降级为「建议」**:若客户端当前用裸 401,建议后续改为按 `AUT00210` 判别,但不作为本 feature 的交付前提

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 新 keystore 生成错误(别名/口令不匹配) | 应用启动失败,服务中断 | 先在 test profile 用新 keystore 跑通全部认证测试,再上生产 |
| `KEYSTORE_LOCATION` 环境变量未在生产设置 | 应用拒绝启动(FR-007 的预期行为) | 部署清单明列;fail-fast 的错误信息须指明变量名 |
| `/etc/eve-helper/` 权限配置错误 | keystore 被非授权用户读取,轮换失去意义 | 目录 700、文件 600、属主为服务账号;部署后 `stat` 核验并记录 |
| 旧 keystore 仍留在服务器上被误用 | 轮换失效 | 轮换后重命名/删除旧文件;FR-013 文档记录 |
| 认为「删了文件就安全了」 | 虚假的安全感 | 本 spec 第 1.3 节已明确论证 |
| 轮换瞬间 refresh 请求突发 | 全部在线客户端在同一时刻收到 401 并同时调 `POST /auth/tokens`,形成尖峰 | 低峰期执行(FR-011);`POST /auth/tokens` 无限流,若在线量大需评估。此为 Q1「无感」方案的固有代价 —— 强制登出方案反而没有此尖峰 |

---

## 8. 与 006 遗留待办的关系

006 的 L-10(启动时安全基线 fail-fast 断言)与本 feature 的 FR-008 是同一处代码。建议合并实现:一个 `ApplicationRunner` 同时校验
`log-impl` / `logging.level.web` / `access-token-endpoint.enabled` / `KEYSTORE_LOCATION` 四项。

---

**下一步**:

1. 执行 `/speckit-plan`,计划阶段调用 `ecc:security-reviewer` 做设计评审(涉及认证与加密,门禁强制要求)
2. 计划须覆盖:
   - `KeyPairConfig` 改造的测试策略 —— 如何在不引入真实 keystore 的前提下测 FR-007 的 fail-fast
   - 与 006 L-10 合并实现的边界(一个 `ApplicationRunner` 校验 4 项:`log-impl`、`logging.level.web`、`access-token-endpoint.enabled`、`KEYSTORE_LOCATION`)
   - `application-prod.yml.example`(006 新增)须同步补 `KEYSTORE_LOCATION` 的正确示例与注释
   - 现有认证测试是否依赖 classpath keystore;若依赖,`KEYSTORE_LOCATION` 的 `classpath:` 分支(FR-006)即为其兼容路径
3. 轮换执行属运维动作,须先在 test profile 用新 keystore 跑通全部认证测试

**状态**: 规格完成,阻断项已解除,可进入 ③。
