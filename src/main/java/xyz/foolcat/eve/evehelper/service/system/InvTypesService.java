package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.foolcat.eve.evehelper.domain.system.InvTypes;
import xyz.foolcat.eve.evehelper.mapper.system.InvTypesMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = RuntimeException.class)
public class InvTypesService extends ServiceImpl<InvTypesMapper, InvTypes> {


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
                .collect(Collectors.toMap(InvTypes::getTypeId, InvTypes::getTypeName));
    }
}

