package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.InsuranceOfShipResponse;

import java.util.List;


@ActiveProfiles("test")
@SpringBootTest
@DisplayName("ESI Industry Api Test")
class InsuranceApiTest {

    @Autowired
    InsuranceApi insuranceApi;

    @Test
    void queryIndustrySystems() {
        List<InsuranceOfShipResponse> insuranceOfShipResponses = insuranceApi.queryIndustrySystems("serenity", "zh").collectList().block();
        System.out.println("insuranceOfShipResponses = " + insuranceOfShipResponses);
    }
}