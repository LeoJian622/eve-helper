# 代码评审记录：006 角色 ESI AccessToken 查询接口

**评审阶段**: ⑦ 评审(实现完成后)
**评审工具**: `ecc:java-reviewer` + `ecc:security-reviewer`(并行,各自独立)
**日期**: 2026-08-11
**Feature**: `006-character-access-token-api`
**结论**: **CONDITIONAL APPROVE** — 两位评审提出的 CRITICAL/HIGH 均已修复或转为登记待办;剩余门禁 2 项无法在当前环境完成(见第五节)

> 前置记录:[设计评审(阶段③)](./2026-08-11-006-character-access-token-design-review.md),结论 BLOCK / 2 项 CRITICAL。
> 本次复审确认原 C1/C2 已实质解除。

---

## 一、评审时序说明(重要)

两位评审 agent 与我的修复工作**部分并行**,因此其报告中若干发现在报告产出后即被修复。下表按「报告当时」与「归档时实际」分列,避免读者误判当前代码状态。

`security-reviewer` 运行 3,713 秒(约 62 分钟),期间我正在修 `java-reviewer` 提出的 H1~H6。故其报告中的 C-NEW-1、H-NEW-1、H-NEW-2 描述的是修复过程中的中间态。

---

## 二、`ecc:java-reviewer` 结论

提出 6 项 HIGH,**全部已修复**:

| 编号 | 问题 | 修复 | 验证 |
|------|------|------|------|
| H1 | 锁 fail-open:`Boolean.FALSE.equals(null)` 为 false,Redis 异常时落入刷新,锁形同虚设 | 改 `!Boolean.TRUE.equals(acquired)`,两处(含既有 `getAuthorizationStatus`) | `EsiApiService.java:238,493`;专项测试覆盖 null 返回 |
| H2 | 锁键不互斥:新键 `esi_access_token_lock:{uid}:{cid}` 与既有 `esi_auth_status_lock:{cid}` 都轮换同一角色 refreshToken 却互不排斥 | 统一为 `esi_refresh_lock:{characterId}` | `EsiApiService.java:138` 常量;既有测试同步更新 |
| H3 | 锁无 owner 令牌 + 临界区无上界:超 TTL 后 `finally` 会删掉他人的锁 | UUID owner + compare-and-delete;`updateRefreshToken` 内两次 ESI 调用补 `.timeout()`;TTL 10s→20s | `newLockOwner()` / `releaseRefreshLock()`;`EsiApiService.java:299,314` |
| H4 | 争用即硬抛 + `MiningTask` 异常逃逸(仅 catch `ParseException`) | `awaitCachedToken` 有界轮询 1 秒后重读缓存;`MiningTask` 改 `catch (ParseException \| EveHelperException)` + 提前 return | `EsiApiService.java:337`;`MiningTask.java:75,102` |
| H5 | **我改坏了 `springdoc.group-configs`**(sed 副作用,一个 group 三属性被拆成三个残缺条目) | 复原为单元素 map | 已用 snakeyaml 解析全部 5 个 yml 验证(见第四节) |
| H6 | kill switch 零测试 | 新增 6 用例,用 `ApplicationContextRunner`(项目惯例的 `@SpringBootTest` 在无 DB 时全 error) | `CharacterAccessTokenControllerTest` |

---

## 三、`ecc:security-reviewer` 结论

### 设计评审各项复核

| 项 | 报告结论 | 归档时实际 |
|---|---|---|
| C1 日志泄露 | 部分修复(依赖不入库文件) | **部分修复** — 已补入库模板缓解,详见第四节 |
| C2 无锁无超时 | 已修复 | ✅ 已修复 |
| H1 userId fail-closed | 已修复,无绕过路径 | ✅ |
| H2 RBAC 继承 | 未修(设计已定为非代码任务) | 登记待办,上线前审计 |
| H3 缓存键 null | 已修复(mapper + 纵深防御双修) | ✅ |
| H4 审计+限流 | 部分修复(如设计决策:只加审计) | ✅ 符合用户确认的范围 |
| H5 refreshToken 明文 `varchar(64)` | **未修复,风险因 C2 修复而上升** | 登记待办,上线前必做 |
| M1 哨兵值 | 已修复 | ✅ |
| M2 上游错误回显 | 部分修复 | 登记待办(`CharacterApi`/`UniverseApi` 未修完) |
| M3 assert | 部分修复 | 登记待办(3 处遗留) |
| M4 端点可发现 | 已修复 | ✅ `@Hidden` + 开关默认关 |
| M5 characterId≤0 | 已修复(双重校验) | ✅ |
| M6 事务 | 已修复(`NOT_SUPPORTED`) | ✅ |
| L1 Cache-Control | 结论成立 | ✅ 不新增代码 |

