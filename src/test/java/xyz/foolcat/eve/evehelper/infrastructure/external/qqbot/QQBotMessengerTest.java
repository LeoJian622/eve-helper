package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import cc.starcube.qqbot.spring.api.QQBotV2Api;
import cc.starcube.qqbot.spring.model.v2.SendGroupMessage;
import cc.starcube.qqbot.spring.model.v2.SendUserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class QQBotMessengerTest {

    private QQBotV2Api qqBotV2Api;
    private QQBotMessenger messenger;

    @BeforeEach
    void setUp() {
        qqBotV2Api = mock(QQBotV2Api.class);
        messenger = new QQBotMessenger(qqBotV2Api);
    }

    @Test
    void groupReplyIncludesIncomingMessageId() {
        QQBotMessageEvent event = new QQBotMessageEvent(
                "incoming-group-message", "member-openid", "group-openid", ".tax a b c",
                QQBotMessageEvent.Scene.GROUP
        );

        assertThat(messenger.reply(event, "reply text")).isTrue();

        ArgumentCaptor<SendGroupMessage.Request> captor = ArgumentCaptor.forClass(SendGroupMessage.Request.class);
        verify(qqBotV2Api).sendGroupMessage(captor.capture());
        assertThat(captor.getValue().groupOpenid()).isEqualTo("group-openid");
        assertThat(captor.getValue().body().msgType()).isZero();
        assertThat(captor.getValue().body().content()).isEqualTo("reply text");
        assertThat(captor.getValue().body().msgId()).isEqualTo("incoming-group-message");
    }

    @Test
    void c2cReplyIncludesIncomingMessageId() {
        QQBotMessageEvent event = new QQBotMessageEvent(
                "incoming-c2c-message", "user-openid", null, "TESTBOT hello",
                QQBotMessageEvent.Scene.C2C
        );

        assertThat(messenger.reply(event, "hello")).isTrue();

        ArgumentCaptor<SendUserMessage.Request> captor = ArgumentCaptor.forClass(SendUserMessage.Request.class);
        verify(qqBotV2Api).sendUserMessage(captor.capture());
        assertThat(captor.getValue().userOpenid()).isEqualTo("user-openid");
        assertThat(captor.getValue().body().content()).isEqualTo("hello");
        assertThat(captor.getValue().body().msgId()).isEqualTo("incoming-c2c-message");
    }

    @Test
    void scheduledMessageSkipsBlankGroupOpenid() {
        assertThat(messenger.sendGroupMessage("", "notification")).isFalse();

        verify(qqBotV2Api, never()).sendGroupMessage(org.mockito.ArgumentMatchers.any());
    }
}
