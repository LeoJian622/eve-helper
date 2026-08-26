# Implementation Plan: 020 ESI 客户端重复代码消除（Phase 1）

**Branch**: `020-esi-client-dedup` | **Date**: 2026-08-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/020-esi-client-dedup/spec.md`

## Summary

消除 ESI API 客户端层的重复代码（~712 行），采用**工具方法增强（最小抽象）**策略：

1. **EsiStatusUtil 增强**：新增 `applyErrorHandlers(ResponseSpec)` 封装 204+ 处 `.onStatus().onStatus()`；新增 `handleResponseWithHeaders(...)` 封装 19 处 `exchangeToMono` 块。
2. **EsiClientConstants 新建**：提取 `ESI_DATA_TIMEOUT` 等公共常量，消除 20 处重复声明。
3. **33 个 API 类改造**：显式调用新工具方法，代码净减 ~552 行，行为不变。

**Phase 2（缓存编排）暂缓**，Auth header 注入保留现状（015 缓存需求）。

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.5.14, Spring WebFlux (Reactor Netty), Lombok, MapStruct, Hutool  
**Storage**: N/A（纯代码重构，不涉及存储层）  
**Testing**: Maven Surefire + JUnit 5 + Mockito + Spring Boot Test  
**Target Platform**: Linux server (Spring Boot application, 端口 9999)  
**Project Type**: web-service (DDD 五层架构)  
**Performance Goals**: <200ms p95（宪法 I 要求），外部调用 5s 超时（宪法 API Performance 要求）  
**Constraints**: 不引入新依赖（宪法 IV 技术栈冻结），行为不变（纯重构）  
**Scale/Scope**: 33 个 API 类（`infrastructure/external/esi/api/`），~712 行重复代码待消除

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ 宪法条款检查（Pre-Design）

| 条款 | 检查项 | 结果 | 理由 |
|------|--------|------|------|
| **I. REST API First** | API 契约不变 | ✅ 通过 | 纯内部重构，不改变任何 REST 端点行为 |
| **I. API Performance** | <200ms p95 + 5s 超时 | ✅ 通过 | 重构后工具方法逻辑等价，超时仍为 5s（`ESI_DATA_TIMEOUT`） |
| **II. Observability** | 结构化日志 | ✅ 通过 | 日志简化但仍结构化（`"ESI {apiName} 错误: status={}"`），保留 trace ID |
| **II. Security** | 敏感数据处理 | ✅ 通过 | 不涉及认证/授权逻辑变更，不处理敏感数据 |
| **III. Code Quality** | YAGNI/DRY | ✅ 通过 | 重构目标正是消除重复（DRY），无过度设计 |
| **III. Test Coverage** | ≥80% | ✅ 通过 | TDD 先行，为工具方法编写单元测试，预计覆盖率 >90% |
| **IV. Technology Stack** | 不引入新依赖 | ✅ 通过 | 仅使用 Spring WebFlux 已有 API（`ResponseSpec`、`ClientResponse`） |
| **V. Spec-First** | 流程合规 | ✅ 通过 | 正在执行 P0→P1→P2 流程，spec.md 已批准（G1 通过） |
| **V.1 特性分支** | 分支隔离 | ✅ 通过 | 已在 `020-esi-client-dedup` 分支，非主干 |

**结论**：所有宪法条款通过，无违规项，无需 Complexity Tracking。

### Test Coverage Gates

- **Unit test coverage plan**: ≥80%（目标 90%+）
  - `EsiStatusUtilTest`：覆盖 `applyErrorHandlers` 和 `handleResponseWithHeaders` 的所有分支（2xx/4xx/403/5xx）
  - 现有 `EsiApiServiceTest`、`EsiApiServiceUnitTest` 保持不变（集成测试验证行为等价）
- **Integration test coverage**: 现有集成测试已覆盖关键用户旅程（钱包查询、订单查询等），重构后回归验证
- **Security test coverage**: 不涉及认证/授权变更，无需新增安全测试

## Project Structure

### Documentation (this feature)

```text
specs/020-esi-client-dedup/
├── plan.md              # 本文件（/speckit-plan 输出）
├── spec.md              # 规格文档（已批准，G1 通过）
└── tasks.md             # 任务清单（/speckit-tasks 输出，P3 生成）
```

**Phase 0/1 产物说明**：
- `research.md`：N/A（纯重构，无新技术研究）
- `data-model.md`：N/A（无数据模型变更）
- `contracts/`：N/A（无 API 契约变更）
- `quickstart.md`：N/A（无新特性需要快速入门指南）

### Source Code (repository root)

```text
src/main/java/xyz/foolcat/eve/evehelper/infrastructure/
├── external/esi/
│   ├── EsiStatusUtil.java              # 增强：新增 applyErrorHandlers + handleResponseWithHeaders
│   ├── EsiClientConstants.java         # 新建：提取 ESI_DATA_TIMEOUT 等公共常量
│   └── api/
│       ├── AlliancesApi.java           # 改造：替换 onStatus + exchangeToMono
│       ├── AssetsApi.java              # 改造
│       ├── BookmarksApi.java           # 改造
│       ├── CalendarApi.java            # 改造
│       ├── CharacterApi.java           # 改造
│       ├── CorporationApi.java         # 改造
│       ├── IndustryApi.java            # 改造
│       ├── MarketApi.java              # 改造
│       ├── SovereigntyApi.java         # 改造
│       ├── UniverseApi.java            # 改造
│       ├── WalletApi.java              # 改造
│       ├── WarApi.java                 # 改造
│       ├── PageTotalApi.java           # 改造（含 .block() 场景）
│       └── ...（共 33 个 API 类）
└── gateway/esi/
    └── EsiApiService.java              # 改造：替换 ESI_DATA_TIMEOUT 引用（如有）

