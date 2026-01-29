package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import xyz.foolcat.eve.evehelper.application.dto.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface BlueprintsMapper extends BaseMapper<BlueprintsPO> {
    IPage<BlueprintsDTO> selectBlueprintsInvtypeUniverse(IPage<BlueprintsDTO> page, String id, String sortField, String sortOrder);

    int updateBatch(List<BlueprintsPO> list);

    int updateBatchSelective(List<BlueprintsPO> list);

    int batchInsert(List<BlueprintsPO> list);

    int insertOrUpdate(BlueprintsPO record);

    int insertOrUpdateSelective(BlueprintsPO record);

    // 只保留基础 CRUD
}
