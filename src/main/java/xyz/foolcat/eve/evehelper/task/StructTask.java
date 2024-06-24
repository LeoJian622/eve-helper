package xyz.foolcat.eve.evehelper.task;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.service.system.EveAccountService;
import xyz.foolcat.eve.evehelper.service.system.StructureService;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 建筑相关任务
 *
 * @author Leojan
 * @date 2024-06-24 9:20
 */

@Component
@RequiredArgsConstructor
public class StructTask {

    private final EveAccountService eveAccountService;

    private final EsiApiService esiApiService;

    private final CorporationApi corporationApi;

    private final StructureService structureService;

    @Scheduled(fixedDelay = 5000)
    public void fixedDelayJob() throws InterruptedException {
        System.out.println("fixedDelay 每隔5秒" + new Date());
    }

    public void updateStruct() throws ParseException {
        List<EveAccount> accounts = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, 2112818290).list();
        if (!accounts.isEmpty()){
            EveAccount eveAccount = accounts.get(0);
            String accessToken = esiApiService.getAccessToken(eveAccount.getCharacterId().toString());
            Integer max = corporationApi.queryCorporationStructuresMaxPage(eveAccount.getCorpId(), EsiClient.SERENITY, accessToken);
            List<StructuresInformationResponse> structuresInformationResponses = Stream.iterate(1, i -> i + 1).limit(max)
                    .map(i -> corporationApi.queryCorporationStructures(eveAccount.getCorpId(), EsiClient.SERENITY, "zh", i, accessToken)
                            .collectList().block())
                    .sequential()
                    .collect(Collectors.toList())
                    .stream().flatMap(Collection::stream)
                    .collect(Collectors.toList());
            structureService.esiBatchInsert(structuresInformationResponses);
        }
    }
}