### 报告新增发现

| 编号 | 级别 | 内容 | 归档时状态 |
|---|---|---|---|
| C-NEW-1 | CRITICAL | springdoc `group-configs` 结构损坏(即 java-reviewer H5) | ✅ **已修** |
| H-NEW-1 | HIGH | 抢锁失败硬抛,`MiningTask` 两个 cron 每周一 19:00 撞同一 characterId(即 H4) | ✅ **已修** |
| H-NEW-2 | HIGH | 锁无 owner 标识,`finally` 无条件 delete(即 H3) | ✅ **已修** |
| M-NEW-1 | MEDIUM | `CharacterAccessTokenResult` 是 record,默认 `toString()` 打印 accessToken 明文 | ✅ **本次修复**,见第四节 |
| M-NEW-2 | MEDIUM | Spring 上下文测试为 error,C1/L1/M4 实测未真正发生 | ⚠️ 部分,见第五节 |
| L-NEW-1 | LOW | 开关关闭时返回 **403 而非 404**(RBAC 在 handler 映射前拦截) | ✅ 无需改动,403 更安全(不泄露路径存在性) |
| L-NEW-2 | LOW | 审计日志无注入风险 | ✅ 确认无风险(三参数均为 `Integer`/枚举) |
| L-NEW-3 | LOW | 测试 SecurityContext 恢复有理论瑕疵(`getContext()` 惰性创建空 context) | 登记待办,不影响断言 |

### 报告中一处事实偏差

报告称 `MiningTask` 两个 cron「均用硬编码 2112818290」。实际第二处(`noticeExtraction7Day`)已改用变量 `characterId`(`MiningTask.java:101`)。撞车结论仍成立,修复方案不变。

---

## 四、本次评审后的补充修复

### M-NEW-1 — `CharacterAccessTokenResult.toString()` 脱敏

record 自动生成的 `toString()` 输出全部组件。修复前实测泄露(RED 测试失败信息):

```
CharacterAccessTokenResult[accessToken=Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...sig, characterId=95465499, expiresIn=1140]
```

覆写后:`CharacterAccessTokenResult[accessToken=***, characterId=95465499, expiresIn=1140]`

- 新增 `CharacterAccessTokenResultTest`(4 用例):不泄露任何 JWT 片段、保留非敏感字段便于排查、null token 不抛、访问器仍返回原值(Jackson 序列化走访问器,响应体不受影响)
- **变异测试**:把 `***` 换回 `accessToken` 变量 → 测试失败(1 Failure),证明非假绿
- 当前代码中无任何位置打印该对象,故此修复是**预防性**的——把安全属性固化在类型上,不依赖调用方自律

### C1 缓解 — 入库的 `application-prod.yml.example`

`security-reviewer` 指出 C1 只能评「部分修复」的唯一原因:`.gitignore:47` 的 `application-*.yml` 使 prod/ali/aliw 全部不入库,**唯一被版本控制保护的修复只有 `application.yml`**。而 profile 属性优先级高于 `application.yml`,所以根配置里的 `Slf4jImpl` **不是兜底**。

新增 `src/main/resources/application-prod.yml.example`(与 `.env.example` 同思路),把三项安全基线连同「为什么」写进模板:

1. `mybatis-plus.configuration.log-impl = Slf4jImpl`
2. `logging.level.web = info`
3. `eve.helper.debug.access-token-endpoint.enabled = false`

