package xyz.foolcat.eve.evehelper.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("建筑信息定时任务测试")
@WithUserDetails("user1")
class StructTaskTest {

    @Autowired
    StructTask structTask;

    @Test
    void updateStruct() throws ParseException {
        structTask.updateStruct();
    }
}