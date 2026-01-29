package xyz.foolcat.eve.evehelper.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("工业定时任务测试")
class IndustryTaskTest {

    @Autowired
    private IndustryTask industryTask;

    @Test
    void updateIndustryJobs() {
        industryTask.updateIndustryJobs();
    }

    @Test
    void noticeJobComplete0() {
        industryTask.noticeJobComplete0();
    }

    @Test
    void noticeJobComplete24() {
        industryTask.noticeJobComplete24();
    }

}