# Implementation Plan: 建筑表只读查询接口

**Branch**: `005-structure-query-api` | **Date**: 2026-08-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-structure-query-api/spec.md`

## Summary

为军团建筑表新增 6 个只读查询接口:分页列表、单建筑详情、燃料预警、服务状态、统计概览、增强/解锚时间提醒。遵循现有 Blueprints 只读查询链模式(Domain Repository -> Application Service -> Controller),复用 AccessGuard 做 IDOR 归属校验,跨库 JOIN 解析类型名/星系名,服务 JSON 容错反序列化。**绝不提供新增/编辑/删除能力**,查询只读已同步本地数据,不调 ESI;现有 Structure 实体、同步定时任务、ESI 转换器零改动。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.5.14 (Web/Security/Validation/Cache), MyBatis Plus 3.5.15, MapStruct 1.6.3, Hutool 5.8.44, Lombok 1.18.40, SpringDoc OpenAPI 2.8.16
**Storage**: MySQL 双数据源(`eve` 游戏静态数据只读 + `eve_helper` 运行时数据),Redis 缓存
**Testing**: JUnit 5 + Spring Boot Test(`@ActiveProfiles("test")`)+ MyBatis Plus 分页测试
**Target Platform**: Linux 服务器(JVM)
**Project Type**: web-service(DDD 5 层)
**Performance Goals**: 95% 分位响应 <200ms;列表查询单 SQL 关联解析名称;统计聚合单次获取
**Constraints**: 只读(无 CUD)、IDOR 归属校验、排序白名单防注入、参数化查询、不调 ESI、不破坏现有定时任务同步链
**Scale/Scope**: 单实例;军团级数据量(单军团数十~数百建筑);6 个新端点 + 领域读模型/仓储方法/应用服务/装配器/VO

> 无 NEEDS CLARIFICATION 项。spec 阶段已通过澄清确认:服务接口形式(详情包含+独立端点)、其他功能(统计/筛选/时间提醒)、数据来源(只读库)。

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### API Performance Gates
- ✅ 所有端点目标 <200ms p95(SC-001);列表查询用单 SQL JOIN 一次性解析类型名/星系名,避免 N+1(SC-002);统计概览用聚合 SQL 一次获取(SC-003)
- ✅ 无外部依赖调用(FR-013 只读本地数据),不适用熔断/超时;ESI 调用由现有定时任务负责(已超时控制)
- ✅ 数据库查询优化:跨库 JOIN + 索引(`structure.corporation_id` 已有 QueryWrapper 查询基础)+ 分页 LIMIT

### Test Coverage Gates
- ✅ 单元测试目标 ≥80%(SC-004):覆盖应用服务归属校验、装配器转换、服务 JSON 容错、排序白名单
- ✅ 集成测试覆盖关键路径:6 个端点正常流 + 越权拒绝流(SC-005 100% 覆盖)
- ✅ 安全测试:IDOR(非本人军团、他团建筑)100% 拒绝(SC-010)、注入防护(白名单+参数化)
- ⚠️ 性能测试:暂无自动化压测设施,以 SQL EXPLAIN + 单次响应时延验收代替(标注为后续 CI 补强项)

### 技术栈冻结(第四条)
- ✅ 不引入新依赖;全部使用冻结技术栈内组件(Java 17 / Spring Boot 3.5.14 / MyBatis Plus / Hutool / MapStruct)

### 分级流程(第五条)
- ✅ L1 跨层变更,正在执行 Spec-Kit(specify ✅ -> clarify 跳过,spec 无 NEEDS CLARIFICATION -> plan 当前 -> tasks -> implement)

## Project Structure

### Documentation (this feature)

```text
specs/005-structure-query-api/
├── plan.md              # 本文件
├── research.md          # Phase 0 技术决策研究
├── data-model.md        # Phase 1 实体与读模型
├── quickstart.md        # Phase 1 验证与访问指引
├── contracts/           # Phase 1 REST API 契约
│   └── api-contracts.md
└── tasks.md             # Phase 2(/speckit-tasks 生成)
```

### Source Code (repository root)

```text
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/
│   ├── model/entity/system/Structure.java        # [现有,只读扩展,不改字段]
│   ├── model/vo/                                  # [新增] 领域读模型
│   │   ├── StructureListItemDTO.java              # 列表项(含类型名/星系名)
│   │   ├── StructureDetailDTO.java                # 详情(含解析后服务列表)
│   │   ├── StructureFuelDTO.java                  # 燃料预警(含剩余时长)
│   │   ├── StructureServiceDTO.java               # 服务状态(服务名+状态)
│   │   ├── StructureSummaryDTO.java               # 统计概览(总数/各状态/缺油数)
│   │   └── StructureTimerDTO.java                 # 增强/解锚提醒
│   ├── model/query/
│   │   └── StructurePageCriteria.java             # [新增] 分页条件+排序白名单枚举
│   └── repository/system/
│       └── StructureRepository.java               # [现有,仅追加只读方法]
├── application/
│   ├── service/
│   │   └── StructureQueryApplicationService.java  # [新增] 只读应用服务
│   ├── dto/request/
│   │   ├── StructureQuery.java                    # [新增] 列表分页请求
│   │   └── StructureFuelQuery.java                # [新增] 燃料预警请求
│   ├── dto/response/
│   │   ├── StructureListItemVO.java
│   │   ├── StructureDetailVO.java
│   │   ├── StructureFuelVO.java
│   │   ├── StructureServiceVO.java
│   │   ├── StructureSummaryVO.java
│   │   └── StructureTimerVO.java
│   └── assembler/system/
│       └── StructureAssembler.java                # [新增] MapStruct DTO->VO
├── infrastructure/
│   ├── persistence/repository/system/
│   │   └── StructureRepositoryImpl.java           # [现有,追加只读方法实现]
│   └── (resources)/mappers/system/StructureMapper.xml  # [现有,追加只读 SQL]
└── interfaces/web/controller/
    └── StructureController.java                   # [新增] REST 控制器
```

**Structure Decision**: 遵循现有 Blueprints 只读查询链的 DDD 分层(参考 `BlueprintsController` / `BlueprintsApplicationService` / `BlueprintsRepositoryImpl`)。领域层定义只读仓储方法(签名仅用领域类型),基础设施层实现跨库 JOIN SQL,应用层协调 IDOR 校验 + 装配,接口层暴露 REST。现有 Structure 实体 / 同步定时任务 / ESI 转换器零改动。

## Complexity Tracking

无宪法违规需辩护。技术栈冻结、分层、只读约束均在既有模式内,无新增复杂度。
