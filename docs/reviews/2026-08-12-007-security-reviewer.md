# 评审记录: 007 JWT 密钥轮换应急响应 — 实现阶段安全评审

- **日期**: 2026-08-12
- **评审者**: ecc:security-reviewer
- **关联 feature**: `specs/007-jwt-key-rotation`
- **评审范围**: `git diff 67333f7...HEAD -- src/main/java src/main/resources src/test docs/DEPLOYMENT.md`(8 个提交),并对照工作区实际 profile 文件与 git 跟踪的密钥材料做实测
- **结论**: **BLOCK**

> ⚠️ **口令处理声明**:涉及 gitignore 私有配置中的字面量口令一律以 `<REDACTED>` 表示,不回显原文。

代码实现本身质量高:5 个失败出口收敛、白名单单一事实来源、fail-closed profile 判定、RSA 位数门禁、TOCTOU 消除均正确落地,契约测试覆盖到位。**BLOCK 的原因不在新代码的逻辑,而在轮换的实际生效状态与限流机制的 DoS 放大**:生产 profile 目前仍指向那把已泄露的私钥,而 `SecurityBaselineValidator` 的四项校验**按设计无法发现这一点**。

---

## 发现

| 级别 | 位置 | 问题 | 建议处置 |
|------|------|------|----------|
| **CRITICAL-1** | `src/main/resources/application-aliw.yml:158-162`(生产 profile,gitignore 私有文件) | **生产 profile 仍加载已泄露的旧私钥**。`security.keystore.location` 指向 `D:\IdeaProjects\eve-jwt.jks`,该文件与 81e5dd8 从 git 索引移除的 `src/main/resources/eve-jwt.jks`(`git show 67333f7:...`)**逐字节相同**(SHA-256 一致;md5 均为 `1be3633d52ebcaa3cd9fd18e1045aa72`)。即:`git rm` 只把泄露密钥移出了索引,**签名主体仍是同一把密钥**。四项基线校验全部通过 —— `looksLikeFilesystemPath` 只校验路径**形状**(含 `\` 即合规),不校验密钥**身份**;`KeyStoreKeyFactory` 只校验 modulus ≥2048。FR-012/SC-002/SC-014 在部署态**未达成** | **待办(合并前置)**:执行 `DEPLOYMENT.md` 步骤 1~6 完成实密钥轮换,`location` 切到新密钥并填写「轮换记录」表;旧密钥副本安全销毁。代码侧同步加身份门禁(MEDIUM-1) |
| **CRITICAL-2** | `src/main/resources/application-aliw.yml:160,162` | 生产 keystore 口令与 key 口令是**字面量明文**(`<REDACTED>`,未用 `${KEYSTORE_PASSWORD}` / `${KEY_PASSWORD}` 占位符),同文件内 DB 口令亦为字面量。007 已把 `application-prod.yml.example:136-141` 改成环境变量占位符,但**真正在跑的生产 profile 没跟上** —— 加固落在了模板上,没落在生效配置上。该文件被 `.gitignore` 覆盖故未入库(非泄露),但口令随文件系统备份/IDE 同步扩散,且与 CRITICAL-1 叠加 = 泄露私钥 + 明文口令同处一台开发机。**SC-011 实际不成立** | **待办(合并前置)**:生产 profile 三项口令改为 `${...}` 占位符,值只走部署环境变量/秘密渠道;轮换时一并更换 |
| **HIGH-1** | `domain/service/security/RefreshRateLimiterService.java:118-125`(`Thread.sleep` 在 :121)+ 调用链 `AuthApplicationService.java:116,128` | **限流的「延迟整形」是 DoS 放大器,不是缓解措施**。`applyFixedDelay()` 在 **Tomcat 工作线程**上同步 `Thread.sleep(100~300ms)`,而 `POST /auth/tokens` 已加白 = 未认证可达。攻击路径:① 60s 内发 1001 个非法 refresh 顶过 `L2_THRESHOLD=1000`(:55);② 此后每个非法 refresh **占住一个工作线程 100~300ms**;③ 未覆盖 `server.tomcat.threads.max`(默认 200),按均值 200ms 计,约 **1000 req/s 即可耗尽全部 200 个工作线程**,拖垮**全站所有端点**。对比未加延迟时每请求仅一次 Redis GET+INCR(~1ms),需 ~200,000 req/s 才有同等占用 —— 该「防护」把攻击者杠杆**放大约两个数量级**。<br>更糟的自伤路径:`DEPLOYMENT.md` 步骤 7 清空 `refresh_token:*` 后全体在线客户端同时 refresh 失败,极易在 60s 内自然突破 1000 次阈值 → **恰在轮换窗口自我触发线程占用**,与 SC-006 的设计意图相反 | **待办(合并前置)**:去掉请求线程上的 `Thread.sleep`。改为 ① 仅保留计数器 + 告警;或 ② Servlet 异步延迟提交;或 ③ 超阈值直接返回 429(零线程占用)。同时显式设定 `server.tomcat.threads.max` 与 `accept-count` |
| **HIGH-2** | `application-prod.yml:118-120`、`application-ali.yml:118-120`、`application-aliw.yml:142-144` | **`POST:/auth/tokens` 加白在三个非 test profile 全部静默失效**。`whiteUrlList` 是 List 属性,profile 文件整体覆盖 `application.yml:123-128`;这三个文件只有 `POST:/user`。后果链:客户端不带 Authorization 调 refresh → 过滤器走「非 JWT 不处理」分支(`JwtAuthorizationTokenFilter:67-72`)→ `RbacAuthorizationManager.check` → 白名单未命中 → 匿名 `isAuthenticated()==false` → **refresh 端点在生产恒不可达**。轮换 + 清空 refresh token 后,这是唯一的自助恢复通道;它一坏,压力之下极可能被「放宽白名单/关掉 RBAC」这类高危手段绕过。开发者知道这个坑(`application.yml:124-127` 与 `DEPLOYMENT.md:894` 都写了警告),但只同步了 `application-test.yml`。**SC-016 实际不成立** | **待办(合并前置)**:三个 profile 补 `POST:/auth/tokens`;更稳的是把白名单改为「基础集(代码常量)∪ 配置追加集」,让关键条目不可被 profile 覆盖丢失 |
| MEDIUM-1 | `SecurityBaselineValidator.java:110-134` | fail-closed 校验只覆盖路径**语法**,不覆盖密钥**身份** —— 这正是 CRITICAL-1 穿过全部四项校验的原因。旁证:`test-only.jks` 随 jar 分发(有意决策),而 `KEYSTORE_LOCATION=/app/BOOT-INF/classes/test-only.jks` 含 `/` → 判为合规 → 生产用公开口令的测试密钥签发 JWT,校验器毫无反应。SC-005 目前实际只靠人工指纹核对兜底 | **待办**:启动期读出实际加载的 `RSAPublicKey`,拒绝命中「已知禁用密钥指纹表」(至少含 `test-only.jks` 与旧 `eve-jwt.jks` 公钥的 SHA-256),并拒绝 alias == `test-only`。指纹表入库无风险(公钥不是秘密) |
| MEDIUM-2 | `SecurityBaselineValidator.java:39`(`implements ApplicationRunner`) | **fail-closed 的时机晚于开始收流量**。Web 容器在 `finishRefresh()` 启动,`ApplicationRunner.run` 在 `callRunners()` 才执行 —— 校验抛异常之前 Tomcat 已在监听并可服务请求。窗口虽短,但在「误配成 `classpath:test-only.jks`」场景里,该窗口内签发的 JWT 是用公开密钥签的。<br>**profile 判定逻辑本身正确**(已逐条核对:`getActiveProfiles()` 空数组 → `length != 1` → 按生产;`spring.profiles.default=test` 走 `getDefaultProfiles()` 不影响 → 按生产;`@ActiveProfiles({"test","prod"})` → length 2 → 按生产。**无生产误判为 test 的路径**) | **待办**:改为容器启动前触发 —— `EnvironmentPostProcessor`、`ApplicationContextInitializer`,或把 `validate()` 挪到 `@PostConstruct`/`InitializingBean`(与 `KeyPairConfig` 的 fail-fast 同阶段) |
| MEDIUM-3 | `SecurityBaselineValidator.java:42,92-99` | `logging.level.web` 单键校验可被等价配置绕过。可复现:生产 profile 写 `logging.level.root: debug`(或 `logging.level.org.springframework.web: debug`)→ 校验器读 `logging.level.web` 得 `info` 或 `null` → **通过** → 但 Spring Web/Security 的 DEBUG 日志照样落盘,`Authorization` 头与请求体随之泄露 | **待办**:同时断言 `logging.level.root`、`logging.level.org.springframework.web`、`logging.level.org.springframework.security` 均非 debug/trace |
| MEDIUM-4 | `docs/DEPLOYMENT.md:939-941` 与 `:1003-1005` | 六表审计的**能力边界声明不完整**。SC-013 强制措辞已正确落地(:1003),且明确禁止「确认未被入侵」(:1005)—— 这部分合格。但六张表的「判断方法」都依赖 `gmt_create`/`gmt_modified` 与时间锚点 `5d139d8`,而**攻击者若已能写库,这两列同样可被伪造**。因此「未发现新增/篡改痕迹」对**精心操作的写入型入侵**也是可能的假阴性,而现有措辞只对**读取型**外泄声明了失明 | **待办**:能力边界补一句「时间戳列可被具备写权限者伪造,故本审计对刻意掩盖痕迹的写入型入侵亦非完备;强判据应取自应用审计日志/DB binlog」;步骤 8 增加「检查 binlog/慢日志保留期」 |
| MEDIUM-5 | `SecurityConfig.java:75` + `handler/AuthenticationFailureServletHandler.java:66` | **登录端点存在用户名枚举**(既存缺陷,非 007 引入)。`setHideUserNotFoundExceptions(false)` 显式关闭存在性遮蔽,`InternalAuthenticationServiceException → "用户账号不存在"` 与 `BadCredentialsException → "用户名或密码错误,剩余尝试次数: N"` 可区分。攻击路径:对 `/login` 逐个提交候选用户名 + 任意口令,按 msg 分流即得有效账号清单;`剩余尝试次数` 还泄露锁定状态。与 007 的「统一 AUT00210 防原因区分」是同类问题的相反做法 | **待办(另开 spec)**:`setHideUserNotFoundExceptions(true)`,归并为「用户名或密码错误」,剩余次数改为仅日志 |
| MEDIUM-6 | `SecurityConfig.java:47`(`MODE_INHERITABLETHREADLOCAL`)+ `JwtAuthorizationTokenFilter.java:112` | `InheritableThreadLocal` + 线程池 = 认证上下文跨用户泄漏风险(既存,007 未加剧但相邻)。`SecurityContextHolderFilter` 只在**请求线程** finally 里 `clearContext()`;项目启用了 `@EnableScheduling` 与 `AsyncConfiguration` 线程池,任何在请求线程中提交任务的路径会把 `Authentication` **继承**给池化线程,而池化线程无人清理 → 后续任务读到残留身份 | **待办(另开 spec)**:改用默认 `MODE_THREADLOCAL` + `DelegatingSecurityContextExecutor`;或在 `TaskDecorator` 中任务结束时 `clearContext()` |
| LOW-1 | `shared/util/ResponseUtils.java:43` 与 `:52` | 401 错误响应与 **token 响应**都写死 `Access-Control-Allow-Origin: *`(:52 的 `writeTokenInfo` 响应体含 `access_token`)。无 `Allow-Credentials` 故不能借浏览器凭证劫持,危害有限,但「任意源可读取含 bearer token 的响应」不应是默认姿态;token 响应用 `Cache-Control: no-cache` 而非 `no-store` | 待办:`writeTokenInfo` 去掉通配 ACAO,`Cache-Control` 改 `no-store`。401 的 charset 声明(007 新增)正确 |
| LOW-2 | `application/dto/request/RefreshTokenRequest.java` + `AuthApplicationService.java:110-118` | 加白后的**未认证**端点缺边界长度校验。`logout` 有 `MAX_TOKEN_LENGTH=2048`,refresh 却无 `@Size`。附带日志伪造:`SensitiveDataMasker.maskToken`(:114,126)取首 4 + 末 4 字符,输入完全由攻击者控制,构造 `"\r\nFA...KE\r\n"` 即可 CRLF 注入日志行,伪造条目干扰取证 | 待办:DTO 加 `@Size(max = 64)` + `@Pattern`(UUID);`maskToken` 输出前剔除 `[\r\n\t]` |
| LOW-3 | `src/main/resources/public.key`(git 跟踪) | 未被任何代码或配置引用的孤立 RSA 公钥文件(全仓 grep 无引用)。非秘密,但与已泄露旧密钥同期入库,留存只增加密钥材料清点噪声 | 待办:确认非在用后 `git rm` |
| LOW-4 | `docs/DEPLOYMENT.md:917-929` | 审计/清理命令用 `redis-cli -a '<REDIS_PASSWORD>'`,口令进入 `ps` 输出与 shell history | 待办:改用 `REDISCLI_AUTH` 环境变量 |

### 未构成发现的核查项(逐条留证,避免后续重复质疑)

- **白名单绕过 —— 未发现可利用路径**。`WhiteUrlMatcher:48-50` 的精确字符串相等在**授予匿名访问**方向上是 fail-closed:任何路径变体(尾斜杠、双斜杠、大小写、`%74` 编码、`;jsessionid=`、前导空格)都只会**减少**命中,不会凭空命中;`WhiteUrlMatcherContractTest:145-161` 已把这批变体固化为断言。反方向也安全:变体请求落到 `RbacAuthorizationManager` 后 `AntPathMatcher` 同样匹配不到 → `authorizedRoles` 为空 → 拒绝。Tomcat 默认拒绝编码斜杠、映射前归一化 `/../`,不存在「安全层看到 A、MVC 路由到 B」的错位。`POST:/auth/tokens` 加白后的未认证可达面:凭证猜测不成立(refresh token 是 122 bit 随机 UUID,`TokenService:106-109`),真正的暴露是**资源型**的,已在 HIGH-1 处置。
- **fail-open —— 可接受的权衡**。`RefreshRateLimiterService:84-87,102-106` 的 Redis 异常 fail-open 不构成鉴权绕过:该服务只在**失败路径**被调用,不产出放行决定,鉴权由 `TokenService.getUserIdFromRefreshToken` 的 Redis 存在性校验独立完成 —— Redis 挂掉时那一步必然抛异常 → refresh 一律失败。即「限流被绕过」与「业务已不可用」同时发生,可绕过的东西已无价值。判定:合理。
- **fail-closed profile 判定 —— 正确**。四条候选误判路径已逐一推演(见 MEDIUM-2 内文),均落在「按生产校验」一侧。
- **信息泄露 —— 401 通路干净**。响应体为 `{"code":"AUT00210","msg":"TOKEN过期"}`,无 token 片段、无口令、无路径;`KeyStoreKeyFactory:140-144` 只回显文件名,口令错误时不回显口令;`KeyPairConfig:49` 只打文件名。
- **时序侧信道 —— 无新增可利用信道**。L2 延迟只在失败路径生效,而失败与否响应体本就明示,不构成新预言机。唯一残留是「token 存在但用户已删」(走 L1,不延迟)与「token 不存在」(走 L2,armed 时延迟)可由耗时区分 —— 但前置条件是攻击者已持有该用户的有效 refresh token,不构成枚举原语。过滤器侧统一 AUT00210 确实消除了「验签失败/过期/被撤销」的原因区分。
- **凭证治理 —— git 跟踪的密钥材料清点完毕**:`test-only.jks`(有意入库,口令公开,不是发现)、`keystore-fixtures/rsa2048.{jks,p12}`(测试 fixture)、`rsa1024.p12`(为 ≥2048 门禁准备的**故意弱** fixture,合理)、`public.key`(见 LOW-3)。`application-test.yml` 的明文测试口令属 FR-019 有意决策。`docs/DEPLOYMENT.md` 全文口令均为占位符,**无真实口令泄露**。
- **`DEPLOYMENT.md` SC-013 措辞 —— 合格**。强制措辞与禁用表述均已落地,MEDIUM-4 只是要求补全边界声明。
- 已知有意决策(统一 AUT00210、`test-only.jks` 随 jar 分发、git 历史保留旧私钥、L1 只告警不锁定、OPTIONS 语义分歧)均按前提接受。`JwtAuthorizationTokenFilter:149` 那个「无可观测行为差异」的 `return` 及其注释,属正确的纵深防御留白,保留。

---

## 处置说明

**CRITICAL-1、CRITICAL-2、HIGH-1、HIGH-2 全部修复后方可合并**(项目规则:CRITICAL/HIGH 未修复禁止合并)。四项性质需区分:

- **HIGH-1** 是纯代码缺陷,改 `RefreshRateLimiterService` 即可,须按 TDD 补「超阈值不占用请求线程」的断言(现有 `RefreshRateLimiterTest:96-112` 恰恰把 `elapsed >= 100ms` 固化成了期望行为,该用例需随方案一起改写,否则会**锁死错误设计**)。
- **HIGH-2、CRITICAL-2** 是配置缺陷,改三个 profile 文件;建议同时按 HIGH-2 的机制化建议消除同型风险复发。
- **CRITICAL-1** 是运维执行缺陷:代码已具备加载外置密钥的能力,但轮换动作尚未执行。**在它完成之前,007 的核心目标(旧 token 干净失效)在生产上并未达成** —— 旧 token 依然能通过验签,因为签名密钥没换。请按 `DEPLOYMENT.md` 步骤 1~9 执行并回填「轮换记录」表,特别是 SC-014(用旧私钥现场新签 token → 断言 401)与新旧 modulus 比对。建议连同 MEDIUM-1 的指纹门禁一起做,把这类误配置从人工 checklist 升级为拒启。

MEDIUM-1/2/3、MEDIUM-4 建议在本 feature 内一并修完(MEDIUM-1+2+3 合起来才使 fail-closed 名副其实)。MEDIUM-5、MEDIUM-6 为既存缺陷、超出 007 边界,登记 tasks.md 并另开 spec,**勿在本分支顺手改**(会破坏 spec 边界)。LOW 四项随后续 polish 处理。

---

## 残余风险(本次实现无法消除)

1. **读取型外泄不可检测,且无补偿控制**。六表审计只能看见新增/篡改;`sys_user` 的 BCrypt 口令哈希、`eve_account.refresh_token`(ESI 长期凭证)是否已被读走,**没有任何技术手段能给出否定结论**。叠加 MEDIUM-4:时间戳可伪造,写入型入侵的检测能力也弱于文档当前暗示的水平。
2. **git 历史仍含旧私钥**(FR-014 有意不改写历史)。任何曾克隆本仓库的人永久持有旧私钥副本。风险中和**完全依赖生产密钥轮换已完成** —— 这正是 CRITICAL-1 尚未成立的那个前提。**轮换完成前,该项是活跃威胁而非残余风险**。
3. **ESI refresh token 若已外泄,只能靠用户重新授权**。这些是第三方(CCP)签发的长期凭证,本项目无法吊销;唯一处置是让受影响角色重走 ESI 授权流程。轮换 JWT 签名密钥对此**毫无作用**。
4. **`test-only.jks` 随生产 jar 分发**(有意决策)。防线是 fail-closed 校验而非文件缺席;MEDIUM-1 未修复前,「把 jar 内解包出的测试密钥当生产密钥加载」技术上仍可通过全部启动校验。
5. **旧私钥副本仍存在于工作区外的文件系统**:已实测确认 `D:\IdeaProjects\eve-jwt.jks` 与泄露密钥字节相同。`git rm` 不清理这些副本,需人工销毁并核对备份、IDE 本地历史、云同步目录。
6. **单实例/停机重启是硬约束**。滚动重启期间新旧密钥并存会造成随机认证失败,且 `TokenService.refreshAccessTokenWithUser` 先删旧 refresh token 再签发,客户端重试循环会把 refresh token 消耗掉。这是设计取舍,不是缺陷,但意味着轮换必然伴随可用性中断,须靠公告与低峰窗口管理,无技术缓解。
