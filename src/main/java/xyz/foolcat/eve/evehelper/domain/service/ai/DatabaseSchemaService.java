package xyz.foolcat.eve.evehelper.domain.service.ai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库Schema元数据服务
 * 提供数据库表结构信息，用于AI生成SQL时的上下文
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSchemaService {

    private final DataSource dataSource;

    private String cachedSchemaContext;

    /**
     * 初始化时加载Schema信息
     */
    @PostConstruct
    public void init() {
        try {
            this.cachedSchemaContext = loadSchemaContext();
            log.info("Database schema loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load database schema: {}", e.getMessage());
            this.cachedSchemaContext = "Schema information unavailable";
        }
    }

    /**
     * 获取数据库Schema上下文
     *
     * @return Schema描述文本
     */
    @Cacheable(value = "dbSchemaCache", key = "'schemaContext'")
    public String getSchemaContext() {
        if (cachedSchemaContext == null) {
            cachedSchemaContext = loadSchemaContext();
        }
        return cachedSchemaContext;
    }

    /**
     * 生成Schema描述文本
     *
     * @return Schema描述
     */
    public String getSchemaDescription() {
        return getSchemaContext();
    }

    private String loadSchemaContext() {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("Database Schema:\n\n");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schemaPattern = null;

            // 获取所有表
            List<String> tables = getTables(metaData, catalog, schemaPattern);

            for (String tableName : tables) {
                // 跳过系统表
                if (isSystemTable(tableName)) {
                    continue;
                }

                schemaBuilder.append("Table: ").append(tableName).append("\n");

                // 获取表注释
                String tableComment = getTableComment(metaData, catalog, schemaPattern, tableName);
                if (tableComment != null && !tableComment.isEmpty()) {
                    schemaBuilder.append("  Comment: ").append(tableComment).append("\n");
                }

                // 获取列信息
                schemaBuilder.append("  Columns:\n");
                try (ResultSet columnsRs = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
                    while (columnsRs.next()) {
                        String columnName = columnsRs.getString("COLUMN_NAME");
                        String columnType = columnsRs.getString("TYPE_NAME");
                        String columnComment = columnsRs.getString("REMARKS");

                        schemaBuilder.append("    - ").append(columnName)
                                .append(" (").append(columnType).append(")");

                        if (columnComment != null && !columnComment.isEmpty()) {
                            schemaBuilder.append(" - ").append(columnComment);
                        }
                        schemaBuilder.append("\n");
                    }
                }
                schemaBuilder.append("\n");
            }

        } catch (SQLException e) {
            log.error("Error loading database schema: {}", e.getMessage());
            throw new RuntimeException("Failed to load database schema", e);
        }

        return schemaBuilder.toString();
    }

    private List<String> getTables(DatabaseMetaData metaData, String catalog, String schemaPattern) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet tablesRs = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
            while (tablesRs.next()) {
                tables.add(tablesRs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private String getTableComment(DatabaseMetaData metaData, String catalog, String schemaPattern, String tableName) throws SQLException {
        try (ResultSet tablesRs = metaData.getTables(catalog, schemaPattern, tableName, new String[]{"TABLE"})) {
            if (tablesRs.next()) {
                return tablesRs.getString("REMARKS");
            }
        }
        return null;
    }

    private boolean isSystemTable(String tableName) {
        String lowerName = tableName.toLowerCase();
        return lowerName.startsWith("sys_")
                || lowerName.startsWith("information_schema")
                || lowerName.startsWith("mysql")
                || lowerName.startsWith("performance_schema");
    }
}
