# Implementation Plan: JWT 签名密钥轮换

**Feature**: `007-jwt-key-rotation`
**规格**: [spec.md](./spec.md)
**创建日期**: 2026-08-11
**状态**: 计划草案(待 `ecc:security-reviewer` 设计评审)

---

## 1. 目标与范围

### 做

| 项 | 对应需求 | 性质 |
|----|---------|------|
| `KeyPairConfig` 支持文件系统路径加载 keystore | FR-005、FR-006 | 代码 |
| keystore 缺失/无法解密时 fail-fast 拒绝启动 | FR-007 | 代码 |
| 生产 profile 下 classpath keystore 拒绝启动 | FR-008 | 代码(与 006 L-10 合并) |
| 启动校验 4 项配置安全基线 | FR-008 + 006 L-10 | 代码 |
| 全部 profile 口令改环境变量引用 | FR-018、FR-019 | 配置(部分不入库) |
| `.env.example` 补 `KEYSTORE_LOCATION`/`KEYSTORE_ALIAS` | FR-004 配套 | 配置 |
| `application-prod.yml.example` 补 keystore 段 | FR-004 配套 | 配置 |
| 从工作区删除 `src/main/resources/eve-jwt.jks` | FR-012 | 代码库清理 |
| 生成测试专用 keystore(入库,标注仅测试用) | FR-019 | 测试资产 |
| `docs/DEPLOYMENT.md` 记录轮换步骤与已泄露事实 | FR-013 | 文档 |

### 不做(运维动作,非代码)

- **生成生产新密钥对与新口令** —— 由用户执行,口令不经 AI(FR-002、FR-017)
- **把 keystore 放到 `/etc/eve-helper/` 并设权限 600** —— 部署动作(FR-005)
- **重启生产实例完成轮换** —— 运维动作(FR-011)
- **轮换后的三项验证** —— 需真实环境(FR-010)

> 代码改动使轮换**可行且安全**;轮换本身由用户在生产执行。本 feature 的交付物是「改造后的加载机制 + 启动门禁 + 文档」,不含密钥材料。

---

## 2. 关键决策

| 决策 | 理由 |
|------|------|
| **不引入 `kid` 双密钥并行** | 一次性轮换。客户端已有 401→refresh 重试(spec 6.1),900 秒窗口由 refresh 流程兜住。引入 kid 需改 JWT header、维护 keyId→key 映射、`JwtAuthorizationTokenFilter` 支持多验签者 —— 为一次性操作留下长期复杂度 |
| **保留 `classpath:` 加载分支** | **58 个 `@SpringBootTest` 类全部会加载 `KeyPairConfig`**。若只支持文件系统路径,这些测试需要一个固定的绝对路径,在 CI/多开发机上不可移植。故 `classpath:` 前缀分支是测试的必要兼容路径,而非"方便留的后门" —— 生产由 FR-008 的启动断言堵住 |
| **测试用独立 keystore 并入库** | 测试 keystore 不保护任何真实凭证。入库可让 58 个测试在任何机器上开箱即跑。须显式命名与注释标明「仅测试用」,避免下一个人误以为它是生产密钥 |
| **启动断言与 006 L-10 合并** | 两者都是「生产 profile 配置安全基线校验」,同一个 `ApplicationRunner` 内完成,避免两处散落的校验逻辑 |
| **`KEYSTORE_ALIAS` 保留环境变量** | 见 4.1 的实测结论:`application.yml:115-117` 的三个占位符在现有任何 profile 下**从未被实际解析过**,须改为带默认值形式并补全 `.env.example` |

### 2.1 实测发现:现有配置存在未被触发的部署陷阱

编写计划时实测各 profile 的 `security` 段:

| profile | `security` 段 | 实际取值来源 |
|---------|--------------|-------------|
| `application.yml`(入库) | 有,全为 `${...}` 占位符 | 需 4 个环境变量 |
| `application-test.yml` | **有,明文** | 完全覆盖 application.yml,不走占位符 |
| `application-aliw.yml` | **有,明文** | 同上 |
| `application-prod.yml` | **无** | 回落到 `application.yml` → **必须设置 4 个环境变量** |
| `application-ali.yml` | **无** | 同上 |

**两个结论**:

1. **`application.yml:115-117` 的占位符从未被真正解析过** —— 唯一会用到它们的 prod/ali 若未设环境变量,启动即因占位符无解而失败。这个路径可能从未被走通过(现有部署或许一直用 aliw)
2. **`.env.example` 缺 `KEYSTORE_LOCATION` 与 `KEYSTORE_ALIAS`** —— 只列了 `KEYSTORE_PASSWORD`、`KEY_PASSWORD`(`.env.example:18-19`)。照模板配置的人在 prod 启动时必然撞墙

