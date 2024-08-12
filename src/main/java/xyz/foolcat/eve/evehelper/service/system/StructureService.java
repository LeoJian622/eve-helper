package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.converter.esi.StructureConverter;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.mapper.system.StructureMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;
import xyz.foolcat.eve.evehelper.util.AuthorizeUtil;
import xyz.foolcat.eve.evehelper.util.UserUtil;

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
@RequiredArgsConstructor
public class StructureService extends ServiceImpl<StructureMapper, Structure> {

    private final StructureConverter structureConverter;

    private final EsiApiService esiApiService;

    private final CorporationApi corporationApi;

    public int updateBatch(List<Structure> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<Structure> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<Structure> list) {
        return baseMapper.batchInsert(list);
    }

    public int batchInsertOrUpdate(List<Structure> list) {
        return baseMapper.batchInsertOrUpdate(list);
    }

    public int insertOrUpdate(Structure record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(Structure record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    /**
     * ESI获取的建筑列表批量获取数据
     *
     * @param cId 角色ID
     */
    @Transactional
    public void batchInsertOrUpdateFromEsi(Integer cId) throws ParseException {

        /*
          获取游戏人物信息及授权
         */
        EveAccount eveAccount = AuthorizeUtil.authorize(cId);
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
                .map(structureConverter::toStructure)
                .collect(Collectors.toList());
        batchInsertOrUpdate(structures);

        /*
         * 移除不在ESI列表的建筑
         */
        Set<Long> newStructureIds = structures.stream().map(Structure::getStructureId).collect(Collectors.toSet());
        List<Long> structureIds = lambdaQuery().select(Structure::getStructureId).eq(Structure::getCorporationId, eveAccount.getCorpId()).list()
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
        return baseMapper.selectFuelExpiresList(hour, corporationId);
    }
}
