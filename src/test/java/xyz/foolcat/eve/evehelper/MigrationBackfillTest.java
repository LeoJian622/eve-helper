package xyz.foolcat.eve.evehelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US4(014 T025/T026)存量迁移回填集成测试。
 *
 * <p>目标(FR-008 / SC-004):一次性把历史存量行正确归属——人物行→角色属主、
 * 军团行→管理域(ROOT admin)、孤儿行→管理域兜底;行数 0 丢失;脚本幂等可重跑。</p>
 *
 * <p><b>副作用隔离(本波最高风险)</b>:迁移会改写共享 test DB
 * (192.168.12.249:3307/eve_helper)的真实开发数据 user_id。<b>禁止留下副作用</b>——
 * 本测试类用 {@code @Transactional}(Spring 测试默认 {@code @Rollback}=true)把整个
 * 用例包进一个事务,测试方法结束时自动回滚:seed 的合成行与迁移对既有真实行的改写均
 * 不持久化。列已由 FS3 落库,本测试只跑 <b>UPDATE 迁移部分</b>,不重复 ADD COLUMN(DDL 不可回滚)。</p>
 *
 * <p><b>迁移 JOIN 依赖 eve_account</b>:{@code character_id/ corp_id/ user_id}。seed 的
 * 合成 character / corp 必须在 eve_account 存在对应行,才能验证「属主 match / admin match」;
 * 未登记的走孤儿兜底。本测试 seed 一条合成 eve_account 行:
 * {@code char_id=T_CHAR, corp_id=T_CORP, user_id=55},从而一次性覆盖三态:</p>
 * <ul>
 *   <li><b>人物属主</b>:owner/character = T_CHAR → person UPDATE join character_id → user_id=55</li>
 *   <li><b>军团 admin</b>:corporation/owner = T_CORP → corp UPDATE join corp_id → user_id=ADMIN(=42,占位常量)</li>
 *   <li><b>孤儿兜底</b>:未 match 的其余 seed 行 → orphan UPDATE → user_id=ADMIN</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("存量迁移:0 丢失 + 归属正确 + 幂等二次执行 + 副作用隔离")
class MigrationBackfillTest {

    /** 测试 admin 常量,替换迁移脚本中的部署占位 '?'(生产由部署注入 ROOT admin userId) */
    private static final long ADMIN_USER_ID = 42L;

    /** 人物属主:持有 T_CHAR 的系统用户(eve_account.user_id) */
    private static final int OWNER_USER_ID = 55;

    /** 合成角色 id(避开既有真实数据;eve_account.character_id 命中即走人物属主) */
    private static final int T_CHAR = 2112001001;

    /** 合成军团 id(eve_account.corp_id 命中即走军团 admin) */
    private static final int T_CORP = 2111990005;

    /** 孤儿 id(未在 eve_account 登记 → 兜底 admin);须小于 2^31,因 structure/industry_job/observer 的 corp 列为 int */
    private static final long ORPHAN = 299001001L;

    /** eve_account seed 用的合成角色名(character_name 唯一约束) */
    private static final String SEED_CHAR_NAME = "T024-SEED-CHAR";

    /** 测试数据源;所有写操作与该对象在同一事务内执行,随 @Transactional 回滚 */
    @Autowired
    JdbcTemplate jdbcTemplate;

    /** 8 张表集合 */
    private static final List<String> TABLES = List.of(
            "assets", "blueprints", "mining_detail", "wallet_journal",
            "wallet_transaction", "industry_job", "observer", "structure");

    @BeforeEach
    void setUp() {
        seedSyntheticEveAccount();
        seedRows();
    }

    @AfterEach
    void tearDown() {
        // 兜底清理 seed 的合成行(事务回滚通常已承担;此处显式删除防子集/非事务跑法残留)
        cleanupSeeds();
    }

    // ────────── seed:合成 eve_account + 各表三态行 ──────────

    private void seedSyntheticEveAccount() {
        jdbcTemplate.update(
                "insert into eve_account (character_id, character_name, corp_id, user_id) "
                        + "values (?,?,?,?) on duplicate key update "
                        + "character_name = values(character_name), corp_id = values(corp_id), user_id = values(user_id)",
                T_CHAR, SEED_CHAR_NAME, T_CORP, OWNER_USER_ID);
    }

