package xyz.foolcat.eve.evehelper.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("卫星矿相关任务")
class MiningTaskTest {

    @Autowired
    private MiningTask miningTask;

    @Test
    void noticeExtraction() {
        miningTask.noticeExtraction();
    }

    @Test
    void noticeExtraction7Day() {
        miningTask.noticeExtraction7Day();
    }
}