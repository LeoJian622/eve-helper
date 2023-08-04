package xyz.foolcat.eve.evehelper.service.esi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI 请求服务测试")
class EsiApiServiceTest {

    @Autowired
    EsiApiService esiApiService;



    @Test
    void testGetAccessToken() throws ParseException {
//        esiChApiService.getAccessToken("char","61BJRObV/ku0XlWb1xzJ9g==");
//        esiChApiService.getAccessToken("char","2112832425");
//        https://esi.evepc.163.com/ui/oauth2-redirect.html?code=30tZ00ssVkGDthezjQo7lA&state=T
        esiApiService.getAccessToken("30tZ00ssVkGDthezjQo7lA");
    }

    @Test
    void getJobList() throws ParseException {
        esiApiService.getJobList("char","2112965519");
    }

}