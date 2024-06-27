package xyz.foolcat.eve.evehelper.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.foolcat.eve.evehelper.domain.system.Assets;
import xyz.foolcat.eve.evehelper.vo.AssetsVO;

import java.util.List;

@Mapper
public interface AssetsMapper extends BaseMapper<Assets> {
    int updateBatch(List<Assets> list);

    int updateBatchSelective(List<Assets> list);

    int batchInsert(@Param("list") List<Assets> list);

    int insertOrUpdate(Assets record);

    int insertOrUpdateSelective(Assets record);

    /**
     * 分页查询
     *
     * @param page
     * @param id
     * @return
     */
    IPage<AssetsVO> selectAssertsInvtypeUniverse(IPage<AssetsVO> page, @Param("id") String id);

    int batchInsertOrUpdate(@Param("list") List<Assets> list);
}