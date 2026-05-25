package xyz.foolcat.eve.evehelper.domain.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AiQueryHistory;
import xyz.foolcat.eve.evehelper.domain.repository.system.AiQueryHistoryRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 查询历史服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class QueryHistoryServiceTest {

    @Mock
    private AiQueryHistoryRepository historyRepository;

    @InjectMocks
    private QueryHistoryService queryHistoryService;

    private AiQueryHistory testHistory1;
    private AiQueryHistory testHistory2;

    @BeforeEach
    void setUp() {
        testHistory1 = new AiQueryHistory();
        testHistory1.setId(1L);
        testHistory1.setUserId(1001);
        testHistory1.setUserQuestion("查询所有订单");
        testHistory1.setGeneratedSql("SELECT * FROM orders LIMIT 50");
        testHistory1.setSuccess(true);
        testHistory1.setSessionId("session-123");

        testHistory2 = new AiQueryHistory();
        testHistory2.setId(2L);
        testHistory2.setUserId(1001);
        testHistory2.setUserQuestion("查询失败的记录");
        testHistory2.setSuccess(false);
        testHistory2.setSessionId("session-123");
    }

    @Test
    @DisplayName("应该获取用户的查询历史")
    void shouldGetUserHistory() {
        when(historyRepository.findByUserId(eq(1001), anyInt()))
                .thenReturn(Arrays.asList(testHistory1, testHistory2));

        List<AiQueryHistory> history = queryHistoryService.getUserHistory(1001, 10);

        assertNotNull(history);
        assertEquals(2, history.size());
        verify(historyRepository).findByUserId(eq(1001), eq(10));
    }

    @Test
    @DisplayName("应该根据ID获取查询详情")
    void shouldGetHistoryById() {
        when(historyRepository.findById(1L)).thenReturn(testHistory1);

        AiQueryHistory result = queryHistoryService.getHistoryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("查询所有订单", result.getUserQuestion());
    }

    @Test
    @DisplayName("应该删除查询历史记录")
    void shouldDeleteHistory() {
        doNothing().when(historyRepository).deleteById(1L);

        queryHistoryService.deleteHistory(1L);

        verify(historyRepository).deleteById(1L);
    }

    @Test
    @DisplayName("应该保存查询历史")
    void shouldSaveHistory() {
        when(historyRepository.save(testHistory1)).thenReturn(testHistory1);

        AiQueryHistory result = queryHistoryService.saveHistory(testHistory1);

        assertNotNull(result);
        assertEquals(testHistory1.getId(), result.getId());
        verify(historyRepository).save(testHistory1);
    }

    @Test
    @DisplayName("应该只获取成功的查询记录")
    void shouldGetOnlySuccessfulQueries() {
        when(historyRepository.findByUserId(eq(1001), anyInt()))
                .thenReturn(Arrays.asList(testHistory1, testHistory2));

        List<AiQueryHistory> result =
                queryHistoryService.getRecentSuccessfulQueries(1001, "session-123", 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getSuccess());
    }

    @Test
    @DisplayName("应该按会话ID过滤查询记录")
    void shouldFilterQueriesBySessionId() {
        AiQueryHistory otherSessionHistory = new AiQueryHistory();
        otherSessionHistory.setId(3L);
        otherSessionHistory.setUserId(1001);
        otherSessionHistory.setSuccess(true);
        otherSessionHistory.setSessionId("other-session");

        when(historyRepository.findByUserId(eq(1001), anyInt()))
                .thenReturn(Arrays.asList(testHistory1, otherSessionHistory));

        List<AiQueryHistory> result =
                queryHistoryService.getRecentSuccessfulQueries(1001, "session-123", 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("session-123", result.get(0).getSessionId());
    }

    @Test
    @DisplayName("会话ID为空时应该返回所有成功记录")
    void shouldReturnAllWhenSessionIdIsNull() {
        AiQueryHistory otherSessionHistory = new AiQueryHistory();
        otherSessionHistory.setId(3L);
        otherSessionHistory.setUserId(1001);
        otherSessionHistory.setSuccess(true);
        otherSessionHistory.setSessionId("other-session");

        when(historyRepository.findByUserId(eq(1001), anyInt()))
                .thenReturn(Arrays.asList(testHistory1, otherSessionHistory));

        List<AiQueryHistory> result =
                queryHistoryService.getRecentSuccessfulQueries(1001, null, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("应该限制返回的记录数量")
    void shouldLimitResultCount() {
        when(historyRepository.findByUserId(eq(1001), anyInt()))
                .thenReturn(Arrays.asList(testHistory1, testHistory1, testHistory1));

        List<AiQueryHistory> result =
                queryHistoryService.getRecentSuccessfulQueries(1001, "session-123", 2);

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
