-- =====================================================================
-- 012_rbac_permissions.sql
-- !!! 部署前必读 ⚠ !!!
-- 本脚本采用【事务内先删后插】：Step 1 会删除这 2 个 url_perm 的【全部】角色绑定
-- (sys_role_permission), 再在 Step 3 仅重新绑定到 ADMIN 角色。
-- 若执行前已有【非 ADMIN】角色(如 OP/财务等)绑定了这些权限, 执行后这些绑定将被清除,
-- 相关角色的权限会离线失效(LEFT JOIN 自然失效), 这可能造成意外的越权收紧。
-- ⚠ 强烈建议执行前先运行下方 pre-check 查询, 核对是否存在非 ADMIN 绑定, 并评估影响。
-- 幂等性质: 见下方「幂等说明」；如需保留其他角色既有绑定, 请勿直接执行本脚本,
--   改用手工 INSERT 前 SELECT 查重(INSERT ... WHERE NOT EXISTS)的方式。
-- =====================================================================

-- ---------------------------------------------------------------------
-- Pre-check(建议先运行核对, 只读不写, 不会影响数据):
--   列出这两个 url_perm 当前的全部角色绑定, 便于确认是否存在会被清掉的非 ADMIN 绑定。
-- ---------------------------------------------------------------------
-- SELECT sp.url_perm, sr.code AS role_code,
--        COUNT(srp.id) AS bind_count
-- FROM sys_permission sp
--          LEFT JOIN sys_role_permission srp ON srp.permission_id = sp.id
--          LEFT JOIN sys_role sr ON srp.role_id = sr.id
-- WHERE sp.url_perm IN ('GET:/wallet/overview/{cid}',
--                       'GET:/wallet/overview/corp/{corpId}')
-- GROUP BY sp.url_perm, sr.code;

-- =====================================================================
-- 012_rbac_permissions.sql
-- 用途: 为 012-wallet-overview(Polish 收尾)登记 2 个 REST 端点的 RBAC 权限。
--       登记两张表:
--         * sys_permission      —— 新增 2 条 URL 权限(url_perm = 'METHOD:PATH')
--         * sys_role_permission —— 将上述权限绑定到 ADMIN 角色(按角色编码关联)
-- 归属: specs/012-wallet-overview —— Polish 收尾 T021 RBAC 权限登记 SQL
--       关联端点(与 RbacAuthorizationManager.restfulPath = method + ':' + path 精确匹配):
--         * GET:/wallet/overview/{cid}        人物钱包总览(WalletOverviewController)
--         * GET:/wallet/overview/corp/{corpId} 军团钱包总览(WalletOverviewController)
--
-- 角色编码依据:
--   RbacAuthorizationManager 将 url_perm -> 角色集合 规则(AntPathMatcher)按
--   sys_role.code 与 token 中的 authority 比对(见 SysPermissionMapper.listPermRoles:
--   LEFT JOIN sys_role sr ON srp.role_id = sr.id, 取 SELECT sr.code)。
--   超级管理员判空常量 GlobalConstants.ROOT_ROLE_CODE = "ADMIN"(大写)。
--   故下方按 sys_role.code = 'ADMIN' 关联 admin 角色, 与 Java 侧比对口径一致。
--   注: admin 角色可能为运维外部预置; 若某环境 admin 角色 code 实际非 'ADMIN', 请改为实际编码。
--
-- 幂等说明(可重复执行):
--   sys_permission.url_perm 与 sys_role_permission(role_id,permission_id) 均【无唯一约束】,
--   直接重复 insert 会产生重复行。故本脚本采用【事务内先删后插】:
--     1) 先删除这 2 个 url_perm 已存在的 sys_permission 行及其全部角色绑定;
--     2) 再重新插入 sys_permission 与 ADMIN 的 sys_role_permission。
--   每次执行结果一致, 幂等可重跑。注意: 删除-重插会令 sys_permission 自增 id 变化,
--   若此前有其他角色绑定了这些权限, 其 sys_role_permission 行会指向已删除的旧 id
--   (无外键不报错, 运行时 LEFT JOIN 自然失效), 请部署时人工核对。若需保留其他角色
--   既有绑定, 请改用带 INSERT 前 SELECT 手工查重的方式。
--
-- 执行时机: 部署 012 特性(新端点上线)后, 执行本脚本并在 sys_permission/sys_role_permission
--   核对行数, 然后刷新权限缓存(refreshPermRolesRules / 对应管理入口)使生效。
-- =====================================================================

START TRANSACTION;

-- ---------------------------------------------------------------------
-- Step 1(幂等清理): 删除这 2 个 url_perm 的既有登记(连同所有角色绑定)。
--   首次执行无脏数据时, 本步影响 0 行, 直接进入 Step 2。
-- ---------------------------------------------------------------------
DELETE srp
FROM sys_role_permission srp
         INNER JOIN sys_permission sp ON srp.permission_id = sp.id
WHERE sp.url_perm IN ('GET:/wallet/overview/{cid}',
                      'GET:/wallet/overview/corp/{corpId}');

DELETE
FROM sys_permission
WHERE url_perm IN ('GET:/wallet/overview/{cid}',
                   'GET:/wallet/overview/corp/{corpId}');

-- ---------------------------------------------------------------------
-- Step 2: 插入 2 条 URL 权限。url_perm 格式 = 'METHOD:PATH',
--   与 RbacAuthorizationManager 的 restfulPath 精确匹配, 不得含上下文路径前缀。
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified)
VALUES ('人物钱包总览', 'GET:/wallet/overview/{cid}', NOW(), NOW()),
       ('军团钱包总览', 'GET:/wallet/overview/corp/{corpId}', NOW(), NOW());

-- ---------------------------------------------------------------------
-- Step 3: 将 2 条权限绑定到 ADMIN 角色。
--   role_id       按 sys_role.code = 'ADMIN' 子查询关联(避免硬编码自增 id);
--   permission_id 按 sys_permission.url_perm 子查询关联 Step 2 刚插入的行。
-- ---------------------------------------------------------------------
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
VALUES ((SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
        (SELECT id FROM sys_permission WHERE url_perm = 'GET:/wallet/overview/{cid}' LIMIT 1),
        NOW(), NOW()),
       ((SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
        (SELECT id FROM sys_permission WHERE url_perm = 'GET:/wallet/overview/corp/{corpId}' LIMIT 1),
        NOW(), NOW());

COMMIT;