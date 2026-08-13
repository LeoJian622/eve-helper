# Implementation Plan: 安全评审遗留修复(007 评审遗留)

**Branch**: `008-security-review-followup` | **Date**: 2026-08-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/008-security-review-followup/spec.md`

## Summary

修复 007 两份评审(2026-08-12 security-reviewer / java-reviewer)核定的既存缺陷:MEDIUM-6(`MODE_INHERITABLETHREADLOCAL` + 线程池导致认证上下文跨用户泄漏)、MEDIUM-5(登录端点用户名枚举)与 12 条 LOW 卫生项。

技术路径(详见 [research.md](./research.md)):

1. **US1**:移除 `SecurityConfig` 的 `MODE_INHERITABLETHREADLOCAL` 覆写(回到默认 `MODE_THREADLOCAL`),并为两个 `ThreadPoolTaskExecutor` 装配统一的 `TaskDecorator`——提交线程快照传播、工作线程执行前后双向清理、同线程回退(CallerRunsPolicy)直通。全仓审计确认**零任务依赖继承身份**,切换无静默失上下文风险。
2. **US2**:`AuthenticationFailureServletHandler` 重写为「对外单一措辞 + 对内细节留痕」双通道;保留 `setHideUserNotFoundExceptions(false)` 以支撑 FR-005 的存在性日志要求(与评审建议的 `true` 方案有有意偏离,理由见 research.md R2);剩余尝试次数仅存日志。
3. **US3**:12 条 LOW 逐条修复——token 响应头姿态、refresh DTO 边界 + 日志控制字符免疫、孤立 `public.key` 移除、DEPLOYMENT.md `REDISCLI_AUTH` 化、`KeyStoreKeyFactory` 四处卫生、`ResponseUtils` 编码一致。

> **设计评审修订(v2,2026-08-13)**:`ecc:security-reviewer` 设计评审 APPROVE-WITH-NOTES,1 HIGH + 3 MEDIUM 已全部吸收进 research.md v2——① HIGH-1:`GlobalExceptionHandler` 校验拒绝日志回显 rejected value → 纳入范围(R9);② MEDIUM-1:`writeTokenInfo` 零调用方死代码 → 删除 + 活路径契约测试钉死 no-store/无 ACAO(R6 重写);③ MEDIUM-2:`LoginRateLimiterService` 原文 username 日志 → 纳入范围(R5 扩围);④ MEDIUM-3:DTO 边界拒绝改变 L2 观测输入 → R4 显式记录。评审记录:`docs/reviews/2026-08-13-008-design-security-reviewer.md`。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14(Web/Security/Validation)、Spring Security(`SecurityContextHolder`/`TaskDecorator` 均为既有依赖内能力)、Lombok、Hutool(JSON 序列化)、SpringDoc(既有 `@Schema`)。**零新增依赖(宪法第四条合规)**
**Storage**: 无数据模型变更;Redis 键空间与 MySQL 模式均不受影响
**Testing**: JUnit 5 + Mockito(单元)、`@SpringBootTest` + `@ActiveProfiles("test")`(端到端,依赖 MySQL/Redis 可达——基线环境性失败按 memory 规则逐用例 diff,不用固定阈值)
**Target Platform**: Windows 开发机 / Linux 单实例部署(与 007 相同)
**Project Type**: web-service(Spring Boot 单体,DDD 五层)
**Performance Goals**: 不劣化既有指标;`TaskDecorator` 每任务新增开销为一次 `SecurityContext` 浅拷贝 + 两次 ThreadLocal 写,纳秒级
**Constraints**: 登录与刷新端点的 HTTP 状态码与响应顶层结构(`Result<T>`)对外不变;技术栈冻结;生产密钥材料不触碰(用户管理)
**Scale/Scope**: 生产代码 12 文件(1 新增 `TaskDecorator`、1 删除 `public.key`、10 修改——含设计评审后纳入的 `GlobalExceptionHandler`、`LoginRateLimiterService`)+ DEPLOYMENT.md 命令块 + 测试文件若干

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 宪法条款 | 评估 | 结论 |
|----------|------|------|
| I. REST API First / DDD 分层 | 变更限于 infrastructure(config/security/handler)、interfaces(advice)、application(dto)、shared(util)、docs;domain 层仅 `LoginRateLimiterService` 日志行脱敏(改用 shared 工具,domain→shared 方向合法,见 research R5;`UserUtil` 只读审计);新 `TaskDecorator` 属技术基础设施关注点,落 infrastructure/config | ✅ 通过 |
| I. TDD 强制 | 每个 FR 先写失败测试(见 tasks.md 规划:US1 线程复用回归、US2 成对比对、US3 逐条对照) | ✅ 流程保证 |
| I. 集成测试 | US2 登录失败响应成对比对为端到端契约;refresh DTO 校验走 `@SpringBootTest` 既有通路 | ✅ 通过 |
| I. API 性能 <200ms p95 | 无新端点、无新 I/O;`TaskDecorator` 开销纳秒级;登录失败路径去掉一次响应分支判断(更快) | ✅ 通过 |
| II. 可观测性/安全 | 本 feature 即安全加固;登录失败细节改结构化留痕(username 脱敏 + reason 类名 + 剩余次数),不降低可观测性 | ✅ 通过 |
| III. 覆盖率 ≥80% | 变更行全部有测试映射(US1 装饰器 + 配置、US2 handler 全分支、US3 逐条);SC-005 验收 | ✅ 计划保证 |
| IV. 技术栈冻结 | 零新增依赖;`TaskDecorator`/`DelegatingSecurityContext*`/Bean Validation 均为既有 Boot 托管能力 | ✅ 通过 |
| V. 统一 Spec-First | spec 已完成(checklist 全过);本 plan 配 `ecc:security-reviewer` 设计评审;后续 tasks/TDD/评审归档 | ✅ 执行中 |

**初检结论:全部通过,无违规需 Complexity Tracking。**

### API Performance Gates

- 全部变更为既有端点的行为加固,无新增 REST 端点;<200ms p95 不受影响
- 无数据库查询变更、无外部服务调用变更
- 无断路器需求变化

### Test Coverage Gates

- 单元测试目标:新增/修改代码行覆盖率 ≥80%(SC-005)
- 集成测试:US2 登录失败成对比对(`@SpringBootTest` test profile)、US1 线程池复用回归
- 安全测试:SC-001(枚举零信息)、SC-002(身份残留回归)、SC-006(独立复审)
- 无性能 SLA 变更,无需新性能测试

## Project Structure

### Documentation (this feature)

```text
specs/008-security-review-followup/
├── plan.md              # 本文件
├── research.md          # Phase 0 输出(9 项决策 R1~R9,v2)
├── data-model.md        # Phase 1 输出(行为对象,无持久化变更)
├── quickstart.md        # Phase 1 输出(验证步骤)
├── contracts/           # Phase 1 输出(登录失败/refresh 校验响应契约)
├── checklists/requirements.md  # specify 阶段产物(已全过)
└── tasks.md             # Phase 2 输出(/speckit-tasks 生成)
```

### Source Code (repository root)

```text
src/main/java/xyz/foolcat/eve/evehelper/
├── infrastructure/config/
│   ├── AsyncConfiguration.java                     # [改] US1:两个线程池装配 TaskDecorator
│   └── SecurityContextTaskDecorator.java           # [新增] US1:快照传播 + 双向清理 + 同线程直通
├── infrastructure/config/security/
│   ├── SecurityConfig.java                         # [改] US1:移除 MODE_INHERITABLETHREADLOCAL;US2:hide=false 保留并注释理由
│   ├── KeyStoreKeyFactory.java                     # [改] US3:LOW-1j~4j(删死代码重载/import Key/提 containsAlias/常量入文案)
│   └── handler/AuthenticationFailureServletHandler.java  # [改] US2+LOW-7j:统一措辞 + 细节仅日志 + 限流调用 try/catch 守护
├── application/dto/request/
│   └── RefreshTokenRequest.java                    # [改] US3 LOW-2s:@Size(max=64) + @Pattern(UUID)
├── interfaces/web/advice/
│   └── GlobalExceptionHandler.java                 # [改,v2 HIGH-1] BindException 日志不回显 rejected value(R9)
├── shared/util/
│   ├── ResponseUtils.java                          # [改,v2] US3:删死方法 writeTokenInfo(R6)+ LOW-5j 重锚定(writeErrorInfo 编码风格,R7)
│   └── SensitiveDataMasker.java                    # [改] US3 LOW-2s:mask* 输出剔除 \p{Cc} 控制字符
├── domain/service/security/
│   └── LoginRateLimiterService.java                # [改,v2 MEDIUM-2] 5 处原文 username 日志改 maskUsername(R5)
└── domain/util/UserUtil.java                       # [只读审计] 调用方全部在请求线程,无改动

