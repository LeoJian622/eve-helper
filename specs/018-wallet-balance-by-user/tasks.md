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
- [x] 运行全量测试，确保无回归
- [ ] 代码评审（待用户确认）
- **依赖**: T1-T6
- **验收**: 编译通过 ✅，单元测试 11/11 ✅

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