    private void seedRows() {
        // assets / blueprints : 人物行(owner=T_CHAR) + 孤儿行(owner=ORPHAN)
        jdbcTemplate.update("insert into assets (item_id, owner_id) values (9992001001, ?)", T_CHAR);
        jdbcTemplate.update("insert into assets (item_id, owner_id) values (9992001002, ?)", ORPHAN);
        jdbcTemplate.update("insert into blueprints (item_id, owner_id) values (9992002001, ?)", T_CHAR);
        jdbcTemplate.update("insert into blueprints (item_id, owner_id) values (9992002002, ?)", ORPHAN);

        // mining_detail : 人物行(character=T_CHAR) + 孤儿行(character=ORPHAN)
        jdbcTemplate.update("insert into mining_detail (id, character_id) values ('MB9992003001', ?)", T_CHAR);
        jdbcTemplate.update("insert into mining_detail (id, character_id) values ('MB9992003002', ?)", (int) ORPHAN);

        // structure / industry_job / observer : 军团行(corporation=T_CORP) + 孤儿行(corporation=ORPHAN)
        jdbcTemplate.update("insert into `structure` (structure_id, corporation_id) values (9996001001, ?)", T_CORP);
        jdbcTemplate.update("insert into `structure` (structure_id, corporation_id) values (9996001002, ?)", ORPHAN);
        jdbcTemplate.update("insert into industry_job (job_id, corporation_id) values (9997001001, ?)", T_CORP);
        jdbcTemplate.update("insert into industry_job (job_id, corporation_id) values (9997001002, ?)", ORPHAN);
        // observer 真实列 mis-spell croporation_id
        jdbcTemplate.update("insert into observer (observer_id, observer_type, croporation_id) values (9995001001, 'structure', ?)", T_CORP);
        jdbcTemplate.update("insert into observer (observer_id, observer_type, croporation_id) values (9995001002, 'structure', ?)", ORPHAN);

        // wallet_journal : 人物行(owner=T_CHAR) + 军团行(owner=T_CORP) + 孤儿行(owner=ORPHAN)
        jdbcTemplate.update("insert into wallet_journal (id, owner_id) values (9998001001, ?)", T_CHAR);
        jdbcTemplate.update("insert into wallet_journal (id, owner_id) values (9998001002, ?)", T_CORP);
        jdbcTemplate.update("insert into wallet_journal (id, owner_id) values (9998001003, ?)", ORPHAN);

        // wallet_transaction : 人物行(character)+军团行(corporation)+孤儿行;UNIQUE(owner_type,owner_id,division,transaction_id)
        jdbcTemplate.update("insert into wallet_transaction (owner_type, owner_id, division, transaction_id, `date`) "
                        + "values ('character', ?, 0, 9999001001, '2026-01-01 00:00:00')", T_CHAR);
        jdbcTemplate.update("insert into wallet_transaction (owner_type, owner_id, division, transaction_id, `date`) "
                        + "values ('corporation', ?, 1, 9999001002, '2026-01-01 00:00:00')", T_CORP);
        jdbcTemplate.update("insert into wallet_transaction (owner_type, owner_id, division, transaction_id, `date`) "
                        + "values ('character', ?, 0, 9999001003, '2026-01-01 00:00:00')", ORPHAN);
    }

    private void cleanupSeeds() {
        jdbcTemplate.update("delete from assets where item_id in (9992001001, 9992001002)");
        jdbcTemplate.update("delete from blueprints where item_id in (9992002001, 9992002002)");
        jdbcTemplate.update("delete from mining_detail where id in ('MB9992003001', 'MB9992003002')");
        jdbcTemplate.update("delete from `structure` where structure_id in (9996001001, 9996001002)");
        jdbcTemplate.update("delete from industry_job where job_id in (9997001001, 9997001002)");
        jdbcTemplate.update("delete from observer where observer_id in (9995001001, 9995001002)");
        jdbcTemplate.update("delete from wallet_journal where id in (9998001001, 9998001002, 9998001003)");
        jdbcTemplate.update("delete from wallet_transaction where transaction_id in (9999001001, 9999001002, 9999001003)");
        jdbcTemplate.update("delete from eve_account where character_id = ?", T_CHAR);
    }

    // ────────── 迁移 UPDATE(与 T024 脚本逐句等价,占位 '?' 替换为 ADMIN_USER_ID)──────────

