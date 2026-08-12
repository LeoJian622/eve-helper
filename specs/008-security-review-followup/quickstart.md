# Quickstart / 验证手册: 安全评审遗留修复(008)

> 实现完成后的端到端验证步骤。测试基线规则(memory):SpringBootTest 依赖 MySQL/Redis,环境性失败逐用例 diff,不用固定阈值。

## 1. 单元测试

```bash
mvn test -Dtest=SecurityContextTaskDecoratorTest      # US1 装饰器(含同线程直通)
mvn test -Dtest=SensitiveDataMaskerTest               # US3 CRLF 免疫
mvn test -Dtest=KeyStoreKeyFactoryTest                # US3 LOW-1j~4j
mvn test -Dtest=ResponseUtilsTest                     # US3 响应头 + 编码一致
```

## 2. US2 登录枚举成对比对(SC-001,需 MySQL/Redis)

```bash
mvn test -Dtest=AuthenticationFailureHandlerEnumerationTest
```

手工验证(应用启动后):

```bash
# 已注册账号 + 错误口令
curl -s -o /tmp/a.json -w '%{http_code}\n' -X POST 'http://localhost:9999/login' \
  -d 'username=<已知账号>&password=wrongpass1'
# 未注册账号 + 任意口令
curl -s -o /tmp/b.json -w '%{http_code}\n' -X POST 'http://localhost:9999/login' \
  -d 'username=no_such_user_008&password=whatever1'
diff /tmp/a.json /tmp/b.json && echo 'SC-001 PASS: 响应逐字节一致'
# 断言:两响应均为 401,msg 均为「用户名或密码错误」,无剩余次数、无「账号不存在」
```

## 3. US1 线程复用零残留(SC-002)

```bash
mvn test -Dtest=ThreadPoolIdentityIsolationTest
```

测试语义:① 用户 A 身份任务先入池 → ② 同一工作线程执行无身份任务,断言观察到空身份;③ 任务结束后断言工作线程上下文为空;④ CallerRuns 路径断言请求线程上下文不被抹除。

## 4. US3 逐项核对(SC-003)

| 项 | 验证 |
|----|------|
| FR-007 | `TokenResponseHeadersContractTest`(活路径契约,v2 R6)+ 手工:`curl -sD - -o /dev/null -X POST http://localhost:9999/auth/tokens -H 'Content-Type: application/json' -d '{"refreshToken":"<合法token>"}'` → 无 `Access-Control-Allow-Origin`,`Cache-Control: no-store`;`grep -rn writeTokenInfo src/` 为空(死方法已删) |
| FR-008 边界 | 10KB refreshToken → DTO 层 4xx;合法 UUID 不误伤 |
| FR-008 日志 | 构造含 `\r\n` 的输入:① 服务层路径经 `maskToken` 无伪造行(`SensitiveDataMaskerTest`);② **边界拒绝路径**日志无 rejected value 原文、无伪造行(`GlobalExceptionHandlerLogTest`,v2 R9);③ 登录路径 username 日志经 `maskUsername`(`LoginRateLimiterService`,v2 R5) |
| FR-009 | `git log -1 --name-status` 见 `D src/main/resources/public.key`;`grep -r "public.key" src/ pom.xml` 为空 |
| FR-010 | `grep -n "redis-cli .* -a" docs/DEPLOYMENT.md` 为空(命令块均走 `REDISCLI_AUTH`) |
| FR-011 | `grep -n "getKeyPair(" src/main/java --include=*.java -r` 仅剩双参定义与调用;位数文案测试随常量通过 |
| FR-012 | `ResponseUtilsTest` 编码断言通过(`writeTokenInfo` 已删,锚点为 `writeErrorInfo` 与全仓响应写出风格一致,research R7) |

## 5. 全量回归(SC-004)与覆盖率(SC-005)

```bash
mvn test                    # 与基线逐用例 diff(Failures/Errors 分段,环境噪声不计)
```

覆盖率:变更文件(`SecurityContextTaskDecorator`/`AuthenticationFailureServletHandler`/`ResponseUtils`/`SensitiveDataMasker`/`KeyStoreKeyFactory`/`RefreshTokenRequest`/`SecurityConfig`/`AsyncConfiguration`/`GlobalExceptionHandler`/`LoginRateLimiterService`)行覆盖率 ≥80%。

## 6. 独立安全复审(SC-006)

`ecc:java-reviewer` 必审 + `ecc:security-reviewer`(涉认证/用户输入)复审,零 CRITICAL/HIGH,记录归档 `docs/reviews/`。

## 已知环境前置

- MySQL 两库(eve / eve_helper)与 Redis 可达,否则 SpringBootTest 上下文加载失败(非代码回归,memory:springboot-test-db-dependency)
- `application-test.yml` 由用户管理,不读不改
