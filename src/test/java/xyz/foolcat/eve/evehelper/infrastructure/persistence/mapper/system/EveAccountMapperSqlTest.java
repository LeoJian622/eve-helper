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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 角色绑定 mapper SQL 解析测试。
 * 仅解析 XML 并生成 SQL 文本，不连接数据库。
 * <p>
 * 对应 006 feature T002(FR-015，设计评审 H3)：{@code queryAccountList} 漏 select
 * {@code user_id}，而 BaseResultMap 映射了该列，导致返回的 EveAccount.userId 恒为 null，
 * 进而使 accessToken 被写入 {@code esi_access_token:null:{characterId}} 孤儿键，
 * 破坏缓存键的用户隔离。
 *
 * @author Leojan
 * date 2026-08-11
 */
@DisplayName("角色绑定 mapper SQL 解析测试")
class EveAccountMapperSqlTest {

    private static final String MAPPER_NAMESPACE =
            "xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper";

    /**
     * 解析 mapper XML 并按给定语句 ID 生成实际 SQL
     */
    private BoundSql boundSql(String statementId, Map<String, Object> params) throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream in = getClass().getResourceAsStream("/mappers/system/EveAccountMapper.xml")) {
            new XMLMapperBuilder(in, configuration,
                    "mappers/system/EveAccountMapper.xml", configuration.getSqlFragments()).parse();
        }
        MappedStatement statement = configuration.getMappedStatement(MAPPER_NAMESPACE + "." + statementId);
        return statement.getBoundSql(params);
    }

    @Test
    @DisplayName("queryAccountList 必须 select user_id，否则 EveAccount.userId 恒为 null 破坏缓存键隔离")
    void queryAccountListSelectsUserId() throws Exception {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 100);

        // Act
        String sql = boundSql("queryAccountList", params).getSql();

        // Assert：user_id 必须出现在 select 列表(where 子句之前)，而非仅出现在 where 条件里
        String selectClause = sql.substring(0, sql.toLowerCase().indexOf("where"));
        assertTrue(selectClause.contains("user_id"),
                "queryAccountList 的 select 列表必须含 user_id，否则 BaseResultMap 无法填充 "
                        + "EveAccount.userId，accessToken 将被写入 esi_access_token:null:{cid} 孤儿键。实际 SQL：" + sql);
    }

    @Test
    @DisplayName("queryAccountList 按 user_id 过滤，参数化传参")
    void queryAccountListFiltersByUserId() throws Exception {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 100);

        // Act
        String sql = boundSql("queryAccountList", params).getSql();

        // Assert
        assertTrue(sql.contains("user_id = ?"), sql);
    }

    @Test
    @DisplayName("queryOneUserIdAndCharacterId 必须同时按 user_id 与 character_id 精确匹配")
    void queryOneRequiresBothUserIdAndCharacterId() throws Exception {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 100);
        params.put("cId", 95465499);

        // Act
        String sql = boundSql("queryOneUserIdAndCharacterId", params).getSql();

        // Assert：这是 006 feature 归属校验的唯一依据，必须是两列等值 AND
        assertTrue(sql.contains("user_id = ?"), sql);
        assertTrue(sql.contains("character_id = ?"), sql);
        assertTrue(sql.toLowerCase().contains("and"),
                "归属校验必须 user_id AND character_id 同时匹配，不得退化为单列查询：" + sql);
    }
}
