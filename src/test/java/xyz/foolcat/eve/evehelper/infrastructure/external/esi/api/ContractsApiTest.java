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
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.ContractsApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ContractBidsResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ContractItemResponse;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.ContractResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.List;
import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Contract Api Test")
class ContractsApiTest {

    @Autowired
    ContractsApi contractsApi;

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
    void queryCharactersContracts() {
        List<ContractResponse> contractResponses = contractsApi.queryCharactersContracts(2112818290, "serenity", 1, at).collectList().block();
        System.out.println("contractResponses = " + contractResponses);
    }

    @Test
    void queryCharactersContractsBids() {
        List<ContractBidsResponse> contractBidsResponses = contractsApi.queryCharactersContractsBids(2112818290, "serenity", 54403173, at).collectList().block();
        System.out.println("contractBidsResponses = " + contractBidsResponses);
    }

    @Test
    void queryCharactersContractsItems() {
        List<ContractItemResponse> contractItemResponses = contractsApi.queryCharactersContractsItems(2112818290, "serenity", 54403173, at).collectList().block();
        System.out.println("contractItemResponses = " + contractItemResponses);
    }

    @Test
    void queryPublicContractsRegion() {
        List<ContractResponse> contractResponses = contractsApi.queryPublicContractsRegion(10000002, "serenity", 2).collectList().block();
        System.out.println("contractResponses = " + contractResponses);
    }

    @Test
    void queryPublicContractsBids() {
        List<ContractBidsResponse> contractBidsResponses = contractsApi.queryPublicContractsBids("serenity", 54544413).collectList().block();
        System.out.println("contractBidsResponses = " + contractBidsResponses);
    }

    @Test
    void queryPublicContractsItems() {
        List<ContractItemResponse> contractItemResponses = contractsApi.queryPublicContractsItems("serenity", 54544413).collectList().block();
        System.out.println("contractItemResponses = " + contractItemResponses);
    }

    @Test
    void queryCorporationsContracts() {
        List<ContractResponse> contractResponses = contractsApi.queryCorporationsContracts(656880659, "serenity", 1, at).collectList().block();
        System.out.println("contractResponses = " + contractResponses);
    }

    @Test
    void queryCorporationsContractsBids() {
        List<ContractBidsResponse> contractBidsResponses = contractsApi.queryCorporationsContractsBids(656880659, "serenity", 54393962, at).collectList().block();
        System.out.println("contractBidsResponses = " + contractBidsResponses);
    }

    @Test
    void queryCorporationsContractsItems() {
        List<ContractItemResponse> contractItemResponses = contractsApi.queryCorporationsContractsItems(656880659, "serenity", 54393962, at).collectList().block();
        System.out.println("contractItemResponses = " + contractItemResponses);
    }
}