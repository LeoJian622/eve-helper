# Research: Spring AI 数据库查询 Agent

**Feature**: 002-spring-ai-db-agent  
**Date**: 2026-05-19

## Research Findings

### 1. Spring AI Alibaba 1.1.2.0 集成

**Decision**: 使用官方 Spring AI Alibaba Starter 集成

**Rationale**:
- Spring AI Alibaba 是阿里云基于 Spring AI 框架的扩展，提供通义千问等大模型支持
- 版本 1.1.2.0 是稳定版本，兼容 Spring Boot 3.x
- 提供标准化的 ChatClient API，简化大模型集成
- 支持 Prompt Template、Function Calling 等高级特性

**Maven Coordinates**:
```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.1.2.0</version>
</dependency>
```

**Configuration Required**:
- `spring.ai.alibaba.api-key`: 阿里云 API Key
- `spring.ai.alibaba.chat.options.model`: 模型名称 (如 qwen-max)

**Alternatives considered**:
- 直接使用阿里云 SDK - 需要更多封装代码
- LangChain4j - 功能更丰富但依赖更重
- OpenAI Spring AI - 不支持国内模型

### 2. 自然语言转 SQL 实现方案

**Decision**: 使用 Prompt Engineering + Function Calling 方案

**Rationale**:
- 通过精心设计的 Prompt 提供数据库 Schema 上下文
- 大模型生成 SQL 后经过安全校验再执行
- Function Calling 可用于结构化输出和工具调用
- 现有项目已有 MyBatis Plus，可复用数据源配置

**Implementation Approach**:
1. 动态获取数据库表结构信息（表名、字段、类型、注释）
2. 构建包含 Schema 信息和业务规则的系统 Prompt
3. 用户问题 + 上下文历史 → 大模型生成 SQL
4. SQL 安全校验（白名单、关键字过滤、LIMIT强制）
5. 执行 SQL 并格式化结果返回

**Key Components**:
- `DatabaseSchemaService`: 提供数据库元数据
- `SqlGenerationService`: 调用大模型生成 SQL
- `SqlSecurityValidator`: SQL 安全校验器
- `QueryExecutionService`: SQL 执行和结果处理

### 3. SQL 安全策略

**Decision**: 多层安全防护机制

**Rationale**:
- 数据库包含敏感业务数据，必须严格保护
- 大模型生成的 SQL 可能包含危险操作
- 用户要求禁止任何会破坏原有数据的操作

**Security Layers**:
1. **操作白名单**: 只允许 SELECT 语句
2. **关键字黑名单**: 禁止 DELETE/DROP/ALTER/TRUNCATE/INSERT/UPDATE 等
3. **强制 LIMIT**: 自动添加 LIMIT 50（用户已确认）
4. **只读数据源**: 使用只读数据库用户连接
5. **语法校验**: 使用 JSqlParser 解析验证 SQL 结构
6. **审计日志**: 记录所有生成的 SQL 和执行结果

### 4. 对话上下文管理

**Decision**: 基于 Session 的内存缓存 + 数据库持久化

**Rationale**:
- 需要支持基于上下文的连续查询（如"再列出前10条"）
- 查询历史需要保存供用户查看（至少100条）
- 内存缓存保证性能，数据库持久化保证可追溯

**Implementation**:
- 使用 Spring Cache (Redis) 存储当前对话的上下文
- 数据库表 `ai_query_history` 持久化查询历史
- 上下文包含：用户问题、生成的 SQL、执行结果、时间戳
- 每个用户独立的会话隔离

### 5. API 设计方案

**Decision**: RESTful API + WebSocket（可选流式响应）

**Rationale**:
- 标准 REST API 易于前端集成
- 查询可能耗时较长（大模型思考 + 数据库执行）
- WebSocket 可用于流式输出改善用户体验（可选 P2）

**API Endpoints**:
- `POST /api/ai/query`: 发送自然语言查询
- `GET /api/ai/history`: 获取查询历史
- `GET /api/ai/history/{id}`: 获取单条查询详情
- `DELETE /api/ai/history/{id}`: 删除查询历史

### 6. 项目代码组织

**Decision**: 遵循现有 DDD 架构规范

**Rationale**:
- 项目已采用成熟的 DDD 分层架构
- 保持代码风格一致性便于维护
- 复用现有的基础设施（安全、缓存、数据库）

**Module Placement**:
```
src/main/java/xyz/foolcat/eve/evehelper/
├── domain/
│   └── service/ai/          # 领域服务 - AI 核心逻辑
│       ├── SqlGenerationService.java
│       ├── SqlSecurityValidator.java
│       └── QueryHistoryService.java
├── application/
│   └── service/ai/          # 应用服务 - 用例编排
│       └── AiQueryApplicationService.java
├── infrastructure/
│   └── external/ai/         # 基础设施 - AI 客户端
│       ├── SpringAiAlibabaClient.java
│       └── DatabaseSchemaServiceImpl.java
└── interfaces/
    └── web/controller/ai/   # 接口层 - REST API
        └── AiQueryController.java
```

## Key Dependencies Summary

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-ai-alibaba-starter | 1.1.2.0 | AI 大模型集成 |
| mybatis-plus-spring-boot3-starter | 3.5.15 | 数据库访问 |
| spring-boot-starter-data-redis | 3.5.14 | 缓存和会话管理 |
| spring-boot-starter-security | 3.5.14 | 认证授权 |
| jsqlparser | 4.9 | SQL 语法解析和校验 |

## Open Questions Resolved

1. ✅ 数据最大返回条数: 50条（只返回前50条）
2. ✅ 安全限制: 严格只读，禁止所有数据修改操作
3. ✅ 框架选择: Spring AI Alibaba 1.1.2.0
4. ✅ 架构: 遵循现有 DDD 分层
