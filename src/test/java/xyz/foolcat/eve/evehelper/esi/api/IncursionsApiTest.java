package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.esi.model.IncursionsResponse;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Incursions Api Test")
class IncursionsApiTest {

    @Autowired
    IncursionsApi incursionsApi;

    @Test
    void queryIncursions() {
        List<IncursionsResponse> incursionsResponses = incursionsApi.queryIncursions("serenity").collectList().block();
        System.out.println("incursionsResponses = " + incursionsResponses);
    }
}