package xyz.foolcat.eve.evehelper.service.esi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.exception.EsiException;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI 请求服务测试")
class EsiChApiServiceTest {

    @Autowired
    EsiChApiService esiChApiService;

    @Test
    void getAccessToken() throws EsiException, ParseException {
        esiChApiService.getAccessToken("char","MFXqKdRtX0uR55RYyd9gow");
    }
}