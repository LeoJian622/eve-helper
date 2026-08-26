# 020 ESI 客户端重复代码消除（Phase 1）

## 特性概述

消除 ESI API 客户端层的重复代码，提升可维护性并减少 ~220 行冗余。采用**最小抽象**策略——工具方法增强，API 类显式调用，不引入继承层次或全局 filter。

**2026-08-26 设计调整**：原计划优化 `.onStatus().onStatus()` 两行，但经实现验证发现 `WebClient.ResponseSpec` 接口无 `as()` 方法，该优化不可行。调整后的范围仅保留 `exchangeToMono` 和常量优化。

## 目标与范围

### Phase 1（本次）

| 重复模式 | 数量 | 处理方式 | 状态 |
|---------|------|---------|------|
| `.onStatus().onStatus()` 两行 | 204+ 处（实际 412 次） | ~~`EsiStatusUtil.applyErrorHandlers(ResponseSpec)`~~ | ❌ **不可行**（ResponseSpec 无 as() 方法） |
| `.exchangeToMono()` 15-20 行块 | 19 处 | `EsiStatusUtil.handleResponseWithHeaders(...)` | ✅ 可行 |
| `ESI_DATA_TIMEOUT` 常量声明 | 20 处 | `EsiClientConstants.ESI_DATA_TIMEOUT` | ✅ 可行 |

### Phase 2（暂缓）

- 缓存编排逻辑（`forceRefreshLimiter.tryAcquire`、`esiCacheTtlExtractor.extract`、`tryClaim/releaseClaim`）——暂不处理，留待后续评估。

### 不处理

- **Auth header 注入（136 处）**：015 特性（ESI 响应缓存）需要区分鉴权/非鉴权请求，保留各 API 类显式注入 `HttpHeaders.AUTHORIZATION`。
- **`.onStatus().onStatus()` 两行（412 处）**：`WebClient.ResponseSpec` 接口无 `as()` 方法，无法用工具方法封装，保持现状。

## 设计方案

### 1. EsiStatusUtil 增强

**位置**：`infrastructure/external/esi/EsiStatusUtil.java`

**新增方法**：

~~```java
// ❌ 已废弃：ResponseSpec 无 as() 方法，此方法不可行
public static ResponseSpec applyErrorHandlers(ResponseSpec spec) {
    return spec
        .onStatus(HttpStatusCode::is4xxClientError, EsiStatusUtil::dataError)
        .onStatus(HttpStatusCode::is5xxServerError, EsiStatusUtil::dataError);
}
```~~

```java
/**
 * 封装 exchangeToMono 块（15-20 行）为单行调用。
 *
 * <p>统一日志格式：{@code "ESI {apiName} 权限不足(403)"} / {@code "ESI {apiName} 服务端错误(5xx): status=XXX"}。</p>
 * <p>不再记录 characterId 等业务上下文（简化日志，换取代码简洁）。</p>
 *
 * @param response WebClient 响应
 * @param typeRef  泛型类型引用（如 {@code new ParameterizedTypeReference<List<WalletJournalResponse>>() {}}）
 * @param apiName  API 名称（用于日志，如 "人物钱包记录"）
 * @return {@code Mono<ResponseEntity<T>>}
 */
public static <T> Mono<ResponseEntity<T>> handleResponseWithHeaders(
        ClientResponse response,
        ParameterizedTypeReference<T> typeRef,
        String apiName) {
    HttpStatusCode status = response.statusCode();
    if (status.is4xxClientError()) {
        if (isForbidden(status)) {
            log.warn("ESI {} 权限不足(403)", apiName);
            return Mono.error(forbiddenAgent());
        }
        return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
    }
    if (status.is5xxServerError()) {
        log.warn("ESI {} 服务端错误(5xx): status={}", apiName, status.value());
        return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
    }
    return response.toEntity(typeRef);
}
```

### 2. EsiClientConstants 新建

**位置**：`infrastructure/external/esi/EsiClientConstants.java`

```java
/**
 * ESI 客户端公共常量。
 */
public final class EsiClientConstants {
    private EsiClientConstants() {
        // 静态工具，禁止实例化
    }

    /**
     * ESI 数据接口调用超时（宪法：外部调用 5s 超时）。
     */
    public static final Duration ESI_DATA_TIMEOUT = Duration.ofSeconds(5);
}
```

### 3. API 类改造

**改造规则**：

1. ~~**onStatus 模式**~~：**❌ 已废弃**（ResponseSpec 无 as() 方法，保持现状）

