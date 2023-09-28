package xyz.foolcat.eve.evehelper.service.esi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI 请求服务测试")
@WithUserDetails("user1")
class EsiApiServiceTest {

    @Autowired
    EsiApiService esiApiService;

    @Test
    void getAccessToken() throws ParseException {
        String accessToken = esiApiService.getAccessToken("odZ2dRCzHUOJCa9KZqILQQ==");
        System.out.println(accessToken);
    }

    @Test
    void getJobList() throws ParseException {
        esiApiService.getJobList("char","2112965519");
    }

}