src/main/resources/
└── public.key                                      # [删除] US3 LOW-3s:git rm(不改写历史)

docs/DEPLOYMENT.md                                  # [改] US3 LOW-4s:命令块改 REDISCLI_AUTH

src/test/java/...(按 tasks.md 展开)
├── SecurityContextTaskDecoratorTest                # [新增] US1 装饰器单元(含同线程直通 + @Async 通路装饰生效)
├── AuthenticationFailureHandlerEnumerationTest     # [新增] US2 成对比对 + 零回显
├── TokenResponseHeadersContractTest                # [新增,v2 MEDIUM-1] 登录成功/refresh 成功响应:no-store + 无 ACAO(R6)
├── ResponseUtilsTest                               # [改] writeTokenInfo 用例删除;writeErrorInfo 编码断言
├── SensitiveDataMaskerTest                         # [新增/扩展] \p{Cc} 免疫
├── GlobalExceptionHandlerLogTest                   # [新增,v2 HIGH-1] BindException 日志零 rejected value(R9)
├── KeyStoreKeyFactoryTest                          # [改] 随 LOW-1j 删单参用例;位数文案断言随常量
└── ThreadPoolIdentityIsolationTest                 # [新增] US1 端到端:复用线程零残留
```

**Structure Decision**:沿用既有 Maven 单体 + DDD 五层布局,不新增模块。唯一新类 `SecurityContextTaskDecorator` 落 `infrastructure/config`(与 `AsyncConfiguration` 同包,线程池装配关注点内聚)。

## Complexity Tracking

> 宪法门禁全部通过,无违规需要论证。

*(空)*

## 风险与边界

1. **CallerRunsPolicy 陷阱**(research.md R1):`esiAuthStatusExecutor` 拒绝策略为 CallerRuns——若装饰器无脑在 finally 清理,请求线程自身的 `Authentication` 会被抹掉,导致该请求后续 `AccessGuard` fail-closed 拒绝。装饰器必须做**同线程直通**(提交线程 == 执行线程时不触碰上下文)。
2. **hide=true 与 FR-005 冲突**(research.md R2):评审建议的 `setHideUserNotFoundExceptions(true)` 会使 `UsernameNotFoundException` 在 provider 层被转为 `BadCredentialsException`,账号存在性连日志都拿不到,违反 FR-005。有意保留 `false`,改在 handler 层统一响应——以回归测试固化。
3. **SpringBootTest 依赖 DB**(memory:springboot-test-db-dependency):端到端用例需 MySQL 可达;失败先按基线对照复跑甄别环境噪声,再下结论。
4. **public.key 删除**(spec edge case):全仓 grep 已确认零代码/配置引用(仅评审文档提及);实现时仍需用户确认后执行 `git rm`,不改写历史(FR-014 同精神)。
5. **响应契约不变边界**:登录失败仍 401 + `Result` 顶层结构;refresh 校验失败走既有 `MethodArgumentNotValidException` 通路,不新增错误码。
6. **LOW-D 邻近残余**(设计评审定向复核登记):`GlobalExceptionHandler` 的 `MethodArgumentTypeMismatchException` 分支仍带异常对象打日志,类型不匹配消息可内嵌原始输入(同类 CWE-117,仅认证后可达,暴露面低于 HIGH-1)。实现期决策:①顺手把 R9「不回显外部输入」原则推广到该分支,或②登记后续 polish——二选一记入 tasks.md。

## Phase 1 设计后宪法复检

- 设计产物(research/data-model/contracts/quickstart)全部生成并经设计评审 v2 修订后复检:分层结论不变(domain 层仅日志行脱敏,方向合法,LOW-A 修正)、冻结条款零新增依赖、TDD 映射 9 项决策全覆盖 → **复检通过**(详见 research.md v2 宪法复检节)。