已验证:`git check-ignore` 确认模板可入库、真实 `application-prod.yml` 仍被忽略、模板内无硬编码密码(真实文件的 `password: 123456` 替换为 `${DB_PASSWORD}`)。

> 这只是缓解,不是根治。根治需要**启动时 fail-fast 断言**——见待办 L-10。

### 配置基线的机械化验证

用 snakeyaml 实际解析(非 grep)全部 5 个 yml,验证 `group-configs` 形态与三项基线:

| 文件 | group-configs | log-impl | logging.level.web |
|---|---|---|---|
| `application.yml`(入库) | ✅ 单元素 3 属性 | `Slf4jImpl` | `info` |
| `application-prod.yml` | ✅ 单元素 3 属性 | `Slf4jImpl` | `info` |
| `application-ali.yml` | ✅ 单元素 3 属性 | `Slf4jImpl` | `info` |
| `application-aliw.yml` | ✅ 单元素 3 属性 | `Slf4jImpl` | `info` |
| `application-test.yml` | ✅ 单元素 3 属性 | `StdOutImpl`(有意保留) | `info` |

C-NEW-1 由此确证修复。另发现:4 个 profile 的 `access-token-endpoint.enabled` 键**不存在**,靠 `@ConditionalOnProperty` 无 `matchIfMissing` 兜底关闭。行为安全但脆弱——这正是模板显式写出该键的理由。

### `StdOutImpl` 危害的字节码级确证(评审贡献)

`security-reviewer` 反编译确认,这不是推测:

- `StdOutImpl.isDebugEnabled()` 编译为 `iconst_1; ireturn` —— **硬编码返回 true,无法通过任何日志级别关闭**
- `Slf4jLoggerImpl.isDebugEnabled()` 委托 `org.slf4j.Logger.isDebugEnabled()`
- `PreparedStatementLogger` 打印 `"Parameters:"` 前调用 `isDebugEnabled()` 做门禁

**结论:`Slf4jImpl` + root=info 下 MyBatis 确实不打印 SQL 参数,C1 修复机理成立。** 这填补了我端到端实测失败留下的空白(见第五节)。

---

## 五、未能完成的门禁项(如实记录)

### 1. `mvn test` 未全绿

当前 `Tests run: 502, Failures: 4, Errors: 216`(006 新增 4 个测试后由 498 增至 502)。

- **Errors 216**:根因是**测试库缺角色 `2112818290` 的有效 ESI 授权行**。该 ID 是全部测试共用的主角色(36 个测试文件、113 处引用,亦即 `TaskConstant.CHARACTER_ID`,配 `GlobalConstants.SYSTEM_USER_ID = 1`)。缺授权导致所有需 ESI 调用的测试报 `EveHelperException: 角色未授权` 或 `EsiException: ESI 未授权,授权过期,请重新授权`。**与 DB/Redis 可达性无关** —— 两者均连通,是数据缺失。非 006 引入
- **Failures 4**:`AssertsControllerTest.syncAssets`、`BlueprintsControllerTest.addBlueprintsList`/`getBlueprintsList`(均 500)、`CharacterControllerTest.addCharacterAuth`(401)。同源:需真实登录态与 ESI 授权数据
- **已用 `git stash` 做确定性回归对比**:stash 前后跑同一组测试,**逐用例、逐状态码完全一致** → 006 无回归

> 修复过程中的一个重要教训:此前我把 282 errors 归因为「远程 MySQL 时通时不通」,并据此写了记忆。**该诊断是错的** —— 真因是我 `sed` 只替换了 `application-test.yml` 第 155 行、157-159 行残留坏 YAML(即 H5/C-NEW-1),导致所有 `@ActiveProfiles("test")` 上下文加载失败。修复后 Errors 282→216(66 个测试恢复)。记忆已更正。
>
> 方法论:见到大批 errors 应先 `grep "Caused by" target/surefire-reports/*.txt`,而非凭直觉归因环境。

### 2. C1/L1/M4 端到端实测未完成

设计评审门禁要求「实测日志输出,非仅检查配置」。我的实测尝试**失败且无法证伪**:

