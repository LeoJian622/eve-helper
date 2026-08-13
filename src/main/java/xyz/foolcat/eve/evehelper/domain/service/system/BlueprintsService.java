package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Blueprints;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.BlueprintsRepository;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.text.ParseException;
import java.util.List;

@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class BlueprintsService {

    private final EsiGateway esiApiService;

    private final BlueprintsRepository blueprintsDataRepository;

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
        // TODO(缺陷修复决议保留):本方法为未完成功能桩,无生产调用方(BlueprintsController 仅提供查询列表,
        // 无同步入口),且 EsiGateway 缺实际蓝图分页查询方法。完整实现需新增底层查询 + 落库 + controller 入口,
        // 属特性开发,不在缺陷修复范围。若需启用,应作为独立 feature 走特性轨。
        /**
         * 获取游戏人物信息及授权
         */
        EveAccount eveAccount = authorizeUtil.authorize(cid);
        String accessToken = esiApiService.getAccessToken(cid, eveAccount.getUserId());

        if (isCor != null && isCor) {
            Integer maxPage = esiApiService.queryCorporationBlueprintsMaxPage(eveAccount.getCorpId(), accessToken);

        }

    }

    public int updateBatch(List<Blueprints> list) {
        return blueprintsDataRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<Blueprints> list) {
        return blueprintsDataRepository.updateBatchSelective(list);
    }

    public boolean insertOrUpdate(Blueprints record) {
        return blueprintsDataRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Blueprints record) {
        return blueprintsDataRepository.insertOrUpdateSelective(record);
    }
}

