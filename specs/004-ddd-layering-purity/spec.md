# Feature Specification: T3 DDD 深度架构重构——消除五层依赖违规根因

**Feature Branch**: `004-ddd-layering-purity`
**Created**: 2026-08-05
**Status**: Draft
**Input**: T3 DDD 深度架构重构:消除五层依赖违规的深层根因(5 项)

> 说明:本特性为**内部架构重构**(技术债治理),"用户"是系统的可维护性与后续开发团队。价值体现在满足项目宪法规定的五层依赖规则(Interfaces → Application → Domain ← Infrastructure,shared 公共)。每项独立可测、独立提交。

## User Scenarios & Testing

### User Story 1 - ESI 外部访问下沉至基础设施防腐层 (Priority: P1)

领域层服务不再直接持有 ESI HTTP 客户端并将其调用细节封装在基础设施层,领域层仅依赖自身定义的抽象端口。

**Why this priority**: 领域层直接 import `infrastructure.external.esi.*`(客户端/模型/异常)并 `.block()` 调用,是数量最大的一类依赖违规(涉及 EsiApiService 与至少 9 个领域服务),影响后续测试与替换能力。

**Independent Test**: 领域层(grep)无任何 `infrastructure.external.esi` 的 import;领域服务通过端口接口调用 ESI。

**Acceptance Scenarios**:

1. **Given** 领域服务需要消费 ESI 数据, **When** 通过领域定义的端口接口调用, **Then** 该服务不 import `infrastructure.external.esi.*` 下的客户端/模型/配置类。
2. **Given** ESI 调用失败, **When** 异常向上传播, **Then** 在应用/接口边界被统一转译为业务异常,HTTP 响应与现有行为一致。
3. **Given** 重构后, **When** 运行既有 ESI 相关功能, **Then** 数据与错误处理行为与重构前完全一致。

---

### User Story 2 - 缓存/限流/黑名单经抽象端口访问 (Priority: P2)

领域服务(鉴权、限流、黑名单、权限缓存)不再直接操作 `RedisTemplate`,改为依赖领域定义的缓存端口,由基础设施层提供实现。

**Why this priority**: 5 个领域服务直操 Redis 是把缓存基础设施耦合进领域,损害可替换性与可测试性;影响登录/鉴权/权限等安全路径。

**Independent Test**: 领域层无 `org.springframework.data.redis.core.RedisTemplate` 的 import;缓存通过端口接口完成。

**Acceptance Scenarios**:

1. **Given** 领域服务需要读写缓存/限流, **When** 通过缓存端口调用, **Then** 不直接 import `RedisTemplate`。
2. **Given** 缓存 key 与 TTL 约定, **When** 端口实现迁移, **Then** 存取行为与 TTL 语义不变。

---

### User Story 3 - PO↔Domain 映射下沉至基础设施 (Priority: P2)

PO(持久化对象)与领域实体的双向映射从 application 组装器下沉至 infrastructure 仓储实现,消除 application↔infrastructure 的双向循环依赖。

**Why this priority**: 26 个 application assembler 同时做 PO↔Domain 与 Domain↔VO 双重映射,是循环依赖(infrastructure→application 30 文件)的根源。

**Independent Test**: application 层(grep)无任何 `infrastructure.persistence.entity.*PO` 的 import;PO 映射仅在 infrastructure 内部。

**Acceptance Scenarios**:

1. **Given** application 组装器, **When** 只保留 Domain↔DTO/VO 映射, **Then** 不 import 任何 `*PO`。
2. **Given** 仓储实现, **When** 完成 PO↔Domain 映射, **Then** 映射逻辑与字段规则与重构前一致。

---

### User Story 4 - 仓储接口契约净化 (Priority: P2)

领域仓储接口与领域服务不再返回 application DTO / interfaces VO,改为返回领域实体或领域读模型。

**Why this priority**: 仓储接口签名泄漏上层类型(如 `MarketGroupsTreeVO`、`InvTypesVO`、`BlueprintsDTO`),使 domain → application/interfaces 反向依赖成立。