2. **exchangeToMono 模式**：
   ```java
   // Before (15-20 行)
   .exchangeToMono(response -> {
       HttpStatusCode httpStatusCode = response.statusCode();
       if (httpStatusCode.is4xxClientError()) {
           if (EsiStatusUtil.isForbidden(httpStatusCode)) {
               log.warn("ESI xxx权限不足(403): characterId={}", characterId);
               return Mono.error(EsiStatusUtil.forbiddenAgent());
           }
           return Mono.error(new EsiException(ResultCode.ESI_AUTHORIZATION_FAILURE));
       }
       if (httpStatusCode.is5xxServerError()) {
           log.warn("ESI xxx服务端错误(5xx): characterId={}, status={}", characterId, httpStatusCode.value());
           return Mono.error(new EsiException(ResultCode.ESI_SERVER_FAILURE));
       }
       return response.toEntity(new ParameterizedTypeReference<List<XxxResponse>>() {});
   }).timeout(ESI_DATA_TIMEOUT);

   // After (1 行)
   .exchangeToMono(response -> EsiStatusUtil.handleResponseWithHeaders(
       response, new ParameterizedTypeReference<List<XxxResponse>>() {}, "xxx"))
   .timeout(EsiClientConstants.ESI_DATA_TIMEOUT);
   ```

3. **常量替换**：
   ```java
   // Before
   private static final Duration ESI_DATA_TIMEOUT = Duration.ofSeconds(5);

   // After（删除本地声明，引用公共常量）
   import static xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClientConstants.ESI_DATA_TIMEOUT;
   ```

**涉及文件**：
- `infrastructure/external/esi/api/*.java`（33 个 API 类）
- `infrastructure/gateway/esi/EsiApiService.java`（如声明了 `ESI_DATA_TIMEOUT` 也需替换）

## 约束与决策

### 约束

1. **行为不变**：重构仅消除重复，不改变任何外部行为（HTTP 调用、错误处理、日志语义除外）。
2. **不加锁**：token 刷新逻辑不加锁（上一会话决策）。
3. **不移回 SDK 包**：`EsiApiService` 保持在 `infrastructure/gateway/esi/`，不回退到 `infrastructure/external/esi/`。
4. **日志简化**：`exchangeToMono` 块的日志从详细（含 characterId）简化为统一格式（仅 apiName + status）。

### 决策记录

| 决策 | 理由 |
|------|------|
| 工具方法增强（非基类/全局 filter） | 用户偏好最小抽象，风险低，调试栈清晰 |
| Phase 2 缓存编排暂缓 | 范围控制，先解决核心重复 |
| Auth header 注入保留现状 | 015 缓存需区分鉴权/非鉴权请求 |
| 日志简化（不记录 characterId） | 换取代码简洁，调试可通过 trace ID 追踪 |

## 预期收益（调整后）

| 指标 | Before | After | 净收益 | 状态 |
|------|--------|-------|--------|------|
| onStatus 重复 | ~408 行 | ~408 行 | 0 行 | ❌ 不可行（ResponseSpec 无 as()） |
| exchangeToMono 重复 | ~304 行 | ~100 行 | -204 行 | ✅ 可行 |
| 常量声明重复 | 20 处 | 20 处单行引用 | -20 行 | ✅ 可行 |
| **总计** | **~712 行重复** | **~492 行** | **-220 行** | 原计划 -552 行，调整为 -220 行 |

## 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| 工具方法引入新 bug | TDD 先行，为 `handleResponseWithHeaders` 编写单元测试 |
| 日志简化影响调试 | 保留 trace ID（Spring 默认 MDC），可通过 trace ID 追踪完整调用链 |
| API 类改造遗漏 | 编译期检查 + 全量 grep 验证无残留 `exchangeToMono` 块 |
| 设计决策错误（已发生） | 实现前验证 API 可用性（ResponseSpec.as() 不存在导致方案调整） |

## 验收标准（调整后）

1. ~~✅ 所有 `.onStatus()` 被替换~~ **已废弃**（ResponseSpec 无 as() 方法）
2. ✅ 所有 19 处 `exchangeToMono` 块被替换为 `EsiStatusUtil.handleResponseWithHeaders(...)`
3. ✅ 所有 20 处 `private static final Duration ESI_DATA_TIMEOUT` 被替换为 `EsiClientConstants.ESI_DATA_TIMEOUT`
4. ✅ 构建通过：`mvn clean package -DskipTests` exit 0
5. ✅ 测试通过：`mvn test` 0 failures, 0 errors
6. ✅ 代码评审：无 Critical/Important 未修复项
