# Implementation Plan: T3 DDD 深度架构重构——消除五层依赖违规根因

**Branch**: `004-ddd-layering-purity` | **Date**: 2026-08-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-ddd-layering-purity/spec.md`

## Summary

对既有 Spring Boot 3.5.14 项目做 DDD 深度架构重构,消除五层依赖违规的深层根因。按 5 个独立项(FR-001~005)分批推进,以**行为等价**为硬约束(FR-006):不新增功能、不改 REST 契约、不引入新依赖(宪法第四条技术栈冻结)。每阶段以编译 + 测试 + grep 依赖扫描验证为完成门槛。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14、MyBatis Plus 3.5.15、Druid 1.2.24、Redis、Spring Security、Nimbus JOSE JWT、MapStruct 1.6.3(全部冻结,不新增)
**Storage**: MySQL(eve / eve_helper 双数据源) + Redis(缓存/会话/黑名单/限流)
**Testing**: JUnit 5 + AssertJ + Mockito;`./mvnw compile` / `./mvnw test-compile`
**Target Platform**: JVM 服务(REST API)
**Project Type**: web-service(分层架构)
**Performance Goals**: 重构不改变既有端点响应(维持 <200ms p95)
**Constraints**: 行为等价;五层依赖规则(Interfaces → Application → Domain ← Infrastructure,shared 公共);实体不直接持久化(PO 才是 MyBatis 映射类)
**Scale/Scope**: 涉及 ESI 防腐层(1 服务 + 9 领域服务)、Redis 端口(5 领域服务)、PO 映射(26 assembler)、仓储契约(4+ 仓储)、SysUser 适配器 — 分批各独立提交

## Constitution Check

### API Performance Gates
- 重构仅内部依赖调整,不新增端点、不改既有端点响应路径 → 端点 <200ms p95 不受影响(无新性能风险)。
- ESI 外部调用已有超时/异常处理;重构保持行为等价,不引入新的慢路径。

### Test Coverage Gates
- 每阶段运行 `mvn compile` + `mvn test-compile` 确保无编译回归。
- 新增端口接口/适配器逻辑补单测 ≥80%。
- 鉴权/限流/黑名单(安全路径)重构后以既有测试 + 行为等价 grep 验证。

## Project Structure

### Documentation (this feature)

```text
specs/004-ddd-layering-purity/
├── plan.md              # 本文件
├── research.md          # Phase 0(端口/防腐蚀层设计决策)
├── data-model.md        # Phase 1(领域端口接口与读模型)
├── quickstart.md        # Phase 1(重构达成验证步骤)
├── contracts/           # Phase 1(端口接口契约)
└── tasks.md             # Phase 2(/speckit-tasks 生成)
```

### Source Code (repository root)

```text
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/
│   ├── port/                    # 新增:EsiPort / CachePort 等抽象端口接口
│   │   ├── esi/EsiGateway.java
│   │   └── cache/CacheGateway.java
│   ├── model/entity/system/SysUser.java        # Phase5:移除 UserDetails 实现
│   └── service/…                # 迁移 ESI/Redis 依赖,改为注端口
├── infrastructure/
│   ├── external/esi/EsiApiService.java          # Phase1:整体迁入(防腐层)
│   ├── persistence/repository/…/*RepositoryImpl # Phase3:PO↔Domain 映射收归
│   └── cache/RedisCacheGateway.java             # Phase2:CachePort 实现
└── application/
    ├── assembler/…              # Phase3:仅保留 Domain↔DTO/VO 映射
    └── service/…                # Phase4:仓储返回实体→组装器转 DTO/VO
```

**Structure Decision**: 不新增项目/模块(单 Maven 项目);在 domain 新增 `port/` 子包承载抽象端口接口,infrastructure 提供 adapter 实现;沿用既有 package 结构,仅调整文件归属与依赖方向。

## Phase Plan

### Phase 0 — Research & 设计决策
- 产出 `research.md`:定义 **EsiGateway**(EsiApiService 的抽象方法签名)与 **CacheGateway**(get/set/delete/incr/expire/黑名单/限流所需操作)的端口接口;确定 domain 读模型如何替代 application DTO / interfaces VO 作为仓储返回类型。
- 决策原则:端口接口方法签名 = 现有 domain 服务实际用到的能力,避免过度抽象(YAGNI)。

### Phase 1 — ESI 外部访问下沉防腐层(FR-001)
- 将 `domain/service/esi/EsiApiService` 迁至 `infrastructure/external/esi/`(反腐层适配器)。
- domain 定义 `EsiGateway` 端口接口;9 个领域服务(Assets/Blueprints/IndustryJob/MiningDetail/Structure/UniverseName/WalletJournal/InvTypes/MarketOrder)改依赖 `EsiGateway` 而非具体 ESI 客户端类/model/config。
- 验证:domain 无 `infrastructure.external.esi` import;行为等价。

### Phase 2 — Redis 经缓存端口抽象(FR-002)
- domain 定义 `CacheGateway` 端口(get/set/setIfAbsent/delete/hasKey/increment/expire/opsForHash.convertAndSend 等现有用到的能力)。
- `TokenService`/`LoginRateLimiterService`/`TokenBlacklistService`/`SysPermissionService`/迁移后的 EsiApiService 改注 `CacheGateway`;infrastructure 提供 `RedisCacheGateway` 实现。
- 验证:domain 无 `RedisTemplate` import;缓存 key/TTL 语义不变。

### Phase 3 — PO↔Domain 映射下沉 infrastructure(FR-003)
- 将 26 个 application assembler 中的 `*PO` 映射(domain2Po/po2Domain)下沉至 `infrastructure/persistence/repository/` 各 RepositoryImpl 内部(或独立的 infra assembler)。
- application assembler 只保留 Domain↔DTO/VO 映射。
- 验证:application 无 `infrastructure.persistence.entity.*PO` import;循环依赖消除。

### Phase 4 — 仓储接口契约净化(FR-004)
- 修改 `BlueprintsRepository`/`BlueprintsDataRepository`/`MarketOrderRepository`/`InvTypesRepository`/`MarketGroupsRepository` 等接口,返回类型改为领域实体或领域读模型。
- 转换(实体→DTO/VO)上移到 application 层组装器;`InvTypesMapper.xml` 的 `InvTypesVO` resultType 改为 PO/读模型。
- 验证:domain 无 application DTO / interfaces VO import;接口行为一致。

### Phase 5 — 安全适配器解耦 + 异步任务归位(FR-005)
- `SysUser` 移除 `implements UserDetails`;新增 `domain/port/UserDetailsPort` 或 infra 适配器封装 Spring Security 契约。
- `domain/service/thread/MarketOrderAsyncService` 迁出 domain(application 或 infrastructure)。
- 验证:domain 实体无 `UserDetails`;domain/thread 为空。

## Complexity Tracking

> 无宪法违规需辩护。本重构为纯技术债治理,不新增依赖、不改 REST 契约,复杂度仅来自迁移规模与行为等价验证,已拆为 5 个独立可测阶段控制风险。