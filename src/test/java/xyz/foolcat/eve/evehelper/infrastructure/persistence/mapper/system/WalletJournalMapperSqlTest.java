package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WalletJournal mapper 动态 SQL 解析测试(US2b 军团维度读私有化)。
 *
 * <p>仅解析 XML 并生成 SQL 文本,不连接数据库。验证军团读语句在
 * {@code userId=null}(ROOT 全量)时不拼接 user_id 谓词、非 null(同步者)时拼接,
 * 而人物读语句({@code selectPageByOwnerId})恒不加谓词(FR-004 零改动)。</p>
 *
 * @author Leojan
 */
@DisplayName("WalletJournal 军团读动态SQL解析测试(US2b)")
class WalletJournalMapperSqlTest {

    private static final String NS =
            "xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletJournalMapper";

    /** 解析 mapper XML 并按给定参数生成实际 SQL */
    private String sql(String statementId, Map<String, Object> params) throws Exception {
        Configuration cfg = new Configuration();
        try (InputStream in = getClass().getResourceAsStream("/mappers/system/WalletJournalMapper.xml")) {
            new XMLMapperBuilder(in, cfg, "mappers/system/WalletJournalMapper.xml",
                    cfg.getSqlFragments()).parse();
        }
        MappedStatement ms = cfg.getMappedStatement(NS + "." + statementId);
        return ms.getBoundSql(params).getSql().toLowerCase();
    }

    // 军团读列出参:owner_id/division 恒有
    private Map<String, Object> corpParams(Object userId) {
        Map<String, Object> p = new HashMap<>();
        p.put("ownerId", 5001L);
        p.put("division", 2);
        p.put("userId", userId);
        return p;
    }

    // 总览参(共享),人物侧 userId=null
    private Map<String, Object> overviewParams(Integer division, Object userId) {
        Map<String, Object> p = new HashMap<>();
        p.put("ownerId", 98454654L);
        p.put("division", division);
        p.put("userId", userId);
        p.put("start", null);
        p.put("end", null);
        return p;
    }

    @Test
    @DisplayName("人物读 selectPageByOwnerId:恒不拼接 user_id 谓词(FR-004 零改动)")
    void characterRead_neverFiltersByUserId() throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("ownerId", 9001L);
        String sql = sql("selectPageByOwnerId", p);
        assertTrue(sql.contains("owner_id = ?"), sql);
        assertFalse(sql.contains("user_id"), "人物读语句不得拼接 user_id 谓词:" + sql);
    }

    @Test
    @DisplayName("军团分账读 selectPageByOwnerAndDivision:userId=null(ROOT)不拼接,非 null 拼接")
    void corpsDivisionRead_userIdPredicate() throws Exception {
        String rootSql = sql("selectPageByOwnerAndDivision", corpParams(null));
        assertTrue(rootSql.contains("owner_id = ?"), rootSql);
        assertFalse(rootSql.contains("user_id"), "ROOT(scope null)不得过滤 user_id:" + rootSql);

        String userSql = sql("selectPageByOwnerAndDivision", corpParams(5L));
        assertTrue(userSql.contains("owner_id = ?"), userSql);
        assertTrue(userSql.contains("and user_id = ?"), "同步者须按 user_id 过滤:" + userSql);
    }

    @Test
    @DisplayName("总览聚合 selectOverviewAggregate:userId 谓词贯穿外层与子查询(ROOT null / 非 null)")
    void overviewAggregate_userIdPredicate() throws Exception {
        String rootSql = sql("selectOverviewAggregate", overviewParams(null, null));
        assertFalse(rootSql.contains("user_id"), "ROOT 全量不得过滤 user_id:" + rootSql);

        String userSql = sql("selectOverviewAggregate", overviewParams(null, 5L));
        assertTrue(userSql.contains("and w.user_id = ?"), "外层聚合须过滤 user_id:" + userSql);
        assertTrue(userSql.contains("and w2.user_id = ?"), "currentBalance 子查询须过滤 user_id:" + userSql);
        assertTrue(userSql.contains("and w3.user_id = ?"), "asOfTime 子查询须过滤 user_id:" + userSql);
    }

    @Test
    @DisplayName("总览类目 selectOverviewCategories:ROOT null 不拼接,非 null 拼接")
    void overviewCategories_userIdPredicate() throws Exception {
        assertFalse(sql("selectOverviewCategories", overviewParams(null, null)).contains("user_id"));
        assertTrue(sql("selectOverviewCategories", overviewParams(null, 5L)).contains("and w.user_id = ?"));
    }

    @Test
    @DisplayName("总览趋势 selectOverviewTrend:ROOT null 不拼接,非 null 拼接")
    void overviewTrend_userIdPredicate() throws Exception {
        Map<String, Object> trend = overviewParams(null, null);
        trend.put("granularity", "%Y-%m");
        assertFalse(sql("selectOverviewTrend", trend).contains("user_id"));

        Map<String, Object> trendU = overviewParams(null, 5L);
        trendU.put("granularity", "%Y-%m");
        assertTrue(sql("selectOverviewTrend", trendU).contains("and w.user_id = ?"));
    }

    @Test
    @DisplayName("分账余额 selectOverviewDivisionBalances:ROOT null 不拼接,非 null 外层+子查询拼接")
    void overviewDivisionBalances_userIdPredicate() throws Exception {
        Map<String, Object> root = new HashMap<>();
        root.put("ownerId", 98454654L);
        root.put("userId", null);
        String rootSql = sql("selectOverviewDivisionBalances", root);
        assertFalse(rootSql.contains("user_id"), "ROOT 全量不得过滤 user_id:" + rootSql);

        Map<String, Object> user = new HashMap<>();
        user.put("ownerId", 98454654L);
        user.put("userId", 5L);
        String userSql = sql("selectOverviewDivisionBalances", user);
        // MAX(id) 子查询按 user_id 过滤(限定 user 各分账最新行),外层 no-谓词由 m 关联限定
        assertTrue(userSql.contains("and user_id = ?"), "MAX(id) 子查询须按 user_id 过滤:" + userSql);
    }

    @Test
    @DisplayName("分账收支 selectOverviewDivisionFlow:ROOT null 不拼接,非 null 拼接")
    void overviewDivisionFlow_userIdPredicate() throws Exception {
        Map<String, Object> root = new HashMap<>();
        root.put("ownerId", 98454654L);
        root.put("userId", null);
        root.put("start", null);
        root.put("end", null);
        assertFalse(sql("selectOverviewDivisionFlow", root).contains("user_id"));

        Map<String, Object> user = new HashMap<>();
        user.put("ownerId", 98454654L);
        user.put("userId", 5L);
        user.put("start", null);
        user.put("end", null);
        assertTrue(sql("selectOverviewDivisionFlow", user).contains("user_id = ?"));
    }
}