package xyz.foolcat.eve.evehelper.domain.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.InvTypes;
import xyz.foolcat.eve.evehelper.domain.port.esi.EsiGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.InvTypesRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Leojan
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@RequiredArgsConstructor
public class InvTypesService {

    private final EsiGateway esiApiService;

    private final InvTypesRepository invTypesRepository;

    public int updateBatch(List<InvTypes> list) {
        return invTypesRepository.updateBatch(list);
    }

    public int updateBatchSelective(List<InvTypes> list) {
        return invTypesRepository.updateBatchSelective(list);
    }

    public int batchInsert(List<InvTypes> list) {
        return invTypesRepository.batchInsert(list);
    }

    public boolean insertOrUpdate(InvTypes record) {
        return invTypesRepository.insertOrUpdate(record);
    }

    public int insertOrUpdateSelective(InvTypes record) {
        return invTypesRepository.insertOrUpdateSelective(record);
    }

    /**
     * 根据typeId获取物品名称
     * @param typeIds 物品类型ID
     * @return 物品ID和名称的key-value对象
     */
    public Map<Integer,String> getNameByTypeIds(List<Integer> typeIds) {
        return invTypesRepository.selectTypeNameByIds(typeIds)
                .stream()
                .collect(Collectors.toMap(InvTypes::getTypeId, InvTypes::getName));
    }

    /**
     * 根据typeId获取物品属性并更新invType
     *
     * @param typeId 物品ID
     * @return 更新成功返回物品信息;写库未生效时返回 {@code null},由调用方据此提示失败
     */
    public InvTypes updateTypeByTypeId(Integer typeId){
        InvTypes invTypes = esiApiService.queryUniverseType(typeId, "zh").block();
        boolean update = this.insertOrUpdate(invTypes);
        if (update) {
            return invTypes;
        }else {
            return null;
        }
    }

    public InvTypes selectOneByName(String name) {
        return invTypesRepository.selectOneByName(name);
    }

    public InvTypes selectOneById(int id) {
        return invTypesRepository.selectOneById(id);
    }
}

