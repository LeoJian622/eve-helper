package xyz.foolcat.eve.evehelper.service.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.util.UserUtil;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("工业服务")
@WithUserDetails("user1")
class IndustryJobServiceTest {

    @Autowired
    private IndustryJobService industryJobService;

    @Test
    void batchInsertOrUpdateFromEsi() throws ParseException {
        industryJobService.batchInsertOrUpdateFromEsi(98061457, UserUtil.getUserId(),true,true);
        industryJobService.batchInsertOrUpdateFromEsi(98061457, UserUtil.getUserId(),true,false);
    }
}