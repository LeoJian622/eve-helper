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
  - 集成测试：3/3 Errors —— 本机 MySQL 不可达导致 ApplicationContext 加载失败，
    属既有环境限制而非代码回归，**该覆盖尚未被任何运行记录证实**（IMPORTANT-7）
  - 评审记录：[docs/reviews/2026-08-24-018-java-reviewer.md](../../docs/reviews/2026-08-24-018-java-reviewer.md)
    —— CONDITIONAL APPROVE，无 CRITICAL，8 个 IMPORTANT

### T8: P6 阻塞项修复明细

| 项 | 处置 | 落点 |
|---|------|------|
| IMPORTANT-1 ROOT 军团余额恒为 0 | ✅ 已修 | 017 分支 `f09c451`：余额改用入口已授权人物，删除 `resolveCharIdInCorp` 反查 |
| IMPORTANT-3 division 非法 → 端点 500 | ✅ 已修 | `mergeCorporationBalances` 入表前校验 1..7 + 2 个新测试 |
| IMPORTANT-4 信任边界外移 | ✅ 已修 | `getUserWalletBalance()` 改无参、服务内自取身份 |
| IMPORTANT-6 spec 与 RBAC 口径不符 | ✅ 已修 | spec 表述更正 + SQL Step 5 入事务、补执行后校验与缓存刷新提醒 |
| IMPORTANT-8 017 混入 018 提交 | ✅ 已修 | 017 拆至独立分支 `017-corp-overview-character-entry` |
| IMPORTANT-2 fail-open 故障不可观测 | ⏳ 延期 | 涉 API 契约变更（VO 增状态位），建议与 spec 修订一并进行 |
| IMPORTANT-5 ESI/DB 调用放大 | ⏳ 延期 | 部分缓解（017 修复顺带省去一次 authorize/DB）；去重、提前 break、超时、接入 015 缓存待独立立项 |
| IMPORTANT-7 IT 无有效运行证据 | ⏳ 环境阻塞 | 需 MySQL/Redis 可达环境重跑；建议引入 Testcontainers 或 failsafe 独立 profile |
| IMPORTANT-6 的 IT 门禁用例 | ⏳ 延期 | "不 mock RBAC" 用例需 Redis 环境，与 IMPORTANT-7 一并处理 |

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
