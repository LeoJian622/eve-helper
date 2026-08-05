# Quickstart: 用户账户 ESI 授权状态返回

**Date**: 2026-08-04 | **Feature**: [spec.md](spec.md)

> Phase 1 产物:本地构建、运行与验证步骤。

## 前置依赖

- JDK 17
- Maven
- MySQL(eve 与 eve_helper 两个库可访问)
- Redis 运行中
- ESI 配置(client_id 等,见 `.env.example` / `application-dev.yml`)

## 构建

```bash
mvn clean package
```

## 运行

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 应用: http://localhost:9999
# Swagger: http://localhost:9999/swagger-ui.html
```

## 测试

```bash
# 全量测试
mvn test

# 仅本功能单元测试
mvn test -Dtest=EsiApiServiceTest,UserApplicationServiceTest

# 集成测试
mvn test -Dtest=UserControllerAuthStatusIT
```

## 手动验证

1. **获取 JWT**: 通过既有登录接口获取 access token。
2. **调用接口**:
   ```bash
   curl -H "Authorization: Bearer <access_token>" \
        http://localhost:9999/user/<userId>
   ```
3. **验证响应**: 每个角色条目应含 `authStatus` 字段,取值为 `AUTHORIZED`/`EXPIRED`/`NOT_AUTHORIZED`/`UNKNOWN` 之一。

## 验证矩阵

| 场景 | 预期 authStatus | 验证方式 |
|------|-----------------|----------|
| 角色有 refreshToken 且 ESI 刷新成功 | `AUTHORIZED` | 单元测试 Mock `updateAccessToken` 返回 token |
| 角色有 refreshToken,ESI 返回 4xx | `EXPIRED` | 单元测试 Mock 抛 `EsiException(ESI_AUTHORIZATION_FAILURE)` |
| 角色无 refreshToken | `NOT_AUTHORIZED` | 单元测试构造 `EveAccount.refreshToken=null` |
| ESI 返回 5xx / 网络异常 / 超时 | `UNKNOWN` | 单元测试 Mock 抛 `EsiException(ESI_SERVER_FAILURE)` 与超时 |
| 多角色,其中 1 个判定异常 | 其他角色正常,异常角色 `UNKNOWN` | 集成测试 |
| 用户无绑定角色 | `data=[]`,成功 | 集成测试 |
| 5min 内重复调用同角色 | 返回缓存态(无 ESI 调用) | 单元测试验证 Redis 被命中 |

## 覆盖率

```bash
mvn test
# 目标:新增代码单元覆盖 ≥80%
```

## 完成判定(verification-before-completion)

- [ ] `mvn test` 全绿
- [ ] 新增代码覆盖率 ≥80%
- [ ] `ecc:java-reviewer` 无 CRITICAL/HIGH
- [ ] `ecc:security-reviewer` 无 CRITICAL/HIGH(涉及认证/外部 API)
- [ ] 手动调用接口验证 `authStatus` 字段存在且取值合法
