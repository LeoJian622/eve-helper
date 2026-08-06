package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.SovereigntyCampaignsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.SovereigntyMapResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.SovereigntyStructuresResponse;

import java.util.List;



@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ESI Sovereignty Api Test")
class SovereigntyApiTest {

    @Autowired
    SovereigntyApi sovereigntyApi;

    @Test
    void querySovereigntyCampaigns() {
        List<SovereigntyCampaignsResponse> sovereigntyCampaignsResponses = sovereigntyApi.querySovereigntyCampaigns("serenity").collectList().block();
        System.out.println("sovereigntyCampaignsResponses = " + sovereigntyCampaignsResponses);
    }

    @Test
    void querySovereigntyMaps() {
        List<SovereigntyMapResponse> sovereigntyMapResponses = sovereigntyApi.querySovereigntyMaps("serenity").collectList().block();
        System.out.println("sovereigntyMapResponses = " + sovereigntyMapResponses);
    }

    @Test
    void querySovereigntyStructures() {
        List<SovereigntyStructuresResponse> sovereigntyStructuresResponses = sovereigntyApi.querySovereigntyStructures("serenity").collectList().block();
        System.out.println("sovereigntyStructuresResponses = " + sovereigntyStructuresResponses);
    }
}