package xyz.foolcat.eve.evehelper.infrastructure.external.esi.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.PageTotalApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.AuthorizeOAuth;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.auth.GrantType;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.AuthTokenResponse;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.util.Objects;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ESI Opportunities Api Test")
class PageTotalApiTest {

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

//    @Test
//    void queryAlliancesContactsMaxPage() {
//        Integer maxPage = pageTotalApi.queryAlliancesContactsMaxPage(562593865L, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharactersAssetsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharactersAssetsMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharacterBlueprintMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharacterBlueprintMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharactersBookmarksMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharactersBookmarksMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharactersBookmarksFoldersMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharactersBookmarksFoldersMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharactersContactsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharactersContactsMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharactersContractsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharactersContractsMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharacterKillMailMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharacterKillMailMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharacterMiningMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharacterMiningMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharacterOrdersHistoryMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharacterOrdersHistoryMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharacterWalletJournalMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharacterWalletJournalMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCharacterWalletTransactionsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCharacterWalletTransactionsMaxPage(2112818290, "serenity", at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryPublicContractsBidsMaxPage() {
//        Integer maxPage = pageTotalApi.queryPublicContractsBidsMaxPage(56479427, "serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryPublicContractsItemsMaxPage() {
//        Integer maxPage = pageTotalApi.queryPublicContractsItemsMaxPage(1505163, "serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryPublicContractsRegionMaxPage() {
//        Integer maxPage = pageTotalApi.queryPublicContractsRegionMaxPage(10000002, "serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationMiningExtractionsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationMiningExtractionsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationMiningObserversMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationMiningObserversMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationMiningObserverMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationMiningObserverMaxPage(656880659, 5465446L,"serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    @Deprecated
//    void queryCorporationAllianceHistoryMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationAllianceHistoryMaxPage(656880659, "serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationsAssetsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationsAssetsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationBlueprintsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationBlueprintsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationsBookmarksMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationsBookmarksMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationsBookmarksFoldersMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationsBookmarksFoldersMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationsContactsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationsContactsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationContainersLogsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationContainersLogsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationsContractsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationsContractsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationsContractsBidsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationsContractsBidsMaxPage(656880659, 56479427,"serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationCustomsOfficesMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationCustomsOfficesMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationIndustryJobsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationIndustryJobsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationMedalsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationMedalsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationIssuedMedalsMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationIssuedMedalsMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationOrdersMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationOrdersMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationOrdersHistoryMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationOrdersHistoryMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationShareHoldersMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationShareHoldersMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationStandingMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationStandingMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationStarBasesMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationStarBasesMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationStructuresMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationStructuresMaxPage(656880659, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryCorporationWalletJournalMaxPage() {
//        Integer maxPage = pageTotalApi.queryCorporationWalletJournalMaxPage(656880659, 1,"serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryStructureOrdersMaxPage() {
//        Integer maxPage = pageTotalApi.queryStructureOrdersMaxPage(1015148880281L, "serenity",at);
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryRegionOrdersMaxPage() {
//        Integer maxPage = pageTotalApi.queryRegionOrdersMaxPage(10000002, 34,"serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryRegionTypesMaxPage() {
//        Integer maxPage = pageTotalApi.queryRegionTypesMaxPage(10000002, "serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryUniverseGroupsMaxPage() {
//        Integer maxPage = pageTotalApi.queryUniverseGroupsMaxPage("serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryUniverseTypesUnMaxPage() {
//        Integer maxPage = pageTotalApi.queryUniverseTypesUnMaxPage("serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
//
//    @Test
//    void queryWarsKillMailsMaxPage() {
//        Integer maxPage = pageTotalApi.queryWarsKillMailsMaxPage(123456,"serenity");
//        System.out.println("maxPage = " + maxPage);
//    }
}