-- AI查询历史记录表
CREATE TABLE IF NOT EXISTS `ai_query_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `user_question` TEXT NOT NULL COMMENT '用户自然语言问题',
    `generated_sql` TEXT NOT NULL COMMENT 'AI生成的SQL语句',
    `result_count` INT DEFAULT 0 COMMENT '返回结果数量',
    `execution_time` BIGINT DEFAULT 0 COMMENT '执行耗时(毫秒)',
    `success` TINYINT(1) DEFAULT 1 COMMENT '是否执行成功',
    `error_message` TEXT COMMENT '错误信息',
    `session_id` VARCHAR(64) COMMENT '会话ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_created_at` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI查询历史记录表';
