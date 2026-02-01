package xyz.foolcat.eve.evehelper.infrastructure.external.onebot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.BotDispatcher;
import xyz.foolcat.eve.evehelper.infrastructure.external.onebot.model.MessageEvent;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bot指令分发测试")
class BotDispatcherTest {

    @Autowired
    private BotDispatcher botDispatcher;

    @Test
    void dispatchers() {
        MessageEvent event = new MessageEvent();
        event.setRaw_message(".struct Cat9QAQ");
        System.out.println("botDispatcher.dispatchers(event) = " + botDispatcher.dispatchers(event));
    }
}