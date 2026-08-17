-- =====================================================================
-- 009_rbac_permissions.sql
-- 用途: 为 009-asset-wallet-aggregate 特性新增的 3 个 REST 端点登记 RBAC 权限。
--       登记两张表:
--         * sys_permission      —— 新增 3 条 URL 权限(url_perm = 'METHOD:PATH')
--         * sys_role_permission —— 将上述权限绑定到 ADMIN 角色(按角色编码关联)
-- 归属: specs/009-asset-wallet-aggregate —— T023 RBAC 权限登记 SQL
--       关联端点(与 RbacAuthorizationManager.restfulPath = method + ':' + path 精确匹配):
--         * GET:/assets/aggregate         资产多角色聚合查询(US1, AssetsController)
--         * POST:/wallet/journal/{cid}/sync 钱包流水手动同步(US2, WalletJournalController)
--         * GET:/wallet/journal/{cid}     钱包流水分页查询(US2, WalletJournalController)
--
-- 角色编码依据:
--   RbacAuthorizationManager 将 url_perm -> 角色集合 规则(AntPathMatcher)按
--   sys_role.code 与 token 中的 authority 比对(见 SysPermissionMapper.listPermRoles:
--   LEFT JOIN sys_role sr ON srp.role_id = sr.id, 取 SELECT sr.code)。
--   超级管理员判空常量 GlobalConstants.ROOT_ROLE_CODE = "ADMIN"(大写)。
--   故下方按 sys_role.code = 'ADMIN' 关联 admin 角色, 与 Java 侧比对口径一致。
--   注: 本建表脚本(数据库创建脚本.sql)仅建空表、无角色种子数据, admin 角色由运维
--   外部预置; 若某环境 admin 角色 code 实际非 'ADMIN', 请改为实际编码后执行
--   (选 name 亦可, 因 sys_role.name 有唯一约束; 但 code 为 Java 侧对比字段, 优先对齐)。
--
-- 幂等说明(可重复执行):
--   sys_permission.url_perm 与 sys_role_permission(role_id,permission_id) 均【无唯一约束】,
--   直接重复 insert 会产生重复行。故本脚本采用【事务内先删后插】:
--     1) 先删除这 3 个 url_perm 已存在的 sys_permission 行及其全部角色绑定;
--     2) 再重新插入 sys_permission 与 ADMIN 的 sys_role_permission。
--   每次执行结果一致, 幂等可重跑。注意: 删除-重插会令 sys_permission 自增 id 变化,
--   若此前有其他角色(非 ADMIN)绑定过这些权限, 其 sys_role_permission 行会指向已删除的
--   旧 permission_id(无外键, 不报错, 运行时 LEFT JOIN 自然失效), 请部署时人工核对
--   sys_role_permission 与 sys_permission 是否按预期一致。
--   若需保留其他角色的既有绑定, 请改用带 INSERT 前 SELECT 手工查重的方式, 勿直接跑本脚本。
--
-- 执行时机: 部署 009 特性(新端点上线)后, 执行本脚本并在 sys_permission/sys_role_permission
--   核对行数, 然后通过系统刷新权限缓存(refreshPermRolesRules / 对应管理入口)使生效。
-- =====================================================================

START TRANSACTION;

-- ---------------------------------------------------------------------
-- Step 1(幂等清理): 删除这 3 个 url_perm 的既有登记(连同所有角色绑定)。
--   首次执行无脏数据时, 本步影响 0 行, 直接进入 Step 2。
-- ---------------------------------------------------------------------
DELETE srp
FROM sys_role_permission srp
         INNER JOIN sys_permission sp ON srp.permission_id = sp.id
WHERE sp.url_perm IN ('GET:/assets/aggregate',
                      'POST:/wallet/journal/{cid}/sync',
                      'GET:/wallet/journal/{cid}');

DELETE
FROM sys_permission
WHERE url_perm IN ('GET:/assets/aggregate',
                   'POST:/wallet/journal/{cid}/sync',
                   'GET:/wallet/journal/{cid}');

-- ---------------------------------------------------------------------
-- Step 2: 插入 3 条 URL 权限。url_perm 格式 = 'METHOD:PATH',
--   与 RbacAuthorizationManager 的 restfulPath 精确匹配, 不得含上下文路径前缀。
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified)
VALUES ('资产多角色聚合查询', 'GET:/assets/aggregate', NOW(), NOW()),
       ('钱包流水手动同步', 'POST:/wallet/journal/{cid}/sync', NOW(), NOW()),
       ('钱包流水分页查询', 'GET:/wallet/journal/{cid}', NOW(), NOW());

-- ---------------------------------------------------------------------
-- Step 3: 将 3 条权限绑定到 ADMIN 角色。
--   role_id       按 sys_role.code = 'ADMIN' 子查询关联(避免硬编码自增 id;
--                 若实况库 admin 角色编码非 'ADMIN', 请先改下面的 'ADMIN' 再执行);
--   permission_id 按 sys_permission.url_perm 子查询关联 Step 2 刚插入的行。
-- ---------------------------------------------------------------------
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
VALUES ((SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
        (SELECT id FROM sys_permission WHERE url_perm = 'GET:/assets/aggregate' LIMIT 1),
        NOW(), NOW()),
       ((SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
        (SELECT id FROM sys_permission WHERE url_perm = 'POST:/wallet/journal/{cid}/sync' LIMIT 1),
        NOW(), NOW()),
       ((SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
        (SELECT id FROM sys_permission WHERE url_perm = 'GET:/wallet/journal/{cid}' LIMIT 1),
        NOW(), NOW());

COMMIT;