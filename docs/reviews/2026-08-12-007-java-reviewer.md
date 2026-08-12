# 评审记录: 007 JWT 密钥轮换 — 实现阶段 Java 代码质量评审

- **日期**: 2026-08-12
- **评审者**: ecc:java-reviewer
- **关联 feature**: `specs/007-jwt-key-rotation`
- **评审范围**: `git diff 67333f7...HEAD -- src/main/java src/main/resources`(12 个 Java 文件 / 2 个 yml);测试侧 10 个文件仅作覆盖判断
- **框架识别**: `pom.xml` 含 `spring-boot-starter-parent` 3.5.14 → 应用 [SPRING] 规则集
- **构建验证**: `./mvnw -o -q -DskipTests compile` 通过;定向测试 `RefreshRateLimiterTest`(6)/`KeyStoreKeyFactoryTest`(7)/`SecurityBaselineValidatorTest`(11)/`WhiteUrlMatcherContractTest`(17)/`ResponseUtilsTest`(4)/`AuthApplicationServiceUnitTest`(7)/`JwtAuthFailureResponseTest`(3)/`RefreshTokenEndpointAccessTest`(3) 全部 0 failure 0 error;其余 error 为 MySQL/ESI 不可达的既有基线噪声
- **结论**: **BLOCK**

> ⚠️ **口令处理声明**:本记录涉及 gitignore 私有配置中的字面量口令,一律以 `<REDACTED>` 表示。评审 agent 原始输出曾回显该口令原文,归档时已遮蔽;该口令属**已泄露旧密钥**的口令,轮换时必然更换。

## 重点核对项(逐条回应)

1. **DDD 分层合规 — 部分通过**。`grep -rn "import xyz.foolcat.eve.evehelper.(application|infrastructure|interfaces)" domain/` 返回**空**;`RefreshRateLimiterService` 确经 `domain.port.cache.CacheGateway` 而非直连 Redis(`:6,61`),该项合格。但它 import 了 `io.micrometer.core.instrument.MeterRegistry`(`:3-4`),是领域层唯一的 micrometer 依赖,属技术设施泄漏,且告警通路实际不通(HIGH-1)。
2. **资源泄漏** — 已修复且有测试守护:`KeyStoreKeyFactory:80` try-with-resources,`KeyStoreKeyFactoryTest.load_inputStreamClosed_afterGetKeyPair` 用 `close()` 探针断言。**通过**。
3. **并发安全** — 去 `synchronized` 后仍线程安全:`resource`/`password` 均 `final`(`:54,56`),`loadKeyStore()` 每次创建局部 `KeyStore`,无共享可变状态;`KeyPairConfig.keyPair()` 为 `@Bean`,单例期只调一次。**通过**。
4. **函数/文件/嵌套/魔法值** — 最大文件 157 行;限流阈值与延迟区间集中为 `public static final`(`:41-59`)。**通过**。
5. **TODO/FIXME/调试语句** — 全部改动文件 grep 返回**空**。**通过**。
6. **不可变性** — 新增类字段全部 `final`,`WhiteUrlMatcher` 无状态。**通过**。
7. **异常信息不泄露** — `KeyStoreKeyFactory.describe():142-145` 只返回 filename;`:104-107` 口令错误分支不回显口令;`KeyPairConfig:49` 日志改为 `getFilename()`。**通过**。
8. **已知有意决策** — 4 项均未计入发现(统一 AUT00210、OPTIONS 语义分歧、`test-only.jks` 随 jar 分发、`rejectOrPass` 防御性冗余 `return`)。

## 发现

