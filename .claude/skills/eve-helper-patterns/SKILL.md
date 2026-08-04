---
name: eve-helper-patterns
description: EVE Helper 项目编码模式和工作流程
version: 1.0.0
source: local-git-analysis
analyzed_commits: 200
repository: eve-helper
---

# EVE Helper 编码模式

本文档基于 Git 历史分析,总结了 EVE Helper 项目的编码模式、工作流程和最佳实践。

## 提交约定

### 提交消息格式

项目**部分使用** Conventional Commits 格式 (约 6% 的提交):

```
<type>: <description>

类型:
- feat:     新功能
- fix:      Bug 修复
- docs:     文档更新
- chore:    杂项任务
- test:     测试相关
- refactor: 重构
```

**示例**:
```
feat: 添加'ESI author Token'为空异常消息
feat: 迁移领域设计模型
docs: 添加CLAUDE.md开发指南
```

### 中文提交消息

项目主要使用**中文提交消息** (约 94% 的提交),格式为:

```
<序号>) <描述1> <序号>) <描述2> ...

示例:
1) DDD改版定稿 0.0.2-SNAPSHOT
1) 修复燃料不足提醒
1) 角色接口封装完成 2) 克隆接口封装完成 3) 联系人接口封装完成 4) 合同接口部分封装 5) 增加相关ESI接口的test
```

**特点**:
- 使用序号标记多个变更
- 描述具体功能或修复
- 常见模式: "存个档" (保存进度)
- 接口封装提交通常包含: 接口实现 + 数据模型 + 测试

## 代码架构

### DDD 分层架构

项目遵循**领域驱动设计 (DDD)** 架构,分为五层:

```
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/              # 领域层 - 核心业务逻辑
│   ├── model/          # 领域模型 (entity, valueobject, aggregate)
│   ├── repository/     # 仓储接口
│   ├── service/        # 领域服务
│   └── specification/  # 业务规则规格
├── application/         # 应用层 - 用例协调
│   ├── service/        # 应用服务 (CommandBus, QueryBus)
│   ├── dto/            # 数据传输对象
│   ├── assembler/      # MapStruct 对象映射
│   ├── command/        # CQRS 命令
│   └── query/          # CQRS 查询
├── infrastructure/      # 基础设施层 - 技术实现
│   ├── persistence/    # 数据库访问 (mapper, repository impl)
│   ├── external/       # 外部服务集成 (ESI API)
│   └── config/         # 配置类
├── interfaces/          # 接口层 - 用户交互
│   └── web/           # REST 控制器
└── shared/             # 共享层 - 通用组件
    ├── kernel/         # 核心组件
    ├── result/         # 统一返回结果
    └── util/           # 工具类
```

### 命名约定

| 组件类型 | 命名模式 | 示例 |
|---------|---------|------|
| **Controller** | `{Entity}Controller` | `AssetsController`, `BlueprintsController` |
| **Service** | `{Entity}Service` | `MiningDetailService`, `EsiApiService` |
| **Repository Interface** | `{Entity}Repository` | `SysUserRepository`, `AssetsRepository` |
| **Repository Impl** | `{Entity}RepositoryImpl` | `SysUserRepositoryImpl` |
| **Mapper** | `{Entity}Mapper` | `AssetsMapper`, `MarketOrderMapper` |
| **Assembler** | `{Entity}Assembler` | `SysUserAssembler`, `BlueprintsAssembler` |
| **DTO** | `{Entity}DTO` | `MarketOrderDTO`, `UserDTO` |
| **PO** | `{Entity}PO` | `SysUserPO`, `AssetsPO` |
| **Test** | `{Class}Test` | `CharacterApiTest`, `MiningDetailServiceTest` |

## 工作流程

### 添加新的 ESI API 接口

这是项目中最常见的工作流程 (基于历史提交分析):

**步骤 1: 创建 API 客户端**
```
infrastructure/external/esi/api/{Module}Api.java
```

**步骤 2: 创建数据模型**
```
infrastructure/external/esi/model/
├── send/          # 请求模型
└── sub/           # 响应模型
```

**步骤 3: 编写测试**
```
src/test/java/xyz/foolcat/eve/evehelper/esi/api/{Module}ApiTest.java
```

**步骤 4: 集成到服务层**
```
domain/service/esi/EsiApiService.java
或
domain/service/system/{Entity}Service.java
```

**提交消息示例**:
```
1) 角色接口封装完成 2) 增加对应接口返回数据对象 3) 增加相关ESI接口的test
```

### 添加新的业务功能

**步骤 1: 定义领域模型**
```
domain/model/entity/system/{Entity}.java
```

**步骤 2: 定义仓储接口**
```
domain/repository/system/{Entity}Repository.java
```

**步骤 3: 创建持久化对象**
```
infrastructure/persistence/entity/system/{Entity}PO.java
```

