package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ContactLabelResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ContactResponse;
import xyz.foolcat.eve.evehelper.domain.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Contact Api Test")
class ContactApiTest {

    @Autowired
    ContactApi contactApi;

    @Autowired
    EveAccountService eveAccountService;

    @Autowired
    AuthorizeOAuth authorizeOAuth;

    String at = "Bearer ";

    @BeforeEach
    void initAccessToken() {
        EveAccount entity = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, 2112818290).one();
        Mono<AuthTokenResponse> authTokenResponseMono = authorizeOAuth.updateAccessToken(GrantType.REFRESH_TOKEN, entity.getRefreshToken());
        at = at + Objects.requireNonNull(authTokenResponseMono.block()).getAccessToken();
        System.out.println("at = " + at);
    }

    @Test
    void queryAlliancesContacts() {
        List<ContactResponse> contactResponses = contactApi.queryAlliancesContacts(562593865L, "serenity", 1, at).collectList().block();
        System.out.println("contactResponses = " + contactResponses);
    }

    @Test
    void queryAlliancesContactsLabel() {
        List<ContactLabelResponse> contactLabelResponses = contactApi.queryAlliancesContactsLabel(562593865L, "serenity", at).collectList().block();
        System.out.println("contactLabelResponses = " + contactLabelResponses);
    }

    @Test
    void deleteCharactersContacts() {
        contactApi.deleteCharactersContacts(2112818290, "serenity", List.of(2112156363L), at).block();
    }

    @Test
    void queryCharactersContacts() {
        List<ContactResponse> contactResponses = contactApi.queryCharactersContacts(2112818290, "serenity", at).collectList().block();
        System.out.println("contactResponses = " + contactResponses);
    }

    @Test
    void addCharactersContacts() {
        List<Long> longs = contactApi.addCharactersContacts(2112818290, "serenity", List.of(2112156363L), List.of(), 5, false, at).collectList().block();
        System.out.println("longs = " + longs);
    }

    @Test
    void updateCharactersContacts() {
        Object objects = contactApi.updateCharactersContacts(2112818290, "serenity", List.of(2112156363L), List.of(), 10, false, at).block();
        System.out.println("objects = " + objects);
    }

    @Test
    void queryCharactersContactsLabel() {
        List<ContactLabelResponse> contactLabelResponses = contactApi.queryCharactersContactsLabel(2112818290, "serenity", at).collectList().block();
        System.out.println("contactLabelResponses = " + contactLabelResponses);
    }

    @Test
    void queryCorporationsContacts() {
        List<ContactResponse> contactResponses = contactApi.queryCorporationsContacts(656880659, "serenity", at).collectList().block();
        System.out.println("contactResponses = " + contactResponses);
    }

    @Test
    void queryCorporationsContactsLabel() {
        List<ContactLabelResponse> contactLabelResponses = contactApi.queryCorporationsContactsLabel(656880659, "serenity", at).collectList().block();
        System.out.println("contactLabelResponses = " + contactLabelResponses);
    }
}