| 级别 | 位置 | 问题 | 处置 |
|------|------|------|------|
| **HIGH-1** | `domain/service/security/RefreshRateLimiterService.java:3-4,63-71` | **告警通路不存在,L2 阈值机制实际只剩日志**。构造器注册 `Counter` 到 `MeterRegistry`,注释称「由 Prometheus 抓取触发告警」。但 `pom.xml` **未声明 `micrometer-registry-prometheus`**(仅有 `io.prometheus:prometheus-metrics-core/-instrumentation-jvm/-exporter-httpserver` 与 `spring-boot-starter-actuator`;`~/.m2/repository/io/micrometer/` 下无 `micrometer-registry-*`,jar 内仅 `micrometer-core/-commons/-observation/-jakarta9`),且 `src/main/resources/` 下**无任何 `management:` 配置** → `/actuator/prometheus` 端点不存在。计数器写入 Boot 兜底的 `SimpleMeterRegistry`,**永不被抓取**。plan §3.3 把「检测 + 为运维响应争取时间」作为 L2 的唯一真价值,该价值当前为 0 | **待办** |
| **HIGH-2** | `src/main/resources/application-aliw.yml:158-162`(gitignore 私有文件) | **fail-closed 基线在 aliw profile 上被绕过,且已泄露密钥的明文口令随 jar 分发**。`location: D:\IdeaProjects\eve-jwt.jks` 含 `\` → `looksLikeFilesystemPath` 返回 true → `checkKeystoreLocation` **放行**;`password` / `key-password` 为字面量 `<REDACTED>`。已用 `keytool -list -storepass <REDACTED>` 对 `git show 67333f7:src/main/resources/eve-jwt.jks` **实测开启成功**(别名 `eve-jwt`,PrivateKeyEntry,2022-3-23)—— 即 T028 `git rm` 的**已泄露旧密钥,其口令仍明文留在会随 jar 分发的 profile 里**(`jar tf` 确认 `BOOT-INF/classes/application-aliw.yml` 在包内)。SecurityBaselineValidator 的 4 项校验对此配置**全部通过**,防线形同虚设 | **待办** |
| **HIGH-3** | `infrastructure/config/security/SecurityBaselineValidator.java:101-108` vs `interfaces/web/controller/CharacterAccessTokenController.java:43` | **基线③ 在生产 profile 上是空校验**。`checkAccessTokenEndpoint` 用 `Boolean.parseBoolean(environment.getProperty(...))`,而 `eve.helper.debug.access-token-endpoint.enabled` 在 `application-prod.yml`/`-ali.yml`/`-aliw.yml` 中**均不存在** → `parseBoolean(null)` = false → 恒放行。控制器用 `@ConditionalOnProperty(havingValue="true")`,缺失即不注册,故当前无暴露;但「基线校验通过(4/4)」的日志会让运维**误信该项已被校验**。真实语义是「只要有人手写 profile 加上 `true` 才会被拦」,与其余 3 项保护强度不对等 | **待办** |
| MEDIUM-1 | `SecurityBaselineValidator.java:92-99` | 基线② 只查 `logging.level.web`,不查 `logging.level.root`。`root: debug` + `web` 未配置时 web logger 继承 debug → **绕过校验**,而 `application.yml:87-92` 的注释明确风险来自 `AbstractHttpMessageConverter` 在 DEBUG 下打印响应体。`SecurityBaselineValidatorTest` 无 `root:debug` 用例 | 待办 |
| MEDIUM-2 | `RefreshRateLimiterService.java:118-125` | `applyFixedDelay()` 在 Tomcat 工作线程上 `Thread.sleep`。全部 yml 均未配 `server.tomcat.threads.max` → 默认 200。洪泛下超阈值后**每个**失败请求占用一个工作线程 100~300ms,整形上限约 200/0.2s ≈ 1000 req/s,与 `L2_THRESHOLD=1000`/60s 同量级 —— 延迟整形本身成为线程池耗尽的放大器,可波及**非 refresh 的正常端点**。`InterruptedException` 处理正确(`:122-124` 恢复中断位)但被吞后仍正常返回 | 待办(与 security-reviewer HIGH-1 同一问题,该处判为 HIGH) |
| MEDIUM-3 | `application/service/AuthApplicationService.java:124-130` + `domain/service/security/TokenService.java:130-135,154-162` | **TOCTOU 只消除了一半**。T015 把「hasKey + get」合并为单次 `get`(`:124`),但 `refreshAccessTokenWithUser`(`TokenService:158`)**又做了一次 `cacheGateway.get(key)`**。两次 get 之间 token 仍可能过期或被并发 `revokeRefreshToken` 删除,窗口从「2 次操作间」平移到「第 1 次与第 3 次 get 间」,且后者跨越 `loadUserById` + `queryRolesByUserId` 两次 DB 往返,**窗口被显著拉长**。原子轮换需 `GETDEL`/Lua(`CacheGateway` 无此原语)。并发双请求可各自生成一套 token 对 | 待办 |
| MEDIUM-4 | `RefreshRateLimiterService.java:79-88` | L1 计数**只写不读**:`grep "L1_KEY_PREFIX\|refresh:fail"` 在 `src/main/java` 仅命中定义与写入两处,无读取方、无指标、无告警(与 HIGH-1 同源)。`refresh:fail:{userId}` 键只能靠人工 `redis-cli` 发现,即 YAGNI 意义上的死数据 | 待办 |
| MEDIUM-5 | `RefreshRateLimiterService.java:97-113` vs `LoginRateLimiterService.java:46-49` | 固定窗口实现与既有同类服务不一致:`LoginRateLimiterService` 仅在 `attempts == 1` 时设 TTL,本类每次 INCR 后无条件 `expire`(`:101`)。plan §254-255 说明这是「杜绝 INCR 与 EXPIRE 间崩溃导致键永不过期」的有意选择,但代价是**洪泛路径 Redis 命令数翻倍**;且无条件重设 TTL 使窗口变为滑动语义,持续洪泛下计数永不归零,与「固定窗口」注释(`:93`)不符 | 待办 |
| MEDIUM-6 | `AuthApplicationService.java:125-130` + `interfaces/web/advice/GlobalExceptionHandler.java:117-122` | `getUserIdFromRefreshToken` 用 `IllegalArgumentException` 作控制流信号。此处已被 catch,但 `TokenService:161,175` 的同类抛出(`refreshAccessTokenWithUser`)**未被 catch**,会直达全局 handler 并把领域异常文案原样回显。项目已有 `EveHelperException` 承载业务语义 | 待办 |
| LOW-1 | `KeyStoreKeyFactory.java:63-65` | `getKeyPair(String alias)` 单参重载**无调用方**(全项目 7 处测试 + `KeyPairConfig:56` 均用双参)。它把 store 口令当 key 口令用,是易误用的死代码 | 待办 |
| LOW-2 | `KeyStoreKeyFactory.java:110` | 用全限定名 `final java.security.Key key;` 而非 import,同文件已 import 6 个 `java.security.*` 类型,风格不一致 | 待办 |
| LOW-3 | `KeyStoreKeyFactory.java:120-121` | `catch (IllegalStateException e) { throw e; }` 重抛自己刚在 try 内抛出的「别名不存在」异常;把 `containsAlias` 提到 try 外可去掉该分支 | 待办 |
| LOW-4 | `KeyStoreKeyFactory.java:72-76` | 位数不足的异常信息硬编码 `"最低要求 2048 位"`,与 `MIN_RSA_MODULUS_BITS`(`:50`)重复,常量改值后文案会撒谎 | 待办 |
| LOW-5 | `shared/util/ResponseUtils.java:42` | 用字符串拼接 `APPLICATION_JSON_VALUE + ";charset=UTF-8"`,而 `AuthenticationFailureServletHandler:78` 用的是 `setCharacterEncoding`。同文件 `writeTokenInfo:51` **未同步**该修正,同一工具类内两方法编码行为不一致 | 待办 |
| LOW-6 | `SecurityBaselineValidator.java:39,55` | 实现 `ApplicationRunner` 而非 `EnvironmentPostProcessor`/`@PostConstruct`。`ApplicationRunner` 在 web 容器已启动并监听端口**之后**才执行(`callRunners` 位于 `refreshContext` 之后);且与 `InitPermissionRolesCache`(`CommandLineRunner`)无 `@Order`,执行序不确定。fail-closed 语义下「拒启」发生在端口已可接受连接之后 | 待办 |
| LOW-7 | `handler/AuthenticationFailureServletHandler.java:68-70` | `else` 分支的 `"登录失败: " + exception.getMessage()` 会把任意 `AuthenticationException` 的原始 message 回显给客户端(既有信息泄露面,本次未引入,仅登记) | 待办 |
| LOW-8 | `application-prod.yml.example:139` vs 生产 profile 实际文件 | 模板已按 007 加上 `- POST:/auth/tokens` 并写了醒目警告,但**实际的** `application-prod.yml`/`-ali.yml`/`-aliw.yml` `whiteUrlList` 仍只有 `- POST:/user`。这正是注释自己警告的「加白在生产静默失效」;仅 `application.yml` 与 `-test.yml` 同步了 | 待办(升级为 security-reviewer HIGH-2) |

## 处置说明

**BLOCK。** 3 项 HIGH 必须修复后方可合并:

- **HIGH-1 与 MEDIUM-4 同源**:两层限流的「观测 + 告警」价值依赖一条不存在的指标通路。修复二选一 —— ① 补 `micrometer-registry-prometheus` + `management.endpoints.web.exposure.include: prometheus`(⚠️ 技术栈冻结条款,新增依赖需走宪法修订或确认属既有 actuator 生态内);② 不引依赖则把 L2 超阈值改为结构化 WARN 日志 + 日志告警规则,并同步修正类注释、plan §3.3 的「Prometheus 抓取触发告警」表述,以及 `RefreshRateLimiterTest.l2_overThreshold_prometheusCounterIncrements` 的立论基础。**当前代码与文档/测试对告警能力的描述不实**,这是 BLOCK 主因之一。
- **HIGH-2** 是 007 自身安全目标的落空,优先级最高(见 security-reviewer CRITICAL-1/2)。建议 `looksLikeFilesystemPath` 升级为「必须绝对路径」+ 增加第 5 项基线(profile 中不得出现字面量口令)。
- **HIGH-3** 建议把 3 项 boolean/枚举型基线从「查到违规值才拒」改为「**必须显式配置为安全值**」的正向白名单(与该类自己在 `isTestProfile` 上的思路一致),否则「4/4 通过」是虚假保证。

**MEDIUM-3(TOCTOU 未完全消除)** 直接对应 T015 的 AC,当前实现只是把窗口平移并拉长。若判定「双 token 对」风险可接受,须在 `spec.md` 显式记录该残留窗口与理由,不应留在标记为已完成的 T015 之下。

MEDIUM-1/2/5/6 与全部 LOW 若延后,逐条登记至 `specs/007-jwt-key-rotation/tasks.md` 并注明原因。

## 测试质量(不计入发现)

新增 4 个测试类分层恰当:`RefreshRateLimiterTest`/`WhiteUrlMatcherContractTest` 用 `@ExtendWith(MockitoExtension.class)` 纯单元,`JwtAuthFailureResponseTest`/`RefreshTokenEndpointAccessTest` 用 `@SpringBootTest` + `@ActiveProfiles("test")` 端到端;测试命名行为可读;`SecurityBaselineValidatorTest` 用 `MockEnvironment` 绕开 Spring 上下文;无 `Thread.sleep` 用于异步等待(`RefreshRateLimiterTest:85-91` 的计时断言是被测行为本身)。唯一缺口是 MEDIUM-1 指出的 `root:debug` 用例缺失。
