package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.text.ParseException;



@ExtendWith({SpringExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("观察者采掘详细")
class MiningDetailServiceTest {

    @Resource
    MiningDetailService miningDetailService;

    @Test
    void saveAllObserverMining() {
    }

    @Test
    void saveObserverMining() throws ParseException {
        miningDetailService.saveObserverMining(2112818290,1014017747012L);
    }
}