# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

EVE Helper 是一个基于 Java Spring Boot 的应用程序,用于 EVE Online 玩家读取和分析角色/军团的订单、资产和市场数据。项目集成了 EVE Online ESI (EVE Swagger Interface) API,并已迁移到领域驱动设计 (DDD) 架构。

**版本**: 0.0.2-SNAPSHOT
**Java**: 11
**Spring Boot**: 2.7.18

## 构建和开发命令

### 构建和运行
```bash
# 构建项目
mvn clean package

# 运行应用 (默认使用 dev profile)
mvn spring-boot:run

# 使用指定 profile 运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassName

# 运行单个测试方法
mvn test -Dtest=ClassName#methodName

# 跳过测试构建
mvn package -DskipTests
```

### 访问端点
- **应用**: http://localhost:9999
- **Swagger UI**: http://localhost:9999/swagger-ui.html
- **API 文档**: http://localhost:9999/v3/api-docs

## DDD 架构概览

### 分层架构

项目遵循领域驱动设计,分为五个层次:

```
xyz.foolcat.eve.evehelper/
├── domain/          # 领域层 - 核心业务逻辑 (无外部依赖)
├── application/     # 应用层 - 用例协调
├── infrastructure/  # 基础设施层 - 技术实现
├── interfaces/      # 接口层 - 用户交互 (REST, WebSocket)
└── shared/          # 共享层 - 通用组件
```

**依赖规则**: `Interfaces → Application → Domain ← Infrastructure`

领域层是核心,不依赖任何其他层。基础设施层实现领域层定义的接口。

### 领域层 (domain/)

包含核心业务逻辑和领域模型:

- **`model/entity/`**: 具有唯一标识的领域实体
  - `eve/`: EVE 游戏静态数据实体 (IndustryBlueprints, IndustryActivityMaterials 等)
  - `system/`: 应用实体 (Blueprints, Assets, EveAccount, SysUser, MarketOrder 等)
  - 所有实体继承 `BaseEntity` (包含 gmtCreate, gmtModified)

- **`repository/`**: 仓储接口 (实现在 infrastructure 层)
  - `eve/`: EVE 游戏数据仓储
  - `system/`: 应用数据仓储
  - 定义数据访问契约,不涉及具体实现

- **`service/`**: 领域服务 - 跨实体的业务逻辑
  - `esi/`: ESI API 集成服务
  - `eve/`: EVE 游戏数据服务
  - `system/`: 系统业务逻辑服务
  - `security/`: 安全相关服务 (TokenService, TokenBlacklistService, LoginRateLimiterService)
  - `thread/`: 异步操作服务

- **`specification/`**: 规格模式 - 封装业务规则

### 应用层 (application/)

协调用例并编排领域对象:

- **`service/`**: 应用服务 (如 BlueprintsApplicationService)
  - `CommandBus`: 命令总线 - 使用泛型反射自动分发命令
  - `QueryBus`: 查询总线 - 使用泛型反射自动分发查询

- **`dto/`**: 数据传输对象
  - `request/`: 请求 DTO (如 RefreshTokenRequest)
  - `response/`: 响应 DTO (如 TokenPair)

- **`assembler/`**: MapStruct 组装器 (26+ 个)
  - 转换 Domain Entity ↔ Persistence Object (PO)
  - 转换 DTO ↔ Domain Entity
  - 编译时自动生成实现

- **`command/`**: 命令处理器 (CQRS 写操作)
  - `model/Command`: 命令接口 (泛型)
  - `handler/CommandHandler`: 命令处理器接口

- **`query/`**: 查询处理器 (CQRS 读操作)
  - `model/Query`: 查询接口 (泛型)
  - `handler/QueryHandler`: 查询处理器接口

### 基础设施层 (infrastructure/)

技术实现和外部集成:

- **`persistence/`**: 数据库访问实现
  - `entity/`: 持久化对象 (PO) - 数据库实体
  - `mapper/`: MyBatis 映射器 (26+ XML 映射器)
  - `repository/`: 仓储实现 - 使用 Mapper 和 Assembler

- **`external/`**: 外部服务集成
  - `esi/`: EVE Online ESI API 客户端 (30+ API 类)
    - OAuth 2.0 with PKCE 认证
    - 支持晨曦 (Serenity) 和宁静 (Tranquility) 服务器
  - `onebot/`: OneBot 消息集成

