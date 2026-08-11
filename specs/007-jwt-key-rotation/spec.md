# 规格：JWT 签名密钥轮换

**Feature**: `007-jwt-key-rotation`
**创建日期**: 2026-08-11
**状态**: 边界已确认(2026-08-11,Q1~Q5);**6.1 客户端 refresh 重试逻辑未决,暂不进入 ③ 计划**
**来源**: 006 提交前安全检查发现 —— `src/main/resources/eve-jwt.jks` 自 `5d139d8「增加token认证」` 起已在版本控制中

---

## 1. 背景与问题陈述

### 1.1 事实认定(已核实,勿重复质疑)

| 事实 | 证据 |
|------|------|
| `eve-jwt.jks`(2172 字节,含 RSA 私钥)已被 git 跟踪 | `git ls-files '*.jks'` 返回该路径;`git log --follow` 显示自 `5d139d8` 入库 |
| `.gitignore` 此前无 `*.jks` 规则 | 已于 `84c099a` 补上,但**已跟踪文件不受 .gitignore 约束**,故其状态未变 |
| keystore 口令**未**泄露 | `application.yml:114-118` 全部使用 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 环境变量;实测 `keytool -storepass changeit` 报 "Keystore was tampered with, or password was incorrect" |
| 私钥用于签发系统 JWT | `TokenService.java:98` `new RSASSASigner(keyPair.getPrivate())`,算法 RS256(`:95`) |
| 公钥用于验签 | `JwtAuthorizationTokenFilter.java:67-68` `new RSASSAVerifier((RSAPublicKey) keyPair.getPublic())` |
| **ESI 授权不受影响** | `EsiApiService.java:414` 解析的是 CCP 签发的 token,只读 claim **不验签**,与本 keystore 无关 |
| **refresh token 不含签名** | `TokenService.java:111-116` refresh token 是随机 UUID,存 Redis `refresh_token:{uuid} -> userId`。轮换密钥**不使其失效** |
| refresh 端点存在 | `POST /auth/tokens`(`AuthController.java:51-52` → `AuthApplicationService:134`) |
| access token TTL = 900s | `JwtTokenProperties:30` `accessTokenExpirationTime = 900` |
| refresh token TTL = 604800s(7 天) | `JwtTokenProperties:35` |

### 1.2 风险评估

**私钥一旦入库即应视为已泄露**:凡 clone 过本仓库的人(含任何 fork、CI 缓存、备份)本地都有一份。删除文件或改写历史都无法收回已分发的副本。

**攻击者若同时取得私钥与口令**,可伪造任意用户的 access token —— 包括 ROOT 角色,从而完全绕过 RBAC。`JwtAuthorizationTokenFilter` 只验签名、过期、黑名单三项,签名有效即信任 claim 中的 `userId` 与 `authorities`(`:90-96`)。

**当前缓解**:口令未入库。攻击者需另外取得 `KEYSTORE_PASSWORD` 与 `KEY_PASSWORD` 才能使用私钥。JKS 使用 PBE 保护私钥条目,离线暴破成本取决于口令强度 —— **口令强度未知,是本方案必须确认的前置事项**。

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
- **FR-003** keystore 别名可沿用 `eve-jwt`(`SecurityProperties:38` 默认值),或改用新别名并同步 `KEYSTORE_ALIAS`
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
- **FR-016** 401 响应须携带可判别错误码 `AUT00210`(`TOKEN_ACCESS_EXPIRED`),使客户端能与「权限不足」区分并据此触发 refresh。该约定须写入 API 文档(见 6.1)

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
- **SC-010** 401 响应体错误码为 `AUT00210`,与权限不足的 `AUT00301` 可区分(FR-016)

---

## 6. 边界决策(已由用户确认,2026-08-11)

| # | 问题 | **决策** | 说明 |
|---|------|---------|------|
| Q1 | 是否强制全体用户重新登录? | **不强制** | 保留 `refresh_token:*`,靠 refresh 流程过渡。**注意其保证范围**,见 6.1 |
| Q2 | 现有 keystore 口令强度 | **由用户单独掌握** | 口令不进入本仓库、不进入任何文档、不由 AI 评估。强度评估与「是否需要紧急轮换」的判断由用户负责 |
| Q3 | keystore 新位置 | **服务器固定路径** | `/etc/eve-helper/eve-jwt.jks`,文件权限 600,属主为运行应用的服务账号 |
| Q4 | 合并实现 006 的 L-10 fail-fast | **是** | 一个 `ApplicationRunner` 同时校验 4 项配置基线,见第 8 节 |
| Q5 | 是否通知用户 | **随 Q1 定** | 选 (a) 无感故不主动公告;但须先解决 6.1 的未决风险 |

