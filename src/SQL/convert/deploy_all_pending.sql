-- =====================================================================
-- deploy_all_pending.sql — 累积 DDL 变更一次性执行脚本
--
-- 生成时间: 2026-08-27
-- 目标库: eve_helper (运行时库)
-- 前提: ROOT admin user_id = 1, ADMIN 角色 code = 'ADMIN', USER 角色 code = 'USER'
--
-- 脚本性质: 整合 009~022 特性至今所有待执行的 DDL + RBAC 权限变更,
--           各段均可独立执行(幂等),按顺序跑一遍即完成全量部署。
--
-- 执行顺序:
--   ① 009 wallet_journal 幂等主键
--   ② 010 wallet_transaction 建表
--   ③ 011 wallet_journal 加 division 列 + 索引
--   ④ 012 wallet_journal 复合索引 (owner_id, division, date)
--   ⑤ 014 8 表加 user_id 列 + 存量迁移 (ROOT=1)
--   ⑥ 015 structure 表 corporation_id 索引
--   ⑦ RBAC 权限登记 (009/010/011/012/017/018/005 建筑端点)
--   ⑧ 022 伴侣 APP 通知表 + RBAC 权限登记 (18 端点)
--
-- ⚠ 最后一步: 刷新 Redis 权限缓存 (refreshPermRolesRules)
-- =====================================================================

-- =====================================================================
-- ① 009: wallet_journal 幂等主键 PRIMARY(id)
--   幂等: 已存在主键时第二段报 Duplicate key name 'PRIMARY'(可忽略)
-- =====================================================================

DELETE w FROM wallet_journal w
JOIN wallet_journal w2 ON w.id = w2.id AND w.date > w2.date;

ALTER TABLE wallet_journal ADD PRIMARY KEY (id);

-- =====================================================================
-- ② 010: wallet_transaction 建表 (IF NOT EXISTS)
--   幂等: 表已存在则跳过
-- =====================================================================

CREATE TABLE IF NOT EXISTS wallet_transaction (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键(不依赖 ESI id)',
  owner_type     VARCHAR(32)  NOT NULL COMMENT '所有者类型:character=人物,corporation=军团',
  owner_id       BIGINT       NOT NULL COMMENT '所有者ID(人物 ID 或军团 ID)',
  division       INT          NOT NULL COMMENT '钱包 division:人物=0,军团 1..7',
  transaction_id BIGINT       NOT NULL COMMENT 'ESI 交易ID(幂等复合键之一)',
  `date`         DATETIME     NOT NULL COMMENT '交易发生时间(ESI date,保留字需反引号)',
  type_id        INT          NULL COMMENT '物品类型ID',
  quantity       INT          NULL COMMENT '数量',
  unit_price     DOUBLE       NULL COMMENT '单价',
  client_id      INT          NULL COMMENT '对手方客户端ID',
  location_id    BIGINT       NULL COMMENT '下单位置ID',
  is_buy         TINYINT(1)   NULL COMMENT '是否为买单',
  is_personal    TINYINT(1)   NULL COMMENT '是否个人交易',
  journal_ref_id BIGINT       NULL COMMENT '关联钱包流水条目ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_div_tx (owner_type, owner_id, division, transaction_id),
  KEY idx_date (`date`),
  KEY idx_owner (owner_type, owner_id, division)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包交易记录表(人物/军团钱包交易流水)';

-- =====================================================================
-- ③ 011: wallet_journal 加 division 列 + 索引
--   幂等: MySQL 无 ADD COLUMN IF NOT EXISTS,已存在则报 Duplicate column(可忽略)
--   索引: MySQL 无 CREATE INDEX IF NOT EXISTS,已存在则报 Duplicate key name(可忽略)
-- =====================================================================

ALTER TABLE wallet_journal ADD COLUMN division INT NOT NULL DEFAULT 0;
UPDATE wallet_journal SET division = 1 WHERE division = 0;
CREATE INDEX idx_owner_div ON wallet_journal(owner_id, division);

-- =====================================================================
-- ④ 012: wallet_journal 复合索引 (owner_id, division, date)
--   幂等: 用 information_schema 判是否存在,不存在才 CREATE
-- =====================================================================

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'wallet_journal'
    AND INDEX_NAME = 'idx_owner_div_date'
);

