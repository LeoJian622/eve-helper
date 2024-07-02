package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.converter.esi.IndustryJobConverter;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.system.IndustryJob;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.mapper.system.IndustryJobMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Leojan
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = RuntimeException.class)
public class IndustryJobService extends ServiceImpl<IndustryJobMapper, IndustryJob> {

    private final EsiApiService esiApiService;

    private final EveAccountService eveAccountService;

    private final IndustryApi industryApi;

    private final IndustryJobConverter industryJobConverter;

    public int updateBatch(List<IndustryJob> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<IndustryJob> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<IndustryJob> list) {
        return baseMapper.batchInsert(list);
    }

    public int batchInsertOrUpdate(List<IndustryJob> list) {
        return baseMapper.batchInsertOrUpdate(list);
    }

    public int insertOrUpdate(IndustryJob record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(IndustryJob record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    /**
     * 获取ESI人物或公司生产线信息
     * @param cid
     * @param userId
     * @param isCor
     * @throws ParseException
     */
    public void batchInsertOrUpdateFromEsi(Integer cid, Integer userId, Boolean includeCompleted, Boolean isCor) throws ParseException {
        /**
         * 获取游戏人物信息及授权
         */
        String accessToken = esiApiService.getAccessToken(cid, userId);
        EveAccount eveAccount = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, cid).or().eq(EveAccount::getCorpId,cid).one();

        if (isCor != null && isCor) {
            /*
             * 获取公司生产线
             */
            Integer max = industryApi.queryCorporationIndustryJobsMaxPage(eveAccount.getCorpId(), EsiClient.SERENITY, includeCompleted, accessToken);

            List<IndustryJob> industryJobs = Stream.iterate(1, i -> i + 1).limit(max).map(i -> industryApi.queryCorporationIndustryJobs(eveAccount.getCorpId(), EsiClient.SERENITY, true, accessToken).collectList().block())
                    .sequential().filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(inJob -> industryJobConverter.toIndustryJob(inJob, eveAccount.getCorpId()))
                    .collect(Collectors.toList());
            if (!industryJobs.isEmpty()) {
                batchInsertOrUpdate(industryJobs);
            }
        } else {
            /*
             * 获取人物生产线
             */

            List<IndustryJob> industryJobs = Objects.requireNonNull(industryApi.queryCharacterIndustryJobs(eveAccount.getCharacterId(), EsiClient.SERENITY, includeCompleted, accessToken).collectList().block())
                    .stream()
                    .map(industryJobPlacedResponse -> industryJobConverter.toIndustryJob(industryJobPlacedResponse, null))
                    .collect(Collectors.toList());
            if (!industryJobs.isEmpty()) {
                batchInsertOrUpdate(industryJobs);
            }
        }

    }
}




