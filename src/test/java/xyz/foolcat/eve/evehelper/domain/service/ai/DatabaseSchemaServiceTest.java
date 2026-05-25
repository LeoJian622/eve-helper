package xyz.foolcat.eve.evehelper.domain.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 数据库Schema服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class DatabaseSchemaServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private ResultSet tablesResultSet;

    @Mock
    private ResultSet columnsResultSet;

    @InjectMocks
    private DatabaseSchemaService schemaService;

    @Test
    @DisplayName("应该成功获取数据库Schema信息")
    void shouldGetDatabaseSchema() throws Exception {
        // Setup
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(false);

        // Execute
        String schema = schemaService.getSchemaContext();

        // Verify
        assertNotNull(schema);
    }

    @Test
    @DisplayName("应该正确处理数据库连接异常")
    void shouldHandleDatabaseConnectionException() throws Exception {
        // Setup
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Execute & Verify
        assertThrows(RuntimeException.class, () -> schemaService.getSchemaContext());
    }
}
