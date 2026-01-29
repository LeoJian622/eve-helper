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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.FittingApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.FittingResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.send.Fitting;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.sub.FittingItem;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Fitting Api Test")
class FittingApiTest {

    @Autowired
    FittingApi fittingApi;

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
    void queryCharacterFittings() {
        List<FittingResponse> fittingResponses = fittingApi.queryCharacterFittings(2112818290, "serenity", at).collectList().block();
        System.out.println("fittingResponses = " + fittingResponses);
    }

    @Test
    void addCharacterFittings() {
        Fitting fitting = new Fitting();
        fitting.setDescription("test");
        fitting.setName("测试装配");
        fitting.setShipTypeId(626);

        ArrayList<FittingItem> fittingItems = new ArrayList<>();
        FittingItem fittingItem = new FittingItem();
        fittingItem.setFlag("DroneBay");
        fittingItem.setQuantity(4);
        fittingItem.setTypeId(21638);
        fittingItems.add(fittingItem);
        fitting.setItems(fittingItems);

        FittingResponse fittingResponse = fittingApi.addCharacterFittings(2112818290, "serenity", fitting, at).block();
        System.out.println("fittingResponse = " + fittingResponse);
    }

    @Test
    void deleteCharacterFittings() {
        Object block = fittingApi.deleteCharacterFittings(2112818290, "serenity", 18419043, at).block();
        System.out.println("block = " + block);
    }
}