package xyz.foolcat.eve.evehelper.domain.service.system;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.AssetsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.AssetsRepository;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.AssetsApi;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

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

    private final EsiApiService esiApiService;

    private final AssetsApi assetsApi;

    private final AssetsAssembler assetsAssembler;

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

    public int insertOrUpdate(Assets record) {
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
        String accessToken = esiApiService.getAccessToken(String.valueOf(eveAccount.getCharacterId()), eveAccount.getUserId());

        /*
         * 获取总页数
         */
        Integer maxPage = assetsApi.queryCharactersAssetsMaxPage(eveAccount.getCharacterId(), EsiClient.SERENITY, accessToken);

        /*
         * 从ESI获取资产列表
         */
        List<Assets> assets = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(page -> assetsApi.queryCharactersAssets(eveAccount.getCharacterId(), EsiClient.SERENITY, page, accessToken).collectList())
                .sequential()
                .collect(Collectors.toList())
                .stream().flatMap(asset -> Objects.requireNonNull(asset.block()).stream())
                .collect(Collectors.toList())
                .stream().map(assetsAssembler::toAssets)
                .collect(Collectors.toList());
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
}


