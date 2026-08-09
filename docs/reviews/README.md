# ECC 评审记录

本目录存放 ECC 质量与安全门禁的评审记录，落实《工程开发规则》「单一事实来源」原则——评审结论必须持久化，禁止口头约定。

## 命名规范

```
YYYY-MM-DD-<feature-编号>-<reviewer>.md
```

示例：
- `2026-08-09-005-java-reviewer.md`
- `2026-08-09-005-security-reviewer.md`

## 记录模板

```markdown
# 评审记录: <标题>

- **日期**: YYYY-MM-DD
- **评审者**: ecc:java-reviewer | ecc:security-reviewer
- **关联 feature**: specs/<NNN>-<slug>
- **评审范围**: <文件或模块清单>
- **结论**: APPROVE | APPROVE WITH COMMENTS | BLOCK

## 发现

| 级别 | 位置 | 问题 | 处置 |
|------|------|------|------|
| CRITICAL / HIGH / MEDIUM / LOW | `path/to/File.java:42` | 描述 | 已修复 / 待办 / 不适用 |

## 处置说明

<CRITICAL 与 HIGH 必须全部修复后方可合并；MEDIUM/LOW 若延后，需说明原因并登记至 tasks.md>
```

## 门禁要求

- Java 代码变更：`ecc:java-reviewer` 必审
- 涉及认证/授权、用户输入、外部 API、加密：追加 `ecc:security-reviewer`
- CRITICAL 或 HIGH 问题未修复，禁止合并

详见 [工程开发规则](../AI_WORKFLOW.md)。
