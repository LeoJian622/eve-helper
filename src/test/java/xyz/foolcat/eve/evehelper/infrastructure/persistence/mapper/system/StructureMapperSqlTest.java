package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
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
 * Structure mapper 动态 SQL 解析测试(US2b 军团维度读私有化)。
 *
 * <p>仅解析 XML 不连接数据库。验证建筑军团读查询(selectStructuresWithNames /
 * selectFuelExpiresListWithNames / selectSummary / selectTimers,均按 s.corporation_id 过滤)
 * 在 userId 非 null 时追加 user_id 谓词、null(ROOT 全量)时不追加;selectDetailById /
 * selectServicesById(按结构 ID,无 corp 过滤)不受影响。</p>
 *
 * @author Leojan
 */
@DisplayName("Structure 军团读动态SQL解析测试(US2b)")
class StructureMapperSqlTest {

    private static final String NS =
            "xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.StructureMapper";

    private String sql(String statementId, Map<String, Object> params) throws Exception {
        Configuration cfg = new Configuration();
        try (InputStream in = getClass().getResourceAsStream("/mappers/system/StructureMapper.xml")) {
            new XMLMapperBuilder(in, cfg, "mappers/system/StructureMapper.xml",
                    cfg.getSqlFragments()).parse();
        }
        MappedStatement ms = cfg.getMappedStatement(NS + "." + statementId);
        return ms.getBoundSql(params).getSql().toLowerCase();
    }

    private Map<String, Object> base() {
        Map<String, Object> p = new HashMap<>();
        p.put("corporationId", "98000001");
        p.put("userId", null);
        return p;
    }

    @Test
    @DisplayName("建筑列表 selectStructuresWithNames:userId=null(ROOT)不拼接,非 null 拼接 s.user_id")
    void structuresWithNames_userIdPredicate() throws Exception {
        Map<String, Object> root = base();
        root.put("name", null);
        root.put("state", null);
        root.put("lowFuelOnly", false);
        root.put("sortColumn", null);
        root.put("ascending", false);
        String rootSql = sql("selectStructuresWithNames", root);
        assertTrue(rootSql.contains("s.corporation_id = ?"), rootSql);
        assertFalse(rootSql.contains("s.user_id"), "ROOT(scope null)不得过滤 user_id:" + rootSql);

        Map<String, Object> user = base();
        user.put("userId", 5L);
        user.put("name", null);
        user.put("state", null);
        user.put("lowFuelOnly", false);
        user.put("sortColumn", null);
        user.put("ascending", false);
        String userSql = sql("selectStructuresWithNames", user);
        assertTrue(userSql.contains("and s.user_id = ?"), "同步者须按 user_id 过滤:" + userSql);
    }

    @Test
    @DisplayName("缺油建筑 selectFuelExpiresListWithNames:userId=null 不拼接,非 null 拼接")
    void fuelExpiresWithNames_userIdPredicate() throws Exception {
        Map<String, Object> root = base();
        root.put("hour", 72);
        assertFalse(sql("selectFuelExpiresListWithNames", root).contains("s.user_id"));

        Map<String, Object> user = base();
        user.put("userId", 5L);
        user.put("hour", 72);
        assertTrue(sql("selectFuelExpiresListWithNames", user).contains("and s.user_id = ?"));
    }

    @Test
    @DisplayName("统计概览 selectSummary:userId=null 不拼接,非 null 拼接")
    void summary_userIdPredicate() throws Exception {
        Map<String, Object> root = base();
        assertFalse(sql("selectSummary", root).contains("s.user_id"));

        Map<String, Object> user = base();
        user.put("userId", 5L);
        assertTrue(sql("selectSummary", user).contains("and s.user_id = ?"));
    }

    @Test
    @DisplayName("时间提醒 selectTimers:userId=null 不拼接,非 null 拼接")
    void timers_userIdPredicate() throws Exception {
        Map<String, Object> root = base();
        assertFalse(sql("selectTimers", root).contains("s.user_id"));

        Map<String, Object> user = base();
        user.put("userId", 5L);
        assertTrue(sql("selectTimers", user).contains("and s.user_id = ?"));
    }
}