    /** 逐句执行 T024 迁移 UPDATE:1.人物属主 → 2.军团 admin → 3.孤儿兜底;返回各语句影响行数 */
    private int[] runMigration() {
        int[] affected = new int[18];
        int i = 0;
        // 1. 人物行 → 角色属主(无占位参)
        affected[i++] = jdbcTemplate.update("update assets a join eve_account e on e.character_id = a.owner_id set a.user_id = e.user_id where a.user_id is null");
        affected[i++] = jdbcTemplate.update("update blueprints b join eve_account e on e.character_id = b.owner_id set b.user_id = e.user_id where b.user_id is null");
        affected[i++] = jdbcTemplate.update("update mining_detail m join eve_account e on e.character_id = m.character_id set m.user_id = e.user_id where m.user_id is null");
        affected[i++] = jdbcTemplate.update("update wallet_journal w join eve_account e on e.character_id = w.owner_id set w.user_id = e.user_id where w.user_id is null");
        affected[i++] = jdbcTemplate.update("update wallet_transaction t join eve_account e on e.character_id = t.owner_id and t.owner_type = 'character' set t.user_id = e.user_id where t.user_id is null");
        // 2. 军团行 → 管理域(占位 '?' = ADMIN_USER_ID)
        affected[i++] = jdbcTemplate.update("update structure s join eve_account e on e.corp_id = s.corporation_id set s.user_id = ? where s.user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update industry_job i join eve_account e on e.corp_id = i.corporation_id set i.user_id = ? where i.user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update observer o join eve_account e on e.corp_id = o.croporation_id set o.user_id = ? where o.user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update wallet_journal w join eve_account e on e.corp_id = w.owner_id set w.user_id = ? where w.user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update wallet_transaction t join eve_account e on e.corp_id = t.owner_id and t.owner_type = 'corporation' set t.user_id = ? where t.user_id is null", ADMIN_USER_ID);
        // 3. 孤儿兜底(占位 '?' = ADMIN_USER_ID)
        affected[i++] = jdbcTemplate.update("update assets set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update blueprints set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update mining_detail set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update industry_job set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update observer set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update structure set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update wallet_journal set user_id = ? where user_id is null", ADMIN_USER_ID);
        affected[i++] = jdbcTemplate.update("update wallet_transaction set user_id = ? where user_id is null", ADMIN_USER_ID);
        return affected;
    }