**步骤 4: 创建 MyBatis Mapper**
```
infrastructure/persistence/mapper/system/{Entity}Mapper.java
src/main/resources/mappers/system/{Entity}Mapper.xml
```

**步骤 5: 实现仓储**
```
infrastructure/persistence/repository/system/{Entity}RepositoryImpl.java
```

**步骤 6: 创建 Assembler**
```
application/assembler/system/{Entity}Assembler.java
```

**步骤 7: 创建领域服务**
```
domain/service/system/{Entity}Service.java
```

**步骤 8: 创建应用服务 (可选)**
```
application/service/{Entity}ApplicationService.java
```

**步骤 9: 创建 Controller**
```
interfaces/web/controller/{Entity}Controller.java
```

**步骤 10: 编写测试**
```
src/test/java/xyz/foolcat/eve/evehelper/domain/service/system/{Entity}ServiceTest.java
src/test/java/xyz/foolcat/eve/evehelper/interfaces/web/controller/{Entity}ControllerTest.java
```

### DDD 迁移工作流程

项目经历了从传统三层架构到 DDD 架构的迁移:

**迁移步骤**:
1. 创建 DDD 目录结构 (domain, application, infrastructure, interfaces, shared)
2. 移动实体到 `domain/model/entity/`
3. 提取仓储接口到 `domain/repository/`
4. 移动服务到 `domain/service/` 或 `application/service/`
5. 移动持久化实现到 `infrastructure/persistence/`
6. 移动控制器到 `interfaces/web/controller/`
7. 创建 Assembler 进行对象映射
8. 实现 CQRS 模式 (CommandBus, QueryBus)

**相关提交**:
```
feat: 迁移领域设计模型
1) DDD改版定稿 0.0.2-SNAPSHOT
```

## 测试模式

### 测试文件组织

```
src/test/java/xyz/foolcat/eve/evehelper/
├── controller/              # Controller 测试
│   ├── AssertsControllerTest.java
│   └── BlueprintsControllerTest.java
├── domain/
│   └── service/            # 领域服务测试
│       └── security/
│           └── LoginRateLimiterServiceTest.java
├── esi/
│   ├── api/                # ESI API 测试
│   │   ├── CharacterApiTest.java
│   │   ├── AssetsApiTest.java
│   │   └── ...
│   └── auth/               # ESI 认证测试
│       └── AuthorizeOAuthTest.java
└── service/
    └── system/             # 系统服务测试
        ├── MiningDetailServiceTest.java
        └── WalletJournalServiceTest.java
```

### 测试命名约定

- **测试类**: `{ClassName}Test.java`
- **测试方法**: 使用描述性名称,通常为中文
- **测试框架**: JUnit + Spring Boot Test

### 测试覆盖重点

基于提交历史,项目重点测试:
1. **ESI API 集成** - 每个 API 类都有对应测试
2. **领域服务** - 核心业务逻辑测试
3. **Controller** - REST 端点测试
4. **认证授权** - Security 相关测试

## 常见文件变更模式

基于 Git 历史分析,以下文件最常被修改:

### 核心文件 (修改频率 > 10 次)

| 文件 | 修改次数 | 说明 |
|------|---------|------|
| `pom.xml` | 16 | 依赖管理和版本更新 |
| `EsiApiService.java` | 15 | ESI API 核心服务 |
| `application.yml` | 11 | 配置文件更新 |
| `EsiCorporationApiService.java` | 11 | 军团 API 服务 |
| `CharacterApi.java` | 11 | 角色 API 客户端 |
| `AssetsApi.java` | 11 | 资产 API 客户端 |

### 文件协同变更模式

以下文件通常一起修改:

**模式 1: API + Test**
```
infrastructure/external/esi/api/{Module}Api.java
src/test/java/xyz/foolcat/eve/evehelper/esi/api/{Module}ApiTest.java
```

**模式 2: Service + Mapper**
```
domain/service/system/{Entity}Service.java
infrastructure/persistence/mapper/system/{Entity}Mapper.java
src/main/resources/mappers/system/{Entity}Mapper.xml
```

**模式 3: Controller + Service**
```
interfaces/web/controller/{Entity}Controller.java
domain/service/system/{Entity}Service.java
```

**模式 4: Entity + PO + Assembler**
```
domain/model/entity/system/{Entity}.java
infrastructure/persistence/entity/system/{Entity}PO.java
application/assembler/system/{Entity}Assembler.java
```

## 技术栈特定模式

### MyBatis Plus 使用模式

**Mapper 接口**:
```java
@Mapper
public interface {Entity}Mapper extends BaseMapper<{Entity}PO> {
    // 自定义查询方法
}
```

**Mapper XML**:
```xml
<!-- src/main/resources/mappers/system/{Entity}Mapper.xml -->
<mapper namespace="xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.{Entity}Mapper">
    <!-- 自定义 SQL -->
</mapper>
```

