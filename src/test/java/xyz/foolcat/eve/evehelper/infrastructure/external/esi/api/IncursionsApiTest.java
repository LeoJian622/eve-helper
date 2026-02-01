package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IncursionsResponse;

import java.util.List;


@ActiveProfiles("test")
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