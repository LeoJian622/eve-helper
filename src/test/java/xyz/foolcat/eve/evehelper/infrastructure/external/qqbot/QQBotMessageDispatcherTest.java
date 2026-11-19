package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.foolcat.eve.evehelper.domain.service.system.InvTypesService;
import xyz.foolcat.eve.evehelper.domain.service.system.StructureService;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class QQBotMessageDispatcherTest {

    private InvTypesService invTypesService;
    private QQBotIntegrationProperties properties;
    private QQBotMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        invTypesService = mock(InvTypesService.class);
        properties = new QQBotIntegrationProperties();
        dispatcher = new QQBotMessageDispatcher(
                invTypesService,
                mock(WalletJournalService.class),
                mock(StructureService.class),
                mock(AuthorizeUtil.class),
                properties
        );
    }

    @Test
    void rejectsAdminCommandForUnknownOpenid() {
        QQBotMessageEvent event = groupEvent("unknown-openid", ".admAddType 34");

        assertThat(dispatcher.dispatch(event)).contains("没有权限");
        verify(invTypesService, never()).updateTypeByTypeId(34);
    }

    @Test
    void allowsAdminCommandForConfiguredOpenid() {
        properties.setAdminOpenids(Set.of("admin-openid"));
        QQBotMessageEvent event = groupEvent("admin-openid", ".admAddType 34");

        assertThat(dispatcher.dispatch(event)).contains("添加ID：34失败");
        verify(invTypesService).updateTypeByTypeId(34);
    }

    @Test
    void keepsTestBotEchoBehavior() {
        assertThat(dispatcher.dispatch(groupEvent("member-openid", "TESTBOT hello"))).contains(" hello");
    }

    private static QQBotMessageEvent groupEvent(String authorOpenid, String content) {
        return new QQBotMessageEvent(
                "message-id", authorOpenid, "group-openid", content, QQBotMessageEvent.Scene.GROUP
        );
    }
}
