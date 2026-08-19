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
 * WalletTransaction mapper 动态 SQL 解析测试(US2b 军团维度读私有化)。
 *
 * <p>仅解析 XML 不连接数据库。验证军团读(owner_type='corporation')在 userId 非 null 时
 * 追加 user_id 谓词、null(ROOT全量)时不追加;人物读(已是同一语句,userId=null)不触发谓词。</p>
 *
 * @author Leojan
 */
@DisplayName("WalletTransaction 军团读动态SQL解析测试(US2b)")
class WalletTransactionMapperSqlTest {

    private static final String NS =
            "xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.WalletTransactionMapper";

    private String sql(Map<String, Object> params) throws Exception {
        Configuration cfg = new Configuration();
        try (InputStream in = getClass().getResourceAsStream("/mappers/system/WalletTransactionMapper.xml")) {
            new XMLMapperBuilder(in, cfg, "mappers/system/WalletTransactionMapper.xml",
                    cfg.getSqlFragments()).parse();
        }
        MappedStatement ms = cfg.getMappedStatement(NS + ".selectPageByOwner");
        return ms.getBoundSql(params).getSql().toLowerCase();
    }

    private Map<String, Object> params(String ownerType, Object userId) {
        Map<String, Object> p = new HashMap<>();
        p.put("ownerType", ownerType);
        p.put("ownerId", 98454654L);
        p.put("division", divisionFor(ownerType));
        p.put("userId", userId);
        return p;
    }

    private Integer divisionFor(String ownerType) {
        return "corporation".equals(ownerType) ? 2 : 0;
    }

    @Test
    @DisplayName("人物读(owner_type=character, userId=null):不拼接 user_id 谓词(FR-004)")
    void characterRead_userIdNull_noPredicate() throws Exception {
        String sql = sql(params("character", null));
        assertTrue(sql.contains("owner_type = ?"), sql);
        assertFalse(sql.contains("user_id"), "人物读不得拼接 user_id 谓词:" + sql);
    }

    @Test
    @DisplayName("军团读(owner_type=corporation):userId=null(ROOT)不拼接,非 null 拼接")
    void corporationRead_userIdPredicate() throws Exception {
        String rootSql = sql(params("corporation", null));
        assertFalse(rootSql.contains("user_id"), "ROOT(scope null)不得过滤 user_id:" + rootSql);

        String userSql = sql(params("corporation", 5L));
        assertTrue(userSql.contains("and user_id = ?"), "同步者须按 user_id 过滤:" + userSql);
    }
}