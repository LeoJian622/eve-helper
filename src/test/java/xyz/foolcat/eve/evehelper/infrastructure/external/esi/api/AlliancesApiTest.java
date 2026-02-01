package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import cn.hutool.core.lang.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.AlliancesApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AlliancesResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.IconResponse;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ESI Alliance Api Test")
class AlliancesApiTest {

    @Autowired
    AlliancesApi alliancesApi;

    @Test
    void queryAllActivePlayerAlliances() {
        List<Long> alliances =
                alliancesApi.queryAllActivePlayerAlliances("serenity").collectList().block();
        Assert.notEmpty(alliances);
        System.out.println("alliances = " + alliances);
    }

    @Test
    void queryAlliancesPublicInformation() {
        AlliancesResponse alliancesResponse = alliancesApi.queryAlliancesPublicInformation(99001238L, "serenity")
                .block();
        Assert.notNull(alliancesResponse);
        System.out.println("alliancesResponse = " + alliancesResponse);
    }

    @Test
    void queryAlliancesCorporations() {
        List<Long> corporations =
                alliancesApi.queryAlliancesCorporations(562593865L, "serenity").collectList().block();
        Assert.notEmpty(corporations);
        System.out.println("corporations = " + corporations);
    }

    @Test
    void queryAlliancesIcon() {
        IconResponse icon = alliancesApi.queryAlliancesIcon(562593865L, "serenity").block();
        Assert.notNull(icon);
        System.out.println("icon = " + icon);
    }
}