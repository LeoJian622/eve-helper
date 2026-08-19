# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


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

- **domain/**: 实体(`model/entity/eve|system`)、领域读模型(`model/vo`,跨层共享的查询结果载体)、仓储接口(`repository/`)、出站端口(`port/esi` EsiGateway、`port/cache` CacheGateway)、领域服务(`service/esi|eve|system|security|thread`)、领域工具(`util/`)
- **application/**: 应用服务、DTO(`dto/request|response`)、MapStruct 组装器(领域↔DTO,7 个)
- **infrastructure/**: 持久化(PO + MyBatis mapper + 仓储实现)、映射转换器(`assembler/persistence` PO↔领域、`assembler/esi` ESI 响应↔领域,共 32 个)、外部集成(`external/esi` 30+ API 类,OAuth2 PKCE;`external/onebot`)、配置(多数据源、Spring Security/JWT/RBAC)、定时任务(`util/`)
- **interfaces/**: REST 控制器(`web/controller`)、全局异常处理与 `@NoWrap` 标记(`web/advice`)
- **shared/**: BaseEntity/PageResult/PageQuery(`kernel/base`)、配置属性(`kernel/config`)、枚举、EveHelperException、常量、Result<T>/ResultCode、工具类

### 添加新功能
1. 从领域模型开始(实体/领域读模型) → 2. 领域层定义仓储接口(签名只用领域类型,禁止出现上层 DTO/VO) → 3. 应用服务协调用例 → 4. 基础设施层实现仓储 + `assembler/persistence` 做 PO↔领域转换 → 5. application 组装器做领域↔DTO → 6. 接口层控制器 → 7. MyBatis mapper XML

> 跨层类型规则:被多层消费的查询结果放 `domain/model/vo`(领域读模型);仅接口层出参用 `application/dto/response`。domain 层不得 import application/infrastructure/interfaces 的任何类型。

### 关键技术细节
- **多数据源**: `eve`(游戏静态数据,只读)与 `eve_helper`(运行时数据),独立 MyBatis Plus 配置
- **JWT**: RSA-256(`eve-jwt.jks`);access 900s / refresh 604800s;refresh token 存 Redis;登出黑名单;登录限流;`JwtAuthorizationTokenFilter`
- **ESI OAuth**: Authorization Code + PKCE;refresh token 加密存库;access token 缓存 Redis 19 分钟;支持晨曦(Serenity)/宁静(Tranquility)服务器
- **RBAC**: `RbacAuthorizationManager`;URL-权限-角色映射缓存 Redis;权限格式 `METHOD:PATH`;白名单在 application-*.yml
- **缓存**: Redis 主缓存,默认 TTL 3000 秒
- **异步**: @EnableScheduling + AsyncConfiguration,市场订单线程池

## 开发工作流法（强制，不得绕过）

本仓库一切代码变更必须执行统一工作流，全文由下方 `@` 引入，随本文件自动生效：

@docs/workflow/DEVELOPMENT-WORKFLOW.md

核心约束（摘要，完整定义见引入文档）：

1. **先分轨再动手**：特性轨（P0–P7）/ 缺陷轨 / 轻量轨，开工即声明；行为变更无轻量轨。
2. **门禁不可跳**：G1 spec、G2 plan、G3 清单、G6 评审，均需工件证据 + 用户明确批准。
3. **TDD 铁律**：没有先失败的测试就没有生产代码。
4. **证据铁律**：任何"完成/通过"声明必须附当前轮次的验证命令输出。
5. **单一事实源**：规格/计划/任务工件只存在于 `specs/<NNN>-<feature>/`。

阶段详细步骤：进入任一阶段前 Read `docs/workflow/PHASE-DETAILS.md`；
工具分工与冲突裁决：`docs/workflow/TOOL-MAP.md`。

## 安全红线

- **永不提交**: `.env.*` 实际配置、`eve-jwt.jks`、任何 token 或密码;敏感配置走环境变量(参考 `.env.example`)
- ESI tokens 敏感 — 加密存储在数据库
- 除白名单外所有端点需 JWT 认证;RBAC 由 `RbacAuthorizationManager` 执行
- 参数化查询(MyBatis);校验所有外部输入;错误消息不泄露敏感数据

## 重要文件

- `pom.xml`: Maven 依赖与构建配置(冻结技术栈的版本以此为准)
- `src/main/resources/application.yml`: 公共配置(数据源/Druid 等,入库;未声明 `spring.profiles.active`,profile 由启动参数指定)
- `src/main/resources/application-{ali,aliw,prod,test}.yml`: 环境配置(**均不入库**,已在 `.gitignore` 忽略;测试用 `@ActiveProfiles("test")` 走 `application-test.yml`)
- `.env.example`: 环境变量模板
- `.specify/memory/constitution.md`: 项目宪法(含技术栈冻结条款与统一 Spec-First 流程)
- `docs/INDEX.md`: 文档索引;`docs/workflow/DEVELOPMENT-WORKFLOW.md`: 统一开发流程(强制);`docs/reviews/`: ECC 评审记录

## 注意事项

- MapStruct 组装器必须在编译期由注解处理器生成
- Redis 必须运行;两个 MySQL 数据库(eve 和 eve_helper)必须可访问
- 中文注释和文档是有意的(目标受众)
- 业务代码变更须走统一 Spec-First 流程(不分级、无例外);技术栈本身不可变更

<!-- SPECKIT START -->
当前 feature: `014-esi-data-user-binding`(ESI 数据 × 系统用户强关联:8 表 user_id 归属 + 军团同步者私有读 + 联盟维度机制 + 存量迁移)
实现计划: [specs/014-esi-data-user-binding/plan.md](specs/014-esi-data-user-binding/plan.md)
<!-- SPECKIT END -->
