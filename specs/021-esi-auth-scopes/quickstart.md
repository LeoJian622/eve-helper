# Quickstart: 021-esi-auth-scopes

**Date**: 2026-09-01 | 本特性如何构建、测试与手动验证

## 前置条件

- MySQL（eve / eve_helper 两库）与 Redis 可达（测试走 `application-test.yml`,连接内网 192.168.12.249）
- 分支 `021-esi-auth-scopes`

## 构建与测试

> 依据仓库实际通道：`mvnw` 不稳定,**首选 IDEA MCP**;`mvn` 可用时备选。

```bash
# 构建（备选）
mvn -q clean package -DskipTests

# 测试（备选;需 DB/Redis 可达）
mvn test -Dtest=EsiApiServiceTest
mvn test -Dtest=UserApplicationServiceUnitTest,UserApplicationServiceTest
mvn test -Dtest=UserControllerAuthStatusIT
```

IDEA MCP 通道：`build_project` 编译;运行配置执行上述测试类,数 teamcity 事件统计 results（回归逐用例 diff,分离 Failures/Errors）。

## 手动验证（实现完成后）

1. 启动应用（需一个 AUTHORIZED 角色,Redis 中已有其 accessToken）：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

2. 以本人（或 ROOT 查任意用户）调用：

```bash
curl -H "Authorization: Bearer <系统JWT>" http://localhost:9999/user/1
```

3. 检查：
   - AUTHORIZED 角色 `scopes` 为非 null 数组,内容与该角色实际授予的 ESI 权限一致
   - 将该角色 refreshToken 置空或用已废 token 触发 EXPIRED 后再查 → `scopes` 为 `null` 且 `authStatus` 正确
   - 响应中任何位置不出现 accessToken/refreshToken 值

4. **真实 token 格式实测**（P4 实现期验证动作）：对晨曦真实 AUTHORIZED 角色调用本接口,确认 `scp` 解析结果与 ESI 开发者站看到的权限一致（验证 R1 双格式假设在国服成立）。

## 关键文件

| 文件 | 角色 |
|------|------|
| `specs/021-esi-auth-scopes/spec.md` | 规格（G1 已批准） |
| `specs/021-esi-auth-scopes/plan.md` | 本特性实现计划 |
| `src/main/java/.../domain/model/vo/EsiAuthorizationDetail.java` | 新增读模型 |
| `src/main/java/.../infrastructure/gateway/esi/EsiApiService.java` | scopes 解析实现 |
| `src/main/java/.../application/service/UserApplicationService.java` | 用例编排切换 |