**Independent Test**: domain 层(grep 仓储/服务)无 application DTO / interfaces VO 的 import;转换在 application 层完成。

**Acceptance Scenarios**:

1. **Given** 领域仓储接口, **When** 声明返回类型, **Then** 不 import `application.dto.*` 或 `interfaces.web.vo.*`。
2. **Given** 接口调用方, **When** 需要 VO/DTO, **Then** 由 application 层组装器转换,行为一致。

---

### User Story 5 - 安全适配器解耦与异步任务归位 (Priority: P3)

领域实体不再实现 Spring Security 契约(`UserDetails`),异步线程任务迁出领域层。

**Why this priority**: `SysUser` 实体实现框架契约污染领域纯净性;`thread` 异步任务属基础设施关注点。

**Independent Test**: 领域实体无 `UserDetails` 实现;异步任务不在 `domain/service/thread`。

**Acceptance Scenarios**:

1. **Given** 安全适配, **When** 需要 `UserDetails`, **Then** 由基础设施/接口层适配,领域实体不实现框架接口。
2. **Given** 异步任务, **When** 归类, **Then** 不在领域层。

---

### Edge Cases

- 重构后 ESI 调用、缓存 TTL/key、鉴权与限流行为必须与重构前逐字节等价,不得引入功能或时序回归。
- `JwtTokenProperties` 仍被 domain `TokenService` 依赖:若属配置属性,评估是否经端口/值对象注入,避免 domain→infrastructure.config 依赖。
- 仓储接口改造后,MyBatis mapper XML 的 resultType 若是 interfaces VO,需同步改为 PO/领域读模型。

## Requirements

### Functional Requirements

- **FR-001**: 领域层 MUST NOT import `infrastructure.external.esi.*`(客户端/模型/配置/异常)。
- **FR-002**: 领域层对 Redis 的访问 MUST 通过抽象端口,infrastructure 提供实现;领域层 MUST NOT import `RedisTemplate`。
- **FR-003**: PO↔Domain 映射 MUST 在 infrastructure 仓储实现内部完成;application MUST NOT import `*PO`。
- **FR-004**: 领域仓储接口与服务 MUST 返回领域实体或领域读模型;MUST NOT import application DTO / interfaces VO。
- **FR-005**: 领域实体 MUST NOT 实现 Spring Security `UserDetails`;`domain/service/thread` 异步任务 MUST 迁出领域层。
- **FR-006**: 重构后全部既有 REST 端点行为、异常码、缓存语义 MUST 与重构前一致。

### Key Entities

- **领域端口接口(EsiPort / CachePort 等)**: 由 domain 定义,infrastructure 提供 adapter 实现,是领域层与传统实现解耦的契约。
- **领域读模型**: 供仓储接口返回的领域内数据结构,替代对上层 DTO/VO 的依赖。

## Success Criteria

### Code Quality Criteria
- **SC-001**: 全项目 grep 无禁止的跨层 import(domain→infrastructure 客户端/PO、domain→application 类型、application→infrastructure PO、仓储接口返回上层 DTO/VO)。
- **SC-002**: 全量 `mvn test-compile` 与 `mvn compile` 通过,无回归。
- **SC-003**: 单测覆盖新增端口/适配器逻辑 ≥80%。

### Measurable Outcomes
- **SC-004**: 重构后既有 ESI/缓存/鉴权/限流功能行为与重构前等价(功能回归为零)。
- **SC-005**: 消除 application↔infrastructure 循环依赖;domain 层对 infrastructure/application 的依赖计数降为 0。

## Assumptions

- 技术栈冻结(宪法第四条)不变,本重构不引入新核心依赖。
- 重构按 5 个独立项分批推进、各自独立提交,每项以编译+测试+grep 验证为完成门槛。
- 行为等价优先;不新增功能、不改 REST 契约。
- 测试代码需同步更新被迁移类型的 import。