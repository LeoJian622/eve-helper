package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.service.system.ObserverService;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("读取观察者对象")
class ObserverServiceTest {

    @Resource
    ObserverService observerService;

}