这不是本次轮换引入的问题,但**轮换会强制走通这条路径**(FR-018 要求 test/aliw 也改用环境变量),所以必须在本 feature 内修好:补全 `.env.example`、给 `alias` 加默认值 `${KEYSTORE_ALIAS:eve-jwt}`(与 `SecurityProperties:38` 的 Java 默认值一致)。

> `location` 与两个口令**不给默认值** —— 缺失就该 fail-fast(FR-007)。只有 `alias` 适合给默认值,因为它不是秘密。

---

## 3. 新增/修改文件清单

### 新增

| 文件 | 用途 |
|------|------|
| `infrastructure/config/security/SecurityBaselineValidator.java` | `ApplicationRunner`,生产 profile 下校验 4 项基线,不合则拒绝启动 |
| `src/test/resources/test-only-jwt.jks` | 测试专用 keystore,文件名自带「test-only」警示 |
| `src/test/.../SecurityBaselineValidatorTest.java` | 校验器单测:各违规组合应拒绝启动,合规应通过 |
| `src/test/.../KeyPairConfigTest.java` | 加载机制单测:classpath 分支、文件系统分支、文件缺失 fail-fast |

### 修改

| 文件 | 改动 |
|------|------|
| `KeyPairConfig.java:43-46` | `ClassPathResource` → 按 `classpath:` 前缀分派 `ClassPathResource` / `FileSystemResource`;补文件存在性检查与明确错误信息 |
| `application.yml:117` | `${KEYSTORE_ALIAS}` → `${KEYSTORE_ALIAS:eve-jwt}`(见 2.1;`location` 与两个口令不给默认值,缺失即 fail-fast) |
| `.env.example:18-19` | 补 `KEYSTORE_LOCATION`、`KEYSTORE_ALIAS`(现仅有两个口令,prod/ali 会因缺变量启动失败 —— 见 2.1) |
| `application-prod.yml.example` | 补 `security.keystore` 段,`location` 示例为 `/etc/eve-helper/eve-jwt.jks` |
| `application-test.yml`(不入库) | 口令改 `${...}`;keystore 改指 `classpath:test-only-jwt.jks` |
| `application-aliw.yml`(不入库) | 口令改 `${...}`;location 改文件系统路径 |
| `docs/DEPLOYMENT.md` | 新增「JWT 密钥轮换」章节:生成命令(口令占位符)、权限设置、验证步骤、已泄露记录 |
| `src/main/resources/eve-jwt.jks` | **删除**(`git rm`) |
| `specs/006-.../spec.md` L-10 | 标注「已由 007 实现」 |

---

## 4. 加载机制设计

```
KEYSTORE_LOCATION 取值            → 解析方式
─────────────────────────────────────────────────────
"classpath:test-only-jwt.jks"    → ClassPathResource("test-only-jwt.jks")
"/etc/eve-helper/eve-jwt.jks"    → FileSystemResource(绝对路径)
"eve-jwt.jks"(无前缀,相对)       → FileSystemResource(相对 cwd) ⚠️ 见下
未设置 / 空                       → 拒绝启动
文件不存在 / 不可读                → 拒绝启动
口令错误(KeyStore.load 抛异常)    → 拒绝启动,错误信息不含口令
```

⚠️ **无前缀相对路径的歧义**:现有 `application-test.yml`/`application-aliw.yml` 写的是裸 `eve-jwt.jks`,现语义为 classpath。改造后若按文件系统解析,这两个配置会静默指向 cwd 下不存在的文件 → 启动失败(fail-fast,可接受)但错误信息须指出「疑似遗留的 classpath 写法,请加 `classpath:` 前缀或改绝对路径」。**不做静默回退** —— 回退会让生产误用 classpath 中的旧 keystore,正是 FR-007 要禁止的。

---

## 5. 启动基线校验设计(FR-008 + 006 L-10)

生产 profile 集合:`prod`、`ali`、`aliw`。仅当 `spring.profiles.active` 含其中之一时校验:

| 校验项 | 违规条件 | 来源 |
|--------|---------|------|
| `mybatis-plus.configuration.log-impl` | 含 `StdOutImpl` | 006 L-10 |
| `logging.level.web` | 为 `debug` / `trace` | 006 L-10 |
| `eve.helper.debug.access-token-endpoint.enabled` | 为 `true` | 006 L-10 |
| `security.keystore.location` | 以 `classpath:` 开头或无前缀相对路径 | 007 FR-008 |

