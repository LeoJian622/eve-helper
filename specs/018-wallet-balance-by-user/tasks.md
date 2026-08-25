# Task List: 018-wallet-balance-by-user

**Feature**: 按用户聚合钱包余额
**Branch**: `018-wallet-balance-by-user`
**Plan**: `specs/018-wallet-balance-by-user/plan.md`

## 任务列表

### T1: 创建领域读模型（VO）
- [x] 创建 `CharacterBalanceVO.java`（domain/model/vo/）
- [x] 创建 `CorporationBalanceVO.java`（domain/model/vo/）
- [x] 创建 `UserWalletBalanceVO.java`（domain/model/vo/）
- **依赖**: 无
- **验收**: 编译通过 ✅

### T2: 创建应用服务
- [x] 创建 `WalletBalanceApplicationService.java`（application/service/）
- [x] 实现 `getUserWalletBalance()` 方法
- [x] 实现人物余额获取逻辑
- [x] 实现军团余额多角色合并逻辑
- **依赖**: T1
- **验收**: 编译通过 ✅

### T3: 创建单元测试
- [x] 创建 `WalletBalanceApplicationServiceTest.java`
- [x] 测试空账户列表场景
- [x] 测试单人物无军团场景
- [x] 测试多人物同军团场景（corpId 去重）
- [x] 测试多角色权限合并场景
- **依赖**: T2
- **验收**: 单元测试全部通过 ✅ (11/11)

### T4: 创建控制器
- [x] 创建 `WalletBalanceController.java`（interfaces/web/controller/）
- [x] 添加 Swagger 注解
- **依赖**: T2
- **验收**: 编译通过 ✅

### T5: 创建集成测试
- [x] 创建 `WalletBalanceControllerIT.java`
- [x] 测试认证校验（无 JWT 返回 401）
- [x] 测试正常查询返回正确结构
- **依赖**: T4
- **验收**: 集成测试代码已创建（需数据库环境验证）

### T6: RBAC 权限配置
- [x] 在 `012_rbac_permissions.sql` 中追加权限项
- [x] 追加角色映射（ADMIN + USER）
- **依赖**: 无
- **验收**: SQL 语法正确 ✅

### T7: 验证与评审
- [x] 运行单元测试，确保无回归
- [x] 代码评审（P6，`ecc:java-reviewer`）
- [x] 修复评审阻塞项 IMPORTANT-1/3/4/6/8
- **依赖**: T1-T6
- **验收**:
  - 编译：IDE `build_project` `isSuccess=true`，零 problems ✅
  - 018 单元测试：13/13 通过（failed 0）✅
  - 017 单元测试：20/20 通过（failed 0，无回归）✅
  - **018 集成测试：3/3 通过（failed 0，exitCode 0）** ✅ —— MySQL/Redis 可达后取得首个有效运行证据
  - **017 集成测试：12/12 通过（failed 0，exitCode 0）** ✅
  - 评审记录：[docs/reviews/2026-08-24-018-java-reviewer.md](../../docs/reviews/2026-08-24-018-java-reviewer.md)
    —— CONDITIONAL APPROVE，无 CRITICAL，8 个 IMPORTANT

### T8: P6 阻塞项修复明细

| 项 | 处置 | 落点 |
|---|------|------|
| IMPORTANT-1 ROOT 军团余额恒为 0 | ✅ 已修 | 017 分支 `f09c451`：余额改用入口已授权人物，删除 `resolveCharIdInCorp` 反查 |
| IMPORTANT-3 division 非法 → 端点 500 | ✅ 已修 | `mergeCorporationBalances` 入表前校验 1..7 + 2 个新测试 |
| IMPORTANT-4 信任边界外移 | ✅ 已修 | `getUserWalletBalance()` 改无参、服务内自取身份 |
| IMPORTANT-6 spec 与 RBAC 口径不符 | ✅ 已修 | spec 表述更正 + SQL Step 5 入事务、补执行后校验与缓存刷新提醒 |
| IMPORTANT-7 IT 无有效运行证据 | ✅ 已解决 | 环境可达后实跑，暴露并修正 3 处错误断言（见下）；018 IT 3/3、017 IT 12/12 |
| IMPORTANT-8 017 混入 018 提交 | ✅ 已修 | 017 拆至独立分支 `017-corp-overview-character-entry` |
| IMPORTANT-2 fail-open 故障不可观测 | ⏳ 延期 | 涉 API 契约变更（VO 增状态位），建议与 spec 修订一并进行。**IT 修复过程中获得真实实例证据**（见下） |
| IMPORTANT-5 ESI/DB 调用放大 | ⏳ 延期 | 部分缓解（017 修复顺带省去一次 authorize/DB）；去重、提前 break、超时、接入 015 缓存待独立立项 |
| IMPORTANT-6 的 IT 门禁用例 | ⏳ 延期 | "不 mock RBAC" 用例需向**远程共享 Redis** 写权限规则，有污染他人环境风险，暂缓 |

### T9: IT 实跑暴露的问题（IMPORTANT-7 闭合过程）

环境（`192.168.12.249` MySQL:3307 / Redis:6379）恢复可达后首次真实运行，3 个用例**全部失败**，
证实评审判断「该覆盖从未被观测过」。三处问题均为**测试断言错误**，生产代码无需改动：

| # | 现象 | 根因 | 处置 |
|---|------|------|------|
| 1 | `$.code` expected `0000000` but was `200` | 断言与实现不符：`ResultCode.SUCCESS` 的 code 是 `"200"`。`spec.md:59` 的响应示例同样写错 | 断言改用 `ResultCode.SUCCESS.getCode()`；spec 示例同步修正 |
| 2 | Status expected `401` but was `403` | `GlobalExceptionHandler:204-205` 把 `ACCESS_UNAUTHORIZED` 映射为 **403**（全项目既有约定）。原断言 401 从一开始就不成立 | 改断言 403 + 错误码 `AUT00301`，并注明本用例实际验证的是**应用服务身份兜底**而非过滤器链（IT 已 mock RBAC 放行） |
| 3 | `characters[0].balance` expected `1000000.0` but was `0.0` | IT 未 mock `AuthorizeUtil`，真实实现按 `(userId, characterId)` 查 `eve_account` 表；测试库无该组合 → 抛「账号不存在」→ **被 fail-open 的 catch 吞成 0.0** | 增加 `@MockitoBean AuthorizeUtil` 并 stub。测试库为远程共享主机，不写入数据 |

两项评审疑虑被实测**推翻**：

- 评审担心 `mockAuthenticated()` 直写 `SecurityContextHolder` 会被 `SecurityContextHolderFilter` 覆盖 →
  **实际有效**，两个已认证用例的 `status().isOk()` 均通过。
- 评审称 401 断言「对任何原因的 401 都成立」→ 实际根本不是 401 而是 403，断言比预想的更无效。

**问题 3 是 IMPORTANT-2 的真实实例证据**：一次归属校验失败（本可暴露测试配置缺陷）被静默降级为「余额 0」，
排查时唯一线索是断言数字不对，异常本身不出现在响应里。这印证了故障不可观测的实际代价。

## 任务依赖图

```
T1 (VO)
 │
 └─→ T2 (Service) ─→ T3 (单元测试)
      │
      └─→ T4 (Controller) ─→ T5 (集成测试)

T6 (RBAC) ──────────────────→ T7 (验证)
```

## 执行顺序

1. T1 → T2 → T3（TDD 先写测试）
2. T4 → T5
3. T6
4. T7（最终验证）