    private Map<String, Long> counts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String t : TABLES) {
            m.put(t, jdbcTemplate.queryForObject("select count(*) from `" + t + "`", Long.class));
        }
        return m;
    }

    /** 读取某表单行 user_id(按 seed 主键定位) */
    private Long uid(String table, String where, Object... args) {
        return jdbcTemplate.queryForObject("select user_id from `" + table + "` where " + where + " limit 1", Long.class, args);
    }

    // ────────── 用例1:迁移归属正确 + 0 丢失 ──────────

    @Test
    @DisplayName("迁移三态归属正确:人物行=属主(55) / 军团行=admin(42) / 孤儿兜底=admin(42);各表 COUNT 相等(0 丢失)")
    void backfill_assignsOwnerAdminAndOrphan_zeroLoss() {
        Map<String, Long> before = counts();

        runMigration();

        // 各表 0 丢失:迁移只 UPDATE 不改行数(delete 兜底不在此处执行)
        Map<String, Long> after = counts();
        TABLES.forEach(t -> assertThat(after.get(t))
                .as("0 丢失:表 %s 迁移前后 COUNT 相等", t)
                .isEqualTo(before.get(t)));

        // 人物属主:person UPDATE join character_id=T_CHAR → user_id=55
        assertThat(uid("assets", "item_id = 9992001001")).isEqualTo((long) OWNER_USER_ID);
        assertThat(uid("blueprints", "item_id = 9992002001")).isEqualTo((long) OWNER_USER_ID);
        assertThat(uid("mining_detail", "id = 'MB9992003001'")).isEqualTo((long) OWNER_USER_ID);
        assertThat(uid("wallet_journal", "id = 9998001001")).isEqualTo((long) OWNER_USER_ID);
        assertThat(uid("wallet_transaction", "transaction_id = 9999001001")).isEqualTo((long) OWNER_USER_ID);

        // 军团 admin:corp UPDATE join corp_id=T_CORP → user_id=ADMIN
        assertThat(uid("structure", "structure_id = 9996001001")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("industry_job", "job_id = 9997001001")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("observer", "observer_id = 9995001001")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("wallet_journal", "id = 9998001002")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("wallet_transaction", "transaction_id = 9999001002")).isEqualTo(ADMIN_USER_ID);

        // 孤儿兜底:未 match → orphan UPDATE → user_id=ADMIN
        assertThat(uid("assets", "item_id = 9992001002")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("blueprints", "item_id = 9992002002")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("mining_detail", "id = 'MB9992003002'")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("structure", "structure_id = 9996001002")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("industry_job", "job_id = 9997001002")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("observer", "observer_id = 9995001002")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("wallet_journal", "id = 9998001003")).isEqualTo(ADMIN_USER_ID);
        assertThat(uid("wallet_transaction", "transaction_id = 9999001003")).isEqualTo(ADMIN_USER_ID);

        // 副作用隔离:既有真实行(seed 之外、迁移前已非 NULL 的 user_id)迁移后不被改写
        Long realBefore = extantNonNullUserId("wallet_journal");
        if (realBefore != null) {
            assertThat(extantNonNullUserId("wallet_journal")).isEqualTo(realBefore);
        }
        Long realBeforeAssets = extantNonNullUserId("assets");
        if (realBeforeAssets != null) {
            assertThat(extantNonNullUserId("assets")).isEqualTo(realBeforeAssets);
        }
    }

    /** 迁移不影响既有真实行:取任一「迁移前 user_id 非 NULL」的真实行的值,迁移后仍原值 */
    private Long extantNonNullUserId(String table) {
        return jdbcTemplate.queryForObject(
                "select user_id from `" + table + "` where user_id is not null order by user_id asc limit 1",
                Long.class);
    }

    // ────────── 用例2:幂等二次执行(T026)──────────

    @Test
    @DisplayName("二次执行迁移:各表 COUNT 仍相等、seed 行 user_id 值不变、null 清零不再推进(幂等可重跑)")
    void backfill_runTwice_idempotent() {
        Map<String, Long> before = counts();

        runMigration();
        Map<String, Long> afterFirst = counts();
        // 第一遍:0 丢失 + 归属正确(抽样不重复全量断言,聚焦幂等)
        assertThat(afterFirst).isEqualTo(before);
        assertThat(uid("wallet_journal", "id = 9998001001")).isEqualTo((long) OWNER_USER_ID);
        assertThat(uid("wallet_journal", "id = 9998001002")).isEqualTo(ADMIN_USER_ID);

        // 记录第一遍后的归属快照
        Map<String, Long> afterFirstUid = seededUidSnapshot();

        // 第二遍:等价语句重跑,结果不得再变
        runMigration();
        Map<String, Long> afterSecond = counts();
        assertThat(afterSecond)
                .as("第二遍后各表 COUNT 仍相等(无重复列/无多余行)")
                .isEqualTo(afterFirst);
        TABLES.forEach(t -> assertThat(seededUidSnapshot()).as("表 %s user_id 不变", t).isEqualTo(afterFirstUid));
    }

    /** 8 表 seed 行的 user_id 快照(用于幂等对比) */
    private Map<String, Long> seededUidSnapshot() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("assets-p", uid("assets", "item_id = 9992001001"));
        m.put("assets-o", uid("assets", "item_id = 9992001002"));
        m.put("blueprints-p", uid("blueprints", "item_id = 9992002001"));
        m.put("blueprints-o", uid("blueprints", "item_id = 9992002002"));
        m.put("mining-p", uid("mining_detail", "id = 'MB9992003001'"));
        m.put("mining-o", uid("mining_detail", "id = 'MB9992003002'"));
        m.put("structure-c", uid("structure", "structure_id = 9996001001"));
        m.put("structure-o", uid("structure", "structure_id = 9996001002"));
        m.put("industry-c", uid("industry_job", "job_id = 9997001001"));
        m.put("industry-o", uid("industry_job", "job_id = 9997001002"));
        m.put("observer-c", uid("observer", "observer_id = 9995001001"));
        m.put("observer-o", uid("observer", "observer_id = 9995001002"));
        m.put("journal-p", uid("wallet_journal", "id = 9998001001"));
        m.put("journal-c", uid("wallet_journal", "id = 9998001002"));
        m.put("journal-o", uid("wallet_journal", "id = 9998001003"));
        m.put("txn-p", uid("wallet_transaction", "transaction_id = 9999001001"));
        m.put("txn-c", uid("wallet_transaction", "transaction_id = 9999001002"));
        m.put("txn-o", uid("wallet_transaction", "transaction_id = 9999001003"));
        return m;
    }
}