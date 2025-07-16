package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.esi.IndustryJobAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.IndustryApi;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.IndustryJobMapper;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.IndustryActivityEnum;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collection;

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

    private final IndustryJobAssembler industryJobAssembler;
    private final InvTypesService invTypesService;

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
     * @param isCor
     * @throws ParseException
     */
    public void batchInsertOrUpdateFromEsi(Integer cid, Boolean includeCompleted, Boolean isCor) throws ParseException {
        /**
         * 获取游戏人物信息及授权
         */
        EveAccount eveAccount = AuthorizeUtil.authorize(cid);
        String accessToken = esiApiService.getAccessToken(cid, eveAccount.getUserId());

        if (isCor != null && isCor) {
            /*
             * 获取公司生产线
             */
            Integer maxPage = industryApi.queryCorporationIndustryJobsMaxPage(eveAccount.getCorpId(), EsiClient.SERENITY, includeCompleted, accessToken);

            List<IndustryJob> industryJobs = Stream.iterate(1, i -> i + 1).limit(maxPage).map(i -> industryApi.queryCorporationIndustryJobs(eveAccount.getCorpId(), EsiClient.SERENITY, true, accessToken).collectList().block())
                    .sequential().filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(inJob -> industryJobAssembler.toIndustryJob(inJob, eveAccount.getCorpId()))
                    .collect(Collectors.toList());
            batchSaveAndSetBlueTypeName(industryJobs);
        } else {
            /*
             * 获取人物生产线
             */
            List<IndustryJob> industryJobs = Objects.requireNonNull(industryApi.queryCharacterIndustryJobs(eveAccount.getCharacterId(), EsiClient.SERENITY, includeCompleted, accessToken).collectList().block())
                    .stream()
                    .map(industryJobPlacedResponse -> industryJobAssembler.toIndustryJob(industryJobPlacedResponse, null))
                    .collect(Collectors.toList());
            batchSaveAndSetBlueTypeName(industryJobs);
        }
    }

    /**
     * 设置蓝图名称并保存
     * @param industryJobs 生产线对象列表
     */
    private void batchSaveAndSetBlueTypeName(List<IndustryJob> industryJobs) {
        Map<Integer, String> nameBlueprintByTypeIds = invTypesService.getNameByTypeIds(industryJobs.stream().map(IndustryJob::getBlueprintTypeId).collect(Collectors.toList()));
        Map<Integer, String> nameProductByTypeIds = invTypesService.getNameByTypeIds(industryJobs.stream().map(IndustryJob::getProductTypeId).collect(Collectors.toList()));
        industryJobs.forEach(industryJob -> {
            industryJob.setBlueprintType(nameBlueprintByTypeIds.get(industryJob.getBlueprintTypeId()));
            industryJob.setProductType(nameProductByTypeIds.get(industryJob.getProductTypeId()));
            industryJob.setActivity(IndustryActivityEnum.getValue(industryJob.getActivityId()));
        });
        if (!industryJobs.isEmpty()) {
            batchInsertOrUpdate(industryJobs);
        }
    }


}




