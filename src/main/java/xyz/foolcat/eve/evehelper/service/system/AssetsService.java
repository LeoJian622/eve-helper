package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dtflys.forest.http.ForestResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.mapper.system.AssetsMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class AssetsService extends ServiceImpl<AssetsMapper, Assets> {

    @Resource
    private EsiApiService esiApiService;

    public int batchInsert(List<Assets> list) {
        return baseMapper.batchInsert(list);
    }

    /**
     * 与ESI资产数据进行同步并返回
     *
     * @param type
     * @param id
     * @return
     * @throws ParseException
     */
    public void saveAndUpdateAssets(String type, String id) throws Throwable {

        ForkJoinPool forkJoinPool = new ForkJoinPool(4);

        SubmissionPublisher<Integer> submissionPublisher = new SubmissionPublisher<>(forkJoinPool, 4);
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
                ForestResponse<List<Assets>> forestResponse = null;
                forestResponse = esiApiService.getAssetsList(type, item, id);
                List<Assets> assetsList1 = forestResponse.getResult();
                Long ownId = Long.valueOf(id);
                assetsList1.forEach(assets -> {
                    assets.setOwnerId(ownId);
                });
                saveOrUpdateBatch(assetsList1);
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
            }catch (Exception e){
                log.info("订阅者取消订阅");
            }

        } while (submissionPublisher.isSubscribed(subscriber));

        submissionPublisher.close();

    }

    /**
     * 获取资产列表
     *
     * @param id
     * @return
     */
    public IPage<AssetsVO> getAssertsListById(IPage<AssetsVO> page, String id) {
        return baseMapper.selectAssetsInvtypeUniverse(page, id);
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
}


