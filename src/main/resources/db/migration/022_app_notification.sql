-- 022 伴侣 APP 通知表。幂等：CREATE TABLE IF NOT EXISTS。
-- 归属：user_id = 接收者系统用户（sys_user.id）；燃料事件按权限扇出，一事件多行（每接收者一行，dedup_key 相同）。
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