**失败行为**:抛异常终止启动,日志列出全部违规项(不止第一项)与对应环境变量名。**不得**只 warn —— warn 会被忽略,那就等于没有门禁。

**实现位置**:`infrastructure/config/security/`。虽然它校验的不止 security 项,但 keystore 校验是其中最关键的一项,且该包已有 `SecurityProperties`/`KeyPairConfig` 上下文。

---

## 6. 测试策略

| 层 | 测试 | 关键点 |
|----|------|--------|
| `KeyPairConfig` | 单测 | `classpath:` 前缀分派、文件系统分派、文件缺失时异常类型与消息、口令错误时**异常消息不含口令** |
| `SecurityBaselineValidator` | 单测 | 4 项各自违规 → 拒绝;全部合规 → 通过;非生产 profile → 跳过校验。用 `ApplicationContextRunner` 或直接构造 `Environment` mock |
| 既有 58 个 `@SpringBootTest` | 回归 | 改用 `classpath:test-only-jwt.jks` 后须仍能加载并签发 token。**这是本 feature 最大的回归面** |
| `TokenService` | 需确认是否已有 | 轮换不改签发逻辑,但须确认换 keystore 后签发/验签闭环仍通 |

**变异测试**(比照 006 的做法):
- 把 `classpath:` 判断改为恒真 → 文件系统分支测试应失败
- 把校验器的拒绝改为 warn → 基线测试应失败
- 把生产 profile 集合改为空 → 生产校验测试应失败

**基线**:当前 `502 tests / Failures 4 / Errors 216`。Errors 216 因测试库缺角色 `2112818290` 的 ESI 授权行,与本 feature 无关;但**换 keystore 后须确认这个数字不上升** —— 若上升说明测试的 keystore 加载被改坏了。

---

## 7. 风险

| 风险 | 缓解 |
|------|------|
| 58 个 `@SpringBootTest` 因 keystore 改造集体失败 | 先做 `classpath:` 分支 + 测试 keystore,跑通全量后再动生产配置。分两个提交,便于回退 |
| **prod/ali 的 4 个环境变量从未被走通过**(见 2.1) | 本 feature 内补全 `.env.example` 与 `application-prod.yml.example`;`alias` 给默认值。轮换前须在 test profile 用**完整环境变量形态**验证一次,而非依赖 aliw 的明文配置 |
| 裸相对路径配置(`eve-jwt.jks`)语义变更引发部署事故 | 错误信息明确提示改法;`DEPLOYMENT.md` 与 `.example` 模板给正确写法 |
| 启动断言误伤本地开发 | 只在生产 profile 生效;`dev`/`test`/无 profile 不校验 |
| 测试 keystore 被误用于生产 | 文件名 `test-only-jwt.jks` 自带警示;放 `src/test/resources`(不进主 jar);FR-008 断言也会拦住 |
| **轮换后攻击者仍持有效 refresh token**(若其曾用伪造 token 取得) | **spec 未讨论,已提交安全评审。若成立,「不强制登出」决策需重新审视** |

---

## 8. 合宪性检查

| 宪法条款 | 符合性 |
|---------|--------|
| 第四条 技术栈冻结 | ✅ 不新增依赖。`ApplicationRunner`、`FileSystemResource` 均为 Spring Boot 自带;算法仍 RS256 |
| 第五条 Spec-First | ✅ 阶段②规格已提交(`0199a57`/`42d9298`),本文件为阶段③ |
| DDD 分层 | ✅ 改动集中在 `infrastructure/config/security`,不触碰 domain 层。`KeyPairConfig` 本就是基础设施配置 |
| TDD | ✅ 第 6 节测试策略;实现须 RED→GREEN→REFACTOR |
| 安全红线「永不提交 token 或密码」 | ⚠️ 本 feature 正是在修复该红线的既有违反。测试 keystore 入库属例外,已论证(不保护真实凭证)且显式标注 |

---

## 9. 阶段④ 拆解方向(预告,详见 tasks.md)

建议分三批,每批可独立提交与回退:

1. **批 A(纯测试兼容)**:生成 `test-only-jwt.jks` + `KeyPairConfig` 的 `classpath:` 分派 + 单测。跑通 58 个 `@SpringBootTest`
2. **批 B(门禁)**:`SecurityBaselineValidator` + 单测 + `.env.example` / `.example` 模板补全
3. **批 C(清理与文档)**:`git rm eve-jwt.jks`、`DEPLOYMENT.md` 轮换章节、006 L-10 标注已实现

生产轮换在批 C 之后由用户执行。

---

**下一步**: `ecc:security-reviewer` 设计评审(已启动),评审通过后 `/speckit-tasks`。
