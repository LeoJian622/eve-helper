package xyz.foolcat.eve.evehelper.domain.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;





@SpringBootTest
@ActiveProfiles("test")
@WithUserDetails("admin")
@DisplayName("blurprints 蓝图信息读取")
class BlueprintsServiceTest {

    @Autowired
    private BlueprintsService blueprintsService;

    @Test
    void saveAndUpdateBlueprints() throws ParseException {
        blueprintsService.saveAndUpdateBlueprints(2112818290,false);
    }
}