- **`config/`**: 配置类
  - `druid/`: 多数据源配置 (EveDataSourceConfig, SystemDatasourceConfig)
  - `mybatis/`: MyBatis Plus 配置
  - `security/`: Spring Security 配置
    - `SecurityConfig`: 安全配置
    - `JwtTokenProperties`: JWT 属性
    - `KeyPairConfig`: RSA 密钥对配置
    - `RbacAuthorizationManager`: RBAC 授权管理器
    - `filter/JwtAuthorizationTokenFilter`: JWT 过滤器
    - `handler/`: 认证成功/失败处理器

### 接口层 (interfaces/)

用户交互接口:

- **`web/controller/`**: REST 控制器
  - AuthController: 认证控制器 (登出, 刷新 Token)
  - BlueprintsController, AssetsController, CharacterController
  - JobController, MarketController, MiningController
  - ObserverController, UserController

- **`web/filter/`**: Web 过滤器
- **`web/advice/`**: 全局异常处理
- **`web/vo/`**: 视图对象

### 共享层 (shared/)

跨层通用组件:

- **`kernel/`**: 核心组件
  - `base/BaseEntity`: 基础实体 (gmtCreate, gmtModified)
  - `base/PageResult`: 分页结果
  - `enums/`: 枚举 (CorporationActivityEnum, IndustryActivityEnum)
  - `exception/EveHelperException`: 自定义异常
  - `constants/`: 常量 (GlobalConstants, SecurityConstant)
  - `annotation/`: 自定义注解 (CommandHandlers, QueryHandlers)

- **`result/`**: 统一返回结果
  - `Result<T>`: 通用返回结果
  - `ResultCode`: 返回码枚举

- **`util/`**: 工具类

## 关键技术细节

### 多数据源配置

两个独立的 MySQL 数据库:
- **`eve`**: EVE Online 静态游戏数据 (只读参考数据)
- **`eve_helper`**: 应用运行时数据 (用户数据、订单、资产)

MyBatis Plus 为每个数据库配置了独立的 Mapper。

### 认证和授权

**JWT 认证**:
- RSA-256 密钥对签名 (存储在 `eve-jwt.jks`)
- Access Token 过期时间: 900 秒 (15 分钟)
- Refresh Token 过期时间: 604800 秒 (7 天)
- Refresh Token 存储在 Redis
- Token 黑名单机制 (登出时加入黑名单)
- 登录限流 (LoginRateLimiterService)
- 自定义过滤器: `JwtAuthorizationTokenFilter`
- Token 格式: `Bearer <token>`

**OAuth 2.0 (ESI 集成)**:
- Authorization Code flow with PKCE
- Refresh tokens 存储在数据库
- Access tokens 缓存在 Redis (19 分钟 TTL)
- 广泛的角色和军团数据权限范围

**RBAC 授权**:
- `RbacAuthorizationManager`: 自定义授权管理器
- URL-权限-角色映射缓存在 Redis
- RESTful 权限格式: `METHOD:PATH`
- 白名单配置在 `application-dev.yml`

### ESI API 集成

位于 `infrastructure/external/esi/`:

- **30+ 专用 API 类**: CharacterApi, CorporationApi, MarketApi, IndustryApi, AssetsApi, WalletApi 等
- **OAuth 流程**: `AuthorizeOAuth` 处理 token 生成和刷新
- **错误处理**: 自定义 `EsiException`
- **缓存**: 角色/军团信息缓存在 Redis
- **自动 token 刷新**: 对应用层透明

### MapStruct 对象映射

26+ 组装器处理以下转换:
- Domain Entity ↔ Persistence Object (PO)
- DTO ↔ View Object (VO)
- Domain Entity ↔ DTO

组装器在编译期通过注解处理自动生成。

### 缓存策略

- **Redis**: 主要缓存存储
- **TTL**: 默认 3000 秒
- **缓存数据**:
  - 权限-角色映射
  - ESI access tokens
  - 角色/军团信息

### 异步处理

- `@EnableScheduling`: 启用定时任务
- `AsyncConfiguration`: 异步方法执行
- 市场订单处理的线程池
- 领域服务: `MarketOrderAsyncService`

## 开发指南

### 使用领域模型

1. **实体** 在 `domain/model/entity/` (eve/ 和 system/)
2. **仓储接口** 在 `domain/repository/` (实现在 infrastructure)
3. **领域服务** 包含不适合放在实体中的业务逻辑
4. 领域层永远不依赖基础设施层或应用层

