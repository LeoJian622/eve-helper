package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.WalletJournal;
import xyz.foolcat.eve.evehelper.mapper.system.WalletJournalMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;

@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class WalletJournalService extends ServiceImpl<WalletJournalMapper, WalletJournal> {

    @Resource
    private EsiApiService esiApiService;

    public int batchInsert(List<WalletJournal> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(WalletJournal record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(WalletJournal record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    /**
     * 与ESI钱包交易数据进行同步并返回
     *
     * @param type
     * @param cid
     * @return
     */
    public void saveAndUpdateWalletJournal(String type, String cid) {

        List<WalletJournal> walletJournals = new ArrayList<>();


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
                List<WalletJournal> walletJournalPageList = esiApiService.getWalletJournalList(type, item, cid);
                Long ownId = Long.valueOf(cid);
                walletJournalPageList.forEach(assets -> {
                    assets.setOwnerId(ownId);
                });
                walletJournals.addAll(walletJournalPageList);
                log.info("进行下一条处理");
                this.subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                log.info("取消订阅");
                log.error("错误信息：" + throwable.getMessage());
                this.subscription.cancel();
            }

            @Override
            public void onComplete() {
            }
        };

        submissionPublisher.subscribe(subscriber);

        int i = 1;
        do {
            submissionPublisher.submit(i++);
        } while (submissionPublisher.isSubscribed(subscriber));

        submissionPublisher.close();

        saveBatch(walletJournals);
    }
}

