# VERIFICATION REPORT: 021-esi-auth-scopes

**Date**: 2026-09-02 | **Branch**: `021-esi-auth-scopes` | **HEAD**: `a24484b` | **P5 验证（证据铁律执行区）**

## 结论：**READY**（021 引入的失败/错误 = 0；所有验证为 2026-09-01/02 当前轮次真实输出）

## 逐项验证

### 1. 构建 ✅
- `mcp__idea__build_project`：`isSuccess=true`，零 error（多次，最近一次含 Minor 修复后）
- 命令行 `mvn test`（IDEA 捆绑 maven3 + graalvm-jdk-17.0.12）：全量编译成功执行 150 测试类

### 2. 测试（021 相关全绿）
| 范围 | 结果 | 通道 |
|------|------|------|
| EsiApiServiceTest 新增 8 用例（A1-A3 + B1-B5） | **8/8 通过**（逐用例运行 + 整类 76 中仅基线 9 失败） | IDEA MCP |
| UserApplicationServiceUnitTest | **21/21 通过**（19 既有 + 4 新） | IDEA MCP |
| UserApplicationServiceTest | **3/3 通过**（2 既有 + 1 新） | IDEA MCP |
| UserControllerAuthStatusIT | **3/3 通过**（T014 扩展 + FR-005 新用例） | IDEA MCP |
| 突变检验 | B1 去短路→红；B4 异常逃逸→红；还原后零残留（git diff 生产 0 行） | IDEA MCP |

### 3. 全量回归与逐用例归因（T012）
`mvn test`（命令行，150 类）：**TOTAL=875 / F18 / E47 / S31**

E47/S31 与记忆基线（2026-08-27）**数字级一致**。F18 逐用例归因：

| 构成 | 数量 | 归因 | 证据 |
|------|------|------|------|
| EsiApiServiceTest | 9 | 既有漂移（08-30 安全加固：compareAndDelete 锁释放/015-H2 键规范化/旧签名 stub） | T001 基线（HEAD=4d22499 仅含骨架）已存在，逐名一致 |
| EsiApiServiceUnitTest(1) + EsiApiServiceAccessTokenTest(3) + AssetsServiceTest(1) + AssetsAggregateRepositoryTest(3) + AssetsWriteUserIdTest(1E) + IndustryJobServiceBatchSyncTest(1E) | 7 | 既有漂移 | **基线 worktree（dc5b772）对照复跑，失败名单逐用例一致** |
| WalletBalanceControllerIT | 3 | **脏 target 伪回归**（target/test-classes 残留历史版本 .class：旧用例名 unauthenticated_returns401 + 硬编码 0000000，源码实际为 unauthenticated_returnsForbidden + ResultCode.SUCCESS） | 源码 dc5b772..HEAD 零 diff + IDEA MCP 单类复跑 3/3 绿 |
| **021 引入** | **0** | — | — |

> 伪回归根因：Maven 增量编译未清除改名前的陈旧 .class。建议后续（缺陷轨候选）：全量回归前 `mvn clean`，或为 401→Forbidden 类历史改名残留做一次 target 清理。

### 4. 覆盖率（T013）——豁免记录
- pom 无 jacoco/surefire 覆盖率插件（技术栈冻结，引入需宪法修订流程）
- 定性核验：`toScopeList` 4 分支（List 数组/空串/空格串/未知类型）+ `parseScopesFromCachedToken` 5 路径（守卫/命中/未命中/损坏/缺 claim）+ resolveDetail 4 异常路径 + DTO 传播/IT 端到端——14 个新用例覆盖全部可达分支，无未覆盖分支

### 5. 安全自查（T015）
- 响应无凭据：`UserAccountDTO` 无 token/签名字段（grep 核验）
- 日志无 token：`EsiApiService` 全部 token 相关日志（7 处）均为固定文案 + userId/characterId；scopes 解析 catch 日志不含异常对象
- 访问控制不变：`requireSelfOrRoot` 未触碰；既有 AccessGuard 单测/IT 锚定
- 两次独立评审代理复核通过（T002-T003、T006-T007 均 APPROVE；T009-T011 测试评审 APPROVE）

### 6. Diff 审查
- 生产改动 6 文件（assembler 重命名/DTO 字段/服务切换/VO/端口/EsiApiService 新增方法），无计划外变更
- 安全敏感区零侵入（getAuthorizationStatus/determineAuthStatus/刷新锁/persistRefreshedToken 逐 hunk 核验）

## 已知遗留（非本特性范围，缺陷轨候选）
1. EsiApiServiceTest 9 个既有失败 + 同族 8 个（EsiApiServiceUnitTest/AceessTokenTest/Assets 族）——08-30 安全加固后测试未跟上，建议立项修复
2. target/test-classes 陈旧 .class 问题——建议 CI/本地全量回归前 mvn clean
3. 测试方法名前缀不一致 Minor（queryAccounts_* vs detail_*）——低优先级
