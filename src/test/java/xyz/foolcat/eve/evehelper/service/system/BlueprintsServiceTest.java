package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.domain.service.system.BlueprintsService;

import java.text.ParseException;



@ExtendWith(SpringExtension.class)
@SpringBootTest
@WithUserDetails("admin")
@DisplayName("blurprints 蓝图信息读取")
class BlueprintsServiceTest {

    @Autowired
    private BlueprintsService blueprintsService;

    @Test
    void saveAndUpdateBlueprints() throws ParseException {
//        blueprintsService.saveAndUpdateBlueprints("char","2112832425");
    }
}