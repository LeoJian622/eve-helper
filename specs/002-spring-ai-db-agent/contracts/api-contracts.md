# API Contracts: Spring AI 数据库查询 Agent

**Feature**: 002-spring-ai-db-agent  
**Date**: 2026-05-19

## Base URL

```
/api/ai
```

## Authentication

All endpoints require JWT authentication.

**Header**:
```
Authorization: Bearer <jwt-token>
```

## Endpoints

### 1. Execute AI Query

执行自然语言查询，返回 SQL 和查询结果。

**POST** `/query`

**Request Body**:

```json
{
  "question": "查询所有价格超过100万ISK的市场订单",
  "sessionId": "abc123xyz",
  "includeSchema": false
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| question | String | ✅ | 用户自然语言问题 |
| sessionId | String | ❌ | 会话ID，用于上下文连续查询。不填则新建会话。 |
| includeSchema | Boolean | ❌ | 是否在结果中包含Schema说明，默认 false |

**Response (200 OK)**:

```json
{
  "queryId": 12345,
  "sessionId": "abc123xyz",
  "userQuestion": "查询所有价格超过100万ISK的市场订单",
  "generatedSql": "SELECT * FROM market_order WHERE price > 1000000 LIMIT 50",
  "results": [
    {"id": 1, "typeId": 34, "price": 1200000, "volume": 100, "stationId": 60000001},
    {"id": 2, "typeId": 35, "price": 1500000, "volume": 50, "stationId": 60000001}
  ],
  "resultCount": 2,
  "executionTime": 2345,
  "success": true,
  "errorMessage": null,
  "timestamp": "2026-05-19T10:30:00Z"
}
```

**Response (400 Bad Request - SQL Validation Failed)**:

```json
{
  "success": false,
  "errorMessage": "生成的SQL包含禁止的操作: DELETE",
  "generatedSql": "DELETE FROM market_order WHERE id = 1",
  "executionTime": 0
}
```

---

### 2. Get Query History

获取当前用户的查询历史列表。

**GET** `/history`

**Query Parameters**:

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| limit | Integer | 100 | 返回记录条数上限 |
| page | Integer | 1 | 页码 |

**Response (200 OK)**:

```json
{
  "data": [
    {
      "id": 12345,
      "userQuestion": "查询所有价格超过100万ISK的市场订单",
      "generatedSql": "SELECT * FROM market_order WHERE price > 1000000 LIMIT 50",
      "resultCount": 2,
      "executionTime": 2345,
      "success": true,
      "createdAt": "2026-05-19T10:30:00Z"
    }
  ],
  "total": 15,
  "page": 1,
  "limit": 100
}
```

---

### 3. Get Single Query Detail

获取单条查询的详细信息。

**GET** `/history/{id}`

**Path Parameters**:

| Param | Type | Description |
|-------|------|-------------|
| id | Long | 查询记录 ID |

**Response (200 OK)**:

```json
{
  "id": 12345,
  "userId": 1,
  "sessionId": "abc123xyz",
  "userQuestion": "查询所有价格超过100万ISK的市场订单",
  "generatedSql": "SELECT * FROM market_order WHERE price > 1000000 LIMIT 50",
  "results": [
    {"id": 1, "typeId": 34, "price": 1200000, "volume": 100, "stationId": 60000001}
  ],
  "resultCount": 2,
  "executionTime": 2345,
  "success": true,
  "errorMessage": null,
  "createdAt": "2026-05-19T10:30:00Z"
}
```

**Response (404 Not Found)**:

```json
{
  "error": "Query history not found",
  "queryId": 99999
}
```

---

### 4. Delete Query History

删除一条查询历史记录。

**DELETE** `/history/{id}`

**Path Parameters**:

| Param | Type | Description |
|-------|------|-------------|
| id | Long | 查询记录 ID |

**Response (204 No Content)**:

```
(Empty body)
```

**Response (404 Not Found)**:

```json
{
  "error": "Query history not found",
  "queryId": 99999
}
```

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | INVALID_QUESTION | 问题无法理解或为空 |
| 400 | SQL_VALIDATION_FAILED | 生成的SQL未通过安全校验 |
| 400 | SQL_EXECUTION_ERROR | SQL执行失败（语法错误、连接问题等） |
| 401 | UNAUTHORIZED | 未认证或Token无效 |
| 403 | FORBIDDEN | 无权限访问该功能 |
| 404 | NOT_FOUND | 查询记录不存在 |
| 500 | AI_SERVICE_ERROR | AI服务调用失败 |
| 500 | DATABASE_ERROR | 数据库连接或查询错误 |
| 504 | TIMEOUT | 查询超时（默认10秒） |

## Common Error Response Format

```json
{
  "success": false,
  "errorCode": "SQL_VALIDATION_FAILED",
  "errorMessage": "生成的SQL包含禁止的关键字: DROP",
  "details": {
    "generatedSql": "DROP TABLE users",
    "validationErrors": ["Forbidden keyword: DROP"]
  }
}
```

## Rate Limiting

- **Max requests per minute**: 30 requests per user
- **Max concurrent queries**: 2 per user
- **Query timeout**: 10 seconds

Rate limit headers:
```
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 28
X-RateLimit-Reset: 1684483200
```
