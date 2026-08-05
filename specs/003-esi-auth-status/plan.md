# Implementation Plan: 用户账户 ESI 授权状态返回

**Branch**: `003-esi-auth-status` | **Date**: 2026-08-04 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-esi-auth-status/spec.md`

## Summary

为 `GET /user/{userId}`(`addUser`)接口的每个返回条目附加 ESI 授权状态。状态分四种:授权正常(AUTHORIZED)、授权过期(EXPIRED)、未授权(NOT_AUTHORIZED)、无法判定(UNKNOWN)。

技术方案:新增 `EsiAuthStatus` 枚举与 `EsiApiService.getAuthorizationStatus(EveAccount)` 方法,通过既有 `AuthorizeOAuth.updateAccessToken(REFRESH_TOKEN, token)` 尝试用 refreshToken 换取 accessToken,依据结果映射四态;以 Redis 状态缓存(5min TTL)+ 5s 超时 + 并行冷路径满足 <200ms p95 与外部调用超时门禁;`UserApplicationService` 新增非事务方法返回带状态的 `List<UserAccountDTO>`,控制器变薄。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14 (Web/Security/Validation/Cache/Data Redis)、Spring WebFlux + Reactor Netty(ESI 响应式客户端)、MyBatis Plus 3.5.15、Redis、Lombok、MapStruct 1.6.3、Hutool 5.8.44、Nimbus JOSE JWT
**Storage**: MySQL(eve_helper.eve_account,只读查询 + 刷新成功后回写 refreshToken)、Redis(access token 缓存 + 新增状态缓存)
**Testing**: JUnit 5 + Spring Boot Test + Mockito(单元)、Spring Boot 集成测试(关键路径)
**Target Platform**: JVM 服务端(http://localhost:9999)
**Project Type**: web-service(DDD 分层)
**Performance Goals**: <200ms p95(状态缓存命中路径为 Redis-only);单角色 ESI 刷新 5s 超时
**Constraints**: 宪法 <200ms p95、外部调用 5s 超时、熔断、80% 覆盖率;ESI refreshToken 一次性使用(刷新成功必须回写新 token);不得改变 `addUser` 既有语义
**Scale/Scope**: 单接口扩展 + 1 枚举 + 1 域服务方法 + 1 应用服务方法 + DTO 字段;典型用户绑定 1–10 个角色

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### API Performance Gates
- **<200ms p95**: 命中状态缓存时,每角色仅一次 Redis GET(N 次顺序 Redis 查询,约 N×1ms),整接口远低于 200ms。冷路径(缓存未命中)触发 ESI 刷新,以并行 + 单调用 5s 超时把冷路径限制在约 5s;常态下缓存命中率 >95%,p95 满足。详见 research.md 决策 D2。
- **数据库查询优化**: 仅一次 `EveAccountMapper.queryAccountList(userId)`(按 user_id 索引查询,返回列表),无 N+1;状态判定不触发额外 DB 查询(刷新成功回写为单条 insertOrUpdate)。
- **熔断**: 冻结技术栈未含熔断库(Resilience4j 等),引入需宪法修订。本功能以"UNKNOWN 降级 + 5s 超时 + 状态缓存"作为等价容错:ESI 5xx/网络异常即返回 UNKNOWN 并以短 TTL(30s)缓存,避免持续冲击失败依赖。完整熔断器作为已知缺口列入 Complexity Tracking。

### Test Coverage Gates
- **单元覆盖 ≥80%**: `EsiAuthStatus` 枚举、`EsiApiService.getAuthorizationStatus` 四态映射与缓存逻辑、`UserApplicationService.queryAccountListWithAuthStatus` 编排逻辑均需单元测试(Mock ESI/Redis)。
- **集成测试关键路径**: 覆盖四态(AUTHORIZED/EXPIRED/NOT_AUTHORIZED/UNKNOWN)+ 空列表 + 单角色异常不影响其他角色。
- **安全测试**: 复用既有 RBAC 与 JWT 鉴权(本功能不改变鉴权),验证 `/user/{userId}` 仍受 `RbacAuthorizationManager` 保护。
- **性能测试**: 手动验证冷/热路径响应时间;自动化负载测试列为后续(超出本功能范围)。

## Project Structure

### Documentation (this feature)

```text
specs/003-esi-auth-status/
├── spec.md              # 规格(/speckit-specify)
├── plan.md              # 本文件(/speckit-plan)
├── research.md          # Phase 0 设计决策(/speckit-plan)
├── data-model.md        # Phase 1 数据模型(/speckit-plan)
├── quickstart.md        # Phase 1 验证步骤(/speckit-plan)
├── contracts/
│   └── api-contract.md  # Phase 1 接口契约(/speckit-plan)
└── tasks.md             # Phase 2(/speckit-tasks,后续生成)
```

### Source Code (repository root)

```text
src/main/java/xyz/foolcat/eve/evehelper/
├── shared/kernel/enums/
│   └── EsiAuthStatus.java                 # 新增:四态枚举
├── domain/
│   ├── model/entity/system/
│   │   └── EveAccount.java                # 不变(已有 refreshToken)
│   └── service/esi/
│       └── EsiApiService.java             # 新增 getAuthorizationStatus(EveAccount)
├── application/
│   ├── dto/
│   │   └── UserAccountDTO.java            # 新增 authStatus 字段
│   ├── assembler/system/
│   │   └── EveAccountAssembler.java       # 不变(MapStruct 自动映射新字段)
│   └── service/
│       └── UserApplicationService.java    # 新增 queryAccountListWithAuthStatus(userId)
└── interfaces/web/controller/
    └── UserController.java                # addUser 改为调用新应用服务方法(变薄)

