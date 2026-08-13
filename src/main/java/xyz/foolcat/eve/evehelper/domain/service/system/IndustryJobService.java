package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;
import xyz.foolcat.eve.evehelper.domain.repository.system.IndustryJobRepository;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.shared.kernel.enums.IndustryActivityEnum;
import xyz.foolcat.eve.evehelper.domain.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.domain.util.UserUtil;
import xyz.foolcat.eve.evehelper.shared.kernel.constants.GlobalConstants;

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
public class IndustryJobService  {

    private final EsiGateway esiApiService;

    private final InvTypesService invTypesService;

    private final AuthorizeUtil authorizeUtil;

    private final IndustryJobRepository industryJobRepository;

    public int updateBatch(List<IndustryJob> list) {
        return industryJobRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<IndustryJob> list) {
        return industryJobRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<IndustryJob> list) {
        return industryJobRepository.batchInsert(list);
    }

    public int batchInsertOrUpdate(List<IndustryJob> list) {
        return industryJobRepository.batchInsertOrUpdate(list);
    }

    public boolean insertOrUpdate(IndustryJob record) {
        return industryJobRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(IndustryJob record) {
        return industryJobRepository.insertOrUpdateSelective(record);
    }

    /**
     * 获取ESI人物或公司生产线信息
     * @param cid
     * @param isCor
     * @throws ParseException
     */
    public void batchInsertOrUpdateFromEsi(Integer cid, Boolean includeCompleted, Boolean isCor) throws ParseException {
        /*
         * 获取游戏人物信息及授权
         * 请求路径有 SecurityContext 用 authorize(校验归属);
         * 定时任务等无上下文路径用 authorizeInternal(显式系统身份),二者均 fail-closed。
         */
        EveAccount eveAccount;
        Integer currentUserId = UserUtil.getUserId();
        if (currentUserId != null && currentUserId > 0) {
            eveAccount = authorizeUtil.authorize(cid);
        } else {
            eveAccount = authorizeUtil.authorizeInternal(GlobalConstants.SYSTEM_USER_ID, cid);
        }
        String accessToken = esiApiService.getAccessToken(cid, eveAccount.getUserId());

        if (isCor != null && isCor) {
            /*
             * 获取公司生产线
             */
            Integer maxPage = esiApiService.queryCorporationIndustryJobsMaxPage(eveAccount.getCorpId(), includeCompleted, accessToken);

            List<IndustryJob> industryJobs = Stream.iterate(1, i -> i + 1).limit(maxPage).map(i -> esiApiService.queryCorporationIndustryJobs(eveAccount.getCorpId(), true, accessToken).collectList().block())
                    .sequential().filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            batchSaveAndSetBlueTypeName(industryJobs);
        } else {
            /*
             * 获取人物生产线
             */
            List<IndustryJob> industryJobs = Objects.requireNonNull(esiApiService.queryCharacterIndustryJobs(eveAccount.getCharacterId(), includeCompleted, accessToken).collectList().block())
                    .stream()
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


    public List<IndustryJob> selectByCorpIdAndStatus(Integer corpId, String statusDelivered) {

        return industryJobRepository.selectByCorpIdAndStatus(corpId, statusDelivered);
    }
}




