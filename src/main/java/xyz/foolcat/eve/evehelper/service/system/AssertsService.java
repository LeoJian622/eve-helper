package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.Asserts;
import xyz.foolcat.eve.evehelper.mapper.system.AssetsMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;

/**
 * @author Leojan
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class AssertsService extends ServiceImpl<AssetsMapper, Asserts> {

    private final EsiApiService esiApiService;

    public int batchInsert(List<Asserts> list) {
        return baseMapper.batchInsert(list);
    }

    /**
     * 与ESI资产数据进行同步并返回
     *
     * @param type
     * @param cid
     * @return
     */
    public void saveAndUpdateAsserts(String type, String cid) {

        lambdaUpdate().eq(Asserts::getOwnerId,cid).remove();

        List<Asserts> assertsList = new ArrayList<>();

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
                List<Asserts> assertsPageList = esiApiService.getAssetsList(type, item, cid);
                Long ownId = Long.valueOf(cid);
                assertsPageList.forEach(assets -> {
                    assets.setOwnerId(ownId);
                });
                assertsList.addAll(assertsPageList);
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

        saveBatch(assertsList);

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

    public int updateBatch(List<Asserts> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<Asserts> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int insertOrUpdate(Asserts record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Asserts record) {
        return baseMapper.insertOrUpdateSelective(record);
    }
}


