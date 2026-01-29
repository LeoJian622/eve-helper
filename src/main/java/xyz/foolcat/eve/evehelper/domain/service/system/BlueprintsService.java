package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.BlueprintsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.text.ParseException;
import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class BlueprintsService {

    private final EsiApiService esiApiService;

    private final CorporationApi corporationApi;

    private final BlueprintsRepository blueprintsDataRepository;

    private final BlueprintsAssembler blueprintsAssembler;

    private final AuthorizeUtil authorizeUtil;

    public int batchInsert(List<Blueprints> list) {
        return blueprintsDataRepository.batchInsert(list);
    }

    /**
     * 与ESI蓝图数据进行同步并返回
     *
     * @param cid   人物或公司ID
     * @param isCor 是否为公司查询
     * @return
     * @throws ParseException
     */
    public void saveAndUpdateBlueprints(Integer cid, Boolean isCor) throws ParseException {
        /**
         * 获取游戏人物信息及授权
         */
        EveAccount eveAccount = authorizeUtil.authorize(cid);
        String accessToken = esiApiService.getAccessToken(cid, eveAccount.getUserId());

        if (isCor != null && isCor) {
            Integer maxPage = corporationApi.queryCorporationBlueprintsMaxPage(eveAccount.getCorpId(), EsiClient.SERENITY, accessToken);

        }

    }

    public int updateBatch(List<Blueprints> list) {
        return blueprintsDataRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<Blueprints> list) {
        return blueprintsDataRepository.updateBatchSelective(list);
    }

    public int insertOrUpdate(Blueprints record) {
        return blueprintsDataRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Blueprints record) {
        return blueprintsDataRepository.insertOrUpdateSelective(record);
    }
}

