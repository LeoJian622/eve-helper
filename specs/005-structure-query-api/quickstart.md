# Quickstart: 建筑表只读查询接口

## 前置条件

- MySQL 双库 `eve`(只读静态数据)+ `eve_helper`(运行时数据)可访问
- Redis 运行中
- `application-dev.yml` / `.env.*` 已配置(不入库,参考 `.env.example`)
- 建筑数据已由现有定时任务 `StructTask` 从 ESI 同步入 `structure` 表

## 构建

```bash
mvn clean package -DskipTests
```

## 运行

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 应用: http://localhost:9999
# Swagger UI: http://localhost:9999/swagger-ui.html
# API 文档: http://localhost:9999/v3/api-docs
```

## 测试(L2 TDD:RED -> GREEN -> IMPROVE)

```bash
# 全量
mvn test
# 单类
mvn test -Dtest=StructureQueryApplicationServiceTest
mvn test -Dtest=StructureControllerTest
mvn test -Dtest=StructureRepositoryImplTest
```

测试使用 `@ActiveProfiles("test")` 走 `application-test.yml`。

## 验收清单(对齐 SC-001~010)

- [ ] SC-001:6 端点单次响应 <200ms(SQL EXPLAIN 验证走索引)
- [ ] SC-002:列表 SQL 单次 JOIN 解析类型名/星系名,无逐条回查
- [ ] SC-003:统计概览单 SQL 聚合
- [ ] SC-004:新增代码单元测试覆盖率 ≥80%
- [ ] SC-005:越权场景(非本人军团、他团建筑)100% 覆盖
- [ ] SC-006:排序白名单 + 参数化通过安全评审
- [ ] SC-010:非本人军团访问 100% 拒绝

## 性能优化(索引)

T061 分析发现 `structure` 表仅有 PK(structure_id),**缺 `corporation_id` 索引**。所有军团维度查询(列表/统计/燃料/时间)会全表扫描,影响 SC-001 <200ms。建议在生产数据库执行:

```sql
CREATE INDEX idx_structure_corporation_id ON eve_helper.`structure`(corporation_id);
```

JOIN 表(inv_types PK type_id / universe_name PK id)已走主键索引,无需额外索引。主键查询(详情/服务)走聚簇索引。

> 实际 EXPLAIN 因测试库连接 query 超时未执行,基于 `get_database_object_description` 表结构分析。建议执行后用 `EXPLAIN` 验证 `type=ref/ref_eq`,`key=idx_structure_corporation_id`。

## 接口示例(携带 JWT)

```bash
# 列表
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:9999/structures/98000001?current=1&size=20&sortField=FUEL_EXPIRES&sortOrder=asc"

# 详情
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:9999/structures/98000001/detail/1000001"

# 燃料预警(默认 72h)
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:9999/structures/98000001/fuel?hours=48"

# 服务状态
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:9999/structures/98000001/detail/1000001/services"

# 统计概览
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:9999/structures/98000001/stats"

# 增强/解锚提醒
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:9999/structures/98000001/timers"
```

## 评审门禁(实现后强制)

1. `ecc:java-reviewer` 必审(CRITICAL/HIGH 须修复)
2. `ecc:security-reviewer`(涉及用户输入/IDOR/数据访问)
3. `superpowers:verification-before-completion`(测试/构建证据)
