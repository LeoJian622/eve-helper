package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
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
     * 与ESI资产数据进行同步并返回
     * 新方法查看{@link xyz.foolcat.eve.evehelper.service.system.AssetsService#saveAndUpdateAsserts(Integer,Integer)}
     *
     * @param type
     * @param cid
     * @return
     */
    @Deprecated
    public void saveAndUpdateAsserts(String type, String cid) {

        lambdaUpdate().eq(Assets::getOwnerId, cid).remove();

        List<Assets> assetsList = new ArrayList<>();

        SubmissionPublisher<Integer> submissionPublisher = new SubmissionPublisher<>(new ForkJoinPool(4), 4);
        Flow.Subscriber<Integer> subscriber = new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                log.info("向数据发布者请求一个数据");
                this.subscription.request(1);
            }

            @SneakyThrows
            @Override
            public void onNext(Integer item) {
                log.info("处理页数：" + item);
                //请求数据
                List<Assets> assetsPageList = esiApiService.getAssetsList(type, item, cid);
                Long ownId = Long.valueOf(cid);
                assetsPageList.forEach(assets -> {
                    assets.setOwnerId(ownId);
                });
                assetsList.addAll(assetsPageList);
                log.info("进行下一条处理");
                this.subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("取消订阅");
                this.subscription.cancel();
            }

            @Override
            public void onComplete() {
            }
        };

        submissionPublisher.subscribe(subscriber);

        int i = 1;
        do {
            try {
                log.info("发送消息" + i);
                submissionPublisher.submit(i++);
            } catch (Exception e) {
                log.info("订阅者取消订阅");
            }

        } while (submissionPublisher.isSubscribed(subscriber));

        submissionPublisher.close();

        saveBatch(assetsList);

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
    public void saveAndUpdateAsserts(Integer cid, Integer userId) throws ParseException {

        /*
         * 获取游戏人物信息及授权
         */
        EveAccount eveAccount = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, cid).or().eq(EveAccount::getCorpId,cid).one();
        String accessToken = esiApiService.getAccessToken(String.valueOf(eveAccount.getCharacterId()), userId);

        /*
         * 获取总页数
         */
        Integer max = assetsApi.queryCharactersAssetsMaxPage(eveAccount.getCharacterId(), EsiClient.SERENITY, accessToken);

        /*
         * 从ESI获取资产列表
         */
        List<Assets> assets = Stream.iterate(1, i -> i + 1).limit(max)
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