src/test/java/xyz/foolcat/eve/evehelper/
├── domain/service/esi/
│   └── EsiApiServiceTest.java             # 单元:四态映射 + 缓存 + 超时
├── application/service/
│   └── UserApplicationServiceTest.java    # 单元:编排 + 空列表
└── interfaces/web/controller/
    └── UserControllerAuthStatusIT.java    # 集成:四态 + 降级
```

**Structure Decision**: 沿用项目既有 DDD 五层结构。新增 `EsiAuthStatus` 枚举于 `shared/kernel/enums`(与既有 `CorporationActivityEnum` 等同位);状态判定逻辑落入 `domain/service/esi/EsiApiService`(已依赖 `AuthorizeOAuth`/`EveAccountService`/`RedisTemplate`,契合既有模式);DTO 字段与编排落入 application 层;控制器仅做转发。

## Complexity Tracking

> 仅记录需理由说明的宪法门禁偏离。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 熔断器门禁未以独立库实现 | 冻结技术栈不含熔断库(Resilience4j 等),引入需宪法修订(版本提升 + Sync Impact Report + 迁移计划),代价超出本功能收益 | "UNKNOWN 降级 + 5s 超时 + 30s 短 TTL 缓存"已提供等价容错:ESI 持续失败时返回 UNKNOWN 并短缓存,避免请求堆积与持续冲击;且既有 ESI 调用(`getAccessToken` 等)同样未配熔断,本功能保持一致。完整熔断器留待宪法修订时统一引入 |
| 冷路径(缓存未命中)单次响应可能 >200ms | 用户明确要求"使用 refreshToken 进行 ESI 认证"以判定状态,真实刷新无法避免外部调用 | (a) 仅用 access token 缓存判定会掩盖已吊销的 refreshToken(违背用户实时判定意图);(b) 异步后台刷新状态需引入定时任务与额外复杂度(违反 YAGNI)。选择"5min 状态缓存 + 并行 + 5s 超时":常态命中率 >95% 满足 p95,冷路径限制在约 5s 且为低频离群点 |
