package xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.AssetsPO;

import java.util.List;

@Mapper
public interface AssetsMapper extends BaseMapper<AssetsPO> {
    int updateBatch(List<AssetsPO> list);

    int updateBatchSelective(List<AssetsPO> list);

    int batchInsert(List<AssetsPO> list);

    int insertOrUpdateSelective(AssetsPO record);

    List<AssetsPO> selectAssertsInvtypeUniverse(@Param("page") IPage<AssetsPO> page, @Param("id") String id);

    int batchInsertOrUpdate(List<AssetsPO> list);

    void removeByItemId(List<Long> itemIds);

    List<AssetsPO> findByOwnerId(Integer characterId);
}
