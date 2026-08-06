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
 * 蓝图 mapper 动态 SQL 解析测试。
 * 仅解析 XML 并生成 SQL 文本，不连接数据库，用于验证 where/choose 分支正确。
 */
@DisplayName("蓝图动态SQL解析测试")
class BlueprintsMapperSqlTest {

    private static final String STATEMENT_ID =
            "xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.BlueprintsMapper"
                    + ".selectBlueprintsInvtypeUniverse";

    /**
     * 解析 mapper XML 并按给定参数生成实际 SQL
     */
    private BoundSql boundSql(Map<String, Object> params) throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream in = getClass().getResourceAsStream("/mappers/system/BlueprintsMapper.xml")) {
            new XMLMapperBuilder(in, configuration,
                    "mappers/system/BlueprintsMapper.xml", configuration.getSqlFragments()).parse();
        }
        MappedStatement statement = configuration.getMappedStatement(STATEMENT_ID);
        return statement.getBoundSql(params);
    }

    private Map<String, Object> params(String blueprintName, Boolean isBlueprintCopy,
                                       String sortColumn, boolean ascending) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", "2112832425");
        params.put("blueprintName", blueprintName);
        params.put("isBlueprintCopy", isBlueprintCopy);
        params.put("sortColumn", sortColumn);
        params.put("ascending", ascending);
        return params;
    }

    @Test
    @DisplayName("无筛选条件时只按所有者查询并使用默认排序")
    void noFilters() throws Exception {
        // Arrange & Act
        String sql = boundSql(params(null, null, null, false)).getSql();

        // Assert
        assertTrue(sql.contains("bt.owner_id = ?"), sql);
        assertFalse(sql.contains("it.name like"), sql);
        assertFalse(sql.contains("is_blueprint_copy ="), sql);
        assertTrue(sql.contains("order by bt.item_id desc"), sql);
    }

    @Test
    @DisplayName("名称筛选生成参数化 like，不拼接字面量")
    void nameFilterIsParameterized() throws Exception {
        // Arrange & Act
        String sql = boundSql(params("恶狼级", null, null, false)).getSql();

        // Assert
        assertTrue(sql.contains("it.name like concat('%', ?, '%')"), sql);
        assertFalse(sql.contains("恶狼级"), "名称必须以占位符传参，不得出现在 SQL 文本中：" + sql);
    }

    @Test
    @DisplayName("拷贝筛选生成参数化等值条件")
    void copyFilterIsParameterized() throws Exception {
        // Arrange & Act
        String sql = boundSql(params(null, Boolean.TRUE, null, false)).getSql();

        // Assert
        assertTrue(sql.contains("ass.is_blueprint_copy = ?"), sql);
    }

    @Test
    @DisplayName("原图筛选须兼容 assets 缺失记录导致的 NULL，否则会漏数据")
    void originalFilterTreatsNullAsOriginal() throws Exception {
        // Arrange & Act：assets 为 left join，缺失记录时 is_blueprint_copy 为 NULL
        String sql = boundSql(params(null, Boolean.FALSE, null, false)).getSql();

        // Assert：NULL = 0 在 SQL 三值逻辑下不成立，必须显式包含 is null
        assertTrue(sql.contains("is null"),
                "原图筛选必须把 NULL 视作原图，否则 assets 无记录的蓝图被静默丢弃：" + sql);
    }

    @Test
    @DisplayName("升序排序按白名单列名生成 order by asc")
    void ascendingSort() throws Exception {
        // Arrange & Act
        String sql = boundSql(params(null, null, "it.name", true)).getSql();

        // Assert：只断言 order by 子句本身，避免被 SQL 其它位置的 desc 字样干扰
        String orderBy = sql.substring(sql.lastIndexOf("order by"));
        assertTrue(orderBy.contains("it.name"), orderBy);
        assertTrue(orderBy.contains("asc"), orderBy);
        assertFalse(orderBy.contains("desc"), orderBy);
    }

    @Test
    @DisplayName("降序排序生成 order by desc")
    void descendingSort() throws Exception {
        // Arrange & Act
        String sql = boundSql(params(null, null, "bt.runs", false)).getSql();

        // Assert
        String orderBy = sql.substring(sql.lastIndexOf("order by"));
        assertTrue(orderBy.contains("bt.runs"), orderBy);
        assertTrue(orderBy.contains("desc"), orderBy);
        assertFalse(orderBy.contains("asc"), orderBy);
    }

    @Test
    @DisplayName("多条件组合时 and 连接正确，无语法残缺")
    void combinedFilters() throws Exception {
        // Arrange & Act
        String sql = boundSql(params("恶狼级", Boolean.FALSE, "bt.quantity", true)).getSql();

        // Assert
        assertTrue(sql.contains("bt.owner_id = ?"), sql);
        assertTrue(sql.contains("and it.name like"), sql);
        assertTrue(sql.contains("ass.is_blueprint_copy"), sql);
        assertTrue(sql.contains("order by bt.quantity"), sql);
    }
}
