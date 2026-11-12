-- =====================================================================
-- 022_rbac_permissions.sql
-- !!! 部署前必读 ⚠ !!!
-- 本脚本采用【事务内先删后插】：Step 1 会删除这 18 个 url_perm 的【全部】角色绑定
-- (sys_role_permission), 再在 Step 3 仅重新绑定到 USER 角色。
-- 若执行前已有【非 USER】角色(如 OP/财务等)绑定了这些权限, 执行后这些绑定将被清除,
-- 相关角色的权限会离线失效(LEFT JOIN 自然失效), 这可能造成意外的越权收紧。
-- ⚠ 强烈建议执行前先运行下方 pre-check 查询, 核对是否存在非 USER 绑定, 并评估影响。
-- 幂等性质: 见下方「幂等说明」；如需保留其他角色既有绑定, 请勿直接执行本脚本,
--   改用手工 INSERT 前 SELECT 查重(INSERT ... WHERE NOT EXISTS)的方式。
-- =====================================================================

-- ---------------------------------------------------------------------
-- Pre-check(建议先运行核对, 只读不写, 不会影响数据):
--   列出这 18 个 url_perm 当前的全部角色绑定, 便于确认是否存在会被清掉的非 USER 绑定。
-- ---------------------------------------------------------------------
-- SELECT sp.url_perm, sr.code AS role_code,
--        COUNT(srp.id) AS bind_count
-- FROM sys_permission sp
--          LEFT JOIN sys_role_permission srp ON srp.permission_id = sp.id
--          LEFT JOIN sys_role sr ON srp.role_id = sr.id
-- WHERE sp.url_perm IN (
--   'GET:/character/{characterId}/panel',
--   'GET:/character/{characterId}/skills',
--   'GET:/character/{characterId}/skill-queue',
--   'GET:/mail/{characterId}/inbox',
--   'GET:/mail/{characterId}/labels',
--   'GET:/mail/{characterId}/{mailId}',
--   'PUT:/mail/{characterId}/{mailId}/read',
--   'POST:/mail/{characterId}/send',
--   'POST:/mail/recipients/resolve',
--   'GET:/job/{type}/{id}',
--   'GET:/corporation/{corpId}/member-tracking',
--   'GET:/notification',
--   'GET:/notification/unread-count',
--   'PUT:/notification/{id}/read',
--   'POST:/notification/ws-ticket',
--   'PUT:/user/password',
--   'DELETE:/user/characters/{characterId}',
--   'GET:/character/auth-url')
-- GROUP BY sp.url_perm, sr.code;

-- =====================================================================
-- 022_rbac_permissions.sql
-- 用途: 为 022-companion-api(伴侣 APP 后端支撑端点)登记 18 个 REST 端点的 RBAC 权限。
--       登记两张表:
--         * sys_permission      —— 新增 18 条 URL 权限(url_perm = 'METHOD:PATH')
--         * sys_role_permission —— 将上述权限绑定到 USER 角色(按角色编码关联)
-- 归属: specs/022-companion-api —— T002 RBAC 权限登记 SQL
--       关联端点(与 RbacAuthorizationManager.restfulPath = method + ':' + path 精确匹配):
--         * GET:/character/{characterId}/panel          角色面板查询
--         * GET:/character/{characterId}/skills         角色技能查询
--         * GET:/character/{characterId}/skill-queue    技能队列查询
--         * GET:/mail/{characterId}/inbox               邮件收件箱
--         * GET:/mail/{characterId}/labels              邮件标签
--         * GET:/mail/{characterId}/{mailId}            邮件详情
--         * PUT:/mail/{characterId}/{mailId}/read       标记邮件已读
--         * POST:/mail/{characterId}/send               发送邮件
--         * POST:/mail/recipients/resolve               收件人解析
--         * GET:/job/{type}/{id}                        工业作业直查
--         * GET:/corporation/{corpId}/member-tracking   团员追踪
--         * GET:/notification                           通知列表
--         * GET:/notification/unread-count              未读通知计数
--         * PUT:/notification/{id}/read                 标记通知已读
--         * POST:/notification/ws-ticket                WebSocket票据
--         * PUT:/user/password                          修改密码
--         * DELETE:/user/characters/{characterId}       解绑角色
--         * GET:/character/auth-url                     ESI授权链接
--
-- 角色编码依据:
--   RbacAuthorizationManager 将 url_perm -> 角色集合 规则(AntPathMatcher)按
--   sys_role.code 与 token 中的 authority 比对(见 SysPermissionMapper.listPermRoles:
--   LEFT JOIN sys_role sr ON srp.role_id = sr.id, 取 SELECT sr.code)。
--   超级管理员判空常量 GlobalConstants.ROOT_ROLE_CODE = "ADMIN"(大写)。
--   ADMIN 角色天然放行(RbacAuthorizationManager 对 ROOT 直接通过), 无需登记绑定。
--   本脚本将 18 端点绑定到 USER 角色, 使普通已认证用户可访问伴侣 APP 功能。
--   注: 若某环境 USER 角色 code 实际非 'USER', 请改为实际编码。
--
-- 幂等说明(可重复执行):
--   sys_permission.url_perm 与 sys_role_permission(role_id,permission_id) 均【无唯一约束】,
--   直接重复 insert 会产生重复行。故本脚本采用【事务内先删后插】:
--     1) 先删除这 18 个 url_perm 已存在的 sys_permission 行及其全部角色绑定;
--     2) 再重新插入 sys_permission 与 USER 的 sys_role_permission。
--   每次执行结果一致, 幂等可重跑。注意: 删除-重插会令 sys_permission 自增 id 变化,
--   若此前有其他角色绑定了这些权限, 其 sys_role_permission 行会指向已删除的旧 id
--   (无外键不报错, 运行时 LEFT JOIN 自然失效), 请部署时人工核对。若需保留其他角色
--   既有绑定, 请改用带 INSERT 前 SELECT 手工查重的方式。
--
-- 执行时机: 部署 022 特性(伴侣 APP 端点上线)后, 执行本脚本并在 sys_permission/
--   sys_role_permission 核对行数, 然后重启应用或刷新权限缓存使生效。
-- =====================================================================

