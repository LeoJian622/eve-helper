package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.model.vo.BlueprintsDTO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintsPO;

import java.util.List;

/**
 * @author Leojan
 */
@Mapper
public interface BlueprintsMapper extends BaseMapper<BlueprintsPO> {

    /**
     * 分页查询蓝图清单
     *
     * @param page            分页参数
     * @param id              所有者ID
     * @param blueprintName   蓝图名称模糊匹配，null 表示不筛选
     * @param isBlueprintCopy 原图/拷贝筛选，null 表示不筛选
     * @param sortColumn      排序列名，来自领域枚举白名单，非用户输入
     * @param ascending       是否升序
     */
    IPage<BlueprintsDTO> selectBlueprintsInvtypeUniverse(IPage<BlueprintsDTO> page,
                                                         @Param("id") String id,
                                                         @Param("blueprintName") String blueprintName,
                                                         @Param("isBlueprintCopy") Boolean isBlueprintCopy,
                                                         @Param("sortColumn") String sortColumn,
                                                         @Param("ascending") boolean ascending);

    int updateBatch(List<BlueprintsPO> list);

    int updateBatchSelective(List<BlueprintsPO> list);

    int batchInsert(List<BlueprintsPO> list);

    int insertOrUpdateSelective(BlueprintsPO record);

}
