package xyz.foolcat.eve.evehelper.domain.service.system;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.system.StructureAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Structure;
import xyz.foolcat.eve.evehelper.domain.repository.system.StructureRepository;
import xyz.foolcat.eve.evehelper.domain.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.shared.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.shared.util.UserUtil;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class StructureService {
    
    private static final Logger logger = LoggerFactory.getLogger(StructureService.class);

    private final StructureAssembler structureAssembler;

    private final EsiApiService esiApiService;

    private final CorporationApi corporationApi;

    private final AuthorizeUtil authorizeUtil;

    private final StructureRepository structureRepository;

    public int updateBatch(List<Structure> list) {
        return structureRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<Structure> list) {
        if (list == null) {
            logger.warn("updateBatchSelective called with null list");
            return 0;
        }
        
        if (list.isEmpty()) {
            logger.debug("updateBatchSelective called with empty list");
            return 0;
        }
        
        try {
            logger.debug("Updating {} structures selectively", list.size());
            int result = structureRepository.updateBatchSelective(list);
            logger.debug("Successfully updated {} structures selectively", result);
            return result;
        } catch (Exception e) {
            logger.error("Error updating structures selectively", e);
            throw new RuntimeException("Error updating structures selectively", e);
        }
    }

    public int batchInsert(List<Structure> list) {
        return structureRepository.batchInsert(list);
    }

    public int batchInsertOrUpdate(List<Structure> list) {
        return structureRepository.batchInsertOrUpdate(list);
    }

    public int insertOrUpdate(Structure record) {
        return structureRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Structure record) {
        return structureRepository.insertOrUpdateSelective(record);
    }


    public void removeBatchByIds(List<Long> ids){
        structureRepository.removeBatchByIds(ids);
    }

    /**
     * ESI获取的建筑列表批量获取数据
     *
     * @param cId 角色ID
     */
    public void batchInsertOrUpdateFromEsi(Integer cId) throws ParseException {

        /*
          获取游戏人物信息及授权
         */
        EveAccount eveAccount = authorizeUtil.authorize(cId);
        String accessToken = esiApiService.getAccessToken(cId, eveAccount.getUserId());

        /*
          获取总页数
         */
        Integer maxPage = corporationApi.queryCorporationStructuresMaxPage(eveAccount.getCorpId(), EsiClient.SERENITY, accessToken);

        /*
          从ESI获取建筑列表
         */
        List<Structure> structures = Stream.iterate(1, i -> i + 1).limit(maxPage)
                .map(i -> corporationApi.queryCorporationStructures(eveAccount.getCorpId(), EsiClient.SERENITY, "zh", i, accessToken)
                        .collectList().block())
                .sequential().filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(structureAssembler::toStructure)
                .collect(Collectors.toList());
        batchInsertOrUpdate(structures);

        /*
         * 移除不在ESI列表的建筑
         */
        Set<Long> newStructureIds = structures.stream().map(Structure::getStructureId).collect(Collectors.toSet());
        List<Long> structureIds = structureRepository.selectByCorporationId(eveAccount.getCorpId())
                .stream()
                .map(Structure::getStructureId)
                .filter(id -> !newStructureIds.contains(id))
                .collect(Collectors.toList());
        removeBatchByIds(structureIds);
    }

    /**
     * 查询X小时后燃料耗尽的建筑清单
     *
     * @param hour 时长
     * @return 建筑列表
     */
    public List<Structure> selectFuelExpiresList(Integer hour, Integer corporationId) {
        Integer userId = UserUtil.getUserId();
        return structureRepository.selectFuelExpiresList(hour, corporationId);
    }

    public Structure selectById(Long structureId) {
        return structureRepository.selectByStructureId(structureId);
    }
}