SET @ddl := IF(@idx_exists = 0,
  'CREATE INDEX idx_owner_div_date ON wallet_journal (owner_id, division, `date`)',
  'SELECT ''idx_owner_div_date already exists, skip'' AS info');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================================
-- ⑤ 014: 8 表加 user_id 列 + 存量迁移 (ROOT admin user_id = 1)
--   幂等: UPDATE 均带 WHERE user_id IS NULL,已归属行不重写
--   ADD COLUMN: MySQL 无 IF NOT EXISTS,已存在则报 Duplicate column(可忽略)
-- =====================================================================

ALTER TABLE `assets`        ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `owner_id`;
ALTER TABLE `blueprints`    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `owner_id`;
ALTER TABLE `mining_detail` ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `character_id`;
ALTER TABLE `wallet_journal`    ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `division`;
ALTER TABLE `wallet_transaction` ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `division`;
ALTER TABLE `industry_job`  ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `corporation_id`;
ALTER TABLE `observer`      ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `croporation_id`;
ALTER TABLE `structure`     ADD COLUMN `user_id` BIGINT NULL COMMENT '同步者系统用户ID' AFTER `corporation_id`;

-- 5a. 人物行 → 角色属主 (join eve_account 按 character_id 取 e.user_id)
UPDATE assets            a JOIN eve_account e ON e.character_id = a.owner_id      SET a.user_id = e.user_id WHERE a.user_id IS NULL;
UPDATE blueprints        b JOIN eve_account e ON e.character_id = b.owner_id      SET b.user_id = e.user_id WHERE b.user_id IS NULL;
UPDATE mining_detail     m JOIN eve_account e ON e.character_id = m.character_id  SET m.user_id = e.user_id WHERE m.user_id IS NULL;
UPDATE wallet_journal    w JOIN eve_account e ON e.character_id = w.owner_id      SET w.user_id = e.user_id WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.character_id = t.owner_id AND t.owner_type = 'character' SET t.user_id = e.user_id WHERE t.user_id IS NULL;

-- 5b. 军团行 → 管理域 (ROOT admin user_id = 1)
UPDATE structure         s JOIN eve_account e ON e.corp_id = s.corporation_id     SET s.user_id = 1 WHERE s.user_id IS NULL;
UPDATE industry_job      i JOIN eve_account e ON e.corp_id = i.corporation_id     SET i.user_id = 1 WHERE i.user_id IS NULL;
UPDATE observer          o JOIN eve_account e ON e.corp_id = o.croporation_id     SET o.user_id = 1 WHERE o.user_id IS NULL;
UPDATE wallet_journal    w JOIN eve_account e ON e.corp_id = w.owner_id            SET w.user_id = 1 WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.corp_id = t.owner_id AND t.owner_type = 'corporation' SET t.user_id = 1 WHERE t.user_id IS NULL;

-- 5c. 孤儿行兜底 (迁移后仍 user_id IS NULL 的未匹配行)
UPDATE assets            SET user_id = 1 WHERE user_id IS NULL;
UPDATE blueprints        SET user_id = 1 WHERE user_id IS NULL;
UPDATE mining_detail     SET user_id = 1 WHERE user_id IS NULL;
UPDATE industry_job      SET user_id = 1 WHERE user_id IS NULL;
UPDATE observer          SET user_id = 1 WHERE user_id IS NULL;
UPDATE structure         SET user_id = 1 WHERE user_id IS NULL;
UPDATE wallet_journal    SET user_id = 1 WHERE user_id IS NULL;
UPDATE wallet_transaction SET user_id = 1 WHERE user_id IS NULL;

-- =====================================================================
-- ⑥ 015: structure 表 corporation_id 索引
--   幂等: MySQL 无 CREATE INDEX IF NOT EXISTS,已存在则报 Duplicate key name(可忽略)
-- =====================================================================

