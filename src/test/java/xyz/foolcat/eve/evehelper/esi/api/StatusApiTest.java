package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.esi.StatusResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Sovereignty Api Test")
class StatusApiTest {

    @Autowired
    StatusApi statusApi;

    @Test
    void queryServerStatus() {
        StatusResponse statusResponse = statusApi.queryServerStatus("serenity").block();
        System.out.println("statusResponse = " + statusResponse);
    }
}