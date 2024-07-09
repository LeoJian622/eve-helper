package xyz.foolcat.eve.evehelper.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.esi.model.CorporationWalletsResponse;
import xyz.foolcat.eve.evehelper.esi.model.WalletJournalResponse;
import xyz.foolcat.eve.evehelper.esi.model.WalletTransactionsResponse;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Wallet Api Test")
class WalletApiTest {

    @Autowired
    WalletApi walletApi;

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
    void queryCharacterWallet() {
        Double wallet = walletApi.queryCharacterWallet(2112818290, "serenity", at).block();
        System.out.println("wallet = " + wallet);
    }

    @Test
    void queryCharacterWalletJournal() {
        List<WalletJournalResponse> walletJournalResponses = walletApi.queryCharacterWalletJournal(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("walletJournalResponses = " + walletJournalResponses);
    }

    @Test
    void queryCharacterWalletTransactions() {
        List<WalletTransactionsResponse> walletTransactionsResponses = walletApi.queryCharacterWalletTransactions(2112818290, "serenity", null, at).collectList().block();
        System.out.println("walletTransactionsResponses = " + walletTransactionsResponses);
    }

    @Test
    void queryCorporationWallet() {
        List<CorporationWalletsResponse> corporationWalletsResponses = walletApi.queryCorporationWallet(656880659, "serenity", at).collectList().block();
        System.out.println("corporationWalletsResponses = " + corporationWalletsResponses);
    }

    @Test
    void queryCorporationWalletJournal() {
        List<WalletJournalResponse> walletJournalResponses = walletApi.queryCorporationWalletJournal(656880659, 1, "serenity", 1, at).collectList().block();
        System.out.println("walletJournalResponses = " + walletJournalResponses);
    }

    @Test
    void queryCorporationWalletTransactions() {
        List<WalletTransactionsResponse> walletTransactionsResponses = walletApi.queryCorporationWalletTransactions(656880659, 1, "serenity", null, at).collectList().block();
        System.out.println("walletTransactionsResponses = " + walletTransactionsResponses);
    }
}