CREATE INDEX idx_structure_corporation_id ON `structure` (`corporation_id`);

-- =====================================================================
-- ⑦ RBAC 权限登记
--
-- 整合 009/010/011/012/017/018 特性的所有端点权限,
-- 每段均为事务内先删后插,幂等可重跑。
--
-- ⚠ 前提: sys_role 中已存在 code='ADMIN' 和 code='USER' 角色,
--         若编码不同请先修正下方 'ADMIN'/'USER' 后执行。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 7a. 009 特性: 资产聚合 + 钱包流水 (3 端点)
-- ---------------------------------------------------------------------
START TRANSACTION;
DELETE srp FROM sys_role_permission srp
  INNER JOIN sys_permission sp ON srp.permission_id = sp.id
  WHERE sp.url_perm IN ('GET:/assets/aggregate',
                        'POST:/wallet/journal/{cid}/sync',
                        'GET:/wallet/journal/{cid}');
DELETE FROM sys_permission
  WHERE url_perm IN ('GET:/assets/aggregate',
                     'POST:/wallet/journal/{cid}/sync',
                     'GET:/wallet/journal/{cid}');
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified) VALUES
  ('资产多角色聚合查询', 'GET:/assets/aggregate', NOW(), NOW()),
  ('钱包流水手动同步', 'POST:/wallet/journal/{cid}/sync', NOW(), NOW()),
  ('钱包流水分页查询', 'GET:/wallet/journal/{cid}', NOW(), NOW());
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = t.perm LIMIT 1),
         NOW(), NOW()
  FROM (SELECT 'GET:/assets/aggregate' AS perm UNION ALL
        SELECT 'POST:/wallet/journal/{cid}/sync' UNION ALL
        SELECT 'GET:/wallet/journal/{cid}') t;
COMMIT;

-- ---------------------------------------------------------------------
-- 7b. 010 特性: 钱包交易 (4 端点)
-- ---------------------------------------------------------------------
START TRANSACTION;
DELETE srp FROM sys_role_permission srp
  INNER JOIN sys_permission sp ON srp.permission_id = sp.id
  WHERE sp.url_perm IN ('POST:/wallet/transaction/{cid}/sync',
                        'GET:/wallet/transaction/{cid}',
                        'POST:/wallet/transaction/corp/{corpId}/sync',
                        'GET:/wallet/transaction/corp/{corpId}');
DELETE FROM sys_permission
  WHERE url_perm IN ('POST:/wallet/transaction/{cid}/sync',
                     'GET:/wallet/transaction/{cid}',
                     'POST:/wallet/transaction/corp/{corpId}/sync',
                     'GET:/wallet/transaction/corp/{corpId}');
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified) VALUES
  ('人物钱包交易手动同步', 'POST:/wallet/transaction/{cid}/sync', NOW(), NOW()),
  ('人物钱包交易分页查询', 'GET:/wallet/transaction/{cid}', NOW(), NOW()),
  ('军团钱包交易手动同步', 'POST:/wallet/transaction/corp/{corpId}/sync', NOW(), NOW()),
  ('军团钱包交易分页查询', 'GET:/wallet/transaction/corp/{corpId}', NOW(), NOW());
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = t.perm LIMIT 1),
         NOW(), NOW()
  FROM (SELECT 'POST:/wallet/transaction/{cid}/sync' AS perm UNION ALL
        SELECT 'GET:/wallet/transaction/{cid}' UNION ALL
        SELECT 'POST:/wallet/transaction/corp/{corpId}/sync' UNION ALL
        SELECT 'GET:/wallet/transaction/corp/{corpId}') t;
COMMIT;

-- ---------------------------------------------------------------------
-- 7c. 011 特性: 军团钱包流水 (2 端点)
-- ---------------------------------------------------------------------
START TRANSACTION;
DELETE srp FROM sys_role_permission srp
  INNER JOIN sys_permission sp ON srp.permission_id = sp.id
  WHERE sp.url_perm IN ('POST:/wallet/journal/corp/{corpId}/sync',
                        'GET:/wallet/journal/corp/{corpId}');