跑真实查库的测试并 grep `==> Parameters` 得 0;但对照实验(临时把 test profile 切回 `StdOutImpl`)**也是 0** —— Surefire 不把 MyBatis stdout 捕获到可 grep 的流。该测量方法无效,我未据此宣称成功。

**当前证据等级**:
- C1 修复机理 = 字节码级确证(见第四节)+ 配置层面全 profile 解析验证
- C1 运行时行为 = **未实测**
- L1 `Cache-Control: no-store` = 静态确认(`SecurityConfig` 无 `.headers(...)`,Spring Security 6 默认启用 `CacheControlHeadersWriter`)
- M4 开关关闭时 403 = 静态推导(`ApplicationContextRunner` 测了 bean 不注册,未测 HTTP 状态码)

**建议**:上线前手工验证一次 —— 启动应用、开启端点开关、触发一次 cache-miss,检查日志文件无 `refresh_token` 明文与 `Bearer eyJ`;`curl` 确认开关关闭时 403、开启时响应带 `Cache-Control: no-store`。

---

## 六、转入遗留待办的项

详见 [`specs/006-character-access-token-api/spec.md`](../../specs/006-character-access-token-api/spec.md) 遗留待办章节。摘要:

**上线前必做**:
- L-2 / H5:`refresh_token varchar(64)` → `varchar(255)` DDL 加宽。ESI refreshToken 实测 76~88 字符,非严格模式下静默截断→绑定永久失效。且 `persistRefreshedToken` 的 `rows <= 0` 检查抓不到截断(截断时 rows=1)。**风险已因 C2 修复而上升**(本端点成为最高频轮换触发者)
- RBAC pattern 审计:确认无 `GET:/character/**` 类宽泛规则意外授权该端点
- 运维清理 Redis 脏键 `esi_access_token:null:*` —— H3 修复前产生,**键中不含 userId 故被多用户共享**,内含有效 accessToken 明文
- L-10(新增):启动时 fail-fast 断言 —— 生产 profile 下若 `log-impl` 含 `StdOutImpl`、`logging.level.web` 为 debug/trace、或端点开关为 true,则拒绝启动。**这是让 C1 从「部分修复」升为「已修复」的唯一途径**

**建议但不阻断**:
- M2 补完 `CharacterApi`/`UniverseApi` 的上游错误文本回显(经 `GlobalExceptionHandler` 原样下发)
- M3 补完 3 处 `assert`(`UniverseNameService:57`、`PageTotalApi:56`、`BotDispatcher:144`)
- Druid `filters: slf4j` 的 `druid.sql.Statement` 通道 —— **不受 `log-impl` 修复保护**,若为排查打开 `druid.sql` debug 会泄露 refreshToken
- 清理 `logging.level.com.bolingcavalry.druidtwosource.mapper` 遗留配置(包名与项目 `xyz.foolcat.*` 不匹配,疑从模板复制,易误导)
- L-NEW-3 测试改用 `SecurityContextHolder.clearContext()` 或 `spring-security-test`

---

## 七、攻击面评估(security-reviewer 原文结论)

1. **新端点本身** — 受控。默认不注册;注册后需 JWT + RBAC + `(userId, characterId)` 精确 SQL 匹配三重门禁;ROOT/ADMIN 不豁免(有测试);corp 维度不放行(有测试);「属他人」与「不存在」不可区分(断言 code 与 message 双等)。**H1 无绕过路径** —— controller 是唯一入口,且即使绕过应用层,`doGetAccessToken` 内部仍走 SQL 精确匹配,fail-closed 在两层
2. **`getAccessTokenWithExpiry` 加入 `EsiGateway`** — 签名只用领域类型,符合 DDD 约束,无新攻击面
3. **新 Redis 锁键** — 轻微 DoS 面但需 Redis 写权限(已属沦陷场景),不构成新增外部攻击面
4. **`remainingTtlSeconds`** — 只暴露「自己 token 还剩多久」,已归一化哨兵值,无风险
5. **唯一实质新增攻击面是 C-NEW-1** —— 已修复

---

**评审人**: Claude Code(`ecc:java-reviewer` + `ecc:security-reviewer`)
**归档日期**: 2026-08-11
