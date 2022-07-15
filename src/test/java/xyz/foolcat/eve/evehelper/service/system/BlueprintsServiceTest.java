package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WithUserDetails("admin")
@DisplayName("blurprints 蓝图信息读取")
class BlueprintsServiceTest {

    @Resource
    private BlueprintsService blueprintsService;

    @Test
    void saveAndUpdateBlueprints() throws ParseException {
        blueprintsService.saveAndUpdateBlueprints("char","2112832425");
    }
}