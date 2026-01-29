package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.OpportunitiesApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OpportunitiesGroupResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OpportunitiesResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.OpportunitiesTaskResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Opportunities Api Test")
class OpportunitiesApiTest {

    @Autowired
    OpportunitiesApi opportunitiesApi;

    @Autowired
    AuthorizeUtil authorizeUtil;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = authorizeUtil.authorize( 2112818290);
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }


    @Test
    void queryCharacterOpportunities() {
        List<OpportunitiesResponse> opportunitiesResponses = opportunitiesApi.queryCharacterOpportunities(2112818290, "serenity", at).collectList().block();
        System.out.println("opportunitiesResponses = " + opportunitiesResponses);
    }

    @Test
    void queryOpportunitiesGroups() {
        List<Integer> integers = opportunitiesApi.queryOpportunitiesGroups("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryOpportunitiesGroupsDetails() {
        OpportunitiesGroupResponse opportunitiesGroupResponse = opportunitiesApi.queryOpportunitiesGroupsDetails(103, "serenity", "zh").block();
        System.out.println("opportunitiesGroupResponse = " + opportunitiesGroupResponse);
    }

    @Test
    void queryOpportunitiesTasks() {
        List<Integer> integers = opportunitiesApi.queryOpportunitiesTasks("serenity").collectList().block();
        System.out.println("integers = " + integers);
    }

    @Test
    void queryOpportunitiesTaskDetails() {
        OpportunitiesTaskResponse opportunitiesTaskResponse = opportunitiesApi.queryOpportunitiesTaskDetails(102, "serenity", "zh").block();
        System.out.println("opportunitiesTaskResponse = " + opportunitiesTaskResponse);
    }
}