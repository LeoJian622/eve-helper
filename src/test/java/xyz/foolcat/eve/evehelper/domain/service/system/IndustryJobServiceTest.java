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
@DisplayName("工业服务")
@WithUserDetails("admin")
class IndustryJobServiceTest {

    @Autowired
    private IndustryJobService industryJobService;

    @Test
    void batchInsertOrUpdateFromEsi() throws ParseException {
        industryJobService.batchInsertOrUpdateFromEsi(2112818290,true,true);
        industryJobService.batchInsertOrUpdateFromEsi(2112818290,true,false);
    }
}