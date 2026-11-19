package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import com.fasterxml.jackson.databind.JsonNode;
import cc.starcube.qqbot.spring.QQBotEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EveQQBotEventHandler implements QQBotEventHandler {

    private final QQBotMessageDispatcher dispatcher;
    private final QQBotMessenger messenger;

    @Override
    public void onEvent(String eventType, JsonNode data, JsonNode envelope) {
        QQBotMessageEvent.fromGateway(eventType, data).ifPresent(event -> {
            try {
                dispatcher.dispatch(event).ifPresent(reply -> messenger.reply(event, reply));
            } catch (RuntimeException e) {
                log.error("QQ Bot event handling failed: eventType={}", eventType, e);
            }
        });
    }
}
