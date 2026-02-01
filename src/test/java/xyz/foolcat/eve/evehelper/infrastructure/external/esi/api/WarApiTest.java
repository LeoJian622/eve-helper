package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.WarApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.KillMailsIdAndHashResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WarDetailsResponse;

import java.util.List;



@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Skill Api Test")
class WarApiTest {

    @Autowired
    WarApi warApi;

    @Test
    void queryWars() {
        List<Integer> warIds = warApi.queryWars("serenity").collectList().block();
        System.out.println("warIds = " + warIds);
    }

    @Test
    void queryWarDetails() {
        WarDetailsResponse warDetailsResponse = warApi.queryWarDetails(303667, "serenity").block();
        System.out.println("warDetailsResponse = " + warDetailsResponse);
    }

    @Test
    void queryWarsKillMails() {
        List<KillMailsIdAndHashResponse> killMailsIdAndHashResponses = warApi.queryWarsKillMails(303667, "serenity",1).collectList().block();
        System.out.println("killMailsIdAndHashResponses = " + killMailsIdAndHashResponses);
    }
}