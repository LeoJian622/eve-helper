package xyz.foolcat.eve.evehelper.domain.service.system;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsAggregateVO;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Leojan
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class AssetsService {

    private final EsiGateway esiApiService;

    private final AssetsRepository assetsRepository;

    private final AuthorizeUtil authorizeUtil;

    public int batchInsert(List<Assets> list) {
        return assetsRepository.batchInsert(list);
    }

    public int batchInsertOrUpdate(List<Assets> list) {
        return assetsRepository.batchInsertOrUpdate(list);
    }

    /**
     * 获取资产列表
     *
     * @param cid
     * @return
     */
    public List<Assets> getAssertsListById(String cid, int pages, int rows) {
        return assetsRepository.selectAssertsInvtypeUniverse(cid, pages, rows);
    }

    public int updateBatch(List<Assets> list) {
        return assetsRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<Assets> list) {
        return assetsRepository.updateBatchSelective(list);
    }

    public boolean insertOrUpdate(Assets record) {
        return assetsRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Assets record) {
        return assetsRepository.insertOrUpdateSelective(record);
    }

    /**
     * 与ESI资产数据进行同步并返回
     *
     * @param cid 角色ID
     */
    public void saveAndUpdateAsserts(Integer cid) throws ParseException {

        /*
         * 获取游戏人物信息及授权
         */
        EveAccount eveAccount = authorizeUtil.authorize(cid);
        // 修正:此前误调 String 重载(AUTHORIZATION_CODE),把 characterId 当 OAuth code 兑换,资产同步不可用
        String accessToken = esiApiService.getAccessToken(eveAccount.getCharacterId(), eveAccount.getUserId());

        /*
         * 获取总页数
         */
        Integer maxPage = esiApiService.queryCharactersAssetsMaxPage(eveAccount.getCharacterId(), accessToken);

        /*
         * 从ESI获取资产列表
         */
        List<Assets> assets = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(page -> esiApiService.queryCharactersAssets(eveAccount.getCharacterId(), page, accessToken).collectList())
                .sequential()
                .collect(Collectors.toList())
                .stream().flatMap(asset -> Objects.requireNonNull(asset.block()).stream())
                .collect(Collectors.toList());

        /*
         * 回填 ownerId(角色ID):EsiAssetsConverter 将 owner_id 置为 ignore,
         * 需在此归因到所属角色,否则资产 owner_id=NULL,按角色聚合/归属鉴权/stale 删除均失效。
         */
        Long ownerId = eveAccount.getCharacterId() == null ? null : eveAccount.getCharacterId().longValue();
        assets.forEach(a -> a.setOwnerId(ownerId));

        batchInsertOrUpdate(assets);

        /*
         * 移除不在ESI列表的物品
         */
        Set<Long> itemIds = assets.stream().map(Assets::getItemId).collect(Collectors.toSet());

        List<Long> removeItemIds = assetsRepository.findByOwnerId(eveAccount.getCharacterId())
                .stream()
                .map(Assets::getItemId)
                .filter(itemId -> !itemIds.contains(itemId))
                .collect(Collectors.toList());
        assetsRepository.removeBatchByIds(removeItemIds);

    }

    /**
     * 按角色 ID 聚合资产读模型。
     *
     * @param ownerId 角色 ID
     * @return 聚合结果;该角色无资产时返回 null
     */
    public AssetsAggregateVO getAggregateByOwnerId(Integer ownerId) {
        return assetsRepository.acquireAggregateByOwnerId(ownerId);
    }
}

