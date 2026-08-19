# 013 — ESI 人物授权基准与 403 友好返回 · 设计安全评审记录

- **特性**：013 (`feat/013-esi-char-auth-403`)
- **评审阶段**：P2 设计规划（G2 前）
- **评审人**：`ecc:security-reviewer` 子代理
- **日期**：2026-08-19
- **评审对象**：`specs/013-esi-char-auth-403/spec.md` + `plan.md`

## 结论

授权基准设计（人物为唯一令牌源 + corpId 从已授权角色行派生 + 归属双重过滤）符合 anti-IDOR 原则；403 差异化在「corpId by-construction 派生」前提下跨账户 oracle 面基本封闭。原 plan 存在两个阻塞 HIGH（H1/H2），已在本次修订中修复，可进入 G2。

## 发现与处置

| ID | 级别 | 问题 | 处置 |
|----|------|------|------|
| H1 | HIGH | 新错误码拟放 shared `ResultCode`，但 `EsiException` 构造器绑定 `infrastructure/external/esi/ResultCode` 枚举，会编译失败/双源 | **已修复**：`ESI_AUTH_PERMISSION_LOW("ESI00403")` 落 infrastructure 枚举 |
| H2 | HIGH | 403 的 message 用 ESI 原始 `error:errorDescription`；`handleEveHelperException` 会原样回显前端 → 需求与安全红线双违约 | **已修复**：message 用枚举报好文案，ESI 原始串仅结构化日志 |
| M1 | MEDIUM | 先 `bodyToMono` 解 body 成功才进 403 判断；body 空/非 JSON 时空流破坏 403 契约 | **已修订**：先判 `statusCode()==403` 再解 body |
| M2 | MEDIUM | `authorizeInternal(SYSTEM_USER_ID, characterId)` 依赖库中对应行；按 characterId 单键取行报越权或 fail-open 未明 | **已修订**：内部路径不得绕过 owner 过滤，补契约测试 |
| M3 | MEDIUM | 403 差异化作为角色-军团权限 oracle 面 | **已修订**：D2 加静态核查，corpId 恒派生无客户端直供 |
| L1 | LOW | 数据路径无 5s 超时 | **已修订**：D2 各入口加 5s 超时 |
| L2 | LOW | 数据 401 与 refresh 失效驻码不可辨 | 留协议注释 |
| L3 | LOW | RBAC 路径模板占位符改名不影响匹配 | 已并入 D4，URL 不变则不增 SQL |

## 归档

本记录由 P2 设计评审产出，供 G6 复核追溯。