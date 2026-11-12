package xyz.foolcat.eve.evehelper.interfaces.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import xyz.foolcat.eve.evehelper.application.dto.response.NotificationItem;
import xyz.foolcat.eve.evehelper.application.dto.response.UnreadCountResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.WsTicketResponse;
import xyz.foolcat.eve.evehelper.application.service.NotificationApplicationService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通知控制器单元测试(T027, 4 用例)。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US4 通知控制器测试")
class NotificationControllerTest {

    @Mock
    private NotificationApplicationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .build();
    }

    // ---------------------------------------------------------------
    //  GET /notification — 分页
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /notification 返回分页信封")
    void page_returnsPageResultEnvelope() throws Exception {
        Date now = new Date();
        NotificationItem item = new NotificationItem(
                1L, "MAIL_RECEIVED", "新邮件", "内容",
                95465499, 98000001, false, now);

        PageResult<NotificationItem> pageResult = PageResult.<NotificationItem>builder()
                .records(List.of(item))
                .total(1L)
                .current(1L)
                .size(20L)
                .pages(1L)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(notificationService.page(isNull(), any(PageQuery.class))).thenReturn(pageResult);

        mockMvc.perform(get("/notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].eventType").value("MAIL_RECEIVED"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(notificationService).page(isNull(), any(PageQuery.class));
    }

    // ---------------------------------------------------------------
    //  GET /notification/unread-count
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /notification/unread-count 返回未读数")
    void unreadCount_returnsCount() throws Exception {
        when(notificationService.unreadCount()).thenReturn(new UnreadCountResponse(5L));

        mockMvc.perform(get("/notification/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.count").value(5));

        verify(notificationService).unreadCount();
    }

    // ---------------------------------------------------------------
    //  PUT /notification/{id}/read
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PUT /notification/{id}/read 返回 200 并调用 service.markRead")
    void markRead_returns200AndCallsService() throws Exception {
        mockMvc.perform(put("/notification/{id}/read", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(notificationService).markRead(42L);
    }

    // ---------------------------------------------------------------
    //  POST /notification/ws-ticket
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /notification/ws-ticket 返回 ticket 和 expiresInSeconds=30")
    void wsTicket_returnsTicketAndExpiry() throws Exception {
        when(notificationService.createWsTicket())
                .thenReturn(new WsTicketResponse("abc-123-uuid", 30));

        mockMvc.perform(post("/notification/ws-ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.ticket").value("abc-123-uuid"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(30));

        verify(notificationService).createWsTicket();
    }
}
