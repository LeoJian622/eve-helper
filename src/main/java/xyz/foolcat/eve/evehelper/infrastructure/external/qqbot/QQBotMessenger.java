package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import cc.starcube.qqbot.spring.api.QQBotV2Api;
import cc.starcube.qqbot.spring.model.v2.SendGroupMessage;
import cc.starcube.qqbot.spring.model.v2.SendUserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class QQBotMessenger {

    private static final int TEXT_MESSAGE_TYPE = 0;

    private final QQBotV2Api qqBotV2Api;

    public boolean reply(QQBotMessageEvent event, String content) {
        if (event == null || content == null || content.isBlank()) {
            return false;
        }
        return switch (event.scene()) {
            case GROUP -> sendGroupMessage(event.groupOpenid(), content, event.messageId());
            case C2C -> sendUserMessage(event.authorOpenid(), content, event.messageId());
        };
    }

    public boolean sendGroupMessage(String groupOpenid, String content) {
        return sendGroupMessage(groupOpenid, content, null);
    }

    private boolean sendGroupMessage(String groupOpenid, String content, String messageId) {
        if (groupOpenid == null || groupOpenid.isBlank() || content == null || content.isBlank()) {
            log.debug("QQ Bot group message skipped because target or content is blank");
            return false;
        }
        try {
            SendGroupMessage.Body body = new SendGroupMessage.Body(
                    TEXT_MESSAGE_TYPE, content, null, null, messageId, null, null, null, null, null
            );
            qqBotV2Api.sendGroupMessage(new SendGroupMessage.Request(groupOpenid, body));
            return true;
        } catch (RuntimeException e) {
            log.error("QQ Bot group message failed", e);
            return false;
        }
    }

    private boolean sendUserMessage(String userOpenid, String content, String messageId) {
        if (userOpenid == null || userOpenid.isBlank() || content == null || content.isBlank()) {
            log.debug("QQ Bot C2C message skipped because target or content is blank");
            return false;
        }
        try {
            SendUserMessage.Body body = new SendUserMessage.Body(
                    TEXT_MESSAGE_TYPE, content, null, null, messageId, null, null, null, null, null, null, null
            );
            qqBotV2Api.sendUserMessage(new SendUserMessage.Request(userOpenid, body));
            return true;
        } catch (RuntimeException e) {
            log.error("QQ Bot C2C message failed", e);
            return false;
        }
    }
}
