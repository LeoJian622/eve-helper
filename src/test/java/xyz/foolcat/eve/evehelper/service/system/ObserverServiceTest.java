package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.util.UserUtil;

import javax.annotation.Resource;
import java.text.ParseException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("读取观察者对象")
class ObserverServiceTest {

    @Resource
    ObserverService observerService;

    @Test
    void saveObserverFromEsi() throws ParseException {
        observerService.saveObserverFromEsi(656880659, UserUtil.getUserId());
    }
}