START TRANSACTION;

-- ---------------------------------------------------------------------
-- Step 1(幂等清理): 删除这 18 个 url_perm 的既有登记(连同所有角色绑定)。
--   首次执行无脏数据时, 本步影响 0 行, 直接进入 Step 2。
-- ---------------------------------------------------------------------
DELETE srp
FROM sys_role_permission srp
         INNER JOIN sys_permission sp ON srp.permission_id = sp.id
WHERE sp.url_perm IN ('GET:/character/{characterId}/panel',
                      'GET:/character/{characterId}/skills',
                      'GET:/character/{characterId}/skill-queue',
                      'GET:/mail/{characterId}/inbox',
                      'GET:/mail/{characterId}/labels',
                      'GET:/mail/{characterId}/{mailId}',
                      'PUT:/mail/{characterId}/{mailId}/read',
                      'POST:/mail/{characterId}/send',
                      'POST:/mail/recipients/resolve',
                      'GET:/job/{type}/{id}',
                      'GET:/corporation/{corpId}/member-tracking',
                      'GET:/notification',
                      'GET:/notification/unread-count',
                      'PUT:/notification/{id}/read',
                      'POST:/notification/ws-ticket',
                      'PUT:/user/password',
                      'DELETE:/user/characters/{characterId}',
                      'GET:/character/auth-url');

DELETE
FROM sys_permission
WHERE url_perm IN ('GET:/character/{characterId}/panel',
                   'GET:/character/{characterId}/skills',
                   'GET:/character/{characterId}/skill-queue',
                   'GET:/mail/{characterId}/inbox',
                   'GET:/mail/{characterId}/labels',
                   'GET:/mail/{characterId}/{mailId}',
                   'PUT:/mail/{characterId}/{mailId}/read',
                   'POST:/mail/{characterId}/send',
                   'POST:/mail/recipients/resolve',
                   'GET:/job/{type}/{id}',
                   'GET:/corporation/{corpId}/member-tracking',
                   'GET:/notification',
                   'GET:/notification/unread-count',
                   'PUT:/notification/{id}/read',
                   'POST:/notification/ws-ticket',
                   'PUT:/user/password',
                   'DELETE:/user/characters/{characterId}',
                   'GET:/character/auth-url');

