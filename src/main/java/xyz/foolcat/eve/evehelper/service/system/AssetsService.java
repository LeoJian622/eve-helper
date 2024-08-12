package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.converter.esi.AssetsConverter;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.AssetsApi;
import xyz.foolcat.eve.evehelper.mapper.system.AssetsMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

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
public class AssetsService extends ServiceImpl<AssetsMapper, Assets> {

    private final EsiApiService esiApiService;

    private final AssetsApi assetsApi;

    private final EveAccountService eveAccountService;

    private final AssetsConverter assetsConverter;


    public int batchInsert(List<Assets> list) {
        return baseMapper.batchInsert(list);
    }

    public int batchInsertOrUpdate(List<Assets> list) {
        return baseMapper.batchInsertOrUpdate(list);
    }

     /**
     * 获取资产列表
     *
     * @param cid
     * @return
     */
    public IPage<AssetsVO> getAssertsListById(IPage<AssetsVO> page, String cid) {
        return baseMapper.selectAssertsInvtypeUniverse(page, cid);
    }

    public int updateBatch(List<Assets> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<Assets> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int insertOrUpdate(Assets record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Assets record) {
        return baseMapper.insertOrUpdateSelective(record);
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
        EveAccount eveAccount = AuthorizeUtil.authorize(cid);
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
                .stream().map(assetsConverter::toAssets)
                .collect(Collectors.toList());
        batchInsertOrUpdate(assets);

        /*
         * 移除不在ESI列表的物品
         */
        Set<Long> itemIds = assets.stream().map(Assets::getItemId).collect(Collectors.toSet());

        List<Long> removeItemIds = lambdaQuery().select(Assets::getItemId).eq(Assets::getOwnerId, eveAccount.getCharacterId()).list()
                .stream()
                .map(Assets::getItemId)
                .filter(itemId -> !itemIds.contains(itemId))
                .collect(Collectors.toList());
        removeBatchByIds(removeItemIds);

    }
}


