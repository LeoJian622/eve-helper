package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.dto.response.NotificationItem;
import xyz.foolcat.eve.evehelper.application.dto.response.UnreadCountResponse;
import xyz.foolcat.eve.evehelper.application.dto.response.WsTicketResponse;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.AppNotification;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.AppNotificationRepository;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageQuery;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.result.ResultCode;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知应用服务单元测试(T027, 5 用例)。
 *
 * @author Leojian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US4 通知应用服务测试")
class NotificationServiceTest {

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private CacheGateway cacheGateway;

    private static final Integer USER_ID = 42;

    // ---------------------------------------------------------------
    //  page — 只返回自己的通知
    // ---------------------------------------------------------------

    @Test
    @DisplayName("page 只返回当前用户的通知并逐字段映射")
    void page_onlyOwnNotifications() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // Arrange — 合成 AppNotification 具体值
            Date now = new Date();
            AppNotification entity = new AppNotification();
            entity.setId(100L);
            entity.setUserId(USER_ID);
            entity.setEventType("MAIL_RECEIVED");
            entity.setTitle("新邮件");
            entity.setContent("你收到了一封邮件");
            entity.setCharacterId(95465499);
            entity.setCorpId(98000001);
            entity.setIsRead(false);
            entity.setGmtCreate(now);

            PageResult<AppNotification> repoResult = PageResult.<AppNotification>builder()
                    .records(List.of(entity))
                    .total(1L)
                    .current(1L)
                    .size(20L)
                    .pages(1L)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();

            when(notificationRepository.pageByUser(eq(USER_ID), eq(null), any(PageQuery.class)))
                    .thenReturn(repoResult);

            NotificationApplicationService service =
                    new NotificationApplicationService(notificationRepository, cacheGateway);
            PageQuery pageQuery = PageQuery.builder().current(1).size(20).build();

            // Act
            PageResult<NotificationItem> result = service.page(null, pageQuery);

            // Assert — 逐字段验证映射
            assertThat(result.getRecords()).hasSize(1);
            NotificationItem item = result.getRecords().get(0);
            assertThat(item.id()).isEqualTo(100L);
            assertThat(item.eventType()).isEqualTo("MAIL_RECEIVED");
            assertThat(item.title()).isEqualTo("新邮件");
            assertThat(item.content()).isEqualTo("你收到了一封邮件");
            assertThat(item.characterId()).isEqualTo(95465499);
            assertThat(item.corpId()).isEqualTo(98000001);
            assertThat(item.isRead()).isFalse();
            assertThat(item.gmtCreate()).isEqualTo(now);

            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getCurrent()).isEqualTo(1L);
            assertThat(result.getSize()).isEqualTo(20L);
            assertThat(result.getPages()).isEqualTo(1L);
            assertThat(result.getHasNext()).isFalse();
            assertThat(result.getHasPrevious()).isFalse();

            verify(notificationRepository).pageByUser(eq(USER_ID), eq(null), any(PageQuery.class));
        }
    }

    // ---------------------------------------------------------------
    //  unreadCount — 仅自己的未读数
    // ---------------------------------------------------------------

    @Test
    @DisplayName("unreadCount 返回当前用户的未读数")
    void unreadCount_ownOnly() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            when(notificationRepository.countUnread(USER_ID)).thenReturn(7L);

            NotificationApplicationService service =
                    new NotificationApplicationService(notificationRepository, cacheGateway);

            // Act
            UnreadCountResponse response = service.unreadCount();

            // Assert — count 透传
            assertThat(response.count()).isEqualTo(7L);
            verify(notificationRepository).countUnread(USER_ID);
        }
    }

    // ---------------------------------------------------------------
    //  markRead — 他人通知被拒绝
    // ---------------------------------------------------------------

    @Test
    @DisplayName("markRead 他人通知抛出统一越权异常(不泄露存在性)")
    void markRead_otherUsersNotification_rejected() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            // 场景 1：markRead 返回 0（他人行或不存在）→ 抛异常
            when(notificationRepository.markRead(999L, USER_ID)).thenReturn(0);

            NotificationApplicationService service =
                    new NotificationApplicationService(notificationRepository, cacheGateway);

            assertThatThrownBy(() -> service.markRead(999L))
                    .isInstanceOf(EveHelperException.class)
                    .satisfies(ex -> assertThat(((EveHelperException) ex).getResultCode())
                            .isEqualTo(ResultCode.ACCESS_UNAUTHORIZED));

            // 场景 2：markRead 返回 1（自己的通知）→ 无异常
            when(notificationRepository.markRead(100L, USER_ID)).thenReturn(1);
            service.markRead(100L); // 不抛异常即通过
        }
    }

    // ---------------------------------------------------------------
    //  wsTicket — 30 秒 TTL 写入
    // ---------------------------------------------------------------

    @Test
    @DisplayName("wsTicket 以 30 秒 TTL 写入缓存键 app:ws:ticket:{uuid}")
    void wsTicket_storedWith30sTtl() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            when(cacheGateway.setIfAbsent(any(String.class), eq(USER_ID), eq(30L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);

            NotificationApplicationService service =
                    new NotificationApplicationService(notificationRepository, cacheGateway);

            // Act
            WsTicketResponse response = service.createWsTicket();

            // Assert — ArgumentCaptor 捕 key
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(cacheGateway).setIfAbsent(keyCaptor.capture(), eq(USER_ID), eq(30L), eq(TimeUnit.SECONDS));

            String key = keyCaptor.getValue();
            assertThat(key).startsWith("app:ws:ticket:");
            assertThat(key.substring("app:ws:ticket:".length())).isNotEmpty();

            assertThat(response.ticket()).isNotEmpty();
            assertThat(response.expiresInSeconds()).isEqualTo(30);
        }
    }

    // ---------------------------------------------------------------
    //  wsTicket — 每次调用唯一
    // ---------------------------------------------------------------

    @Test
    @DisplayName("wsTicket 每次调用返回不同的 ticket")
    void wsTicket_uniquePerCall() {
        try (MockedStatic<UserUtil> userUtilMock = mockStatic(UserUtil.class)) {
            userUtilMock.when(UserUtil::getUserId).thenReturn(USER_ID);

            when(cacheGateway.setIfAbsent(any(String.class), eq(USER_ID), eq(30L), eq(TimeUnit.SECONDS)))
                    .thenReturn(true);

            NotificationApplicationService service =
                    new NotificationApplicationService(notificationRepository, cacheGateway);

            // Act
            WsTicketResponse first = service.createWsTicket();
            WsTicketResponse second = service.createWsTicket();

            // Assert
            assertThat(first.ticket()).isNotEqualTo(second.ticket());
        }
    }
}