DELETE FROM sys_permission
  WHERE url_perm IN ('POST:/wallet/journal/corp/{corpId}/sync',
                     'GET:/wallet/journal/corp/{corpId}');
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified) VALUES
  ('军团钱包流水手动同步', 'POST:/wallet/journal/corp/{corpId}/sync', NOW(), NOW()),
  ('军团钱包流水分账分页查询', 'GET:/wallet/journal/corp/{corpId}', NOW(), NOW());
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = t.perm LIMIT 1),
         NOW(), NOW()
  FROM (SELECT 'POST:/wallet/journal/corp/{corpId}/sync' AS perm UNION ALL
        SELECT 'GET:/wallet/journal/corp/{corpId}') t;
COMMIT;

-- ---------------------------------------------------------------------
-- 7d. 012/017/018 特性: 钱包总览 + 余额聚合 (3 端点)
--   注: 012 原 /corp/{corpId} 已被 017 改为 /corp/{cid}
-- ---------------------------------------------------------------------
START TRANSACTION;
DELETE srp FROM sys_role_permission srp
  INNER JOIN sys_permission sp ON srp.permission_id = sp.id
  WHERE sp.url_perm IN ('GET:/wallet/overview/{cid}',
                        'GET:/wallet/overview/corp/{cid}',
                        'GET:/wallet/balance');
DELETE FROM sys_permission
  WHERE url_perm IN ('GET:/wallet/overview/{cid}',
                     'GET:/wallet/overview/corp/{cid}',
                     'GET:/wallet/balance');
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified) VALUES
  ('人物钱包总览', 'GET:/wallet/overview/{cid}', NOW(), NOW()),
  ('军团钱包总览', 'GET:/wallet/overview/corp/{cid}', NOW(), NOW()),
  ('用户钱包余额聚合', 'GET:/wallet/balance', NOW(), NOW());
-- overview 仅 ADMIN, balance 为 ADMIN + USER
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = t.perm LIMIT 1),
         NOW(), NOW()
  FROM (SELECT 'GET:/wallet/overview/{cid}' AS perm UNION ALL
        SELECT 'GET:/wallet/overview/corp/{cid}' UNION ALL
        SELECT 'GET:/wallet/balance') t;
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'USER' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = 'GET:/wallet/balance' LIMIT 1),
         NOW(), NOW();
COMMIT;

-- ---------------------------------------------------------------------
-- 7e. 005 特性: 建筑查询 API (6 端点) — 新发现遗漏!
--   StructureController @RequestMapping("/structures")
--   ⚠ 这些端点不在 Spring Security 白名单中,
--     不补权限的话生产环境会 403。
-- ---------------------------------------------------------------------
START TRANSACTION;
DELETE srp FROM sys_role_permission srp
  INNER JOIN sys_permission sp ON srp.permission_id = sp.id
  WHERE sp.url_perm IN ('GET:/structures/{corpId}',
                        'GET:/structures/{corpId}/detail/{structureId}',
                        'GET:/structures/{corpId}/fuel',
                        'GET:/structures/{corpId}/detail/{structureId}/services',
                        'GET:/structures/{corpId}/stats',
                        'GET:/structures/{corpId}/timers');
