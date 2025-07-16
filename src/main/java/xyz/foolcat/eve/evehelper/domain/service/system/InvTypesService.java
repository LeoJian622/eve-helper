package xyz.foolcat.eve.evehelper.domain.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.application.assembler.esi.InvTypesAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.EsiClient;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.api.UniverseApi;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.TypeInfoResponse;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.InvTypesMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class InvTypesService extends ServiceImpl<InvTypesMapper, InvTypes> {

    private final UniverseApi universeApi;

    private final InvTypesAssembler invTypesAssembler;

    public int updateBatch(List<InvTypes> list) {
        return baseMapper.updateBatch(list);
    }

    public int updateBatchSelective(List<InvTypes> list) {
        return baseMapper.updateBatchSelective(list);
    }

    public int batchInsert(List<InvTypes> list) {
        return baseMapper.batchInsert(list);
    }

    public int insertOrUpdate(InvTypes record) {
        return baseMapper.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(InvTypes record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    /**
     * 根据typeId获取物品名称
     * @param typeIds 物品类型ID
     * @return
     */
    public Map<Integer,String> getNameByTypeIds(List<Integer> typeIds) {
        return lambdaQuery().in(InvTypes::getTypeId, typeIds).list()
                .stream()
                .collect(Collectors.toMap(InvTypes::getTypeId, InvTypes::getName));
    }

    /**
     * 根据typeId获取物品属性并更新invType
     * @param typeId
     * @return
     */
    public InvTypes updateTypeByTypeId(Integer typeId){
        TypeInfoResponse typeInfoResponse = universeApi.queryUniverseType(typeId, EsiClient.SERENITY, EsiClient.ZH_CN).block();
        InvTypes invTypes = invTypesAssembler.toInvTypes(typeInfoResponse);
        int update = this.insertOrUpdate(invTypes);
        if (update != 0) {
            return invTypes;
        }else {
            return null;
        }
    }
}

