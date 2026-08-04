# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**开始任何开发任务前先阅读**: @docs/AI_WORKFLOW.md (分级开发流程与工具链规则)

## 项目概述

EVE Helper 是一个基于 Java Spring Boot 的应用程序,用于 EVE Online 玩家读取和分析角色/军团的订单、资产和市场数据。项目集成了 EVE Online ESI (EVE Swagger Interface) API,采用领域驱动设计 (DDD) 架构。

**版本**: 0.0.2-SNAPSHOT
**Java**: 17
**Spring Boot**: 3.5.14

## 技术栈(已冻结)

> ⚠️ **硬约束**: 技术栈已冻结(宪法第四条)。未经宪法修订程序,不得升级或替换任何核心依赖。

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 语言 |
| Spring Boot | 3.5.14 | 核心框架 (Web/Security/Validation/Cache/Data Redis/WebSocket/Actuator) |
| Spring WebFlux + Reactor Netty | Boot 托管 | ESI 响应式 HTTP 客户端 |
| MyBatis Plus | 3.5.15 (spring-boot3-starter) | ORM 与分页 |
| Druid | 1.2.24 | 连接池,多数据源 |
| MySQL Connector/J | 9.6.0 | 数据库驱动 |
| Redis | Boot 托管 | 缓存与会话存储 |
| Lombok | 1.18.40 | 减少样板代码 |
| MapStruct | 1.6.3 | 对象映射(编译期注解处理) |
| Hutool | 5.8.44 | 工具库 |
| SpringDoc OpenAPI | 2.8.16 | API 文档 |
| Nimbus JOSE JWT | 10.0.2 | JWT 处理 |
| Prometheus metrics | 1.0.0 | 指标暴露 |

## 构建和开发命令

```bash
# 构建项目
mvn clean package

# 运行应用 (默认 dev profile)
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 运行所有测试 / 单个测试类 / 单个方法
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName

# 跳过测试构建
mvn package -DskipTests
```

### 访问端点
- **应用**: http://localhost:9999
- **Swagger UI**: http://localhost:9999/swagger-ui.html
- **API 文档**: http://localhost:9999/v3/api-docs

## DDD 架构概览

五层结构,依赖规则: `Interfaces → Application → Domain ← Infrastructure`。领域层是核心,不依赖任何其他层;基础设施层实现领域层定义的接口。

- **domain/**: 实体(`model/entity/eve|system`,均继承 `BaseEntity`)、仓储接口(`repository/`)、领域服务(`service/esi|eve|system|security|thread`)、Specification 模式
- **application/**: 应用服务、CommandBus/QueryBus(泛型反射分发)、DTO、MapStruct 组装器(26+)、命令/查询处理器(CQRS)
- **infrastructure/**: 持久化(PO + MyBatis mapper + 仓储实现)、外部集成(`external/esi` 30+ API 类,OAuth2 PKCE;`external/onebot`)、配置(多数据源、Spring Security/JWT/RBAC)
- **interfaces/**: REST 控制器、过滤器、全局异常处理、VO
- **shared/**: BaseEntity/PageResult、枚举、EveHelperException、常量、注解、Result<T>/ResultCode、工具类

### 添加新功能
1. 从领域模型开始(实体/值对象/聚合) → 2. 领域层定义仓储接口 → 3. 应用服务协调用例 → 4. 基础设施层实现仓储 → 5. MapStruct 组装器 → 6. 接口层控制器 → 7. MyBatis mapper XML

### 关键技术细节
- **多数据源**: `eve`(游戏静态数据,只读)与 `eve_helper`(运行时数据),独立 MyBatis Plus 配置
- **JWT**: RSA-256(`eve-jwt.jks`);access 900s / refresh 604800s;refresh token 存 Redis;登出黑名单;登录限流;`JwtAuthorizationTokenFilter`
- **ESI OAuth**: Authorization Code + PKCE;refresh token 加密存库;access token 缓存 Redis 19 分钟;支持晨曦(Serenity)/宁静(Tranquility)服务器
- **RBAC**: `RbacAuthorizationManager`;URL-权限-角色映射缓存 Redis;权限格式 `METHOD:PATH`;白名单在 application-*.yml
- **缓存**: Redis 主缓存,默认 TTL 3000 秒
- **异步**: @EnableScheduling + AsyncConfiguration,市场订单线程池

## 分级开发流程(速查)

| 级别 | 适用场景 | 流程 |
|------|----------|------|
| **L1** | 中大型功能、跨层变更 | Spec-Kit: `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement` |
| **L2** | 小改动(单文件/局部逻辑) | Superpowers TDD: RED → GREEN → IMPROVE |
| **L3** | 紧急修复 | `superpowers:systematic-debugging` → 最小修复 → 回归测试 |

所有级别实现后强制评审: `ecc:java-reviewer` 必审;涉及认证/用户输入/外部 API/加密时追加 `ecc:security-reviewer`;构建失败用 `ecc:java-build-resolver`。详见 @docs/AI_WORKFLOW.md。

## 安全红线

- **永不提交**: `.env.*` 实际配置、`eve-jwt.jks`、任何 token 或密码;敏感配置走环境变量(参考 `.env.example`)
- ESI tokens 敏感 — 加密存储在数据库
- 除白名单外所有端点需 JWT 认证;RBAC 由 `RbacAuthorizationManager` 执行
- 参数化查询(MyBatis);校验所有外部输入;错误消息不泄露敏感数据

## 重要文件

- `pom.xml`: Maven 依赖与构建配置(冻结技术栈的版本以此为准)
- `src/main/resources/application.yml`: profile 选择 (active: dev)
- `src/main/resources/application-{dev,ali,aliw,pro}.yml`: 环境配置
- `.env.example`: 环境变量模板
- `.specify/memory/constitution.md`: 项目宪法(含技术栈冻结条款)
- `docs/INDEX.md`: 文档索引;`docs/AI_WORKFLOW.md`: AI 开发工作流

## 注意事项

- MapStruct 组装器必须在编译期由注解处理器生成
- Redis 必须运行;两个 MySQL 数据库(eve 和 eve_helper)必须可访问
- 中文注释和文档是有意的(目标受众)
- 业务代码变更须走分级开发流程;技术栈本身不可变更