DELETE FROM sys_permission
  WHERE url_perm IN ('GET:/structures/{corpId}',
                     'GET:/structures/{corpId}/detail/{structureId}',
                     'GET:/structures/{corpId}/fuel',
                     'GET:/structures/{corpId}/detail/{structureId}/services',
                     'GET:/structures/{corpId}/stats',
                     'GET:/structures/{corpId}/timers');
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified) VALUES
  ('建筑清单',     'GET:/structures/{corpId}',                        NOW(), NOW()),
  ('建筑详情',     'GET:/structures/{corpId}/detail/{structureId}',    NOW(), NOW()),
  ('建筑燃料',     'GET:/structures/{corpId}/fuel',                    NOW(), NOW()),
  ('建筑服务状态', 'GET:/structures/{corpId}/detail/{structureId}/services', NOW(), NOW()),
  ('建筑统计概览', 'GET:/structures/{corpId}/stats',                   NOW(), NOW()),
  ('建筑增强解锚', 'GET:/structures/{corpId}/timers',                  NOW(), NOW());
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'ADMIN' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = t.perm LIMIT 1),
         NOW(), NOW()
  FROM (SELECT 'GET:/structures/{corpId}' AS perm UNION ALL
        SELECT 'GET:/structures/{corpId}/detail/{structureId}' UNION ALL
        SELECT 'GET:/structures/{corpId}/fuel' UNION ALL
        SELECT 'GET:/structures/{corpId}/detail/{structureId}/services' UNION ALL
        SELECT 'GET:/structures/{corpId}/stats' UNION ALL
        SELECT 'GET:/structures/{corpId}/timers') t;
COMMIT;