### 6.1 未决风险:「不强制登出」≠「用户无感」(必须在 ③ 计划前澄清)

Q1 选「不强制」保证的是 **`refresh_token:*` 键不被清空**。它**不**保证用户无感 —— 实际体验取决于客户端行为:

| 客户端是否实现 401 → `POST /auth/tokens` → 重试 | 用户实际体验 |
|---|---|
| **是** | 真正无感。一次 401 被静默吞掉,自动换新 token 继续 |
| **否** | 看到 401 被弹回登录页 —— **效果等同强制登出**,只是 refresh token 还留在 Redis 里(直到 7 天 TTL 自然过期) |

**本仓库不含前端代码**(仅 `src/main` 的 Java 后端,无 `package.json`),故**无法在此核实该逻辑是否存在**。

**行动项(阻断性)**:进入 ③ 计划前必须确认客户端行为。三种走向:
1. 客户端已实现重试 → 按原方案,真正无感
2. 客户端未实现 → 要么先补客户端重试逻辑,要么改按 Q1 (b) 强制登出并提前公告(不要假装无感)
3. 无法确认 → 按最坏情况处理:选低峰期执行 + 提前公告

> 后端可协助的部分:确认 401 响应体携带可判别的错误码 —— 现有 `ResultCode.TOKEN_ACCESS_EXPIRED = "AUT00210"`(`ResultCode.java:43`)与 `ACCESS_UNAUTHORIZED = "AUT00301"`(`:46`)。客户端应据 `AUT00210` 触发 refresh,而非仅凭 HTTP 401(后者也可能是权限不足)。此约定须写入 API 文档。

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| **客户端无 401→refresh 重试逻辑** | Q1 的「无感」失效,用户被弹回登录页 | **阻断性,见 6.1**。进入 ③ 计划前必须确认;无法确认则按最坏情况:低峰期 + 提前公告 |
| 新 keystore 生成错误(别名/口令不匹配) | 应用启动失败,服务中断 | 先在 test profile 用新 keystore 跑通全部认证测试,再上生产 |
| `KEYSTORE_LOCATION` 环境变量未在生产设置 | 应用拒绝启动(FR-007 的预期行为) | 部署清单明列;fail-fast 的错误信息须指明变量名 |
| `/etc/eve-helper/` 权限配置错误 | keystore 被非授权用户读取,轮换失去意义 | 目录 700、文件 600、属主为服务账号;部署后 `stat` 核验并记录 |
| 旧 keystore 仍留在服务器上被误用 | 轮换失效 | 轮换后重命名/删除旧文件;FR-013 文档记录 |
| 认为「删了文件就安全了」 | 虚假的安全感 | 本 spec 第 1.3 节已明确论证 |
| 认为「不强制登出就等于用户无感」 | 上线后被投诉「怎么都被登出了」 | 6.1 已明确区分两者;决策记录在案 |

---

## 8. 与 006 遗留待办的关系

006 的 L-10(启动时安全基线 fail-fast 断言)与本 feature 的 FR-008 是同一处代码。建议合并实现:一个 `ApplicationRunner` 同时校验
`log-impl` / `logging.level.web` / `access-token-endpoint.enabled` / `KEYSTORE_LOCATION` 四项。

---

**下一步**:

1. **先解决 6.1 的阻断项** —— 确认客户端是否实现 401(`AUT00210`)→ `POST /auth/tokens` → 重试。本仓库无前端代码,无法自行核实
2. 执行 `/speckit-plan`,计划阶段调用 `ecc:security-reviewer` 做设计评审(涉及认证与加密,门禁强制要求)
3. 计划须覆盖:`KeyPairConfig` 改造的测试策略(如何在不引入真实 keystore 的前提下测 fail-fast)、与 006 L-10 合并实现的边界

**状态**: 边界已确认(Q1~Q5),但 6.1 未决,**尚不可进入 ③**。
