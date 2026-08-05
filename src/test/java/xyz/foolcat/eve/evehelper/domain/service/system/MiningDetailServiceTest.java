package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;




@SpringBootTest
@DisplayName("观察者采掘详细")
class MiningDetailServiceTest {

    @Autowired
    MiningDetailService miningDetailService;

    @Test
    void saveAllObserverMining() {
    }

    @Test
    void saveObserverMining() throws ParseException {
        miningDetailService.saveObserverMining(2112818290,1014017747012L);

    }
}