-- ---------------------------------------------------------------------
-- Step 2: 插入 18 条 URL 权限。url_perm 格式 = 'METHOD:PATH',
--   与 RbacAuthorizationManager 的 restfulPath 精确匹配, 不得含上下文路径前缀。
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified)
VALUES ('角色面板查询',         'GET:/character/{characterId}/panel',          NOW(), NOW()),
       ('角色技能查询',         'GET:/character/{characterId}/skills',         NOW(), NOW()),
       ('技能队列查询',         'GET:/character/{characterId}/skill-queue',    NOW(), NOW()),
       ('邮件收件箱',           'GET:/mail/{characterId}/inbox',               NOW(), NOW()),
       ('邮件标签',             'GET:/mail/{characterId}/labels',              NOW(), NOW()),
       ('邮件详情',             'GET:/mail/{characterId}/{mailId}',            NOW(), NOW()),
       ('标记邮件已读',         'PUT:/mail/{characterId}/{mailId}/read',       NOW(), NOW()),
       ('发送邮件',             'POST:/mail/{characterId}/send',               NOW(), NOW()),
       ('收件人解析',           'POST:/mail/recipients/resolve',               NOW(), NOW()),
       ('工业作业直查',         'GET:/job/{type}/{id}',                        NOW(), NOW()),
       ('团员追踪',             'GET:/corporation/{corpId}/member-tracking',   NOW(), NOW()),
       ('通知列表',             'GET:/notification',                           NOW(), NOW()),
       ('未读通知计数',         'GET:/notification/unread-count',              NOW(), NOW()),
       ('标记通知已读',         'PUT:/notification/{id}/read',                 NOW(), NOW()),
       ('WebSocket票据',        'POST:/notification/ws-ticket',                NOW(), NOW()),
       ('修改密码',             'PUT:/user/password',                          NOW(), NOW()),
       ('解绑角色',             'DELETE:/user/characters/{characterId}',       NOW(), NOW()),
       ('ESI授权链接',          'GET:/character/auth-url',                     NOW(), NOW());

-- ---------------------------------------------------------------------
-- Step 3: 将 18 条权限绑定到 USER 角色。
--   role_id       按 sys_role.code = 'USER' 子查询关联(避免硬编码自增 id);
--   permission_id 按 sys_permission.url_perm 子查询关联 Step 2 刚插入的行。
--   ADMIN 角色天然放行(GlobalConstants.ROOT_ROLE_CODE), 无需登记。
-- ---------------------------------------------------------------------
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
SELECT sr.id, sp.id, NOW(), NOW()
FROM sys_role sr, sys_permission sp
WHERE sp.url_perm IN ('GET:/character/{characterId}/panel',
                      'GET:/character/{characterId}/skills',
                      'GET:/character/{characterId}/skill-queue',
                      'GET:/mail/{characterId}/inbox',
                      'GET:/mail/{characterId}/labels',
                      'GET:/mail/{characterId}/{mailId}',
                      'PUT:/mail/{characterId}/{mailId}/read',
                      'POST:/mail/{characterId}/send',
                      'POST:/mail/recipients/resolve',
                      'GET:/job/{type}/{id}',
                      'GET:/corporation/{corpId}/member-tracking',
                      'GET:/notification',
                      'GET:/notification/unread-count',
                      'PUT:/notification/{id}/read',
                      'POST:/notification/ws-ticket',
                      'PUT:/user/password',
                      'DELETE:/user/characters/{characterId}',
                      'GET:/character/auth-url')
  AND sr.code = 'USER';

COMMIT;

-- ---------------------------------------------------------------------
-- Step 3 执行后校验(必跑): 确认 18 个绑定都已写入。
--   期望 18 行(USER 角色各一)。
--   ⚠ 若少于 18 行,说明目标库缺少 sys_role.code = 'USER' 角色 —— 上面的
--     INSERT ... SELECT 在角色不存在时【静默少插且不报错】,必须人工核对后补齐。
-- ---------------------------------------------------------------------
-- SELECT sp.url_perm, sr.code
-- FROM sys_permission sp
--          JOIN sys_role_permission srp ON srp.permission_id = sp.id
--          JOIN sys_role sr ON sr.id = srp.role_id
-- WHERE sp.url_perm IN (
--   'GET:/character/{characterId}/panel',
--   'GET:/character/{characterId}/skills',
--   'GET:/character/{characterId}/skill-queue',
--   'GET:/mail/{characterId}/inbox',
--   'GET:/mail/{characterId}/labels',
--   'GET:/mail/{characterId}/{mailId}',
--   'PUT:/mail/{characterId}/{mailId}/read',
--   'POST:/mail/{characterId}/send',
--   'POST:/mail/recipients/resolve',
--   'GET:/job/{type}/{id}',
--   'GET:/corporation/{corpId}/member-tracking',
--   'GET:/notification',
--   'GET:/notification/unread-count',
--   'PUT:/notification/{id}/read',
--   'POST:/notification/ws-ticket',
--   'PUT:/user/password',
--   'DELETE:/user/characters/{characterId}',
--   'GET:/character/auth-url');

-- ---------------------------------------------------------------------
-- ⚠ 最后一步(不可省): 刷新权限缓存使规则生效。
--   RbacAuthorizationManager 的 URL-权限-角色映射缓存在 Redis(URL_PERM_ROLES_KEY),
--   仅执行本脚本不会让新权限立即生效,须重启应用(InitPermissionRolesCache 仅启动重建)
--   或调用 refreshPermRolesRules(或对应管理入口)重建缓存。
-- ---------------------------------------------------------------------
