# Data Model: Spring AI 数据库查询 Agent

**Feature**: 002-spring-ai-db-agent  
**Date**: 2026-05-19

## Domain Entities

### 1. AiQueryHistory

存储用户的 AI 查询历史记录。

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| id | Long | 主键 ID | AUTO_INCREMENT, PRIMARY KEY |
| userId | Integer | 用户 ID | NOT NULL, INDEX |
| userQuestion | String | 用户自然语言问题 | NOT NULL, TEXT |
| generatedSql | String | AI 生成的 SQL 语句 | NOT NULL, TEXT |
| resultCount | Integer | 返回结果数量 | |
| executionTime | Long | 执行耗时 (毫秒) | |
| success | Boolean | 是否执行成功 | NOT NULL, DEFAULT true |
| errorMessage | String | 错误信息 | |
| sessionId | String | 会话 ID | INDEX |
| createdAt | Instant | 创建时间 | NOT NULL |
| updatedAt | Instant | 更新时间 | |

**Relationships**:
- 多对一: 多个查询记录属于一个用户 (userId → SysUser.id)

**Validation Rules**:
- userQuestion 不能为空
- generatedSql 不能为空且必须通过安全校验
- 每个用户可查询最近 100 条记录

### 2. DatabaseSchemaContext

数据库 Schema 上下文信息（运行时动态获取，不持久化）

| Field | Type | Description |
|-------|------|-------------|
| tableName | String | 表名 |
| tableComment | String | 表注释 |
| columns | List&lt;ColumnInfo&gt; | 字段列表 |
| foreignKeys | List&lt;ForeignKey&gt; | 外键关系 |

**ColumnInfo**:
- columnName: 字段名
- columnType: 字段类型
- columnComment: 字段注释
- isNullable: 是否可为空
- isPrimaryKey: 是否主键

### 3. AiConversationContext

会话上下文（Redis 缓存存储）

| Field | Type | Description | TTL |
|-------|------|-------------|-----|
| sessionId | String | 会话 ID | |
| userId | Integer | 用户 ID | |
| messages | List&lt;ChatMessage&gt; | 对话消息历史 | 30 minutes |
| lastQueryId | Long | 最后一次查询 ID | |

## Database Table Schema

### Table: ai_query_history

```sql
CREATE TABLE `ai_query_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `user_question` TEXT NOT NULL COMMENT '用户自然语言问题',
  `generated_sql` TEXT NOT NULL COMMENT 'AI生成的SQL语句',
  `result_count` INT DEFAULT 0 COMMENT '返回结果数量',
  `execution_time` BIGINT DEFAULT 0 COMMENT '执行耗时(毫秒)',
  `success` TINYINT(1) DEFAULT 1 COMMENT '是否执行成功',
  `error_message` TEXT COMMENT '错误信息',
  `session_id` VARCHAR(64) COMMENT '会话ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_session_id` (`session_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI查询历史记录表';
```

## Data Access Contracts

### Repository Interfaces

```java
// AiQueryHistoryRepository
public interface AiQueryHistoryRepository {
    AiQueryHistory save(AiQueryHistory history);
    List<AiQueryHistory> findByUserId(Integer userId, int limit);
    AiQueryHistory findById(Long id);
    void deleteById(Long id);
}
```

## Security Model

### SqlSecurityPolicy

SQL 安全策略配置（可通过配置文件或数据库管理）

| Rule | Description |
|------|-------------|
| ALLOWED_KEYWORDS | SELECT, FROM, WHERE, JOIN, LEFT JOIN, INNER JOIN, ON, AND, OR, ORDER BY, GROUP BY, HAVING, LIMIT, AS, DISTINCT, COUNT, SUM, AVG, MAX, MIN |
| FORBIDDEN_KEYWORDS | DELETE, DROP, ALTER, TRUNCATE, INSERT, UPDATE, CREATE, REPLACE, GRANT, REVOKE, EXEC, EXECUTE, CALL |
| MAX_RESULT_LIMIT | 50 |
| ALLOWED_TABLES | (可配置) 允许查询的表白名单 |

## Validation Rules

### SQL Validation Pipeline

1. **Syntax Check**: 使用 JSqlParser 解析验证语法正确性
2. **Keyword Check**: 扫描是否包含禁止关键字
3. **Statement Type Check**: 确保只允许 SELECT 语句
4. **Limit Check**: 确保包含 LIMIT 50（或自动添加）
5. **Table Whitelist Check**: 确保查询的表在白名单内
6. **Subquery Depth Check**: 限制子查询嵌套深度

## Caching Strategy

| Cache Name | Key Pattern | TTL | Description |
|------------|-------------|-----|-------------|
| ai_conversation | `ai:conversation:{sessionId}` | 30 min | 会话上下文 |
| db_schema | `ai:schema:metadata` | 24 h | 数据库 Schema 元数据 |
| sql_validation | `ai:validation:{sql-hash}` | 1 h | 已验证 SQL 缓存 |

## DTO Models

### Request DTOs

```java
// AiQueryRequest
public class AiQueryRequest {
    private String question;          // 用户问题
    private String sessionId;         // 会话ID（可选，新建会话不填）
    private boolean includeSchema;    // 是否包含Schema说明
}
```

### Response DTOs

```java
// AiQueryResponse
public class AiQueryResponse {
    private Long queryId;
    private String sessionId;
    private String userQuestion;
    private String generatedSql;
    private List<Map<String, Object>> results;  // 最多50条
    private int resultCount;
    private long executionTime;
    private boolean success;
    private String errorMessage;
    private Instant timestamp;
}

// QueryHistoryResponse
public class QueryHistoryResponse {
    private Long id;
    private String userQuestion;
    private String generatedSql;
    private int resultCount;
    private long executionTime;
    private boolean success;
    private Instant createdAt;
}
```
