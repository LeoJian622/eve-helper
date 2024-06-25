package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.converter.esi.StructureConverter;
import xyz.foolcat.eve.evehelper.domain.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.esi.EsiClient;
import xyz.foolcat.eve.evehelper.esi.api.CorporationApi;
import xyz.foolcat.eve.evehelper.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.mapper.system.StructureMapper;
import xyz.foolcat.eve.evehelper.service.esi.EsiApiService;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
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

    private final EveAccountService eveAccountService;

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
     * @param characterId 角色ID
     */
    public void esiBatchInsert(Integer characterId) throws ParseException {
        List<EveAccount> accounts = eveAccountService.lambdaQuery().eq(EveAccount::getCharacterId, characterId).list();
        if (!accounts.isEmpty()) {
            EveAccount eveAccount = accounts.get(0);
            String accessToken = esiApiService.getAccessToken(eveAccount.getCharacterId().toString());
            Integer max = corporationApi.queryCorporationStructuresMaxPage(eveAccount.getCorpId(), EsiClient.SERENITY, accessToken);
            List<StructuresInformationResponse> structuresInformationResponses = Stream.iterate(1, i -> i + 1).limit(max)
                    .map(i -> corporationApi.queryCorporationStructures(eveAccount.getCorpId(), EsiClient.SERENITY, "zh", i, accessToken)
                            .collectList().block())
                    .sequential()
                    .collect(Collectors.toList())
                    .stream().flatMap(Collection::stream)
                    .collect(Collectors.toList());
            List<Structure> structures = structuresInformationResponses.stream()
                    .map(structureConverter::structuresInformationResponse2Structure)
                    .collect(Collectors.toList());
            batchInsertOrUpdate(structures);
        }
    }

    /**
     * 查询X小时后燃料耗尽的建筑清单
     * @param hour 时长
     * @return 建筑列表
     */
    public List<Structure> selectFuelExpiresList(Integer hour,Integer corporationId) {
        return baseMapper.selectFuelExpiresList(hour,corporationId);
    }
}
