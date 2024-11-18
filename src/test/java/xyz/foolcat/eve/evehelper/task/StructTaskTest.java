package xyz.foolcat.eve.evehelper.task;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.service.system.StructureService;

import java.util.regex.Pattern;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("建筑信息定时任务测试")
//@WithUserDetails("user1")
class StructTaskTest {

    @Autowired
    StructTask structTask;

    @Autowired
    StructureService structureService;

    @Test
    void updateStruct() {
        structTask.updateStruct();
    }

    @Test
    void noticeFuelExpires() {
        structTask.noticeFuelExpires();
    }

    @Test
    void test() {
        int groesInt = 1;
        String[] groesStrings = "0/1".split("/");
        for (String s : groesStrings) {
            System.out.println("s = " + s);
        }
        if (StrUtil.isNotEmpty(groesStrings[0]) && isInteger(groesStrings[0])) {
            groesInt = Integer.parseInt(groesStrings[0]);
        }
        System.out.println("groesInt = " + groesInt);
    }
    public static final String PATTERN_STRING = "^[-\\+]?[\\d]*$";
    public boolean isInteger(String str) {
        Pattern pattern = Pattern.compile(PATTERN_STRING);
        return pattern.matcher(str).matches();
    }
}