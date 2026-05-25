# Quickstart: Spring AI 数据库查询 Agent

**Feature**: 002-spring-ai-db-agent  
**Date**: 2026-05-19

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+ (with `eve` and `eve_helper` databases)
- Redis 6.0+
- Alibaba Cloud API Key (for Tongyi Qianwen)

## Quick Start Guide

### 1. Configure Environment Variables

Create `.env` file in project root:

```bash
# AI Configuration
SPRING_AI_ALIBABA_API_KEY=your-alibaba-cloud-api-key
SPRING_AI_ALIBABA_CHAT_MODEL=qwen-max

# Database (reuse existing eve_helper datasource)
DB_READONLY_USER=readonly_user
DB_READONLY_PASSWORD=readonly_password
```

### 2. Create Database Table

Execute the following SQL in `eve_helper` database:

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

### 3. Add Maven Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.1.2.0</version>
</dependency>
<dependency>
    <groupId>com.github.jsqlparser</groupId>
    <artifactId>jsqlparser</artifactId>
    <version>4.9</version>
</dependency>
```

### 4. Configure Application

Add to `application-dev.yml`:

```yaml
spring:
  ai:
    alibaba:
      api-key: ${SPRING_AI_ALIBABA_API_KEY}
      chat:
        options:
          model: ${SPRING_AI_ALIBABA_CHAT_MODEL:qwen-max}
          temperature: 0.1
          max-tokens: 2000

  # AI Query Configuration
  ai-query:
    max-result-limit: 50
    query-timeout-ms: 10000
    session-ttl-minutes: 30
    allowed-tables:
      - market_order
      - assets
      - industry_blueprints
      - character
      - corporation

# Security Configuration
security:
  ai-query:
    require-auth: true
    allowed-roles:
      - USER
      - ADMIN
```

### 5. Run the Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 6. Test the API

**Execute a Query**:

```bash
curl -X POST http://localhost:9999/api/ai/query \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"question": "查询价格超过100万ISK的市场订单，列出前10条"}'
```

**Get Query History**:

```bash
curl http://localhost:9999/api/ai/history?limit=10 \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Development Workflow

### Local Development

1. Start MySQL and Redis locally
2. Configure environment variables
3. Run `mvn spring-boot:run`
4. Access Swagger UI: http://localhost:9999/swagger-ui.html
5. Test endpoints using Swagger or curl

### Testing Strategy

```bash
# Run unit tests
mvn test -Dtest=*Ai*Test

# Run integration tests (requires database)
mvn test -Dtest=*Ai*IntegrationTest

# Run specific test
mvn test -Dtest=SqlSecurityValidatorTest
```

## Key Configuration Reference

| Configuration | Default | Description |
|---------------|---------|-------------|
| spring.ai.alibaba.api-key | (required) | 阿里云 API Key |
| spring.ai.alibaba.chat.options.model | qwen-max | 大模型名称 |
| spring.ai.alibaba.chat.options.temperature | 0.1 | 温度参数，越低越精确 |
| ai-query.max-result-limit | 50 | 最大返回条数 |
| ai-query.query-timeout-ms | 10000 | 查询超时时间(毫秒) |
| ai-query.session-ttl-minutes | 30 | 会话缓存过期时间(分钟) |

## Troubleshooting

### Common Issues

1. **AI service connection failed**
   - Check API key validity
   - Verify network connectivity to Alibaba Cloud
   - Check API quota and billing status

2. **SQL validation always fails**
   - Review allowed-tables configuration
   - Check if the query uses forbidden keywords
   - Enable DEBUG logging for `SqlSecurityValidator`

3. **Query returns empty results**
   - Verify the database has data
   - Check if the AI generated correct SQL
   - Review the SQL manually in a database client

### Enable Debug Logging

```yaml
logging:
  level:
    xyz.foolcat.eve.evehelper.domain.service.ai: DEBUG
    com.alibaba.cloud.ai: DEBUG
```