### 添加新功能

1. 从领域模型开始 (实体、值对象、聚合)
2. 在领域层定义仓储接口
3. 创建应用服务来协调用例
4. 在基础设施层实现仓储
5. 创建 MapStruct 组装器进行对象映射
6. 在接口层添加控制器
7. 编写 MyBatis mapper XML 进行数据库查询

### CQRS 模式

- **Commands**: 写操作 (创建、更新、删除)
- **Queries**: 读操作,支持分页
- 查询处理器在 `application/query/`
- 命令和查询通过 CommandBus 和 QueryBus 分发

### 安全考虑

- 除白名单外,所有端点都需要 JWT 认证
- RBAC 检查由 `RbacAuthorizationManager` 执行
- ESI tokens 敏感 - 加密存储在数据库
- 永远不要提交凭证或 tokens 到仓库
- 使用环境变量配置敏感信息 (见 `.env.example`)

### 数据库操作

- MyBatis Plus 处理基本 CRUD 操作
- 自定义查询在 mapper XML 文件中
- 使用 `MyMetaObjectHandler` 自动填充时间戳字段
- 批量操作可用于批量插入/更新

## 重要文件

- **`pom.xml`**: Maven 依赖和构建配置
- **`src/main/resources/application.yml`**: Profile 选择 (active: dev)
- **`src/main/resources/application-dev.yml`**: 开发环境配置
- **`.env.example`**: 环境变量示例
- **`eve-jwt.jks`**: JWT 签名密钥 (不在仓库中)

## 技术栈

- **Java 11**: 语言版本
- **Spring Boot 2.7.18**: 核心框架
- **Spring Security**: 认证和授权
- **Spring WebFlux**: ESI 的响应式 HTTP 客户端
- **MyBatis Plus 3.5.4**: ORM 框架,支持分页
- **Druid 1.2.8**: 数据库连接池
- **MySQL 8.0.28**: 数据库
- **Redis**: 缓存和会话存储
- **Lombok 1.18.22**: 减少样板代码
- **MapStruct 1.5.5**: 对象映射
- **Hutool 5.8.25**: Java 工具库
- **SpringDoc OpenAPI 1.7.0**: API 文档
- **Nimbus JOSE JWT 10.0.2**: JWT 处理

## 常见模式

### 仓储模式
```java
// 接口在 domain/repository/
public interface SysUserRepository {
    SysUser queryById(Integer id);
    SysUser queryByUsername(String username);
    int insert(SysUser sysUser);
}

// 实现在 infrastructure/persistence/repository/
@Repository
public class SysUserRepositoryImpl implements SysUserRepository {
    private final SysUserMapper mapper;
    private final SysUserAssembler assembler;

    public SysUser queryById(Integer id) {
        return assembler.po2Domain(mapper.selectById(id));
    }
}
```

### 应用服务模式
```java
// 在 application/service/
@Service
public class BlueprintsApplicationService {
    // 协调领域对象和仓储
    // 处理事务
    // 返回 DTOs
}
```

### CQRS 模式
```java
// 命令
@CommandHandlers
public class CreateBlueprintCommandHandler implements CommandHandler<CreateBlueprintCommand, Void> {
    public Void handle(CreateBlueprintCommand command) {
        // 处理命令
    }
}

// 查询
@QueryHandlers
public class BlueprintsQueryHandler implements QueryHandler<BlueprintsQuery, PageResult<BlueprintsDTO>> {
    public PageResult<BlueprintsDTO> handle(BlueprintsQuery query) {
        // 处理查询
    }
}
```

## ESI OAuth 权限范围

应用需要广泛的 ESI 权限范围用于角色和军团数据。详见 README.md 中的完整权限列表,包括:
- 角色: assets, blueprints, wallet, market orders, industry jobs 等
- 军团: assets, contracts, industry jobs, market orders, wallets 等

## 注意事项

- 项目积极维护并遵循 DDD 原则
- 所有中文注释和文档都是有意的 (目标受众)
- 代码库使用英文和中文命名约定
- MapStruct 处理器必须在编译期运行才能生成组装器
- Redis 必须运行才能启动应用
- 两个 MySQL 数据库 (eve 和 eve_helper) 必须可访问
- 使用 `.env.example` 作为环境变量配置参考
