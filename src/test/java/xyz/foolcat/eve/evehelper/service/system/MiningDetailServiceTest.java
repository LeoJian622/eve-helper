package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.util.UserUtil;

import javax.annotation.Resource;

import java.text.ParseException;



@ExtendWith({SpringExtension.class})
@SpringBootTest
@DisplayName("观察者采掘详细")
class MiningDetailServiceTest {

    @Resource
    MiningDetailService miningDetailService;

    @Test
    void saveAllObserverMining() {
    }

    @Test
    void saveObserverMining() throws ParseException {
        miningDetailService.saveObserverMining(656880659,1014012914900L, UserUtil.getUserId());
    }
}