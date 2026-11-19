package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QQBotMessageEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesGroupMessageEvent() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "id": "group-message-id",
                  "author": {"member_openid": "member-openid", "id": "legacy-id"},
                  "content": "  .tax 10 5 202411  ",
                  "group_openid": "group-openid"
                }
                """);

        QQBotMessageEvent event = QQBotMessageEvent
                .fromGateway("GROUP_AT_MESSAGE_CREATE", data)
                .orElseThrow();

        assertThat(event.messageId()).isEqualTo("group-message-id");
        assertThat(event.authorOpenid()).isEqualTo("member-openid");
        assertThat(event.groupOpenid()).isEqualTo("group-openid");
        assertThat(event.content()).isEqualTo(".tax 10 5 202411");
        assertThat(event.scene()).isEqualTo(QQBotMessageEvent.Scene.GROUP);
    }

    @Test
    void parsesC2cMessageEvent() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "id": "c2c-message-id",
                  "author": {"user_openid": "user-openid"},
                  "content": "TESTBOT hello"
                }
                """);

        QQBotMessageEvent event = QQBotMessageEvent
                .fromGateway("C2C_MESSAGE_CREATE", data)
                .orElseThrow();

        assertThat(event.messageId()).isEqualTo("c2c-message-id");
        assertThat(event.authorOpenid()).isEqualTo("user-openid");
        assertThat(event.groupOpenid()).isNull();
        assertThat(event.scene()).isEqualTo(QQBotMessageEvent.Scene.C2C);
    }

    @Test
    void ignoresUnsupportedEvents() throws Exception {
        JsonNode data = objectMapper.readTree("{\"id\":\"event-id\"}");

        assertThat(QQBotMessageEvent.fromGateway("READY", data)).isEmpty();
    }
}