### MapStruct 使用模式

**Assembler 定义**:
```java
@Mapper(componentModel = "spring")
public interface {Entity}Assembler {
    // PO -> Domain
    {Entity} po2Domain({Entity}PO po);

    // Domain -> PO
    {Entity}PO domain2Po({Entity} entity);

    // Domain -> DTO
    {Entity}DTO domain2Dto({Entity} entity);

    // DTO -> Domain
    {Entity} dto2Domain({Entity}DTO dto);
}
```

### Spring Security 模式

**认证流程**:
1. JWT Token 认证 (RSA-256)
2. Token 黑名单检查
3. 登录限流
4. RBAC 授权

**关键组件**:
- `JwtAuthorizationTokenFilter`: JWT 过滤器
- `TokenService`: Token 管理
- `TokenBlacklistService`: Token 黑名单
- `LoginRateLimiterService`: 登录限流
- `RbacAuthorizationManager`: RBAC 授权

### ESI OAuth 集成模式

**OAuth 流程**:
1. Authorization Code with PKCE
2. Token 存储 (Refresh Token 在数据库, Access Token 在 Redis)
3. 自动 Token 刷新
4. 支持晨曦和宁静服务器

**API 调用模式**:
```java
// 使用 WebClient 进行响应式调用
WebClient client = WebClient.builder()
    .baseUrl(esiUrl)
    .defaultHeader("Authorization", "Bearer " + accessToken)
    .build();
```

## 开发最佳实践

### 1. 遵循 DDD 分层

- **领域层**不依赖其他层
- **应用层**协调领域对象
- **基础设施层**实现技术细节
- **接口层**处理用户交互

### 2. 使用 CQRS 模式

- **命令** (Command): 写操作,改变系统状态
- **查询** (Query): 读操作,不改变系统状态
- 使用 `CommandBus` 和 `QueryBus` 分发

### 3. 对象映射

- 使用 **MapStruct** 进行对象转换
- 不要手动编写映射代码
- Assembler 在编译期自动生成

### 4. 测试驱动

- 每个 API 接口都要有测试
- 核心业务逻辑必须测试
- 使用 Spring Boot Test 进行集成测试

### 5. 配置管理

- 敏感信息使用环境变量 (参考 `.env.example`)
- 不同环境使用不同 profile (dev, prod)
- 配置类使用 `@ConfigurationProperties`

### 6. 异常处理

- 使用自定义异常 `EveHelperException`
- 全局异常处理在 `interfaces/web/advice/`
- ESI API 错误使用 `EsiException`

### 7. 缓存策略

- 使用 Redis 缓存
- 权限-角色映射缓存
- ESI Access Token 缓存 (19 分钟 TTL)
- 角色/军团信息缓存

## 常见任务

### 更新依赖

```bash
# 编辑 pom.xml
# 更新版本号
mvn clean install
```

### 添加新的 ESI API

1. 创建 `{Module}Api.java` 在 `infrastructure/external/esi/api/`
2. 创建数据模型在 `infrastructure/external/esi/model/`
3. 编写测试 `{Module}ApiTest.java`
4. 集成到 `EsiApiService.java`

### 添加新的业务实体

1. 创建领域实体在 `domain/model/entity/system/`
2. 定义仓储接口在 `domain/repository/system/`
3. 创建 PO 在 `infrastructure/persistence/entity/system/`
4. 创建 Mapper 和 XML
5. 实现仓储在 `infrastructure/persistence/repository/system/`
6. 创建 Assembler
7. 创建服务和控制器
8. 编写测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=CharacterApiTest

# 运行单个测试方法
mvn test -Dtest=CharacterApiTest#testGetCharacterInfo
```

## 项目演进历史

### 阶段 1: 初始开发 (2022-01 ~ 2023-07)
- 基础架构搭建
- Spring Security 集成
- ESI OAuth 认证
- 基础功能实现

### 阶段 2: ESI 接口封装 (2023-08 ~ 2023-11)
- 30+ ESI API 接口封装
- 响应式 HTTP 客户端 (WebFlux)
- 完整的测试覆盖

### 阶段 3: 功能扩展 (2024-06 ~ 2024-11)
- 建筑燃料提醒
- 生产线通知
- 开采通知
- 军团税退税功能

### 阶段 4: DDD 迁移 (2025-07 ~ 2026-01)
- 迁移到 DDD 架构
- 实现 CQRS 模式
- 引入 MapStruct
- 完善安全机制 (Token 黑名单, 登录限流)

## 相关文档

- **CLAUDE.md**: 项目架构和开发指南
- **README.md**: 项目介绍和 ESI 权限说明
- **.env.example**: 环境变量配置示例

---

*本文档基于 200 条 Git 提交历史自动生成*
*最后更新: 2026-02-01*
