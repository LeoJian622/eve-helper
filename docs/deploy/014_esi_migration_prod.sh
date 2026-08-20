#!/usr/bin/env bash
# =====================================================================
# 014_esi_migration_prod.sh — 特性 014「ESI 数据 × 系统用户强关联」生产库迁移（手动部署）
#
# 目标库 : eve_helper（MySQL）   |   回填目标 ROOT admin user_id = 1
# 性质   : 不可逆持久变更(ADD COLUMN + UPDATE 回填)，执行前务必先备份。
# 幂等   : 全部 UPDATE 带 WHERE user_id IS NULL；ADD COLUMN 无 IF NOT EXISTS，
#          需先经第 0 步核对列存在性，缺失者才在第 1 步补（避免 Duplicate column）。
#
# 用法   : 在部署机填写下方 DB_*，逐段执行；建议每步核对输出后再进入下一步。
#       sh docs/deploy/014_esi_migration_prod.sh   # 先 dry 看第 0 步核对
# =====================================================================

set -euo pipefail

# ---------- 连接参数（部署机现场填写） ----------
DB_HOST="${DB_HOST:?DB_HOST 未设置}"
DB_PORT="${DB_PORT:?DB_PORT 未设置}"
DB_USER="${DB_SYSTEM_USERNAME:?DB_SYSTEM_USERNAME 未设置}"
DB_PASS="${DB_SYSTEM_PASSWORD:?DB_SYSTEM_PASSWORD 未设置}"
DB_NAME=eve_helper

ROOT_ADMIN_USER_ID=1

MYSQL="mysql -h $DB_HOST -P $DB_PORT -u$DB_USER -p$DB_PASS $DB_NAME"
MYSQLDUMP="mysqldump -h $DB_HOST -P $DB_PORT -u$DB_USER -p$DB_PASS"

# ---------- 第 0 步：备份 + 核对列现状 ----------
# 全库逻辑备份（务必先做）
"$MYSQLDUMP" "$DB_NAME" > "eve_helper_pre_014_$(date +%F).sql"
echo "[0] 已备份 -> eve_helper_pre_014_$(date +%F).sql"

# 核对哪 8 表已加 user_id 列（输出缺失的即第 1 步需补）
echo "[0] 已存在 user_id 列的表："
"$MYSQL" -e "
SELECT table_name FROM information_schema.columns
WHERE table_schema='$DB_NAME' AND column_name='user_id'
  AND table_name IN ('assets','blueprints','industry_job','mining_detail',
                     'observer','structure','wallet_journal','wallet_transaction')
ORDER BY table_name;"
echo "================================ 上面未列出的表需在第 1 步补列 ================================"

# ---------- 第 1 步：ADD COLUMN（仅对第 0 步缺失的表执行，POST 各自归属列） ----------
# 手动逐表执行（observer 真实列拼写 croporation_id）：
#   ALTER TABLE assets         ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER owner_id;
#   ALTER TABLE blueprints     ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER owner_id;
#   ALTER TABLE mining_detail  ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER character_id;
#   ALTER TABLE wallet_journal ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER division;
#   ALTER TABLE wallet_transaction ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER division;
#   ALTER TABLE industry_job   ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER corporation_id;
#   ALTER TABLE observer       ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER croporation_id;
#   ALTER TABLE structure      ADD COLUMN user_id BIGINT NULL COMMENT '同步者系统用户ID' AFTER corporation_id;

# ---------- 第 2 步：迁移回填（ROOT_ADMIN_USER_ID=1；事务原子，出错 ROLLBACK） ----------
"$MYSQL" <<'SQL'
START TRANSACTION;

-- 人物行 → 角色属主（join eve_account 按 character_id）
UPDATE assets         a JOIN eve_account e ON e.character_id=a.owner_id     SET a.user_id=e.user_id WHERE a.user_id IS NULL;
UPDATE blueprints     b JOIN eve_account e ON e.character_id=b.owner_id     SET b.user_id=e.user_id WHERE b.user_id IS NULL;
UPDATE mining_detail  m JOIN eve_account e ON e.character_id=m.character_id SET m.user_id=e.user_id WHERE m.user_id IS NULL;
UPDATE wallet_journal w JOIN eve_account e ON e.character_id=w.owner_id      SET w.user_id=e.user_id WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.character_id=t.owner_id AND t.owner_type='character' SET t.user_id=e.user_id WHERE t.user_id IS NULL;

-- 军团行 → 管理域（user_id=1）
UPDATE structure      s JOIN eve_account e ON e.corp_id=s.corporation_id SET s.user_id=1 WHERE s.user_id IS NULL;
UPDATE industry_job   i JOIN eve_account e ON e.corp_id=i.corporation_id SET i.user_id=1 WHERE i.user_id IS NULL;
UPDATE observer       o JOIN eve_account e ON e.corp_id=o.croporation_id SET o.user_id=1 WHERE o.user_id IS NULL;
UPDATE wallet_journal w JOIN eve_account e ON e.corp_id=w.owner_id       SET w.user_id=1 WHERE w.user_id IS NULL;
UPDATE wallet_transaction t JOIN eve_account e ON e.corp_id=t.owner_id AND t.owner_type='corporation' SET t.user_id=1 WHERE t.user_id IS NULL;

-- 孤儿行 → 管理域兜底（user_id=1）
UPDATE assets           SET user_id=1 WHERE user_id IS NULL;
UPDATE blueprints       SET user_id=1 WHERE user_id IS NULL;
UPDATE mining_detail    SET user_id=1 WHERE user_id IS NULL;
UPDATE industry_job     SET user_id=1 WHERE user_id IS NULL;
UPDATE observer         SET user_id=1 WHERE user_id IS NULL;
UPDATE structure        SET user_id=1 WHERE user_id IS NULL;
UPDATE wallet_journal   SET user_id=1 WHERE user_id IS NULL;
UPDATE wallet_transaction SET user_id=1 WHERE user_id IS NULL;

COMMIT;
SQL
echo "[2] 回填完成（COMMIT 成功）"

# ---------- 第 3 步：0 丢失核验（每表 COUNT 应与备份前快照一致） ----------
echo "[3] 迁移后各表 COUNT："
"$MYSQL" -e "
SELECT 'assets' t, COUNT(*) c FROM assets
UNION ALL SELECT 'blueprints', COUNT(*) FROM blueprints
UNION ALL SELECT 'industry_job', COUNT(*) FROM industry_job
UNION ALL SELECT 'mining_detail', COUNT(*) FROM mining_detail
UNION ALL SELECT 'observer', COUNT(*) FROM observer
UNION ALL SELECT 'structure', COUNT(*) FROM structure
UNION ALL SELECT 'wallet_journal', COUNT(*) FROM wallet_journal
UNION ALL SELECT 'wallet_transaction', COUNT(*) FROM wallet_transaction;"
echo "======== 核对：每表行数应与备份 eve_helper_pre_014_<date>.sql 前快照一致（0 丢失） ========"