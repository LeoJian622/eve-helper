-- =====================================================================
-- 010_wallet_transaction_create.sql
-- 用途: 新建钱包交易表 wallet_transaction(人物/军团 ESI /wallet/transactions 落库)。
-- 归属: specs/010-wallet-transactions —— 批次 A 底座 T002 建表迁移
--        US1(人物交易)/US2(军团交易) 的同步幂等与分页查询依赖该表与复合唯一键。
--
-- 【幂等语义】(本脚本 CREATE TABLE IF NOT EXISTS,可重复执行,重复执行不报错不改结构)
--   1. 已存在该表:首段忽略(IF NOT EXISTS),直接结束;
--   2. 首次执行:建表 + 复合唯一键 + 两个索引一并就位。
--
-- 【幂等唯一键设计 D1】
--   与 009 journal 的 PRIMARY(id) 不同,交易流水的 transaction_id 跨 division 是否全局
--   唯一无法保证(不同军团 division 可能各自独立编号),故以复合键承担幂等:
--     UNIQUE(owner_type, owner_id, division, transaction_id)
--   人物侧 owner_type='character' / division=0;军团侧 owner_type='corporation' / division=1..7。
--   insertOrUpdateSelective 的 ON DUPLICATE KEY UPDATE 依该复合键触发,满足同步幂等
--   (T004 saveOrUpdateBatch),重复同步不产生重复行。
--
-- 【索引】
--   idx_owner (owner_type, owner_id, division):分页查询过滤键(plan D4 分页 <500ms);
--   idx_date  (date):ORDER BY `date` DESC 排序复用,date 为 MySQL 保留字,索引列须反引号。
--
-- 主键 id:自增 BIGINT(不依赖 ESI transaction_id 作为主键,唯一约束由复合键承担)。
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