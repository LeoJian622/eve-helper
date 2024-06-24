package xyz.foolcat.eve.evehelper.service.system;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.converter.esi.StructureConverter;
import xyz.foolcat.eve.evehelper.domain.system.Structure;
import xyz.foolcat.eve.evehelper.esi.model.StructuresInformationResponse;
import xyz.foolcat.eve.evehelper.mapper.system.StructureMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StructureService extends ServiceImpl<StructureMapper, Structure> {

    private final StructureConverter structureConverter;

    public int updateBatch(List<Structure> list) {
        return baseMapper.updateBatch(list);
    }
    
    public int updateBatchSelective(List<Structure> list) {
        return baseMapper.updateBatchSelective(list);
    }
    
    public int batchInsert(List<Structure> list) {
        return baseMapper.batchInsert(list);
    }
    
    public int insertOrUpdate(Structure record) {
        return baseMapper.insertOrUpdate(record);
    }
    
    public int insertOrUpdateSelective(Structure record) {
        return baseMapper.insertOrUpdateSelective(record);
    }

    public int esiBatchInsert(List<StructuresInformationResponse> structuresInformationResponses){
        List<Structure> structures = structuresInformationResponses.stream()
                .map(structureConverter::structuresInformationResponse2Structure)
                .collect(Collectors.toList());
        return batchInsert(structures);
    }
}