src/test/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/
└── EsiStatusUtilTest.java              # 新建：工具方法单元测试
```

**Structure Decision**：
采用现有 DDD 五层架构，本次重构仅涉及 `infrastructure/external/esi/` 层（SDK 包），不触及 domain/application/interfaces 层。工具类（`EsiStatusUtil`、`EsiClientConstants`）保留在 `infrastructure/external/esi/`，符合防腐层定位。

## Phase 0: Research (N/A for Refactoring)

**不适用**：本次为纯代码重构，无新技术引入、无未知依赖、无集成模式变更。所有技术细节已在 spec.md 和现有代码中明确。

## Phase 1: Design & Contracts (Simplified for Refactoring)

**简化说明**：
- `data-model.md`：N/A（无数据模型变更）
- `contracts/`：N/A（无 API 契约变更，REST 端点行为不变）
- `quickstart.md`：N/A（无新特性需要快速入门）

**实际产出**：本 plan.md 已包含完整设计（工具方法签名、改造规则、文件清单），替代传统 Phase 1 产物。

## Implementation Strategy

### 改造顺序（依赖排序）

1. **T1: 新建 EsiClientConstants**（无依赖）
   - 提取 `ESI_DATA_TIMEOUT = Duration.ofSeconds(5)`
   - 文件：`infrastructure/external/esi/EsiClientConstants.java`

2. **T2: 增强 EsiStatusUtil**（依赖 T1）
   - 新增 `applyErrorHandlers(ResponseSpec)`
   - 新增 `handleResponseWithHeaders(ClientResponse, ParameterizedTypeReference<T>, String)`
   - 文件：`infrastructure/external/esi/EsiStatusUtil.java`

3. **T3: 为工具方法编写单元测试**（依赖 T2，TDD 铁律）
   - 文件：`infrastructure/external/esi/EsiStatusUtilTest.java`
   - 覆盖：2xx 成功、4xx 通用、403 特判、5xx 服务端错误

4. **T4: 改造 33 个 API 类**（依赖 T1 + T2 + T3）
   - 按字母顺序逐个改造（AlliancesApi → WarApi）
   - 每个类替换：
     - `.onStatus().onStatus()` → `.as(EsiStatusUtil::applyErrorHandlers)`
     - `exchangeToMono` 块 → `EsiStatusUtil.handleResponseWithHeaders(...)`
     - `private static final Duration ESI_DATA_TIMEOUT` → `import static EsiClientConstants.ESI_DATA_TIMEOUT`
   - 每改造 5-10 个类后运行一次 `mvn test` 验证

5. **T5: 改造 EsiApiService**（依赖 T1）
   - 替换 `private static final Duration ESI_DATA_TIMEOUT` 引用（如有）
   - 文件：`infrastructure/gateway/esi/EsiApiService.java`

6. **T6: 全量验证**（依赖 T4 + T5）
   - `mvn clean package -DskipTests` → exit 0
   - `mvn test` → 0 failures, 0 errors
   - `grep -r ".onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil.dataError())" src/main/java/xyz/foolcat/eve/evehelper/infrastructure/external/esi/api/` → 无匹配（验证无遗漏）

### 改造规则（Before → After）

详见 spec.md §3 "API 类改造"。

### 风险缓解

| 风险 | 缓解措施 |
|------|---------|
| 工具方法引入新 bug | TDD 先行（T3），单元测试覆盖所有分支 |
| 日志简化影响调试 | 保留 trace ID（Spring 默认 MDC） |
| API 类改造遗漏 | T6 grep 验证 + 编译期检查 |
| 重构引入性能回归 | 工具方法逻辑等价，无额外开销；P5 全量测试验证 |

## Complexity Tracking

> **N/A**：所有宪法条款通过，无违规项需证明。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