-- =====================================================================
-- ⑧ 022: 伴侣 APP 通知表 + RBAC 权限登记
--   ⚠ 执行后须重启应用刷新权限缓存（InitPermissionRolesCache 仅启动重建）
--   或调用 refreshPermRolesRules 管理接口。
--
--   8a. app_notification 建表 (IF NOT EXISTS)
--   8b. 18 个伴侣 APP 端点 RBAC 权限登记(绑定 USER 角色;ADMIN 天然放行)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 8a. app_notification 建表
--   幂等: 表已存在则跳过
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_notification (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       INT          NOT NULL COMMENT '接收者系统用户ID(sys_user.id)',
  event_type    VARCHAR(32)  NOT NULL COMMENT 'INDUSTRY_COMPLETE/FUEL_LOW/SKILL_QUEUE_EMPTY',
  dedup_key     VARCHAR(128) NOT NULL COMMENT '去重键=事件+对象;窗口由查询判定(见1.3)',
  character_id  INT          NULL COMMENT '相关EVE角色ID(燃料事件为NULL)',
  corp_id       INT          NULL COMMENT '相关军团ID(燃料事件)',
  title         VARCHAR(128) NOT NULL COMMENT '通知标题',
  content       VARCHAR(512) NOT NULL COMMENT '通知正文(纯文本)',
  is_read       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '已读标记',
  gmt_create    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  gmt_modified  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  KEY idx_user_created (user_id, gmt_create),
  KEY idx_dedup (dedup_key, gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP通知表(022)';

-- ---------------------------------------------------------------------
-- 8b. 022 特性: 伴侣 APP 端点 RBAC 权限 (18 端点)
--   绑定 USER 角色;ADMIN 天然放行(GlobalConstants.ROOT_ROLE_CODE)无需登记。
--   事务内先删后插, 幂等可重跑。
-- ---------------------------------------------------------------------
START TRANSACTION;
DELETE srp FROM sys_role_permission srp
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
DELETE FROM sys_permission
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
INSERT INTO sys_permission (name, url_perm, gmt_create, gmt_modified) VALUES
  ('角色面板查询',       'GET:/character/{characterId}/panel',          NOW(), NOW()),
  ('角色技能查询',       'GET:/character/{characterId}/skills',         NOW(), NOW()),
  ('技能队列查询',       'GET:/character/{characterId}/skill-queue',    NOW(), NOW()),
  ('邮件收件箱',         'GET:/mail/{characterId}/inbox',               NOW(), NOW()),
  ('邮件标签',           'GET:/mail/{characterId}/labels',              NOW(), NOW()),
  ('邮件详情',           'GET:/mail/{characterId}/{mailId}',            NOW(), NOW()),
  ('标记邮件已读',       'PUT:/mail/{characterId}/{mailId}/read',       NOW(), NOW()),
  ('发送邮件',           'POST:/mail/{characterId}/send',               NOW(), NOW()),
  ('收件人解析',         'POST:/mail/recipients/resolve',               NOW(), NOW()),
  ('工业作业直查',       'GET:/job/{type}/{id}',                        NOW(), NOW()),
  ('团员追踪',           'GET:/corporation/{corpId}/member-tracking',   NOW(), NOW()),
  ('通知列表',           'GET:/notification',                           NOW(), NOW()),
  ('未读通知计数',       'GET:/notification/unread-count',              NOW(), NOW()),
  ('标记通知已读',       'PUT:/notification/{id}/read',                 NOW(), NOW()),
  ('WebSocket票据',      'POST:/notification/ws-ticket',                NOW(), NOW()),
  ('修改密码',           'PUT:/user/password',                          NOW(), NOW()),
  ('解绑角色',           'DELETE:/user/characters/{characterId}',       NOW(), NOW()),
  ('ESI授权链接',        'GET:/character/auth-url',                     NOW(), NOW());
INSERT INTO sys_role_permission (role_id, permission_id, gmt_create, gmt_modified)
  SELECT (SELECT id FROM sys_role WHERE code = 'USER' LIMIT 1),
         (SELECT id FROM sys_permission WHERE url_perm = t.perm LIMIT 1),
         NOW(), NOW()
  FROM (SELECT 'GET:/character/{characterId}/panel' AS perm UNION ALL
        SELECT 'GET:/character/{characterId}/skills' UNION ALL
        SELECT 'GET:/character/{characterId}/skill-queue' UNION ALL
        SELECT 'GET:/mail/{characterId}/inbox' UNION ALL
        SELECT 'GET:/mail/{characterId}/labels' UNION ALL
        SELECT 'GET:/mail/{characterId}/{mailId}' UNION ALL
        SELECT 'PUT:/mail/{characterId}/{mailId}/read' UNION ALL
        SELECT 'POST:/mail/{characterId}/send' UNION ALL
        SELECT 'POST:/mail/recipients/resolve' UNION ALL
        SELECT 'GET:/job/{type}/{id}' UNION ALL
        SELECT 'GET:/corporation/{corpId}/member-tracking' UNION ALL
        SELECT 'GET:/notification' UNION ALL
        SELECT 'GET:/notification/unread-count' UNION ALL
        SELECT 'PUT:/notification/{id}/read' UNION ALL
        SELECT 'POST:/notification/ws-ticket' UNION ALL
        SELECT 'PUT:/user/password' UNION ALL
        SELECT 'DELETE:/user/characters/{characterId}' UNION ALL
        SELECT 'GET:/character/auth-url') t;
COMMIT;

-- =====================================================================
-- ⚠ 最后一步(不可省): 刷新 Redis 权限缓存使 RBAC 规则生效
--
-- 方式一: 调用管理接口(需 ADMIN token)
--   POST /system/permission/refresh
--
-- 方式二: 直接清 Redis 缓存 key
--   redis-cli DEL url_perm_roles_key
--
-- 022 新增端点须重启应用或刷新缓存(InitPermissionRolesCache 仅启动重建)
-- 不刷新缓存的话,新增权限不会生效(RbacAuthorizationManager 读的是 Redis 缓存)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 执行后校验查询(建议跑一遍确认)
-- ---------------------------------------------------------------------

-- 1. 检查所有新增权限是否已登记
SELECT id, name, url_perm FROM sys_permission
WHERE url_perm LIKE '/%'
ORDER BY id DESC;

-- 2. 检查 ADMIN 角色绑定的权限数
SELECT COUNT(*) AS admin_perm_count
FROM sys_role_permission srp
  JOIN sys_role sr ON sr.id = srp.role_id
WHERE sr.code = 'ADMIN';

-- 3. 检查 USER 角色绑定的权限(应仅 GET:/wallet/balance)
SELECT sp.url_perm
FROM sys_role_permission srp
  JOIN sys_role sr ON sr.id = srp.role_id
  JOIN sys_permission sp ON sp.id = srp.permission_id
WHERE sr.code = 'USER';

-- 4. 检查 8 张表的 user_id 列是否已存在
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME = 'user_id'
ORDER BY TABLE_NAME;

-- 5. 检查 structure.corporation_id 索引
SHOW INDEX FROM structure WHERE Key_name = 'idx_structure_corporation_id';
